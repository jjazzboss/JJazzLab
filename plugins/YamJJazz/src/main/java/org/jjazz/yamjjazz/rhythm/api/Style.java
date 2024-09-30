/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.yamjjazz.rhythm.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.parser.MidiParser;
import org.jjazz.rhythm.api.Division;
import org.jjazz.yamjjazz.CASMDataReader;
import org.jjazz.yamjjazz.FormatNotSupportedException;
import org.jjazz.yamjjazz.MPL_ExtensionFile;
import org.jjazz.yamjjazz.MPL_MiscData;
import org.jjazz.yamjjazz.MPL_MusicData;
import org.jjazz.yamjjazz.MPL_SInt;

/**
 * Store all the data of a Yamaha style.
 * <p>
 * The feel parameter is estimated programatically from the analysis of the notes positions.
 * <p>
 * Limitations:<br>
 * - ignore SFF2 files <br>
 * - in the Midi music data ignore the following data: sysex, program changes, pitch bends, controller changes<br>
 * - Program changes are loaded from the SInt section. <br>
 */
public class Style
{

    public String name;
    public TimeSignature timeSignature;
    public int ticksPerQuarter;
    public int tempo;
    public Division division;     // Not part of the style file: computed by our parser

    public enum SFFtype
    {
        SFF1, SFF2
    };
    public SFFtype sffType;
    private final HashMap<StylePartType, StylePart> mapTypeStylePart = new HashMap<>();
    private final SInt sInt = new SInt();
    private final HashSet<AccType> returnedAccTypes = new HashSet<>();         // Internal state data, set and used by getAccType(channel)

    private static final Logger LOGGER = Logger.getLogger(Style.class.getSimpleName());

    /**
     * Create a StylePart of specified type and add it to this object.
     * <p>
     * If StylePart already exists, just return it.<br>
     *
     * @param type
     * @return The created StylePart.
     */
    public StylePart addStylePart(StylePartType type)
    {
        StylePart sp = mapTypeStylePart.get(type);
        if (sp == null)
        {
            sp = new StylePart(type);
            mapTypeStylePart.put(sp.getType(), sp);
        }
        return sp;
    }

    public StylePart getStylePart(StylePartType spType)
    {
        return mapTypeStylePart.get(spType);
    }

    public int getStylePartSizeInBars(StylePartType spType)
    {
        StylePart sp = getStylePart(spType);
        int nbBars = Math.round(sp.getSizeInBeats() / timeSignature.getNbNaturalBeats());
        return nbBars;
    }

    /**
     * The list used StylePartTypes sorted according to natural ordering.
     *
     * @return
     */
    public List<StylePartType> getStylePartTypes()
    {
        ArrayList<StylePartType> result = new ArrayList<>(mapTypeStylePart.keySet());
        Collections.sort(result);
        return result;
    }

    public SInt getSInt()
    {
        return sInt;
    }

    /**
     * @return All the different AccTypes used in this style, in the AccType natural order.
     */
    public List<AccType> getAllAccTypes()
    {
        HashSet<AccType> set = new HashSet<>();
        for (StylePartType type : mapTypeStylePart.keySet())
        {
            set.addAll(mapTypeStylePart.get(type).getAccTypes());
        }
        ArrayList<AccType> res = new ArrayList<>(set);
        Collections.sort(res);
        return res;
    }

    /**
     * Get the AccType for a specified channel (which can be a "secondary" source channel or the "main" one).
     * <p>
     * Search the first StylePart which uses this channel and return the associated AccType. If one channel is used by two
     * AccTypes (it can happen, in 2 different StyleParts), try to return each of them successively upon each method call
     * (otherwise problem will happen).
     *
     * @return Null if this channel is not used in this Style.
     */
    public AccType getAccType(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   //NOI18N
        }
        ArrayList<AccType> results = new ArrayList<>();
        for (StylePartType type : mapTypeStylePart.keySet())
        {
            // Find the StyleParts which use this channel
            StylePart sp = mapTypeStylePart.get(type);
            AccType at = sp.getAccType(channel);
            if (at != null)
            {
                results.add(at);        // and save the corresponding AccType
            }
        }
        // Analyze results
        AccType res = null;
        if (results.size() == 1)
        {
            // Easy
            res = results.get(0);
        } else if (results.size() > 1)
        {
            // Choose the first one which is new
            for (AccType at : results)
            {
                if (!returnedAccTypes.contains(at))
                {
                    res = at;
                }
            }
            if (res == null)
            {
                // Not sure this case can really happen, choose one just for safety
                res = results.get(0);
            }
        }
        if (res != null)
        {
            returnedAccTypes.add(res);
        }
        return res;
    }

    /**
     * Read only the non MusicData (Midi notes) of a standard Yamaha style file : CASM (StyleParts creation), SInt, tempo,
     * signature, name.
     *
     * @param stdFile A standard Yamaha style file
     * @throws java.io.IOException
     * @throws org.jjazz.yamjjazz.FormatNotSupportedException
     * @throws java.io.FileNotFoundException
     * @throws javax.sound.midi.InvalidMidiDataException
     */
    public void readNonMusicData(File stdFile) throws IOException, FormatNotSupportedException, FileNotFoundException, InvalidMidiDataException
    {
        // CASM
        CASMDataReader cdr = new CASMDataReader(this, stdFile.getName());
        cdr.read(stdFile);

        // SInt, TimeSignature, name, tempo etc.
        MidiParser midiParser = new MidiParser();
        MPL_SInt sIntMPL = new MPL_SInt(this, stdFile.getName());
        MPL_MiscData miscMPL = new MPL_MiscData(this);
        midiParser.addParserListener(miscMPL);
        midiParser.addParserListener(sIntMPL);
        try (FileInputStream in = new FileInputStream(stdFile))
        {
            // Parse the midi file which will feed our listeners
            midiParser.parse(MidiSystem.getSequence(in), stdFile.getName());
        }

        // Check for errors
        if (timeSignature == null)
        {
            throw new FormatNotSupportedException("Invalid time signature");
        }
    }

    /**
     * Read only the MusicData (Midi notes) which will fill/create all the SourcePhrase objects in the StyleParts.
     * <p>
     * This method must be called AFTER readNonMusicData() has been called, so that the StyleParts are already created.
     *
     * @param stdFile A standard Yamaha style file
     * @throws IOException
     * @throws FileNotFoundException
     * @throws InvalidMidiDataException
     */
    public void readMusicData(File stdFile) throws IOException, FileNotFoundException, InvalidMidiDataException
    {
        MidiParser midiParser = new MidiParser();
        MPL_MusicData musicMPL = new MPL_MusicData(this, stdFile.getName());
        midiParser.addParserListener(musicMPL);
        try (FileInputStream in = new FileInputStream(stdFile))
        {
            // Parse the midi file which will feed our listeners
            midiParser.parse(MidiSystem.getSequence(in), stdFile.getName());
        }
        if (musicMPL.IS_BUGGED)
        {
            // Data is corrupted
            throw new InvalidMidiDataException("Invalid Midi data");
        }

        checkConsistency();
    }

    /**
     * Get the non music data by merging data from an extension style file and a base style file.
     * <p>
     *
     * @param extFile A YamJJazz extension file
     * @param stdFile A standard Yamaha style file
     * @throws java.io.IOException
     * @throws org.jjazz.yamjjazz.FormatNotSupportedException
     * @throws java.io.FileNotFoundException
     * @throws javax.sound.midi.InvalidMidiDataException
     */
    public void readNonMusicData(File extFile, File stdFile) throws IOException, FormatNotSupportedException, FileNotFoundException, InvalidMidiDataException
    {
        if (extFile == null || stdFile == null)
        {
            throw new NullPointerException("extFile=" + extFile + " stdFile=" + stdFile);   //NOI18N
        }
        LOGGER.log(Level.FINE, "readNonMusicData() -- extStyleFile={0} baseStyleFile={1}", new Object[]
        {
            extFile, stdFile
        });

        // Get the styleParts created, the instruments, tempo, song name, etc.
        readNonMusicData(stdFile);

        // Get the used styleParts size and complexity levels from the extension file, but skip music data
        MPL_ExtensionFile midiPhrasesMPL = new MPL_ExtensionFile(this, true, extFile.getName());
        MidiParser midiParser = new MidiParser();
        midiParser.addParserListener(midiPhrasesMPL);
        try (FileInputStream in = new FileInputStream(extFile))
        {
            midiParser.parse(MidiSystem.getSequence(in), extFile.getName());
        }
        if (midiPhrasesMPL.IS_BUGGED)
        {
            // Data is corrupted
            throw new InvalidMidiDataException("Inconsistent data in file " + extFile.getAbsolutePath());
        }
    }

    /**
     * Get the music data by merging music data from an extension style file and a base style file.
     * <p>
     * This method must be called AFTER readNonMusicData() has been called, so that the StyleParts are already created.
     *
     * @param extFile A YamJJazz extension file
     * @param stdFile A standard Yamaha style file
     * @throws IOException
     * @throws FileNotFoundException
     * @throws InvalidMidiDataException
     * @throws org.jjazz.yamjjazz.FormatNotSupportedException
     */
    public void readMusicData(File extFile, File stdFile) throws IOException, FileNotFoundException, InvalidMidiDataException, FormatNotSupportedException
    {
        LOGGER.log(Level.FINE, "readMusicData() -- extStyleFile={0} baseStyleFile={1}", new Object[]
        {
            extFile, stdFile
        });

        // Read all music data from the base file
        // Note that some SourcePhraseSets might be overridden by music from the extension file
        readMusicData(stdFile);

        checkConsistency();

        // Add music data from the extension style file             
        MPL_ExtensionFile midiPhrasesMPL = new MPL_ExtensionFile(this, false, extFile.getName());
        MidiParser midiParser = new MidiParser();
        midiParser.addParserListener(midiPhrasesMPL);
        try (FileInputStream in = new FileInputStream(extFile))
        {
            // Parse the midi file which will feed our listeners
            midiParser.parse(MidiSystem.getSequence(in), extFile.getName());
        }
        if (midiPhrasesMPL.IS_BUGGED)
        {
            // Data is corrupted
            throw new InvalidMidiDataException("Inconsistent data in file " + extFile.getAbsolutePath());
        }
    }

    public void dump(boolean showMusicData, boolean showSInt, boolean showCASM)
    {
        LOGGER.info("\n\n\n===================STYLE DUMP ==========================");
        LOGGER.log(Level.INFO, "name={0}", name);
        LOGGER.log(Level.INFO, "timeSignature={0}", timeSignature);
        LOGGER.log(Level.INFO, "ticksPerQuarter={0}", ticksPerQuarter);
        LOGGER.log(Level.INFO, "tempo={0}", tempo);
        LOGGER.log(Level.INFO, "styleType={0}", sffType);
        LOGGER.log(Level.INFO, "AccTypes={0}", getAllAccTypes());
        LOGGER.log(Level.INFO, "Style parts={0}", getStylePartTypes());
        LOGGER.info("\n\n\n====== STYLE PARTS");
        for (StylePartType type : getStylePartTypes())
        {
            StylePart sp = getStylePart(type);
            LOGGER.log(Level.INFO, "\n== {0}", type);
            sp.dump(showMusicData, showCASM);
        }
        LOGGER.info("\n\n\n====== META DATA SInt");
        if (showSInt)
        {
            LOGGER.info(sInt.toString());
        }
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================
    /**
     * Check consistency of style after reading all data from file.
     */
    private void checkConsistency() throws InvalidMidiDataException
    {
        // StylePart is defined in CASM but there is no notes. Ex: JazzGtrTrio184 9K.s460.sty
        for (StylePart spt : mapTypeStylePart.values())
        {
            if (spt.getSizeInBeats() == 0)
            {
                String msg = "StylePart " + spt + " defined in CASM but is empty (no notes)";
                LOGGER.log(Level.SEVERE, "checkConsistency() {0} - {1}", new Object[]{name, msg});
                throw new InvalidMidiDataException(msg);
            }
        }

    }

}

/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.outputsynth.api;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.SynthUtilities;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.util.api.Utilities;

/**
 * One or more MidiSynth instances, typically obtained from a .ins definition file.
 * <p>
 * This an immutable class.
 */
public class MidiSynthList
{

    private final List<MidiSynth> midiSynths = new ArrayList<>();
    private File file;
    private static final Logger LOGGER = Logger.getLogger(MidiSynthList.class.getSimpleName());

    /**
     * Construct a default MidiSynthList with only the GMSynth instance.
     */
    public MidiSynthList()
    {
        this(Arrays.asList(GMSynth.getInstance()));
    }

    /**
     * Construct a MidiSynthList from the specified MidiSynths.
     * <p>
     * @param synths Can't be empty
     */
    public MidiSynthList(List<MidiSynth> synths)
    {
        Preconditions.checkArgument(synths != null && !synths.isEmpty());
        this.midiSynths.addAll(synths);
    }

    /**
     * Construct a MidiSynthList from the MidiSynths found in the specified instrument definition file (.ins).
     * .<p>
     * @param file
     * @throws java.io.IOException If file access, or if no valid (non-empty) MidiSynth found in the file.
     */
    public MidiSynthList(File file) throws IOException
    {
        var reader = MidiSynthFileReader.getReader(Utilities.getExtension(file.getName()));
        if (reader == null)
        {
            throw new IOException("No MidiSynthFileReader instance found to read file=" + file.getAbsolutePath());
        }

        // Read the file and add the non-empty synths
        FileInputStream fis = new FileInputStream(file);
        var synths = reader.readSynthsFromStream(fis, file);    // Can raise exception
        synths.stream()
                .filter(s -> s.getNbInstruments() > 0)
                .forEach(s -> midiSynths.add(s));

        if (midiSynths.isEmpty())
        {
            throw new IOException("No valid (non-empty) MidiSynth found in file=" + file.getAbsolutePath());
        }

        this.file = file;
    }

    /**
     * Get the name of this MidiSynthList.
     * <p>
     * If loaded from a file, return the file name. Otherwise return the first MidiSynth's name.
     *
     * @return
     */
    public String getName()
    {
        return (file != null) ? file.getName() : midiSynths.get(0).getName();
    }


    /**
     * Get the associated MidiSynth definition file (.ins).
     *
     * @return Null if this instance was not created using an instrument definition file.
     * @see #MidiSynthList(java.io.File)
     */
    public File getFile()
    {
        return file;
    }


    /**
     * Get the MidiSynths.
     *
     * @return Can be an empty list.
     */
    public List<MidiSynth> getMidiSynths()
    {
        return new ArrayList<>(midiSynths);
    }

    /**
     * Check whether this MidiSynthList contains this instrument.
     *
     * @param ins
     * @return
     */
    public boolean contains(Instrument ins)
    {
        InstrumentBank<?> bank = ins.getBank();
        if (bank != null)
        {
            MidiSynth synth = bank.getMidiSynth();
            if (synth != null)
            {
                return midiSynths.contains(synth);
            }
        }
        return false;
    }


    /**
     * Return the first drums instrument found.
     *
     * @return The VoidInstrument if no drums instrument found.
     */
    public Instrument getDrumsInstrumentSample()
    {
        Instrument ins = GMSynth.getInstance().getVoidInstrument();
        for (MidiSynth synth : midiSynths)
        {
            List<Instrument> drumsInstruments = synth.getDrumsInstruments();
            if (!drumsInstruments.isEmpty())
            {
                ins = drumsInstruments.get(0);
                break;
            }
        }
        return ins;
    }

   

    @Override
    public String toString()
    {
        return "MidiSynthList-" + getName();
    }

    /**
     * Save this MidiSynthList as a string so that it can be retrieved by loadFromString().
     * <p>
     * Use the file name if this MidiSynthList came from a file. Otherwise use the list of MidiSynth names, which should be
     * standard synths (eg GMSynth, XGSynth, etc.).
     *
     * @return
     * @see loadFromString(String)
     */
    public String saveAsString()
    {
        if (file != null)
        {
            return "FILE=" + file.getAbsolutePath();
        }

        StringJoiner joiner = new StringJoiner(";");
        midiSynths.forEach(ms -> joiner.add(ms.getName()));
        return joiner.toString();
    }

    /**
     * Get the MidiSynthList corresponding to the string produced by saveAsString().
     * <p>
     *
     * @param s
     * @return
     * @throws java.io.IOException If the MidiSynthList could not be retrieved from the string
     * @see saveAsString()
     *
     */
    static public MidiSynthList loadFromString(String s) throws IOException
    {
        Preconditions.checkNotNull(s);
        s = s.trim();

        if (s.startsWith("FILE="))
        {
            if (s.length() <= 5)
            {
                throw new IOException("Invalid string=" + s);
            }
            File f = new File(s.substring(5));
            return new MidiSynthList(f);
        }


        // This is a list of standard MidiSynth
        var strs = s.split(";");
        if (strs.length == 0)
        {
            throw new IOException("Invalid string=" + s);
        }

        List<MidiSynth> synths = new ArrayList<>();
        for (String synthName : strs)
        {
            MidiSynth synth = SynthUtilities.getStandardSynth(synthName);
            if (synth == null)
            {
                LOGGER.warning("loadFromString() non-standard synth name found=" + synthName + ", ignored. s=" + s);
            } else
            {
                synths.add(synth);
            }
        }

        if (synths.isEmpty())
        {
            throw new IOException("No valid standard MidiSynths found in string=" + s);
        }

        return new MidiSynthList(synths);
    }

    // ==============================================================================================
    // Private methods
    // ==============================================================================================    
}

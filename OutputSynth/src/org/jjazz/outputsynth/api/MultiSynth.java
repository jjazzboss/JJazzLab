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
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;

/**
 * A MultiSynth is made of one or more MidiSynth instances, typically obtained from a .ins definition file.
 * <p>
 * This an immutable class.
 */
public class MultiSynth
{

    private final List<MidiSynth> midiSynths = new ArrayList<>();
    private File file;
    private static final Logger LOGGER = Logger.getLogger(MultiSynth.class.getSimpleName());

    /**
     * Construct a default MultiSynth with only the GMSynth instance.
     */
    public MultiSynth()
    {
        this(Arrays.asList(GMSynth.getInstance()));
    }

    /**
     * Construct a MultiSynth from the specified MidiSynths.
     * <p>
     * @param synths Can't be empty
     */
    public MultiSynth(List<MidiSynth> synths)
    {
        Preconditions.checkArgument(synths != null && !synths.isEmpty());
        this.midiSynths.addAll(synths);
    }

    /**
     * Construct a MultiSynth from the specified MidiSynths.
     * <p>
     * @param synths
     */
    public MultiSynth(MidiSynth... synths)
    {
        this(Arrays.asList(synths));
    }

    /**
     * Construct a MultiSynth from the MidiSynths found in the specified instrument definition file (.ins).
     * .<p>
     * @param file
     * @throws java.io.IOException If file access, or if no valid (non-empty) MidiSynth found in the file.
     */
    public MultiSynth(File file) throws IOException
    {
        var reader = MidiSynthFileReader.getReader(Utilities.getExtension(file.getName()));
        if (reader == null)
        {
            String msg = ResUtil.getString(getClass(), "ERR_NoSynthReaderForFile", file.getAbsolutePath());
            LOGGER.log(Level.WARNING, "MultiSynth(file) " + msg);   //NOI18N
            throw new IOException(msg);
        }

        // Read the file and add the non-empty synths
        FileInputStream fis = new FileInputStream(file);
        var synths = reader.readSynthsFromStream(fis, file);    // Can raise exception
        synths.stream()
                .filter(s -> s.getNbInstruments() > 0)
                .forEach(s -> midiSynths.add(s));


        if (midiSynths.isEmpty())
        {
            String msg = ResUtil.getString(getClass(), "ERR_NoSynthFoundInFile", file.getAbsolutePath());
            LOGGER.log(Level.WARNING, "MultiSynth(file) " + msg);   //NOI18N
            throw new IOException(msg);
        }

        this.file = file;
    }

    /**
     * Get the name of this MultiSynth.
     * <p>
     * If loaded from a file, return file.getName(). Otherwise return the first MidiSynth's name.
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
     * @see #MultiSynth(java.io.File)
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
     * Get the MidiSynth with the specified name.
     *
     * @param name
     * @return Null if not found in this MultiSynth
     */
    public MidiSynth getMidiSynth(String name)
    {
        return midiSynths.stream()
                .filter(ms -> ms.getName().equals(name))
                .findAny()
                .orElse(null);
    }

    /**
     * Check whether this MultiSynth contains this instrument.
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
     * Get all the Drums/Percussion instruments from this MultiSynth.
     *
     * @return Returned instruments have isDrumKit() set to true.
     */
    public List<Instrument> getDrumsInstruments()
    {
        List<Instrument> res = new ArrayList<>();
        for (var synth : midiSynths)
        {
            res.addAll(synth.getDrumsInstruments());
        }
        return res;
    }

    /**
     * Get all the drums/percussion instruments which match the specified DrumKit.
     *
     * @param kit
     * @param tryHarder If true and no instrument matched the specified kit, then try again but with a more flexible matching
     *                  algorithm. Default implementation starts a second search using kit.Type.STANDARD.
     * @return Can be empty.
     */
    public List<Instrument> getDrumsInstruments(DrumKit kit, boolean tryHarder)
    {
        List<Instrument> res = new ArrayList<>();
        for (var synth : midiSynths)
        {
            res.addAll(synth.getDrumsInstruments(kit, tryHarder));
        }
        return res;
    }

    /**
     * Get all the non Drums/Percussion instruments from this MultiSynth.
     *
     * @return Returned instruments have isDrumKit() set to false.
     */

    public List<Instrument> getNonDrumsInstruments()
    {
        List<Instrument> res = new ArrayList<>();
        for (var synth : midiSynths)
        {
            res.addAll(synth.getNonDrumsInstruments());
        }
        return res;
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
    public int hashCode()
    {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.midiSynths);
        hash = 67 * hash + Objects.hashCode(this.file);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final MultiSynth other = (MultiSynth) obj;
        return Objects.equals(this.midiSynths, other.midiSynths);
    }


    @Override
    public String toString()
    {
        return "MultiSynth-" + getName();
    }

    /**
     * Save this MultiSynth as a string so that it can be retrieved by loadFromString().
     * <p>
     * Use the "FILE=file_name if this MultiSynth came from a file, otherwise just use its name.
     *
     * @return
     * @see loadFromString(String)
     */
    public String saveAsString()
    {
        return (file != null) ? "FILE=" + file.getAbsolutePath() : getName();
    }

    /**
     * Get the MultiSynth corresponding to the string produced by saveAsString().
     * <p>
     * <p>
     * If MultiSynth comes from a file, it will be added as file-based in the MultiSynthManager.
     *
     * @param s
     * @return Can't be null
     * @throws java.io.IOException If the MultiSynth could not be retrieved from the string
     * @see saveAsString()
     * @see MultiSynthManager#addFileBasedMultiSynth(org.jjazz.outputsynth.api.MultiSynth)
     *
     */
    static public MultiSynth loadFromString(String s) throws IOException
    {
        Preconditions.checkNotNull(s);
        s = s.trim();

        var msm = MultiSynthManager.getInstance();
        MultiSynth res;

        if (s.startsWith("FILE="))
        {
            if (s.length() <= 5)
            {
                throw new IOException("Invalid string=" + s);
            }
            File f = new File(s.substring(5).trim());


            // Create the MultiSynth unless it was already loaded before
            res = msm.getMultiSynth(f.getName());
            if (res == null)
            {
                res = new MultiSynth(f);            // throw IOException
                MultiSynthManager.getInstance().addFileBasedMultiSynth(res);
            }

            
        } else
        {
            res = MultiSynthManager.getInstance().getMultiSynth(s);
        }

        if (res == null)
        {
            throw new IOException("Can't retrieve MultiSynth from string s=" + s);
        }


        return res;
    }

    // ==============================================================================================
    // Private methods
    // ==============================================================================================    
}

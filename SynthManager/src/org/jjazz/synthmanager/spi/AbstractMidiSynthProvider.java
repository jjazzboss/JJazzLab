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
package org.jjazz.synthmanager.spi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.midi.MidiSynth;

/**
 * An abstract class to help build a MidiSynthProvider.
 * <p>
 * Subclasses just need to call this constructor and provide a readFile() method.
 */
public abstract class AbstractMidiSynthProvider implements MidiSynthProvider
{

    private String id;
    protected ArrayList<MidiSynth> builtinSynths = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(AbstractMidiSynthProvider.class.getSimpleName());

    /**
     * Initialize the object with no default MidiSynths.
     *
     * @param id A non-empty string.
     */
    public AbstractMidiSynthProvider(String id)
    {
        if (id == null || id.isEmpty())
        {
            throw new IllegalArgumentException("name=" + id);
        }
        this.id = id;
    }

    /**
     * To be used by subclass constructor to add a builtin synth.
     *
     * @param synth
     */
    protected void addBuiltinSynth(MidiSynth synth)
    {
        if (synth == null)
        {
            throw new NullPointerException("synth");
        }
        if (!builtinSynths.contains(this))
        {
            builtinSynths.add(synth);
        }
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public List<MidiSynth> getBuiltinMidiSynths()
    {
        return new ArrayList<>(builtinSynths);
    }

    @Override
    public List<MidiSynth> getSynthsFromFile(File f) throws IOException
    {
        if (f == null)
        {
            throw new NullPointerException("f");
        }
        ArrayList<MidiSynth> res = new ArrayList<>();
        List<MidiSynth> synths = readFile(f); // Do the actual file reading
        for (MidiSynth synth : synths)
        {
            synth.setFile(f);
            res.add(synth);
        }
        return res;
    }

    @Override
    public String saveSynthAsString(MidiSynth synth)
    {
        if (synth == null)
        {
            throw new IllegalArgumentException("synth=" + synth);
        }
        String fileString = synth.getFile() == null ? "*NOFILE*" : synth.getFile().getAbsolutePath();
        return getId() + ", " + synth.getName() + ", " + fileString;
    }

    @Override
    public MidiSynth getSynthFromString(String str)
    {
        String[] strs = str.split(" *, *");
        if (strs.length != 3 || !strs[0].trim().equals(getId()))
        {
            return null;
        }
        MidiSynth synth = null;
        String synthName = strs[1].trim();
        String fileName = strs[2].trim();
        if (fileName.equals("*NOFILE*"))
        {
            // It's a defaultSynth
            for (MidiSynth synthi : builtinSynths)
            {
                if (synthName.equals(synthi.getName()))
                {
                    synth = synthi;
                    break;
                }
            }
        } else
        {
            // It's a file synth
            File f = new File(fileName);

            // Get all the MidiSynth from that file
            List<MidiSynth> synths = null;
            try
            {
                synths = getSynthsFromFile(f);
            } catch (IOException ex)
            {
                LOGGER.warning("getSynthFromString() error reading file: " + ex.getLocalizedMessage());
                return null;
            }
            for (MidiSynth synthi : synths)
            {
                if (synthName.equals(synthi.getName()))
                {
                    synth = synthi;
                    break;
                }
            }
        }
        return synth;
    }

    /**
     * Provide one or more MidiSynth extracted from the specified file.
     *
     * @param f
     * @return Can be an empty list.
     * @throws java.io.IOException
     */
    abstract protected List<MidiSynth> readFile(File f) throws IOException;

}

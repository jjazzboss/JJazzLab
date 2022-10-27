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
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.util.api.Utilities;

/**
 * One or more MidiSynth instances, typically obtained from a .ins definition file.
 * 
 */
public class MidiSynthList
{
    private final List<MidiSynth> midiSynths = new ArrayList<>();
    private File file;
    
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

}

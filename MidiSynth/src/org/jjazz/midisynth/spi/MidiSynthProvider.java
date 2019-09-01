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
package org.jjazz.midisynth.spi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.midi.MidiSynth;
import org.openide.util.Lookup;

/**
 * A provider of MidiSynths: it can provide default Synths and load/save Synths from files.
 */
public interface MidiSynthProvider
{

    static public class Utilities
    {

        /**
         * Search a MidiSynthProvider instance with specified name in the global lookup.
         *
         * @param providerName
         * @return
         */
        static public MidiSynthProvider findProvider(String providerName)
        {
            if (providerName == null)
            {
                throw new NullPointerException("providerName");
            }
            for (MidiSynthProvider p : getProviders())
            {
                if (p.getId().equals(providerName))
                {
                    return p;
                }
            }
            return null;
        }

        /**
         * Get all the MidiSynthProvider instances in the global lookup.
         *
         * @return
         */
        static public List<MidiSynthProvider> getProviders()
        {
            ArrayList<MidiSynthProvider> providers = new ArrayList<>();
            for (MidiSynthProvider p : Lookup.getDefault().lookupAll(MidiSynthProvider.class))
            {
                providers.add(p);
            }
            return providers;
        }
    }

    /**
     * Must be unique amongst MidiSynthProviders.
     *
     * @return
     */
    public String getId();

    /**
     * Get the MidiSynths that are directly available when this object is created.
     *
     * @return List can be null.
     */
    public List<MidiSynth> getBuiltinMidiSynths();

    /**
     * Get the list of file types accepted by this provider.
     *
     * @return Can be en empty list if this provider can't read files.
     * @see getSynthsFromFile()
     */
    public List<FileNameExtensionFilter> getSupportedFileTypes();

    /**
     * Get synths from specified file.
     * <p>
     * File must be compatible with this MidiSynthProvider.
     *
     * @param f
     * @return An empty list if no synth could be created.
     * @throws java.io.IOException
     */
    public List<MidiSynth> getSynthsFromFile(File f) throws IOException;

    /**
     * Build a string reference of the specified synth.
     * <p>
     * The string must store enough information to be used as input for the getSynthFromString() method. Typically used to store a
     * midiSynth reference as a string property.
     *
     * @param synth Must be a synth obtained from this provider.
     * @return A string allowing this object to retrieve the synth later (e.g. recreating it or reading it from a file). Return
     * null if synth could not be saved as string.
     * @see getSynthFromString()
     */
    public String saveSynthAsString(MidiSynth synth);

    /**
     * Try to get a MidiSynth from the specified string reference, e.g. recreating the MidiSynth from scratch or reading it from a
     * file.
     *
     * @param s A string produced by saveSynthAsString()
     * @return Possibly null if this provider was not able to create a MidiSynth from s.
     * @see saveSynthAsString()
     */
    public MidiSynth getSynthFromString(String s);

}

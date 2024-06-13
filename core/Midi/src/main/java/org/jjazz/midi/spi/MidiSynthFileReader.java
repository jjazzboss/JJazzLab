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
package org.jjazz.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.midi.api.MidiSynth;
import org.openide.util.Lookup;

/**
 * A reader of MidiSynths.
 */
public interface MidiSynthFileReader
{

    /**
     * Get the first reader which can read the specified file extension.
     *
     * @param fileExtension For example "ins"
     * @return
     */
    static public MidiSynthFileReader getReader(String fileExtension)
    {
        MidiSynthFileReader res = null;
        for (MidiSynthFileReader reader : Lookup.getDefault().lookupAll(MidiSynthFileReader.class))
        {
            for (FileNameExtensionFilter f : reader.getSupportedFileTypes())
            {
                if (Arrays.asList(f.getExtensions()).contains(fileExtension))
                {
                    res = reader;
                    break;
                }
            }
        }
        return res;
    }


    /**
     * Must be unique amongst MidiSynthProviders.
     *
     * @return
     */
    public String getId();

    /**
     * Get the list of file types accepted by this provider.
     *
     * @return
     */
    public List<FileNameExtensionFilter> getSupportedFileTypes();

    /**
     * Get synth(s) from an input stream.
     * <p>
     * Returned synths must have at least one InstrumentBank. InstrumentBanks can't be empty.
     *
     * @param in
     * @param f  Needed for error messages and logging. Can be null.
     *
     * @return Can be an empty list if no synth could be created.
     * @throws java.io.IOException
     */
    public List<MidiSynth> readSynthsFromStream(InputStream in, File f) throws IOException;

}

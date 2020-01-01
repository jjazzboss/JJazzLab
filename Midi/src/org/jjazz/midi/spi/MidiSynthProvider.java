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
package org.jjazz.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.midi.MidiSynth;

/**
 * A provider of MidiSynths.
 */
public interface MidiSynthProvider
{
    /**
     * Must be unique amongst MidiSynthProviders.
     *
     * @return
     */
    public String getId();

    /**
     * Get the list of file types accepted by this provider.
     *
     * @return Can be en empty list if this provider can't read files.
     */
    public List<FileNameExtensionFilter> getSupportedFileTypes();

    /**
     * Get synths from an input stream.
     * <p>
     *
     * @param in
     * @param f  If f is non null, the created synths will be associated to this file.
     *
     * @return An empty list if no synth could be created.
     * @throws java.io.IOException
     */
    public List<MidiSynth> getSynthsFromStream(InputStream in, File f) throws IOException;

}

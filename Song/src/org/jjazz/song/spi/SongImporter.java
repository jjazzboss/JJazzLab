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
package org.jjazz.song.spi;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.song.api.Song;

/**
 * An interface for objects able to import a song object from a file.
 */
public interface SongImporter
{

    public interface PostProcessor
    {

        /**
         * Post-process a Song object created by a SongImporter (see importFromFile()).
         *
         * This should be used for example to adjust the selected rhythm or add/modify SongParts.
         *
         * @param importer The caller of this method
         * @param song The passed song will have its ChordLeadSheet initialized and a SongStructure with only 1 default SongPart.
         * @param parameters Keys are the parameters names, e.g. "TITLE" or "COMMENTS" and the value are the parameters values.
         * See the importer documentation.
         */
        void postProcessImportedSong(SongImporter importer, Song song, HashMap<String, Object> parameters);
    }

    /**
     * A unique id or name for the importer.
     *
     * @return
     */
    public String getId();

    /**
     * Get the list of file types which can be read by this object.
     *
     * @return
     */
    public List<FileNameExtensionFilter> getSupportedFileTypes();

    /**
     * Try to build a Song object from the specified file.
     *
     * If a service provider implementing the PostProcessor interface is found in the global lookup, then this method must use it
     * on the imported song. If several service providers are found, only the first one must be used.
     *
     * @param f
     * @return
     * @throws java.io.IOException
     */
    public Song importFromFile(File f) throws IOException;

}

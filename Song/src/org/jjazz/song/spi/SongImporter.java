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
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.song.api.Song;

/**
 * An interface for objects able to import a song object from a file.
 */
public interface SongImporter
{

    /**
     * A unique id or name for the importer.
     *
     * @return
     */
    public String getId();

    /**
     * Get the list of file types which can be read by this object.
     *
     * @return Can't be empty.
     */
    public List<FileNameExtensionFilter> getSupportedFileTypes();

    /**
     * Try to build a Song object from the specified file.
     * <p>
     *
     * @param f
     * @return
     * @throws java.io.IOException
     */
    public Song importFromFile(File f) throws IOException;

}

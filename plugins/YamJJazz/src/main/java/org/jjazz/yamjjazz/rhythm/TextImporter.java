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
package org.jjazz.yamjjazz.rhythm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.importers.api.TextReader;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SongImporter.class)
public class TextImporter implements SongImporter
{

    private final FileNameExtensionFilter FILTER = new FileNameExtensionFilter(ResUtil.getString(getClass(), "CTL_TextFiles") + " (.txt)", "txt");
    protected static final Logger LOGGER = Logger.getLogger(TextImporter.class.getName());


    @Override
    public String getId()
    {
        return ResUtil.getString(getClass(), "CTL_TextImporter");
    }

    @Override
    public List<FileNameExtensionFilter> getSupportedFileTypes()
    {
        return Arrays.asList(FILTER);
    }

    @Override
    public Song importFromFile(File f) throws IOException, SongCreationException
    {
        TextReader reader = new TextReader(f);
        Song song = reader.readSong();
        if (song == null)
        {
            throw new SongCreationException("Error importing song");
        }
        postProcessSong(song);
        return song;
    }

    // =================================================================================================
    // Private methods
    // =================================================================================================
    private void postProcessSong(Song song)
    {
        // TBD
    }


}

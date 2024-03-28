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
package org.jjazz.song.spi;

import com.google.common.base.Preconditions;
import org.jjazz.song.api.SongCreationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;

/**
 * An interface for objects able to import a song object from a file.
 */
public interface SongImporter
{

    /**
     * Get all available SongImporters instances present in the global lookup.
     *
     * @return
     */
    public static List<SongImporter> getImporters()
    {
        ArrayList<SongImporter> providers = new ArrayList<>();
        for (SongImporter p : Lookup.getDefault().lookupAll(SongImporter.class))
        {
            providers.add(p);
        }
        return providers;
    }


    /**
     * Get the file extensions supported by all SongImporter instances.
     *
     * @return
     */
    public static Collection<String> getAllSupportedFileExtensions()
    {
        Set<String> res = new HashSet<>();
        for (SongImporter importer : getImporters())
        {
            for (FileNameExtensionFilter filter : importer.getSupportedFileTypes())
            {
                Collections.addAll(res, filter.getExtensions());
            }
        }
        return res;
    }

    /**
     * Select the importers which accept the specified file extension.
     *
     * @param importers
     * @param fileExtension e.g. "sng"
     * @return
     */
    public static List<SongImporter> getMatchingImporters(List<SongImporter> importers, String fileExtension)
    {
        Preconditions.checkNotNull(importers);
        Preconditions.checkNotNull(fileExtension);

        ArrayList<SongImporter> res = new ArrayList<>();

        for (SongImporter importer : importers)
        {
            for (FileNameExtensionFilter f : importer.getSupportedFileTypes())
            {
                for (String ext : f.getExtensions())
                {
                    if (ext.toLowerCase().equals(fileExtension.toLowerCase()))
                    {
                        if (!res.contains(importer))
                        {
                            res.add(importer);
                        }
                    }
                }
            }
        }
        return res;
    }

    /**
     * A unique id or name for the importer.
     *
     * @return
     */
    String getId();

    /**
     * Get the list of file types which can be read by this object.
     *
     * @return Can't be empty.
     */
    List<FileNameExtensionFilter> getSupportedFileTypes();

    /**
     * Try to build a Song object from the specified file.
     * <p>
     *
     * @param f
     * @return
     * @throws java.io.IOException
     * @throws org.jjazz.song.api.SongCreationException
     */
    Song importFromFile(File f) throws IOException, SongCreationException;


}

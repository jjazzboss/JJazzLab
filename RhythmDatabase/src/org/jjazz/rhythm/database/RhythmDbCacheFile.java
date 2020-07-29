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
package org.jjazz.rhythm.database;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.rhythm.database.api.RhythmInfo;

/**
 * The cache file of RhythmInfos for file-based rhythms.
 */
public class RhythmDbCacheFile implements Serializable
{

    private static final String DB_CACHE_FILE = "RhythmDbCache.json";

    /**
     * Save the specified RhythmInfos.
     *
     * @param ris
     * @return
     * @throws IOException
     */
    public void save(List<RhythmInfo> ris) throws IOException
    {

    }


    /**
     * Load file to retrieve the RhythmInfos of file-based rhythms.
     *
     * @return A map with keys=RhythmProviderIds
     * @throws IOException
     */
    public HashMap<String, List<RhythmInfo>> load() throws IOException
    {
        return null;
    }

    public File getFile()
    {
        var fdm = FileDirectoryManager.getInstance();
        File dir = fdm.getAppConfigDirectory(null);
        assert dir != null;
        return new File(dir, DB_CACHE_FILE);
    }

    // =========================================================================
    // Private methods
    // =========================================================================    
}

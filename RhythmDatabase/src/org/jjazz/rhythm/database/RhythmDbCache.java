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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.spi.RhythmProvider;

/**
 * Contains the serializable cached data of the RhythmDatabase.
 * <p>
 */
public class RhythmDbCache implements Serializable
{

    private static final long serialVersionUID = 2922229276100L;
    private static final String DB_CACHE_FILE = "RhythmDbCache.dat";

    private HashMap<String, List<RhythmInfo>> data = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(RhythmDbCache.class.getSimpleName());

    /**
     * Create the cache object.
     * <p>
     * Cache will contain only file-based RhythmInfo instances and no AdaptedRhythms.
     *
     * @param map
     */
    public RhythmDbCache(HashMap<RhythmProvider, List<RhythmInfo>> map)
    {
        // Copy data : just change RhythmProvider by its id
        for (RhythmProvider rp : map.keySet())
        {
            var rhythms = new ArrayList<RhythmInfo>();
            map.get(rp).stream()
                    .filter(ri -> !ri.getFile().getName().equals("") && !ri.isAdaptedRhythm())
                    .forEach(ri -> rhythms.add(ri));
            if (!rhythms.isEmpty())
            {
                data.put(rp.getInfo().getUniqueId(), rhythms);
            }
        }
    }

    /**
     * The cached data.
     * <p>
     * Cache data is used only for file-based rhythms.
     *
     * @return RhyhtmProviderId strings are used as kHashMap keys.
     */
    public HashMap<String, List<RhythmInfo>> getData()
    {
        return data;
    }

    public void dump()
    {
        LOGGER.info("dump():");
        for (String rpId : data.keySet())
        {
            var rhythms = data.get(rpId);
            LOGGER.info("- " + rpId + ": total=" + rhythms.size());
        }
    }

    /**
     * The number of RhythmInfo instances.
     *
     * @return
     */
    public int getSize()
    {
        int n = 0;
        for (String rpId : this.data.keySet())
        {
            n += data.get(rpId).size();
        }
        return n;
    }


    static public File getFile()
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

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
package org.jjazz.rhythmdatabaseimpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;

/**
 * Contains the serializable cached data of the RhythmDatabase.
 * <p>
 * Cache file contains only file-based RhythmInfo instances and no AdaptedRhythms.
 */
public class RhythmDbCache implements Serializable
{

    private static final long serialVersionUID = 2922229276100L;
    private static final String DB_CACHE_FILE = "RhythmDbCache.dat";

    private Map<String, List<RhythmInfo>> savedData;
    private transient static final Logger LOGGER = Logger.getLogger(RhythmDbCache.class.getSimpleName());

    public RhythmDbCache()
    {
    }

    /**
     * Create a cache object for the specified database.
     * <p>
     * @param rdb
     */
    public RhythmDbCache(RhythmDatabase rdb)
    {
        savedData = new HashMap<>();

        // Copy data : just change RhythmProvider by its id
        for (var rp : rdb.getRhythmProviders())
        {
            var rhythms = new ArrayList<>(rdb.getRhythms(rp) // Use ArrayList because returned object by toList() might not be serializable     
                    .stream()
                    .filter(ri -> !ri.file().getName().equals("") && !ri.isAdaptedRhythm())
                    .toList());
            if (!rhythms.isEmpty())
            {
                savedData.put(rp.getInfo().getUniqueId(), rhythms);
            }
        }
    }


    /**
     * Write the cache file.
     *
     * @param file
     * @throws java.io.IOException
     */
    public void saveToFile(File file) throws IOException
    {
        Objects.requireNonNull(file);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file)))
        {
            oos.writeObject(this);
        }
    }


    public void dump()
    {
        LOGGER.info("dump():");
        for (String rpId : savedData.keySet())
        {
            var rhythms = savedData.get(rpId);
            LOGGER.log(Level.INFO, "- {0}: total={1}", new Object[]
            {
                rpId, rhythms.size()
            });
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
        for (String rpId : this.savedData.keySet())
        {
            n += savedData.get(rpId).size();
        }
        return n;
    }

    /**
     * Read a cache file to update the specified database accordingly.
     *
     * @param f
     * @param rdb
     * @return Number of rhythms successfully added to rdb
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    static public int loadFromFile(File f, RhythmDatabase rdb) throws IOException, ClassNotFoundException
    {

        // Read the file
        RhythmDbCache cache;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f)))
        {
            cache = (RhythmDbCache) ois.readObject();           // throws IOException, possibly ClassNotFoundException
        }


        // Process it
        var rps = RhythmProvider.getRhythmProviders();
        int added = 0;
        for (String rpId : cache.savedData.keySet())
        {
            List<RhythmInfo> rhythmInfos = cache.savedData.get(rpId);


            // Check that database is using this RhythmProvider
            var rp = rps.stream()
                    .filter(rpi -> rpi.getInfo().getUniqueId().equals(rpId))
                    .findAny()
                    .orElse(null);
            if (rp == null)
            {
                LOGGER.log(Level.WARNING, "loadFromFile() No RhythmProvider found for rpId={0}. Ignoring {1} rhythms.", new Object[]
                {
                    rpId,
                    rhythmInfos.size()
                });
                continue;
            }

            // Update database
            for (var ri : rhythmInfos)
            {
                if (rdb.addRhythm(rp, ri))
                {
                    added++;
                }
            }
        }

        return added;
    }

    static public File getDefaultFile()
    {
        var fdm = FileDirectoryManager.getInstance();
        File dir = fdm.getAppConfigDirectory(null);
        return new File(dir, DB_CACHE_FILE);
    }

    // =========================================================================
    // Private methods
    // =========================================================================   

}

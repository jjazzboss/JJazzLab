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
package org.jjazz.upgrade;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.openide.modules.OnStart;
import org.openide.modules.Places;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 * Manage the tasks to upgrade settings from a previous version of JJazzLab to the current version.
 * <p>
 * Call all the UpgradeTasks found in the global Lookup upon fresh start.
 * <p>
 */
public class UpgradeManager
{

    /**
     * The previous versions of JJazzLab released to public.
     */
    public static final String[] PREVIOUS_VERSIONS = new String[]
    {
        "2.0.1", "2.0.0"
    };
    private static final String PREF_FRESH_START = "FreshStart";
    private static UpgradeManager INSTANCE;
    private final boolean isFreshStart;
    private static Preferences prefs = NbPreferences.forModule(UpgradeManager.class);
    private static final Logger LOGGER = Logger.getLogger(UpgradeManager.class.getSimpleName());

    static public UpgradeManager getInstance()
    {
        synchronized (UpgradeManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new UpgradeManager();
            }
        }
        return INSTANCE;
    }


    private UpgradeManager()
    {
        isFreshStart = prefs.getBoolean(PREF_FRESH_START, true);
        if (isFreshStart)
        {
            prefs.putBoolean(PREF_FRESH_START, false);
        }
    }

    /**
     * @return True if it's the first run of this JJazzLab version (netbeans user dir. was cleaned)
     */
    public boolean isFreshStart()
    {
        return isFreshStart;
    }
       

    /**
     * Get the properties object of a module Netbeans preferences from an old JJazzLab version.
     * <p>
     * Try each oldVersion until the relevant properties is found.
     *
     * @param nbPrefs The Netbeans preferences of a module
     * @param oldVersions A list of JJazzLab version strings, like "2.0.1". If null use PREVIOUS_VERSIONS.
     * @return Null if not found
     */
    public Properties getPropertiesFromPrefs(Preferences nbPrefs, List<String> oldVersions)
    {
        File f = getOldPreferencesFile(nbPrefs, oldVersions);
        if (f == null)
        {
            return null;
        }

        Properties prop = new Properties();
        try (FileReader reader = new FileReader(f))
        {
            prop.load(reader);
        } catch (IOException ex)
        {
            return null;
        }

        return prop;
    }

    /**
     * Copy all the old preferences key/value pairs into nbPrefs.
     *
     * @param nbPrefs The Netbeans preferences of a module.
     * @param oldVersions A list of JJazzLab version strings, like "2.0.1". If null use PREVIOUS_VERSIONS.
     * @return False if preferences could not be duplicated.
     */
    public boolean duplicateOldPreferences(Preferences nbPrefs, List<String> oldVersions)
    {
        LOGGER.fine("duplicateOldPreferences() -- nbPrefs=" + nbPrefs.absolutePath());
        Properties prop = getPropertiesFromPrefs(nbPrefs, oldVersions);
        if (prop == null)
        {
            return false;
        }

        for (String key : prop.stringPropertyNames())
        {
            nbPrefs.put(key, prop.getProperty(key));
        }
        try
        {
            nbPrefs.flush();        // Make sure it's copied to disk now
        } catch (BackingStoreException ex)
        {
            LOGGER.warning("duplicateOldPreferences() Can't flush copied preferences. ex=" + ex.getLocalizedMessage());
        }
        return true;
    }

    /**
     * Get the file corresponding to a module Netbeans preferences from an old JJazzLab version.
     * <p>
     * Try each oldVersion until a relevant properties file is found.
     *
     * @param nbPrefs The Netbeans preferences of a module.
     * @param oldVersions A list of JJazzLab version strings, like "2.0.1". If null use PREVIOUS_VERSIONS.
     * @return Null if not found
     */
    public File getOldPreferencesFile(Preferences nbPrefs, List<String> oldVersions)
    {
        if ((oldVersions != null && oldVersions.isEmpty()) || nbPrefs == null)
        {
            throw new IllegalArgumentException("oldVersions=" + oldVersions + " nbPrefs=" + nbPrefs);
        }

        if (oldVersions == null)
        {
            oldVersions = Arrays.asList(PREVIOUS_VERSIONS);
        }

        File userDir = Places.getUserDirectory();
        if (userDir == null || !userDir.isDirectory() || userDir.getParentFile() == null)
        {
            LOGGER.warning("getOldPreferencesFile() Invalid Netbeans User Directory userDir=" + userDir);
            return null;
        }
        Path parentPath = userDir.getParentFile().toPath();


        for (var oldVersion : oldVersions)
        {

            Path p = parentPath.resolve(oldVersion + "/config/Preferences" + nbPrefs.absolutePath() + ".properties");
            File f = p.toFile();

            if (f.exists())
            {
                LOGGER.fine("getOldPreferencesFile() FOUND f=" + f.getAbsolutePath());
                return f;
            }

            LOGGER.fine("getOldPreferencesFile Not found f=" + f.getAbsolutePath());
        }

        return null;
    }


    // =============================================================================================================
    // Internal class
    // =============================================================================================================
    @OnStart
    public static class FreshStartUpgrader implements Runnable
    {

        @Override
        public void run()
        {
            if (!getInstance().isFreshStart())
            {
                return;
            }
            LOGGER.info("FreshStartUpgrader() -- ");
            for (var task : Lookup.getDefault().lookupAll(UpgradeTask.class))
            {
                task.upgrade();
            }
        }

    }


}

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
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.jjazz.util.ResUtil;
import org.openide.*;
import org.openide.modules.OnStart;
import org.openide.modules.Places;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 * Manage the tasks to upgrade settings from a previous version of JJazzLab to the current version.
 * <p>
 * Find the source import JJazzLab version. Call all the UpgradeTasks found in the global Lookup upon fresh start at module
 * install (UI is not yet ready!).
 * <p>
 */
public class UpgradeManager
{

    /**
     * The previous versions of JJazzLab released to public.
     */
    public static final String[] PREVIOUS_VERSIONS = new String[]
    {
        "2.2", "2.2.beta3", "2.2.beta2", "2.1.2a", "2.1.2", "2.1.1", "2.1.0", "2.0.1", "2.0.0"
    };
    private static final String PREF_FRESH_START = "FreshStart";

    private static UpgradeManager INSTANCE;
    private boolean importSourceVersionComputed;
    private String importSourceVersion;
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
     * Get the properties object of a module Netbeans preferences from the import source version.
     * <p>
     *
     * @param nbPrefs The Netbeans preferences of a module
     * @return Null if not found
     */
    public Properties getPropertiesFromPrefs(Preferences nbPrefs)
    {
        File f = getOldPreferencesFile(nbPrefs);
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
            LOGGER.warning("getPropertiesFromPrefs() problem reading file=" + f.getAbsolutePath() + ": ex=" + ex.getMessage());   //NOI18N
            return null;
        }

        return prop;
    }

    /**
     * Copy into nbPrefs all the old preferences key/value pairs from the corresponding preferences in import source version.
     *
     * @param nbPrefs The Netbeans preferences of a module.
     * @return False if preferences could not be duplicated.
     */
    public boolean duplicateOldPreferences(Preferences nbPrefs)
    {
        LOGGER.fine("duplicateOldPreferences() -- nbPrefs=" + nbPrefs.absolutePath());   //NOI18N
        Properties prop = getPropertiesFromPrefs(nbPrefs);
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
            LOGGER.warning("duplicateOldPreferences() Can't flush copied preferences. ex=" + ex.getMessage());   //NOI18N
        }
        return true;
    }

    /**
     * Get the file corresponding to a module Netbeans preferences from the JJazzLab import source version.
     * <p>
     *
     * @param nbPrefs The Netbeans preferences of a module.
     * @return Null if not found
     */
    public File getOldPreferencesFile(Preferences nbPrefs)
    {
        if (nbPrefs == null)
        {
            throw new IllegalArgumentException("nbPrefs=" + nbPrefs);   //NOI18N
        }

        String importVersion = getImportSourceVersion();        // If non null validate the Netbeans User Dir.
        if (importVersion == null)
        {
            return null;
        }

        Path parentPath = Places.getUserDirectory().getParentFile().toPath();
        Path p = parentPath.resolve(importVersion).resolve("config").resolve("Preferences" + nbPrefs.absolutePath() + ".properties");
        File f = p.toFile();

        if (!f.exists())
        {
            LOGGER.fine("getOldPreferencesFile Not found f=" + f.getAbsolutePath());   //NOI18N
            f = null;
        } else
        {
            LOGGER.fine("getOldPreferencesFile() FOUND f=" + f.getAbsolutePath());   //NOI18N
        }
        return f;
    }

    /**
     * Get the JJazzLab version from which to import settings.
     * <p>
     * Take first directory from PREVIOUS_VERSIONS where config/Preferences subdir is present.
     *
     * @return E.g. "2.0.1". Null if no import version available.
     */
    public String getImportSourceVersion()
    {
        if (importSourceVersionComputed)
        {
            return importSourceVersion;
        }

        importSourceVersionComputed = true;
        importSourceVersion = null;

        File userDir = Places.getUserDirectory();
        if (userDir == null || !userDir.isDirectory() || userDir.getParentFile() == null)
        {
            LOGGER.warning("getImportSourceVersion() Invalid Netbeans User Directory userDir=" + userDir);   //NOI18N
            return importSourceVersion;
        }

        Path parentPath = userDir.getParentFile().toPath();

        for (var oldVersion : PREVIOUS_VERSIONS)
        {
            Path p = parentPath.resolve(oldVersion).resolve("config").resolve("Preferences");
            File f = p.toFile();

            if (f.exists())
            {
                LOGGER.fine("getImportSourceVersion() FOUND f=" + f.getAbsolutePath());   //NOI18N
                importSourceVersion = oldVersion;
                break;
            }
            LOGGER.fine("getImportSourceVersion Not found f=" + f.getAbsolutePath());   //NOI18N
        }

        return importSourceVersion;
    }

    // =============================================================================================================
    // Private methods
    // =============================================================================================================
    // =============================================================================================================
    // Internal class
    // =============================================================================================================
    @OnStart
    public static class FreshStartUpgrader implements Runnable
    {

        @Override
        public void run()
        {
            UpgradeManager um = UpgradeManager.getInstance();
            if (!um.isFreshStart())
            {
                return;
            }


            String importVersion = um.getImportSourceVersion();     // May be null


            // If not null ask user confirmation before importing
            if (importVersion != null)
            {
                String msg = ResUtil.getString(getClass(),"CTL_ConfirmImportSettings", importVersion);
                NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                Object result = DialogDisplayer.getDefault().notify(d);
                if (NotifyDescriptor.YES_OPTION != result)
                {
                    LOGGER.info("FreshStartUpgrader() -- importVersion=" + importVersion + ", import dismissed by user");   //NOI18N
                    importVersion = null;
                } else
                {
                    LOGGER.info("FreshStartUpgrader() -- importVersion=" + importVersion + ", import authorized by user");   //NOI18N
                }
            } else
            {
                LOGGER.info("FreshStartUpgrader() -- importVersion=" + importVersion);   //NOI18N
            }


            // Call the upgrade tasks
            for (var task : Lookup.getDefault().lookupAll(UpgradeTask.class))
            {
                task.upgrade(importVersion);
            }
        }
    }

}

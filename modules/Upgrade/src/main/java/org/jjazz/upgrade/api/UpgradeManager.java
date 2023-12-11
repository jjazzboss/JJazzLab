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
package org.jjazz.upgrade.api;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.jjazz.utilities.api.ResUtil;
import org.openide.*;
import org.openide.modules.OnStart;
import org.openide.modules.Places;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 * Manage the tasks to upgrade settings from a previous version of JJazzLab to the current version.
 * <p>
 * Find the source import JJazzLab version. Call all the UpgradeTasks found in the global Lookup upon fresh start at module install (UI is
 * not yet ready!).
 * <p>
 */
public class UpgradeManager
{

    /**
     * The previous versions of JJazzLab released to public.
     */
    public static final String[] PREVIOUS_VERSIONS = new String[]
    {
        "4.0-BETA1",
        "3.2.1", "3.2.0", "3.1.0", "3.0.3", "3.0.2a", "3.0.2", "3.0.1", "3.0.beta1",
        "2.3.1", "2.3.beta", "2.2.0", "2.2.beta3", "2.2.beta2", "2.1.2a", "2.1.2", "2.1.1", "2.1.0", "2.0.1", "2.0.0"
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
     * Get the old Properties (from the directory given by getImportSourceVersion()) which corresponds to the specified current module
     * Netbeans preferences file.
     * <p>
     *
     * @param nbPrefs The Netbeans preferences of a module
     * @return Null if not found
     */
    public Properties getOldPropertiesFromPrefs(Preferences nbPrefs)
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
            LOGGER.log(Level.WARNING, "getPropertiesFromPrefs() problem reading file={0}: ex={1}", new Object[]
            {
                f.getAbsolutePath(),
                ex.getMessage()
            });
            return null;
        }

        return prop;
    }

    /**
     * Get the old properties file (from the directory given by getImportSourceVersion()) which corresponds to the specified current module
     * Netbeans preferences file.
     * <p>
     *
     * @param nbPrefs The current Netbeans preferences of a module.
     * @return Null if not found
     */
    public File getOldPreferencesFile(Preferences nbPrefs)
    {
        if (nbPrefs == null)
        {
            throw new IllegalArgumentException("nbPrefs=" + nbPrefs);
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
            LOGGER.log(Level.FINE, "getOldPreferencesFile Not found f={0}", f.getAbsolutePath());
            f = null;
        } else
        {
            LOGGER.log(Level.FINE, "getOldPreferencesFile() FOUND f={0}", f.getAbsolutePath());
        }
        return f;
    }

    /**
     * Copy into nbPrefs all the old preferences key/value pairs from the corresponding preferences found from the getImportSourceVersion()
     * directory.
     *
     * @param nbPrefs The Netbeans preferences of a module.
     * @return False if preferences could not be duplicated.
     */
    public boolean duplicateOldPreferences(Preferences nbPrefs)
    {
        LOGGER.log(Level.FINE, "duplicateOldPreferences() -- nbPrefs={0}", nbPrefs.absolutePath());
        Properties prop = getOldPropertiesFromPrefs(nbPrefs);
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
            LOGGER.log(Level.WARNING, "duplicateOldPreferences() Can''t flush copied preferences. ex={0}", ex.getMessage());
        }
        return true;
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
            LOGGER.log(Level.WARNING, "getImportSourceVersion() Invalid Netbeans User Directory userDir={0}", userDir);
            return importSourceVersion;
        }

        Path parentPath = userDir.getParentFile().toPath();

        for (var oldVersion : PREVIOUS_VERSIONS)
        {
            Path p = parentPath.resolve(oldVersion).resolve("config").resolve("Preferences");
            File f = p.toFile();

            if (f.exists())
            {
                LOGGER.log(Level.FINE, "getImportSourceVersion() FOUND f={0}", f.getAbsolutePath());
                importSourceVersion = oldVersion;
                break;
            }
            LOGGER.log(Level.FINE, "getImportSourceVersion Not found f={0}", f.getAbsolutePath());
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
                String msg = ResUtil.getString(getClass(), "CTL_ConfirmImportSettings", importVersion);
                NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                Object result = DialogDisplayer.getDefault().notify(d);
                if (NotifyDescriptor.YES_OPTION != result)
                {
                    LOGGER.log(Level.INFO, "FreshStartUpgrader() -- importVersion={0}, import dismissed by user", importVersion);
                    importVersion = null;
                } else
                {
                    LOGGER.log(Level.INFO, "FreshStartUpgrader() -- importVersion={0}, import authorized by user", importVersion);
                }
            } else
            {
                LOGGER.log(Level.INFO, "FreshStartUpgrader() -- importVersion={0}", importVersion);
            }


            // Call the upgrade tasks
            for (var task : Lookup.getDefault().lookupAll(UpgradeTask.class))
            {
                task.upgrade(importVersion);
            }
        }
    }

}

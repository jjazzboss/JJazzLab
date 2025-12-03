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

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.jjazz.startup.spi.OnStartTask;
import org.jjazz.utilities.api.ResUtil;
import org.openide.*;
import org.openide.modules.Places;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * Manage the tasks to upgrade settings from a previous version of JJazzLab to the current version.
 * <p>
 * Find the source import JJazzLab version. Call all the UpgradeTasks found in the global Lookup upon fresh start at module install (UI is not yet ready!).
 * <p>
 */
public class UpgradeManager
{

    /**
     * If system property is defined force a fresh start.
     */
    private static final String SYSTEM_PROP_FORCE_FRESH_START = "force.fresh.start";
    /**
     * The previous versions of JJazzLab released to public.
     */
    public static final String[] PREVIOUS_VERSIONS = new String[]
    {
        "5.0.1", "5.0.0",
        "4.1.2", "4.1.1", "4.1.0a", "4.1.0", "4.0.2", "4.0.1",
        "3.2.1", "3.2.0", "3.1.0", "3.0.3", "3.0.2a", "3.0.2", "3.0.1", "3.0.beta1",
        "2.3.1", "2.3.beta", "2.2.0", "2.2.beta3", "2.2.beta2", "2.1.2a", "2.1.2", "2.1.1", "2.1.0", "2.0.1", "2.0.0"
    };
    private static final String PREF_FRESH_START = "FreshStart";
    private static final String NOT_SET = "NotSet";
    private boolean warningShown;
    private static UpgradeManager INSTANCE;
    private String importSourceVersion = NOT_SET;
    private String currentVersion;
    private final boolean isFreshStart;
    private static final Preferences prefs = NbPreferences.forModule(UpgradeManager.class);
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
        boolean forceFreshStart = false;
        if (System.getProperty(SYSTEM_PROP_FORCE_FRESH_START) != null)
        {
            LOGGER.info("UpgradeManager() " + SYSTEM_PROP_FORCE_FRESH_START);
            forceFreshStart = true;
        }

        isFreshStart = prefs.getBoolean(PREF_FRESH_START, true) || forceFreshStart;

        if (isFreshStart)
        {
            prefs.putBoolean(PREF_FRESH_START, false);
        }

        currentVersion = System.getProperty("jjazzlab.version");
        if (currentVersion != null && (currentVersion.isBlank() || currentVersion.charAt(0) < '0' || currentVersion.charAt(0) > '9'))
        {
            currentVersion = null;
        }
        LOGGER.log(Level.INFO, "UpgradeManager() Started -- isFreshStart={0}", isFreshStart);
    }

    /**
     * The current JJazzLab version.
     *
     * @return Can be null.
     */
    public String getCurrentVersion()
    {
        return currentVersion;
    }

    /**
     * @return True if it's the first run of this JJazzLab version (or if this module's preferences file could not be found in the Netbeans user directory).
     */
    public boolean isFreshStart()
    {
        return isFreshStart;
    }

    /**
     * Get the old properties from a specific file in the getImportSourceVersion() directory structure.
     * <p>
     *
     * @param relPath Relative path from ...config/Preferences, eg "org/jjazz/rhythm/database.properties"
     * @return Can be empty if file not found or read error
     */
    public Properties getOldPreferencesFromRelativePath(String relPath)
    {
        Preconditions.checkNotNull(relPath);
        Preconditions.checkArgument(!relPath.isBlank() && relPath.charAt(0) != '/', "relPath=%s", relPath);
        Properties prop = new Properties();

        String importVersion = getImportSourceVersion();        // If non null validate the Netbeans User Dir.
        if (importVersion == null)
        {
            return prop;
        }

        Path parentPath = Places.getUserDirectory().getParentFile().toPath();
        Path p = parentPath.resolve(importVersion).resolve("config").resolve("Preferences").resolve(relPath);
        File f = p.toFile();


        if (f.exists())
        {
            try (FileReader reader = new FileReader(f))
            {
                prop.load(reader);
                LOGGER.log(Level.FINE, "getOldPreferencesFromRelativePath() read f={0}", f.getAbsolutePath());
            } catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "getOldPreferencesFromRelativePath() problem reading file={0}: ex={1}", new Object[]
                {
                    f.getAbsolutePath(),
                    ex.getMessage()
                });
                prop = new Properties();
            }
        } else
        {
            LOGGER.log(Level.FINE, "getOldPreferencesFromRelativePath() File not found f={0}", f.getAbsolutePath());
        }


        return prop;
    }

    /**
     * Copy into modulePrefs all the "old" key/value pairs from the corresponding Properties file found in the getImportSourceVersion() directory structure.
     * <p>
     * To be used when module codebase has not changed between 2 versions. But note that app-level codebase name changes, which impacted *all* (or almost all)
     * module, are handled by this method via adaptPropertiesFileRelativePath().
     *
     * @param modulePrefs The Netbeans preferences of a module.
     * @see #adaptPropertiesFileRelativePath(java.lang.String)
     */
    public void duplicateOldPreferences(Preferences modulePrefs)
    {
        Preconditions.checkNotNull(modulePrefs);
        LOGGER.log(Level.FINE, "duplicateOldPreferences() -- modulePrefs={0}", modulePrefs.absolutePath());

        String relPath = getPropertiesFileRelativePath(modulePrefs);
        relPath = adaptPropertiesFileRelativePath(relPath);
        duplicateOldPreferences(modulePrefs, relPath);
    }

    /**
     * Adapt relPath to the import version.
     * <p>
     * The returned properties file relative path takes into account possible codebase name changes which occured between getImportSourceVersion() and
     * getCurrentVersion().
     *
     * @param relPath A relative path of a properties file for the current version (&gt;=4.1.0), eg "org/jjazzlab/core/midi.properties"
     * @return eg "org/jjazzlab/org/jjazz/midi.properties" for getImportSourceVersion()=4.0.2
     * @see #getCurrentVersion()
     * @see #getImportSourceVersion()
     */
    public String adaptPropertiesFileRelativePath(String relPath)
    {
        // With Ant codename base is defined by a variable in Manifest file, eg "org.jjazz.midi"
        // With Maven, codename base is groupId/artefactId , converting '-'s into '/'s, eg groupId="org.jjjazlab", id="yeah-midi" -> "org/jjazzlab/yeah/midi.properties"
        String importVersion = getImportSourceVersion();
        if (currentVersion != null && importVersion != null)
        {

            // The code below was written for 4.1.0
            assert currentVersion.compareTo("4.1.0") >= 0 : "currentVersion=" + currentVersion;


            if (importVersion.compareTo("4.1.0") < 0)
            {
                // From 4.1.0, groupId was changed "org/jjazzlab" -> "org/jjazzlab/core" or "org/jjazzlab/plugins" or "org/jjazzlab/app"                           
                // Ex: org/jjazzlab/core/midi.properties => org/jjazzlab/midi.properties   
                relPath = relPath.replace("org/jjazzlab/core/", "org/jjazzlab/");
                relPath = relPath.replace("org/jjazzlab/plugins/", "org/jjazzlab/");
                relPath = relPath.replace("org/jjazzlab/app/", "org/jjazzlab/");


                // From 4 to 4.0.2 artefactId name was "org-jjazz-midi", from 4.1.0 it was simplified to "midi"
                if (importVersion.compareTo("4") >= 0)
                {
                    // Ex: "org/jjazzlab/midi.properties" => "org/jjazzlab/org/jjazz/midi.properties"
                    Path p = Path.of(relPath);
                    relPath = p.getParent().resolve("org/jjazz").resolve(p.getFileName()).toString();
                } else
                {
                    // Before JJazzLab 4 we used Ant with most modules code base name=org.jjazz.module (exceptions are handled by the module's RestoreSettingsTask 
                    // by calling duplicateOldPreference(Preferences, String), see for example module ss_editorimpl).                    
                    // "org/jjazzlab/midi.properties" => "org/jjazz/midi.properties"
                    relPath = relPath.replace("org/jjazzlab/", "org/jjazz/");
                }

            }
        }
        return relPath;
    }

    /**
     * Copy into modulePrefs all the "old" key/value pairs from the specified file in the getImportSourceVersion() directory structure.
     * <p>
     * To be used when package codebase has changed between versions.
     *
     * @param modulePrefs          The Netbeans preferences of a module.
     * @param relPathToOldPrefFile Relative path from ...config/Preferences, eg "org/jjazz/rhythm/database.properties"
     */
    public void duplicateOldPreferences(Preferences modulePrefs, String relPathToOldPrefFile)
    {
        Preconditions.checkNotNull(modulePrefs);
        LOGGER.log(Level.FINE, "duplicateOldPreferences() -- modulePrefs={0} relPathToOldPrefFile={1}", new Object[]
        {
            modulePrefs.absolutePath(),
            relPathToOldPrefFile
        });


        if (getImportSourceVersion() == null)
        {
            if (!warningShown)
            {
                LOGGER.info("duplicateOldPreferences() aborted because getImportSourceVersion() is null. This message is shown only once.");
                warningShown = true;
            }
            return;
        }


        Properties oldProps = getOldPreferencesFromRelativePath(relPathToOldPrefFile);
        if (!oldProps.isEmpty())
        {
            // Copy properties
            for (String key : oldProps.stringPropertyNames())
            {
                modulePrefs.put(key, oldProps.getProperty(key));
            }
            try
            {
                modulePrefs.flush();        // Make sure it's copied to disk now
                LOGGER.log(Level.INFO, "duplicateOldPreferences() imported {0} preferences from {1}:{2} to {3}:{4}", new Object[]
                {
                    oldProps.stringPropertyNames().size(),
                    getImportSourceVersion(),
                    relPathToOldPrefFile,
                    getCurrentVersion(),
                    getPropertiesFileRelativePath(modulePrefs)
                });
            } catch (BackingStoreException ex)
            {
                LOGGER.log(Level.WARNING, "duplicateOldPreferences() Can''t flush copied preferences. ex={0}", ex.getMessage());
            }
        }
    }

    /**
     * Get the JJazzLab version from which to import settings.
     * <p>
     * Take first directory from PREVIOUS_VERSIONS where config/Preferences subdir is present.
     *
     * @return Either a valid version string (eg "2.0.1") or null if no import version available.
     */
    public String getImportSourceVersion()
    {
        if (!NOT_SET.equals(importSourceVersion))
        {
            return importSourceVersion;
        }

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
    private void resetImportSourceVersion()
    {
        importSourceVersion = null;
    }

    /**
     * Get the relative path from ..config/Preferences corresponding to nbPrefs.
     *
     * @param nbPrefs
     * @return e.g. "org/jazzlab/midi.properties"
     */
    private String getPropertiesFileRelativePath(Preferences nbPrefs)
    {
        String relPath = nbPrefs.absolutePath().substring(1);
        return relPath + ".properties";
    }

    // =============================================================================================================
    // Internal class
    // =============================================================================================================
    @ServiceProvider(service = OnStartTask.class)
    public static class FreshStartUpgrader implements OnStartTask
    {

        /**
         * Should run early in order to update the modules preferences before instances are created
         */
        public static final int ONSTART_TASK_PRIORITY = 0;

        @Override
        public void run()
        {
            UpgradeManager um = UpgradeManager.getInstance();
            if (!um.isFreshStart())
            {
                return;
            }


            String version = um.getImportSourceVersion();     // May be null


            // If not null ask user confirmation before importing
            if (version != null)
            {
                String msg = ResUtil.getString(getClass(), "CTL_ConfirmImportSettings", version);
                NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                Object result = DialogDisplayer.getDefault().notify(d);
                if (NotifyDescriptor.YES_OPTION != result)
                {
                    LOGGER.log(Level.INFO, "FreshStartUpgrader() -- importVersion={0}, import dismissed by user", version);
                    version = null;
                    um.resetImportSourceVersion();
                } else
                {
                    LOGGER.log(Level.INFO, "FreshStartUpgrader() -- importVersion={0}, import authorized by user", version);
                }
            } else
            {
                LOGGER.log(Level.INFO, "FreshStartUpgrader() -- importVersion={0}", version);
            }


            // Call the upgrade tasks
            for (var task : Lookup.getDefault().lookupAll(UpgradeTask.class))
            {
                task.upgrade(version);
            }
        }

        @Override
        public int getPriority()
        {
            return ONSTART_TASK_PRIORITY;
        }

        @Override
        public String getName()
        {
            return "UpgradeManager";
        }
    }

}

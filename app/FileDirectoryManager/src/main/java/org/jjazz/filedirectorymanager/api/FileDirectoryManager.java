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
package org.jjazz.filedirectorymanager.api;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.Modules;
import org.openide.modules.Places;
import org.openide.util.NbPreferences;

/**
 * Manage the various directories used by the application.
 */
public class FileDirectoryManager
{

    public static final String APP_CONFIG_PREFIX_DIR = ".jjazz";
    public static final String JJAZZLAB_USER_DIR = "JJazzLab";
    public static final String PROP_LAST_SONG_DIRECTORY = "PropLastSongDirectory";
    public static final String MIDISYNTH_FILES_DEST_DIRNAME = "MidiSynthFiles";

    private static FileDirectoryManager INSTANCE;
    /**
     * The Preferences of this object.
     */
    private static final Preferences prefs = NbPreferences.forModule(FileDirectoryManager.class);

    private static final Logger LOGGER = Logger.getLogger(FileDirectoryManager.class.getSimpleName());
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    public static FileDirectoryManager getInstance()
    {
        synchronized (FileDirectoryManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new FileDirectoryManager();
                LOGGER.info("FileDirectoryManager() Started");
            }
        }
        return INSTANCE;
    }

    private FileDirectoryManager()
    {
    }


    /**
     * The directory user.home/JJAZZLAB_USER_DIR.
     * <p>
     * Directory is created if it does not exist.
     *
     * @return An existing directory. Default to user.home property.
     */
    public File getJJazzLabUserDirectory()
    {
        // We need a valid user.home directory        
        String uh = System.getProperty("user.home");
        assert uh != null : "user.home property does not exist: " + uh;
        File userHome = new File(uh);
        assert userHome.isDirectory() : "user.home directory does not exist: " + uh;


        // Our user dir
        File userDir = new File(uh, JJAZZLAB_USER_DIR);
        if (!userDir.isDirectory())
        {
            // Create the directory
            if (!userDir.mkdir())
            {
                LOGGER.log(Level.WARNING, "getJJazzLabUserDirectory() Can''t create directory {0}. Using {1} instead.", new Object[]
                {
                    userDir.getAbsolutePath(),
                    uh
                });
                userDir = userHome;
            } else
            {
                LOGGER.log(Level.INFO, "getJJazzLabUserDirectory() Created JJazzLab user directory {0}", userDir.getAbsolutePath());
            }
        }

        return userDir;
    }

    /**
     * Get the Netbeans application installation directory.
     * <p>
     * This is the top-level directory which contains subdirectories such as bin, etc, platform, ...
     *
     * @return Can't be null
     */
    public File getInstallationDirectory()
    {
        File f = InstalledFileLocator.getDefault().locate("modules", Modules.getDefault().ownerOf(getClass()).getCodeNameBase(), false).getAbsoluteFile();
        f = f.getParentFile().getParentFile();
        assert f != null && f.isDirectory() : "f=" + f;
        return f;
    }


    /**
     * Get the user specific JJazz configuration directory.
     * <p>
     * <p>
     * Use the APP_CONFIG_PREFIX_DIR subdir of the Netbeans user directory, or if not set of the user.home system property. Create the directory if it does not
     * exist.
     *
     * @param subDirName An optional extra subdirectory name (APP_CONFIG_PREFIX_DIR/subDir). Ignored if null or empty.
     * @return Could be null if no user directory found.
     */
    public File getAppConfigDirectory(String subDirName)
    {
        File res;
        // Make sure that the JJazz dir is here
        File userDir = Places.getUserDirectory();
        if (userDir == null)
        {
            userDir = new File(System.getProperty("user.home"));
            LOGGER.log(Level.WARNING, "getAppConfigDirectory() Netbeans user directory is null. Using user''s home directory={0}",
                    userDir.getAbsolutePath());
        }
        if (!userDir.isDirectory())
        {
            LOGGER.log(Level.SEVERE, "getAppConfigDirectory() Can''t find a valid user directory userDir={0}", userDir);
            return null;
        }
        File appConfigDir = userDir.toPath().resolve(APP_CONFIG_PREFIX_DIR).toFile();
        if (!appConfigDir.isDirectory())
        {
            try
            {
                appConfigDir.mkdir();
            } catch (SecurityException e)
            {
                LOGGER.log(Level.SEVERE,
                        "getAppConfigDirectory() impossible to create application config dir={0}. Using user home directory instead.",
                        appConfigDir.getAbsolutePath());
                appConfigDir = new File(System.getProperty("user.home"));
            }
        }
        res = appConfigDir;

        if (subDirName != null && !subDirName.isEmpty())
        {
            res = appConfigDir.toPath().resolve(subDirName).toFile();
            if (!res.isDirectory())
            {
                try
                {
                    res.mkdir();

                } catch (SecurityException e)
                {
                    LOGGER.log(Level.WARNING, "getAppConfigDirectory() impossible to create {0}", res.getAbsolutePath());
                    res = appConfigDir;
                }
            }
        }
        LOGGER.log(Level.FINE, "getAppConfigDirectory() res={0}", res);
        return res;
    }

    /**
     * The directory where the default MidiSynth files bundled with JJazzLab are stored.
     *
     * @return Might be null
     */
    public File getAppConfigDirForSynths()
    {
        var dir = FileDirectoryManager.getInstance().getAppConfigDirectory(MIDISYNTH_FILES_DEST_DIRNAME);
        return dir;
    }


    /**
     * Get the File object corresponding to an existing file (or subdirectory) in the AppConfig directory of an old JJazzLab version.
     * <p>
     * For example on Windows, if oldVersion="2.0.1" and relPath="subdir/hello.properties" and this files exists, method returns the File at
     * &lt;WindowsUserDir&gt;/AppData/Roaming/jjazzlab/2.0.1/.jjazz/subdir/hello.properties".
     *
     * @param relPath    The relative path to the file from the old AppConfig directory. If null returns the AppConfig directory.
     * @param oldVersion E.g. "2.0.1"
     * @return Null if relPath does not corresponding to an existing file
     */
    public File getOldAppConfigFile(String oldVersion, String relPath)
    {
        if (oldVersion == null)
        {
            throw new IllegalArgumentException("oldVersion=" + oldVersion + " subDirname=" + relPath);
        }

        File userDir = Places.getUserDirectory();
        if (userDir == null || !userDir.isDirectory() || userDir.getParentFile() == null)
        {
            LOGGER.log(Level.WARNING, "getOldAppConfigFile() Invalid Netbeans User Directory userDir={0}", userDir);
            return null;
        }


        Path parentPath = userDir.getParentFile().toPath();
        Path p = parentPath.resolve(oldVersion).resolve(APP_CONFIG_PREFIX_DIR);
        if (relPath != null)
        {
            p = p.resolve(relPath);
        }


        File f = p.toFile();
        if (!f.exists())
        {
            f = null;
        }

        return f;
    }

    /**
     * The last directory used for song open or song save.
     * <p>
     * If not set return getJJazzLabUserDirectory() unless it does not exist anymore.
     *
     * @return Can be null
     */
    public File getLastSongDirectory()
    {
        String s = prefs.get(PROP_LAST_SONG_DIRECTORY, null);
        File f = null;
        if (s != null)
        {
            f = new File(s);
            if (!f.isDirectory())
            {
                // A directory was set but does not exist anymore, reset to null
                f = null;
            }
        }
        if (f == null)
        {
            f = getJJazzLabUserDirectory();
            if (!f.isDirectory())
            {
                f = null;
            }
        }
        LOGGER.log(Level.FINE, "getLastSongDirectory() f={0}", f);
        return f;
    }

    /**
     * Set the last song directory.
     *
     * @param dir
     */
    public void setLastSongDirectory(File dir)
    {
        if (!dir.isDirectory())
        {
            throw new IllegalArgumentException("dir=" + dir);
        }
        File old = getLastSongDirectory();
        prefs.put(PROP_LAST_SONG_DIRECTORY, dir.getAbsolutePath());
        LOGGER.log(Level.FINE, "setLastSongDirectory() old={0} new={1}", new Object[]
        {
            old, dir
        });
        pcs.firePropertyChange(PROP_LAST_SONG_DIRECTORY, old, dir);
    }


}

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
package org.jjazz.filedirectorymanager;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.util.Utilities;
import org.openide.modules.Places;
import org.openide.util.NbPreferences;

/**
 * Manage the various directories and file types used by the application.
 */
public class FileDirectoryManager
{

    public static final String TEMPLATE_SONG_NAME = "NewSongTemplate";
    public static final String MIX_FILE_EXTENSION = "mix";
    public static final String SONG_EXTENSION = "sng";
    public static final String PROP_LAST_SONG_DIRECTORY = "PropLastSongDirectory";
    public static final String PROP_RHYTHM_USER_DIRECTORY = "PropRhythmUserDirectory";
    public static final String PROP_RHYTHM_MIX_DIRECTORY = "PropRhythmMixDirectory";

    private static FileDirectoryManager INSTANCE;
    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(FileDirectoryManager.class);

    /**
     * The listeners for changes of this object.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(FileDirectoryManager.class.getSimpleName());

    public static FileDirectoryManager getInstance()
    {
        synchronized (FileDirectoryManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new FileDirectoryManager();
            }
        }
        return INSTANCE;
    }

//   /**
//    *
//    * @return
//    */
//   public File getJJazzResourcesDirectory()
//   {
//      Path curDir = Paths.get(".").toAbsolutePath().resolve(RESOURCES_DIR_NAME);
//      return curDir.toFile();
//   }
    /**
     * Build the song mix File object for specified song file.
     * <p>
     * SongMix file will be located in the same directory than songFile.
     *
     * @param songFile
     * @return Return a new file identical to songFile except the extension. If songFile is null returns null.
     */
    public File getSongMixFile(File songFile)
    {
        if (songFile == null)
        {
            return null;
        }
        String songMixName = Utilities.replaceExtension(songFile.getName(), MIX_FILE_EXTENSION);
        return new File(songFile.getParent(), songMixName);
    }

    /**
     * Get the rhythm mix File object for the specified rhythm.
     * <p>
     *
     * @param rhythm A non-null object. Note that the actual file on the file system might not exist yet.
     * @return
     */
    public File getRhythmMixFile(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new IllegalArgumentException("rhythm=" + rhythm);
        }
        File rf = rhythm.getFile();
        String base = rf.getName().isEmpty() ? rhythm.getName() : rf.getName();
        String rhythmMixFileName = Utilities.replaceExtension(base.replace(" ", ""), MIX_FILE_EXTENSION);
        File f = new File(getRhythmMixDirectory(), rhythmMixFileName);
        return f;
    }

    public File getNewSongTemplateMixFile()
    {
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        File dir = fdm.getAppConfigDirectory(null);
        File f = new File(dir, TEMPLATE_SONG_NAME + "." + FileDirectoryManager.MIX_FILE_EXTENSION);
        return f;
    }

    public File getNewSongTemplateSongFile()
    {
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        File dir = fdm.getAppConfigDirectory(null);
        File f = new File(dir, TEMPLATE_SONG_NAME + "." + FileDirectoryManager.SONG_EXTENSION);
        return f;
    }

    /**
     * Get the user directory for Rhythms files.
     * <p>
     * If not set use by default the System property "user.home".
     *
     * @return Can't be null
     */
    public File getUserRhythmDirectory()
    {
        String uh = System.getProperty("user.home");
        String s = prefs.get(PROP_RHYTHM_USER_DIRECTORY, uh);
        File f = new File(s);
        if (!f.isDirectory())
        {
            LOGGER.warning("getUserRhythmDirectory() User rhythm directory not found: " + s + " Using: " + uh + " instead.");
            f = new File(uh);
            if (!f.isDirectory())
            {
                LOGGER.severe("getUserRhythmDirectory() No valid user rhythm directory found. Can't reuse system user directory because it does not exist: " + f.getAbsolutePath());
            }
        }
        LOGGER.fine("getUserRhythmDirectory() f=" + f);
        return f;
    }

    /**
     * Set the user directory where to find rhythm files.
     *
     * @param dir
     */
    public void setUserRhythmDirectory(File dir)
    {
        if (!dir.isDirectory())
        {
            throw new IllegalArgumentException("dir=" + dir);
        }
        File old = getUserRhythmDirectory();
        prefs.put(PROP_RHYTHM_USER_DIRECTORY, dir.getAbsolutePath());
        LOGGER.fine("setUserRhythmDirectory() old=" + old + " new=" + dir);
        pcs.firePropertyChange(PROP_RHYTHM_USER_DIRECTORY, old, dir);
    }

    /**
     * Get the directory used for default rhythm mix files.
     * <p>
     * If not set use the same default value than getUserRhythmDirectory().
     *
     * @return Can't be null.
     */
    public File getRhythmMixDirectory()
    {
        String uh = System.getProperty("user.home");
        String s = prefs.get(PROP_RHYTHM_MIX_DIRECTORY, uh);
        File f = new File(s);
        if (!f.isDirectory())
        {
            LOGGER.warning("getRhythmMixDirectory() rhythm mix directory not found: " + s + " Using: " + uh + " instead.");
            f = new File(uh);
            if (!f.isDirectory())
            {
                LOGGER.severe("getRhythmMixDirectory() No valid rhythm mix directory found. Can't reuse system user directory because it does not exist: " + f.getAbsolutePath());
            }
        }
        LOGGER.fine("getRhythmMixDirectory() f=" + f);
        return f;

    }

    /**
     * Set the user directory where to find rhythm mix files.
     *
     * @param dir
     */
    public void setRhythmMixDirectory(File dir)
    {
        if (dir == null || !dir.isDirectory())
        {
            throw new IllegalArgumentException("dir=" + dir);
        }
        File old = getRhythmMixDirectory();
        prefs.put(PROP_RHYTHM_MIX_DIRECTORY, dir.getAbsolutePath());
        LOGGER.fine("setUserRhythmMixDirectory() old=" + old + " new=" + dir);
        pcs.firePropertyChange(PROP_RHYTHM_MIX_DIRECTORY, old, dir);
    }

    /**
     * Get the user specific JJazz configuration directory.
     * <p>
     * <p>
     * Use the ".jjazz" subdir of the Netbeans user directory, or if not set of the user.home system property. Create the directory if it
     * does not exist.
     *
     * @param subDirName An optional extra subdirectory name (".jjazz/subDir"). Ignored if null or empty.
     * @return Could be null if no user directory found.
     */
    public File getAppConfigDirectory(String subDirName)
    {
        File res;
        // Make sure that the JJazz dir is here
        File userDir = Places.getUserDirectory();
        if (userDir == null)
        {
            LOGGER.warning("getAppConfigDirectory() Netbeans user directory is null. Using user's home directory.");
            userDir = new File(System.getProperty("user.home"));
        }
        if (!userDir.isDirectory())
        {
            LOGGER.severe("getAppConfigDirectory() Can't find a valid user directory userDir=" + userDir);
            return null;
        }
        File appConfigDir = userDir.toPath().resolve(".jjazz").toFile();
        if (!appConfigDir.isDirectory())
        {
            try
            {
                appConfigDir.mkdir();
            } catch (SecurityException e)
            {
                LOGGER.severe("getAppConfigDirectory() impossible to create application config dir=" + appConfigDir.getAbsolutePath() + ". Using user home directory instead.");
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
                    LOGGER.warning("getAppConfigDirectory() impossible to create " + res.getAbsolutePath());
                    res = appConfigDir;
                }
            }
        }
        LOGGER.fine("getAppConfigDirectory() res=" + res);
        return res;
    }

    /**
     * The last directory used for song open or song save.
     *
     * @return Can be null if not set yet, or if the saved directory is not a directory anymore.
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
                f = null;
            }
        }
        LOGGER.fine("getLastSongDirectory() f=" + f);
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
        LOGGER.fine("setLastSongDirectory() old=" + old + " new=" + dir);
        pcs.firePropertyChange(PROP_LAST_SONG_DIRECTORY, old, dir);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

}

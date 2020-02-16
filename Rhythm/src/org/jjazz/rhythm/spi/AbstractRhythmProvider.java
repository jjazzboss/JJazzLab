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
package org.jjazz.rhythm.spi;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.util.Utilities;
import org.openide.*;
import org.openide.util.NbPreferences;

/**
 * A base class to help build a RhythmProvider.
 * <p>
 * Analyze the rhythm directory and its subdirectories for rhythm files. <br>
 * Manage a list of blacklisted files saved as Preferences to avoid re-opening them.
 * <p>
 * Subclasses need only to implement the abstract methods and call the constructor with the appropriate arguments.
 */
public abstract class AbstractRhythmProvider implements RhythmProvider
{

    public static final String PREFIX_IGNORED_SUBDIR = "_";
    private Info info;
    private FilenameFilter filenameFilter;
    private static final Logger LOGGER = Logger.getLogger(AbstractRhythmProvider.class.getSimpleName());
    private static String PREF_FILES_BLACK_LIST = "FilesBlackList";
    private static Preferences prefs = NbPreferences.forModule(AbstractRhythmProvider.class);

    /**
     *
     * @param uniqueId
     * @param name
     * @param desc
     * @param author
     * @param version
     * @param fileExtensions The recognized rhythm files extensions. Can be null if no files.
     */
    protected AbstractRhythmProvider(String uniqueId, String name, String desc, String author, int version, String... fileExtensions)
    {
        info = new Info(uniqueId, name, desc, author, String.valueOf(version));
        if (fileExtensions != null)
        {
            filenameFilter = new OurFilter(fileExtensions);
        }
    }

    protected FilenameFilter getFilenameFilter()
    {
        return filenameFilter;
    }

    @Override
    public Info getInfo()
    {
        return info;
    }

    /**
     * Get the file-based rhythms provided by this object.
     * <p>
     * If prevList is null then use readFast() to fast scan all rhythms files found in getRhythmFilesDir() and subdirs -except the subdirs
     * starting with PREFIX_IGNORED_SUBDIR.<br>
     * If prevList is non null then fast scan only the new files. Also check for files which have been removed from the directory (present
     * in prevList but not in the getRhythmFilesDir() tree)
     * .<p>
     * Files which can't be read are added to a blacklist which is saved as a Preferences.
     *
     * @param prevRhythmList A list of file-based rhythms.
     * @return
     */
    @Override
    public List<Rhythm> getFileRhythms(List<Rhythm> prevRhythmList)
    {
        LOGGER.fine("getRhythms() -- prevRhythmList=" + prevRhythmList);
        ArrayList<Rhythm> result = new ArrayList<>();

        File rDir = getRhythmFilesDir();
        if (getFilenameFilter() == null || rDir == null)
        {
            LOGGER.fine("getRhythms() RhythmProvider=" + info.getName() + " - Return an empty list because filenameFilter=" + filenameFilter + ", rDir=" + rDir);
            return result;
        }

        if (!rDir.isDirectory())
        {
            LOGGER.warning("getRhythms() RhythmProvider=" + info.getName() + " - Rhythm file directory does not exist : " + rDir.getAbsolutePath());
            return result;
        }

        // The HashSet of RhythmFiles built from the prevRhythmList
        HashSet<RhythmPathPair> prevListSet = new HashSet<>();
        if (prevRhythmList != null)
        {
            for (Rhythm r : prevRhythmList)
            {
                String fn = r.getFile().getName();       // ri.getFile() can't return null
                if (!fn.equals(""))
                {
                    prevListSet.add(new RhythmPathPair(r.getFile().toPath(), r));
                } else
                {
                    // It's a builtin rhythm !
                    throw new IllegalArgumentException("r=" + r + " prevList=" + prevRhythmList);
                }
            }
        }

        // Get the rhythm files in the directory tree
        HashSet<Path> rhythmFiles = Utilities.listFiles(rDir, getFilenameFilter(), PREFIX_IGNORED_SUBDIR);
        LOGGER.fine("getRhythms()   rhythmFiles=" + rhythmFiles);

        // Remove the black listed files
        HashSet<Path> blackListedFiles = getBlackListedFiles();
        LOGGER.fine("getRhythms()   blackList=" + blackListedFiles);
        for (Path blackListedFile : blackListedFiles.toArray(new Path[0]))
        {
            if (rhythmFiles.contains(blackListedFile))
            {
                // File is still there, remove it 
                rhythmFiles.remove(blackListedFile);
            } else
            {
                // File is no more here, update our black list
                blackListedFiles.remove(blackListedFile);
            }
        }
        if (!blackListedFiles.isEmpty())
        {
            LOGGER.info("getRhythms() Ignoring previously blacklisted files: " + blackListedFiles);
        }

        // Compare the previous list to what's in the directory
        // Detect deleted files in the dir and subdirs, leave only new files in rhythmFiles
        for (RhythmPathPair rpp : prevListSet)
        {
            // Do we find the previous file in the dir. tree ? 
            if (rhythmFiles.contains(rpp.path))
            {
                // Yes: add directly the Rhythm in the result and remove the path from rhythmFiles
                result.add(rpp.rhythm);
                rhythmFiles.remove(rpp.path);
            } else
            {
                // No, prevFile must have been deleted during execution, don't add it to the result
            }
        }

        LOGGER.fine("getRhythms()   rhythmFiles after processing of prevList=" + rhythmFiles);

        // Now rhythmFiles contains only NEW rhythm files, ie which are not in prevList
        HashSet<Path> blackListUpdate = new HashSet<>();
        for (Path rhythmFile : rhythmFiles)
        {
            // Read the file to create the appropriate Rhythm, then add to result
            File f = rhythmFile.toFile();
            Rhythm r = readFast(f);
            if (r != null)
            {
                result.add(r);
            } else
            {
                // Problem occured, black list file
                blackListUpdate.add(rhythmFile);
            }
        }

        if (!blackListUpdate.isEmpty())
        {
            String strFiles = blackListUpdate.toString();
            LOGGER.warning("getRhythms() The following rhythm file(s) could not be read : " + strFiles);
            String msg = "The following rhythm file(s) could not be read. Consult the log window for details.";
            if (strFiles.length() > 120)
            {
                strFiles = strFiles.substring(0, 120) + "...]";
            }
            NotifyDescriptor d = new NotifyDescriptor.Message(msg + "\n\n" + strFiles, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(d);

            // Update the blacklist
            blackListedFiles.addAll(blackListUpdate);
        }

        // Save update blacklist
        storeFileBlackList(blackListedFiles);

        LOGGER.fine("getRhythms()   result=" + result);

        return result;
    }

    /**
     * Return the builtin rhythms of this provider, ie not depending on the user-provided file.
     *
     * @return A list of rhythms for which getFile() returns an empty path file.
     */
    @Override
    public abstract List<Rhythm> getBuiltinRhythms();

    /**
     * A fast method to read specified file and extract only the description Rhythm object complete enough for description purposes.
     * <p>
     * If the returned rhythm uses a heavy-memory MidiMusicGenerator, it should delay its memory-loading in the lookup in its
     * loadResources() method.
     *
     * @param f
     * @return Can be null if problem.
     */
    protected abstract Rhythm readFast(File f);

    /**
     * Get the blacked listed files.
     *
     * @return
     */
    public HashSet<Path> getBlackListedFiles()
    {
        HashSet<Path> res = new HashSet<>();
        String strs = prefs.get(getBlackListPreferenceKey(), "").trim();
        for (String s : strs.split("\\s*,\\s*"))
        {
            res.add(Paths.get(s));
        }
        LOGGER.fine("getFileBlackList() res=" + res);
        return res;
    }

    @Override
    public String toString()
    {
        return getInfo().getName();
    }

    /**
     * Delegate to getInfo().equals().
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == null || !(o instanceof RhythmProvider))
        {
            return false;
        }
        RhythmProvider rp = (RhythmProvider) o;
        return getInfo().equals(rp.getInfo());
    }

    /**
     * Delegate to getInfo().hashCode().
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        return getInfo().hashCode();
    }

    /**
     * Where getFileRhythms() look for rhythm files.
     *
     * @return A directory.
     */
    abstract protected File getRhythmFilesDir();

    // ===================================================================================
    // Private mehods
    // ===================================================================================
    /**
     * Store the list by RhythmProvider.
     *
     * @param blackList
     */
    private void storeFileBlackList(HashSet<Path> blackList)
    {
        if (blackList.isEmpty())
        {
            LOGGER.fine("storeFileBlackList() key=" + getBlackListPreferenceKey() + " => empty value");
            prefs.put(getBlackListPreferenceKey(), "");
            return;
        }
        Iterator<Path> it = blackList.iterator();
        StringBuilder sb = new StringBuilder(it.next().toString());
        while (it.hasNext())
        {
            sb.append(",").append(it.next().toString());
        }
        LOGGER.fine("storeFileBlackList() key=" + getBlackListPreferenceKey() + " value=" + sb.toString());
        prefs.put(getBlackListPreferenceKey(), sb.toString());
    }

    private String getBlackListPreferenceKey()
    {
        return PREF_FILES_BLACK_LIST + "-" + getInfo().getUniqueId();
    }

    // ===================================================================================
    // Private classes
    // ===================================================================================
    /**
     * A pair class to help perform diff operations on Rhythm lists.
     */
    private class RhythmPathPair
    {

        Path path;
        Rhythm rhythm;

        RhythmPathPair(Path p, Rhythm r)
        {
            path = p;
            rhythm = r;
        }
    }

    /**
     * A file filter for the specified file extensions.
     */
    private class OurFilter implements FilenameFilter
    {

        String[] fileExtensions;

        protected OurFilter(String[] extensions)
        {
            fileExtensions = extensions;
        }

        @Override
        public boolean accept(File dir, String name)
        {
            for (String ext : fileExtensions)
            {
                if (name.toLowerCase().endsWith(ext.toLowerCase()))
                {
                    return true;
                }
            }
            return false;
        }
    }

}

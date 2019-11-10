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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.rhythm.api.Rhythm;
import org.openide.*;
import org.openide.util.NbPreferences;

/**
 * A base class to help build a RhythmProvider.
 * <p>
 * Analyze the rhythm directory for rhythm files. <br>
 * Manage a list of blacklisted files saved as Preferences to avoid re-opening them.
 * <p>
 * Subclasses need only to implement the abstract methods and call the constructor with the appropriate arguments.
 */
public abstract class AbstractRhythmProvider implements RhythmProvider
{

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
     * @param fileExtensions The allowed rhythm files extensions. Can be null if no files.
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
     * Get the rhythms of this object.
     * <p>
     * If prevList is null then use readFast() to fast scan all rhythms files found in directory returned by
     * getRhythmDirectory().<br>
     * If prevList is non null then fast scan only the added files (present in the directory but not in prevList). Also check for
     * files which have been removed from the directory (present in prevList but not in the directory)
     * .<p>
     * Files which can't be read are added to a blacklist which is saved as a Preferences.
     *
     * @param prevList A list of non-builtin rhythms
     * @return
     */
    @Override
    public List<Rhythm> getFileRhythms(List<Rhythm> prevList)
    {
        LOGGER.fine("getRhythms() -- prevList=" + prevList);
        ArrayList<Rhythm> result = new ArrayList<>();

        // Prepare a workable sorted prevList
        ArrayList<InfoFileName> prevListWork = new ArrayList<>();
        if (prevList != null)
        {
            for (Rhythm r : prevList)
            {
                String fn = r.getFile().getName();       // ri.getFile() can't return null
                if (!fn.equals(""))
                {
                    prevListWork.add(new InfoFileName(fn, r));
                } else
                {
                    // It's a builtin rhythm !
                    throw new IllegalArgumentException("r=" + r + " prevList=" + prevList);
                }
            }
        }
        Collections.sort(prevListWork);

        // Get the sorted filename list in the rhythm directory
        ArrayList<String> dirFilenames = new ArrayList<>();
        File dirFiles = getRhythmFilesDir();
        if (filenameFilter != null && dirFiles != null)
        {
            dirFilenames.addAll(Arrays.asList(dirFiles.list(filenameFilter)));          // Arrays.asList returns a fix-size list
        }
        Collections.sort(dirFilenames);
        LOGGER.fine("getRhythms()   dirFilenames BEFORE=" + dirFilenames);

        // Remove the black listed files from the directory list
        ArrayList<String> blackList = new ArrayList<>(getFileBlackList());
        LOGGER.fine("getRhythms()   blackList BEFORE=" + blackList);
        for (String s : blackList.toArray(new String[0]))
        {
            int index = Collections.binarySearch(dirFilenames, s);
            if (index >= 0)
            {
                // File is still there, remove it 
                dirFilenames.remove(index);
            } else
            {
                // File is no more here, update our black list
                blackList.remove(s);
            }
        }
        if (!blackList.isEmpty())
        {
            LOGGER.info("getRhythms() Ignoring previously blacklisted files: " + blackList);
        }

        // Compare the previous list to what's in the directory: detect deleted files in the dir
        for (InfoFileName ifn : prevListWork)
        {
            // Do we find the previous name in the directory ?
            int index = Collections.binarySearch(dirFilenames, ifn.filename);
            if (index >= 0)
            {
                // Yes, keep the Rhythm in the result
                result.add(ifn.rhythm);
                // Remove the processed data 
                dirFilenames.remove(index);
            } else
            {
                // No, prevFile must have been deleted during execution, don't add it to the result
            }
        }

        LOGGER.fine("getRhythms()   dirFilenames AFTER=" + dirFilenames);

        // Now dirFilenames contains only new filenames, ie which are not in prevList
        ArrayList<String> blackListUpdate = new ArrayList<>();
        for (String filename : dirFilenames)
        {
            // Read the file to create the appropriate Rhythm, then add to result
            File f = new File(dirFiles, filename);
            Rhythm r = readFast(f);
            if (r != null)
            {
                result.add(r);
            } else
            {
                // Problem occured, black list file
                blackListUpdate.add(filename);
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
            for (String fileName : blackListUpdate)
            {
                if (!blackList.contains(fileName))      // For robustness
                {
                    blackList.add(fileName);
                }
            }
        }

        // Save update blacklist
        storeFileBlackList(blackList);

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
     * A fast method to read specified file and extract only the description Rhythm object complete enough for description
     * purposes.
     * <p>
     * If the returned rhythm uses a heavy-memory MidiMusicGenerator, it should delay its memory-loading in the lookup in its
     * loadResources() method.
     *
     * @param f
     * @return Can be null if problem.
     */
    protected abstract Rhythm readFast(File f);

    /**
     *
     * @return A fixed-size list.
     */
    public List<String> getFileBlackList()
    {
        String strs = prefs.get(getBlackListPreferenceKey(), "").trim();
        List<String> res = Arrays.asList(strs.split("\\s*,\\s*"));
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
    private void storeFileBlackList(List<String> blackList)
    {
        if (blackList.isEmpty())
        {
            LOGGER.fine("storeFileBlackList() key=" + getBlackListPreferenceKey() + " => empty value");
            prefs.put(getBlackListPreferenceKey(), "");
            return;
        }
        StringBuilder sb = new StringBuilder(blackList.get(0));
        for (int i = 1; i < blackList.size(); i++)
        {
            sb.append(",").append(blackList.get(i));
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
    private class InfoFileName implements Comparable<InfoFileName>
    {

        String filename;
        Rhythm rhythm;

        InfoFileName(String f, Rhythm r)
        {
            filename = f;
            rhythm = r;
        }

        @Override
        public int compareTo(InfoFileName o)
        {
            return filename.compareTo(o.filename);
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
                if (name.toLowerCase().endsWith(ext))
                {
                    return true;
                }
            }
            return false;
        }
    }

}

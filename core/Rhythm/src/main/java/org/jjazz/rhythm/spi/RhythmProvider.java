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
package org.jjazz.rhythm.spi;

import org.jjazz.utilities.api.MultipleErrorsReport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.openide.util.Lookup;

/**
 * An object that can provide Rhythms instances.
 */
public interface RhythmProvider
{


    /**
     * See getFileRhythms().
     */
    public static final String PREFIX_IGNORED_SUBDIR = "_";
    /**
     * See getFileRhythms().
     */
    public static final int SUBDIR_MAX_DEPTH = 3;

    /**
     * The RhythmProviders instances available in the global lookup, sorted by name.
     *
     * @return
     */
    static public List<RhythmProvider> getRhythmProviders()
    {
        List<RhythmProvider> res = new ArrayList<>(Lookup.getDefault().lookupAll(RhythmProvider.class));
        res.sort((rp1, rp2) -> rp1.getInfo().getName().compareTo(rp2.getInfo().getName()));
        return res;
    }

    /**
     * Get the RhythmProvider in the global lookup which matches rpId.
     *
     * @param rpId
     * @return Can be null
     */
    static public RhythmProvider getRhythmProvider(String rpId)
    {
        return getRhythmProviders()
                .stream()
                .filter(rp -> rp.getInfo().getUniqueId().equals(rpId))
                .findAny()
                .orElse(null);
    }

    /**
     * Descriptive information about this provider.
     *
     * @return
     */
    public Info getInfo();

    /**
     * Get the built-in rhythms.
     * <p>
     *
     * @param errRpt Can't be null. RhythmProvider should update this object so that the framework can notify user about problems.
     * @return All non file-based rhythms provided by this RhythmProvider. List can be empty but not null.
     */
    public List<Rhythm> getBuiltinRhythms(MultipleErrorsReport errRpt);

    /**
     * Get the file-based rhythms.
     * <p>
     * User-provided rhythm files should be scanned in the User directory for rhythm files, see FileDirectoryManager.getUserRhythmDirectory(). SUBDIR_MAX_DEPTH
     * levels of subdirectories should be scanned. Subdirectories starting with PREFIX_IGNORED_SUBDIR must be ignored.
     *
     * @param forceRescan If true RhythmProvider should not rely on its cached data.
     * @param errRpt      Can't be null. RhythmProvider should update this object so that the framework can notify user about problems.
     * @return All non builtin rhythms provided by this RhythmProvider. List can be empty but not null.
     */
    public List<Rhythm> getFileRhythms(boolean forceRescan, MultipleErrorsReport errRpt);

    /**
     * Get the file extensions accepted by readFast().
     * <p>
     * No dot, lowercase.
     *
     * @return E.g. "prs", "sty". Can be an empty list if RhythmProvider has only builtin rhythms.
     */
    public String[] getSupportedFileExtensions();

    /**
     * A fast method to read specified rhythm file and extract only information needed for description/catalog purposes.
     * <p>
     * Caller must use loadResources() on the returned rhythm before using it to generate music (possibly lenghty operation, eg if new file reading required).
     *
     * @param f
     * @return
     * @throws java.io.IOException
     */
    public Rhythm readFast(File f) throws IOException;

    /**
     * Provide a new rhythm which is an adapted version of r for a different time signature.
     * <p>
     *
     * @param r
     * @param ts
     * @return Can be null if no adapted rhythm is available.
     * @throws IllegalArgumentException If ts is already the time signature of r.
     */
    public AdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts);

    /**
     * Show a modal dialog to modify the user settings of this RhythmProvider.
     * <p>
     * The RhythmProvider is responsible for the persistence of its settings. The method does nothing if hasUserSettings() returns false.
     *
     * @see hasUserSettings()
     */
    public void showUserSettingsDialog();

    /**
     * Return true if RhythmProvider has settings which can be modified by end-user.
     * <p>
     *
     * @return @see showUserSettingsDialog()
     */
    public boolean hasUserSettings();

    /**
     * RhythmProvider descriptive information.
     */
    public static class Info
    {

        private String name;
        private String description;
        private String author;
        private String version;
        private String uniqueId;

        /**
         * @param uniqueId
         * @param name        Must be a non empty string (spaces are trimmed).
         * @param description
         * @param author
         * @param version
         */
        public Info(String uniqueId, String name, String description, String author, String version)
        {
            if (uniqueId == null || uniqueId.trim().isEmpty() || name == null || name.trim().isEmpty() || description == null || author == null || version == null)
            {
                throw new IllegalArgumentException(
                        "uniqueId=" + uniqueId + " name=" + name + ", description=" + description + ", author=" + author + ", version=" + version);
            }
            this.uniqueId = uniqueId;
            this.name = name.trim();
            this.description = description;
            this.author = author;
            this.version = version;
        }

        public String getDescription()
        {
            return description;
        }

        public String getName()
        {
            return name;
        }

        public String getAuthor()
        {
            return author;
        }

        public String getVersion()
        {
            return version;
        }

        /**
         * A unique identification string that can be used in serialization to store/retrieve a RhythmProvider instance.
         */
        public String getUniqueId()
        {
            return uniqueId;
        }

        @Override
        public String toString()
        {
            return "RhythmProvider.Info[name=" + name + ", description=" + description + ", author=" + author + ", version=" + version + "]";
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.uniqueId);
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final Info other = (Info) obj;
            if (!Objects.equals(this.uniqueId, other.uniqueId))
            {
                return false;
            }
            return true;
        }

    }

}

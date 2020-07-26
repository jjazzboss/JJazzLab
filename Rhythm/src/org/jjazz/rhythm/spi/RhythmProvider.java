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
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.AdaptedRhythm;

/**
 * An object that can provide Rhythms instances.
 */
public interface RhythmProvider
{

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
     * @return
     */
    public List<Rhythm> getBuiltinRhythms();

    /**
     * Get the file-based rhythms.
     * <p>
     * If prevList is non-null and non-empty, prevList must be used to detect added or removed Rhythm files. They might be
     * hundreds of rhythm files in a directory, so this allows to only parse new added files.
     *
     * @param prevList Can be null.
     * @param forceRescan If true, force rescan of the files, possibly using prevList if not null. If false the method may return
     * cached data.
     * @return All non builtin rhythms provided by this RhythmProvider. List can be empty but not null.
     */
    public List<Rhythm> getFileRhythms(List<Rhythm> prevList, boolean forceRescan);


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
     * Caller must use loadResources() on the returned rhythm before using it to generate music (possibly lenghty operation, eg if
     * new file reading required).
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
     * The RhythmProvider is responsible for the persistence of its settings. The method does nothing if hasUserSettings() returns
     * false.
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
         * @param name Must be a non empty string (spaces are trimmed).
         * @param description
         * @param author
         * @param version
         */
        public Info(String uniqueId, String name, String description, String author, String version)
        {
            if (uniqueId == null || uniqueId.trim().isEmpty() || name == null || name.trim().isEmpty() || description == null || author == null || version == null)
            {
                throw new IllegalArgumentException("uniqueId=" + uniqueId + " name=" + name + ", description=" + description + ", author=" + author + ", version=" + version);
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

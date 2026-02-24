/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.rhythm.spi;

import java.beans.PropertyChangeListener;
import java.io.File;
import org.jjazz.rhythm.DefaultRhythmDirsLocatorImpl;
import org.openide.util.Lookup;

/**
 * Provides the location of the rhythm-related directories
 */
public interface RhythmDirsLocator
{

    static final String DEFAULT_RHYTHMS_SUBDIR = "Default";

    /**
     * A property change event must be fired when rhythm user directory is changed.
     */
    public static final String PROP_RHYTHM_USER_DIRECTORY = "PropRhythmUserDirectory";

    /**
     * Get the default implementation found in the global lookup, or a basic implementation if nothing found.
     *
     * @return
     */
    static public RhythmDirsLocator getDefault()
    {
        var res = Lookup.getDefault().lookup(RhythmDirsLocator.class);
        if (res == null)
        {
            return DefaultRhythmDirsLocatorImpl.getInstance();
        }
        return res;
    }


    /**
     * Get the user base directory for Rhythm files.
     * <p>
     * Create the directory if required.
     *
     * @return An existing directory
     */
    File getUserRhythmsDirectory();

    /**
     * Set the user base directory where to find rhythm files.
     * <p>
     * If changed the method fires a PROP_RHYTHM_USER_DIRECTORY property change event.
     *
     * @param dir Must exist
     */
    void setUserRhythmsDirectory(File dir);

    /**
     * Get the directory for default rhythm files.
     * <p>
     * Default implementation returns getUserRhythmsDirectory()/DEFAULT_RHYTHMS_SUBDIR. Create the directory if required.
     *
     * @return An existing directory
     */
    default File getDefaultRhythmsDirectory()
    {
        var res = new File(getUserRhythmsDirectory(), DEFAULT_RHYTHMS_SUBDIR);
        if (!res.isDirectory() && !res.mkdir())
        {
            throw new IllegalStateException("Impossible to create directory " + res.getAbsolutePath());
        }
        return res;
    }

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}

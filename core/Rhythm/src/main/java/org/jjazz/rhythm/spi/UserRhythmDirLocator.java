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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 * Provides the location of the user rhythm directory.
 */
public interface UserRhythmDirLocator
{

    /**
     * A property change event is fired when rhythm user directory is changed.
     */
    public static final String PROP_RHYTHM_USER_DIRECTORY = "PropRhythmUserDirectory";

    /**
     * Get the default implementation found in the global lookup, or a basic implementation if nothing found.
     *
     * @return
     */
    static public UserRhythmDirLocator getDefault()
    {
        var res = Lookup.getDefault().lookup(UserRhythmDirLocator.class);
        if (res == null)
        {
            return DefaultUserRhythmDirLocator.getInstance();
        }
        return res;
    }


    /**
     * Get the user base directory for Rhythm files.
     * <p>
     *
     * @return Can't be null
     */
    public File getUserRhythmDirectory();

    /**
     * Set the user base directory where to find rhythm files.
     *
     * @param dir
     */
    public void setUserRhythmDirectory(File dir);


    //===================================================================================================
    // Inner classes
    //===================================================================================================    
    /**
     * A default implementation which uses the System property "user.home" by default.
     * <p>
     * Location is saved as preferences.
     */
    static class DefaultUserRhythmDirLocator implements UserRhythmDirLocator
    {

        private static DefaultUserRhythmDirLocator INSTANCE;
        protected final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
        private static final Logger LOGGER = Logger.getLogger(DefaultUserRhythmDirLocator.class.getSimpleName());


        private static UserRhythmDirLocator getInstance()
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DefaultUserRhythmDirLocator();
            }
            return INSTANCE;
        }


        @Override
        public File getUserRhythmDirectory()
        {
            String uh = System.getProperty("user.home");
            assert uh != null;
            String s = getPreferences().get(PROP_RHYTHM_USER_DIRECTORY, uh);
            File directory = new File(s);
            if (!s.equals(uh) && !directory.isDirectory())
            {
                LOGGER.log(Level.WARNING, "DefaultUserRhythmDirLocator() User rhythm directory not found: {0} Using: {1} instead.", new Object[]
                {
                    s,
                    uh
                });
                directory = new File(uh);
            }
            LOGGER.log(Level.FINE, "DefaultUserRhythmDirLocator() directory={0}", directory);

            return directory;
        }

        @Override
        public void setUserRhythmDirectory(File dir)
        {
            Objects.requireNonNull(dir);
            if (!dir.isDirectory())
            {
                throw new IllegalArgumentException("dir=" + dir);
            }
            File old = getUserRhythmDirectory();
            getPreferences().put(PROP_RHYTHM_USER_DIRECTORY, dir.getAbsolutePath());
            LOGGER.log(Level.FINE, "setUserRhythmDirectory() old={0} new={1}", new Object[]
            {
                old, dir
            });
            pcs.firePropertyChange(PROP_RHYTHM_USER_DIRECTORY, old, dir);
        }

        public void addPropertyChangeListener(PropertyChangeListener l)
        {
            pcs.addPropertyChangeListener(l);
        }

        public void removePropertyChangeListener(PropertyChangeListener l)
        {
            pcs.removePropertyChangeListener(l);
        }

        /**
         * Subclass may override the preferences used.
         *
         * @return
         */
        protected Preferences getPreferences()
        {
            return NbPreferences.forModule(DefaultUserRhythmDirLocator.class);
        }
    }
}

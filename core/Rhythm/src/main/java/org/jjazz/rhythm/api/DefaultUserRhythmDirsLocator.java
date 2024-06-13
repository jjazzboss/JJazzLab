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
package org.jjazz.rhythm.api;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.openide.util.NbPreferences;

/**
 * A default implementation.
 * <p>
 * Location is saved as preferences.
 */
public class DefaultUserRhythmDirsLocator implements RhythmDirsLocator
{

    private static DefaultUserRhythmDirsLocator INSTANCE;
    private static final String DEFAULT_USER_RHYTHM_DIR_PATH = System.getProperty("user.home") + "/JJazzLabRhythms";
    protected final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(DefaultUserRhythmDirsLocator.class.getSimpleName());

    static public RhythmDirsLocator getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new DefaultUserRhythmDirsLocator();
        }
        return INSTANCE;
    }

    /**
     * Use the System property "user.home"+USER_RHYTHM_DIR by default.
     *
     * @return
     */
    @Override
    public File getUserRhythmsDirectory()
    {
        String s = getPreferences().get(PROP_RHYTHM_USER_DIRECTORY, DEFAULT_USER_RHYTHM_DIR_PATH);
        File directory = new File(s);
        if (!directory.isDirectory() && !directory.mkdir())
        {
            LOGGER.log(Level.WARNING, "getUserRhythmsDirectory() User rhythm directory not found: {0} Using: {1} instead.", new Object[]
            {
                s, DEFAULT_USER_RHYTHM_DIR_PATH
            });
            directory = new File(DEFAULT_USER_RHYTHM_DIR_PATH);
            if (!directory.isDirectory())
            {
                throw new IllegalStateException("Can't find a valid user rhythms directory. DEFAULT_USER_RHYTHM_DIR_PATH=" + DEFAULT_USER_RHYTHM_DIR_PATH);
            }
        }

        LOGGER.log(Level.FINE, "getUserRhythmsDirectory() directory={0}", directory);
        return directory;
    }

    @Override
    public void setUserRhythmsDirectory(File dir)
    {
        Objects.requireNonNull(dir);
        if (!dir.isDirectory())
        {
            throw new IllegalArgumentException("dir=" + dir);
        }
        File old = getUserRhythmsDirectory();
        getPreferences().put(PROP_RHYTHM_USER_DIRECTORY, dir.getAbsolutePath());
        LOGGER.log(Level.INFO, "setUserRhythmDirectory() old={0} new={1}", new Object[]
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
        return NbPreferences.forModule(DefaultUserRhythmDirsLocator.class);
    }

}

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

/**
 * A parameterized implementation of the RhythmDirsLocator interface.
 * <p>
 */
public class RhythmDirsLocatorImpl implements RhythmDirsLocator
{
    private static final String PREF_RHYTHM_USER_DIRECTORY = "PrefRhythmUserDirectory";

    private final Preferences prefs;
    private final String defaultUserRhythmDir;
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(RhythmDirsLocatorImpl.class.getSimpleName());

    /**
     *
     * @param defaultUserRhythmDir Path to the default user rhythm directory.
     * @param prefs                Preferences to store a customized user rhythm directory
     */
    public RhythmDirsLocatorImpl(String defaultUserRhythmDir, Preferences prefs)
    {
        Objects.requireNonNull(defaultUserRhythmDir);
        Objects.requireNonNull(prefs);
        this.prefs = prefs;
        this.defaultUserRhythmDir = defaultUserRhythmDir;
    }

    @Override
    public File getUserRhythmsDirectory()
    {
        File res;
        String s = prefs.get(PREF_RHYTHM_USER_DIRECTORY, null);

        if (s == null)
        {
            res = new File(defaultUserRhythmDir);
            if (!res.isDirectory() && !res.mkdir())
            {
                throw new IllegalStateException("Can't find a valid user rhythms directory. defaultUserRhythmDir=" + defaultUserRhythmDir);
            }
        } else
        {
            res = new File(s);
            if (!res.isDirectory() && !res.mkdir())
            {
                prefs.remove(PREF_RHYTHM_USER_DIRECTORY);
                LOGGER.log(Level.WARNING, "getUserRhythmsDirectory() Can''t create user rhythms directory ''{0}''. Using ''{1}'' instead.", new Object[]
                {
                    s, defaultUserRhythmDir
                });
                res = new File(defaultUserRhythmDir);
                if (!res.isDirectory() && !res.mkdir())
                {
                    throw new IllegalStateException("Can't find a valid user rhythms directory. defaultUserRhythmDir=" + defaultUserRhythmDir);
                }
            }
        }

        LOGGER.log(Level.FINE, "getUserRhythmsDirectory() directory={0}", res);
        return res;
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
        prefs.put(PREF_RHYTHM_USER_DIRECTORY, dir.getAbsolutePath());
        LOGGER.log(Level.INFO, "setUserRhythmDirectory() old={0} new={1}", new Object[]
        {
            old, dir
        });
        pcs.firePropertyChange(PROP_RHYTHM_USER_DIRECTORY, old, dir);

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }
}

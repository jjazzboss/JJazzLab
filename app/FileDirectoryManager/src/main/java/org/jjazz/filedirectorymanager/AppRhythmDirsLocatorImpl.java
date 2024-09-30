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
package org.jjazz.filedirectorymanager;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.rhythm.api.RhythmDirsLocatorImpl;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * Application's implementation of RhythmDirsLocator.
 */
@ServiceProvider(service = RhythmDirsLocator.class)
public class AppRhythmDirsLocatorImpl extends RhythmDirsLocatorImpl
{

    private static final Logger LOGGER = Logger.getLogger(AppRhythmDirsLocatorImpl.class.getSimpleName());
    private static final Preferences prefs = NbPreferences.forModule(AppRhythmDirsLocatorImpl.class);


    public AppRhythmDirsLocatorImpl()
    {
        super(getDefaultUserRhythmsDir(), prefs);
        LOGGER.info("AppRhythmDirsLocatorImpl() Started");
    }


    /**
     * Use AppConfigDirectory + DEFAULT_RHYTHMS_SUBDIR.
     *
     * @return
     */
    @Override
    public File getDefaultRhythmsDirectory()
    {
        var res = FileDirectoryManager.getInstance().getAppConfigDirectory(DEFAULT_RHYTHMS_SUBDIR);
        if (!res.isDirectory() && !res.mkdir())
        {
            throw new IllegalStateException("Impossible to create directory " + res.getAbsolutePath());
        }
        return res;
    }


    /**
     * 
     * @return JJazzLab user directory + "/Rhythms"
     */
    static private String getDefaultUserRhythmsDir()
    {
        String res;
        File jjDir = FileDirectoryManager.getInstance().getJJazzLabUserDirectory();
        assert jjDir != null;
        File dir = new File(jjDir, "Rhythms");
        if (!dir.isDirectory() && !dir.mkdir())
        {
            LOGGER.log(Level.WARNING, "AppRhythmDirsLocator() Can''t create user rhythm directory: {0} Using: {1} instead.", new Object[]
            {
                dir.getAbsolutePath(), jjDir.getAbsolutePath()
            });
            res = jjDir.getAbsolutePath();
        } else
        {
            res = dir.getAbsolutePath();
        }
        return res;
    }
}

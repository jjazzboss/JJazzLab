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
package org.jjazz.rhythm;

import java.util.prefs.Preferences;
import org.jjazz.rhythm.api.RhythmDirsLocatorImpl;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.openide.util.NbPreferences;

/**
 * Default implementation.
 * <p>
 */
public class DefaultRhythmDirsLocatorImpl extends RhythmDirsLocatorImpl
{

    private static DefaultRhythmDirsLocatorImpl INSTANCE;
    private static final Preferences prefs = NbPreferences.forModule(DefaultRhythmDirsLocatorImpl.class);

    static public RhythmDirsLocator getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new DefaultRhythmDirsLocatorImpl();
        }
        return INSTANCE;
    }

    private DefaultRhythmDirsLocatorImpl()
    {
        super(getDefaultUserRhythmsDir(), prefs);
    }

    /**
     * @return "user.home" system property + "/JJazzLabRhythms"
     */
    static private String getDefaultUserRhythmsDir()
    {
        return System.getProperty("user.home") + "/" + "JJazzLabRhythms";
    }
}

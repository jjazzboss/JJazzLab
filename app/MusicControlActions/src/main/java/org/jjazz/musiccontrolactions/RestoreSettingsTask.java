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
package org.jjazz.musiccontrolactions;

import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * Import properties when upgrading.
 */
@ServiceProvider(service = UpgradeTask.class)
public class RestoreSettingsTask implements UpgradeTask
{

    @Override
    public void upgrade(String oldVersion)
    {
        if (oldVersion == null)
        {
            return;
        }
        UpgradeManager um = UpgradeManager.getInstance();
        if (oldVersion.charAt(0) <= '3')
        {
            // package codebase has changed from JJazzLab 3 to JJazzLab 4: org/jjazz/ui/musiccontrolactions => org/jjazzlab/musiccontrolactions (4.0.3)
            um.duplicateOldPreferences(NbPreferences.forModule(getClass()), "org/jjazz/ui/musiccontrolactions.properties");
        } else
        {
            um.duplicateOldPreferences(NbPreferences.forModule(getClass()));
        }
    }
    
}

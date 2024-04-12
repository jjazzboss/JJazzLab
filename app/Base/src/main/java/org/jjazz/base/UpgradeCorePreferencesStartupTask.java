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
package org.jjazz.base;

import java.util.logging.*;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;


/**
 * Upgrade the preferences of JJazzLab core components (which do not have access to the UpgradeManager/UpgradeTask service).
 */
@ServiceProvider(service = UpgradeTask.class)
public class UpgradeCorePreferencesStartupTask implements UpgradeTask
{

    private static final Logger LOGGER = Logger.getLogger(UpgradeCorePreferencesStartupTask.class.getSimpleName());

    @Override
    public void upgrade(String oldVersion)
    {
        UpgradeManager um = UpgradeManager.getInstance();

        // ChordTypeDatabase
        var prefs = NbPreferences.forModule(ChordTypeDatabase.class);
        um.duplicateOldPreferences(prefs);


        // JJazzMidiSystem
        prefs = NbPreferences.forModule(JJazzMidiSystem.class);
        um.duplicateOldPreferences(prefs);

        // PlaybackSettings
        prefs = NbPreferences.forModule(PlaybackSettings.class);
        um.duplicateOldPreferences(prefs);
    }

}

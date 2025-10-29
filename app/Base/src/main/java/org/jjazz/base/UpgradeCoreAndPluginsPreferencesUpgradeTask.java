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
import org.jjazz.flatcomponents.api.FlatComponentsGlobalSettings;
import org.jjazz.fluidsynthembeddedsynth.api.FluidSynthEmbeddedSynth;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.jjswing.api.JJSwingRhythm;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.outputsynth.api.DefaultOutputSynthManager;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * Upgrade the preferences of JJazzLab core or plugins components (which do not have access to the UpgradeManager/UpgradeTask service).
 */
@ServiceProvider(service = UpgradeTask.class)
public class UpgradeCoreAndPluginsPreferencesUpgradeTask implements UpgradeTask
{

    private static final Logger LOGGER = Logger.getLogger(UpgradeCoreAndPluginsPreferencesUpgradeTask.class.getSimpleName());

    @Override
    public void upgrade(String oldVersion)
    {
        if (oldVersion == null)
        {
            return;
        }

        UpgradeManager um = UpgradeManager.getInstance();

        // ChordTypeDatabase
        var prefs = NbPreferences.forModule(ChordTypeDatabase.class);
        um.duplicateOldPreferences(prefs);


        // OutputSynthManager        
        if (oldVersion.charAt(0) >= '4')
        {
            prefs = NbPreferences.forModule(DefaultOutputSynthManager.class);
            um.duplicateOldPreferences(prefs);
        }


        // JJazzMidiSystem
        prefs = NbPreferences.forModule(JJazzMidiSystem.class);
        um.duplicateOldPreferences(prefs);


        // PlaybackSettings
        prefs = NbPreferences.forModule(PlaybackSettings.class);
        um.duplicateOldPreferences(prefs);


        // FlatComponentsGlobalSettings
        prefs = NbPreferences.forModule(FlatComponentsGlobalSettings.class);
        if (oldVersion.compareTo("4.1.0") < 0)
        {
            // Before 4.1.0, this setting was in module uisettings
            String adaptedRelPath = um.adaptPropertiesFileRelativePath("org/jjazzlab/uisettings.properties");
            um.duplicateOldPreferences(prefs, adaptedRelPath);
        } else
        {
            um.duplicateOldPreferences(prefs);
        }

        // FluidSynthEmbeddedSynth        
        if (oldVersion.charAt(0) >= '4')
        {
            prefs = NbPreferences.forModule(FluidSynthEmbeddedSynth.class);
            um.duplicateOldPreferences(prefs);
        }


        // jjSwing
        prefs = NbPreferences.forModule(JJSwingRhythm.class);
        um.duplicateOldPreferences(prefs);
    }

}

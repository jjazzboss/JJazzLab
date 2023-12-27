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
package org.jjazz.embeddedsynth;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.modules.OnStart;
import org.openide.util.*;

/**
 * Manage the fresh startup case and restore EmbeddedSynth active state upon application restarts.
 */
@OnStart
public class StartupEmbeddedSynthInitTask implements Runnable
{

    private static final String PREF_EMBEDDED_SYNTH_ACTIVATED = "PrefEmbeddedSynthActivated";
    private static final String PREF_FLUIDSYNTH_DISABLED_WARNING_SHOWN = "PrefFluidSynthDisabledWarningShown";
    private static final Preferences prefs = NbPreferences.forModule(JJazzMidiSystem.class);
    private static final Logger LOGGER = Logger.getLogger(StartupEmbeddedSynthInitTask.class.getSimpleName());

    @Override
    public void run()
    {

        var um = UpgradeManager.getInstance();
        var provider = EmbeddedSynthProvider.getDefaultProvider();
        if (provider == null || !provider.isEnabled())
        {
            showOnceNoFluidSynthWarning();
            return;
        } else
        {
            // Save the EmbeddedSynth active status
            provider.addPropertyChangeListener(e -> prefs.putBoolean(PREF_EMBEDDED_SYNTH_ACTIVATED, (Boolean) e.getNewValue()));
        }

        boolean activate = um.isFreshStart() || prefs.getBoolean(PREF_EMBEDDED_SYNTH_ACTIVATED, false);

        if (activate)
        {
            try
            {
                provider.setEmbeddedSynthActive(true);
                prefs.putBoolean(PREF_FLUIDSYNTH_DISABLED_WARNING_SHOWN, false);
            } catch (EmbeddedSynthException ex)
            {
                LOGGER.log(Level.WARNING, "run() Can''t activate embedded synth: {0}", ex.getMessage());
                showOnceNoFluidSynthWarning();
            }
        }
    }

    private void showOnceNoFluidSynthWarning()
    {
        if (!prefs.getBoolean(PREF_FLUIDSYNTH_DISABLED_WARNING_SHOWN, false))
        {
            String msg = ResUtil.getString(getClass(), "FluidSynthNotAvailable");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            prefs.putBoolean(PREF_FLUIDSYNTH_DISABLED_WARNING_SHOWN, true);
        }
    }
}

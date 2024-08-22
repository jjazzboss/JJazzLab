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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.startup.api.OnStartMessageNotifier;
import org.jjazz.startup.spi.OnStartTask;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.*;
import org.openide.util.lookup.ServiceProvider;

/**
 * Manage the fresh startup case and restore EmbeddedSynth active state upon application restarts.
 * <p>
 * Should be done as early as possible since synth initialization (eg load soundfont) might take some time.
 */
@ServiceProvider(service = OnStartTask.class)
public class InitEmbeddedSynthOnStartTask implements OnStartTask
{

    public static final int ONSTART_TASK_PRIORITY = 75;
    private static final String PREF_EMBEDDED_SYNTH_ACTIVATED = "PrefEmbeddedSynthActivated";
    private static final String PREF_FLUIDSYNTH_DISABLED_WARNING_SHOWN = "PrefFluidSynthDisabledWarningShown";
    private static final Preferences prefs = NbPreferences.forModule(JJazzMidiSystem.class);
    private static final Logger LOGGER = Logger.getLogger(InitEmbeddedSynthOnStartTask.class.getSimpleName());

    @Override
    public void run()
    {

        var um = UpgradeManager.getInstance();
        var provider = EmbeddedSynthProvider.getDefaultProvider();
        if (provider == null || !provider.isEnabled())
        {
            showOnceNoFluidSynthWarning();
            return;
        }


        // EmbeddedSynth is available
        // Make sure to always save the EmbeddedSynth active status as a preference
        provider.addPropertyChangeListener(e -> prefs.putBoolean(PREF_EMBEDDED_SYNTH_ACTIVATED, (Boolean) e.getNewValue()));


        // Force activate upon fresh start only if current Midi OUT is not defined or it's a crappy one...
        var md = JJazzMidiSystem.getInstance().getDefaultOutDevice();
        var mdName = md == null ? null : md.getDeviceInfo().getName().toLowerCase();
        boolean freshStartActivate = um.isFreshStart() && (mdName == null || mdName.contains("java") || mdName.contains("microsoft") || mdName.contains("gervil"));

        boolean activate = prefs.getBoolean(PREF_EMBEDDED_SYNTH_ACTIVATED, false) || freshStartActivate;
        
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
            OnStartMessageNotifier.postErrorMessage(msg);
            prefs.putBoolean(PREF_FLUIDSYNTH_DISABLED_WARNING_SHOWN, true);
        }
    }

    @Override
    public int getPriority()
    {
        return ONSTART_TASK_PRIORITY;
    }

    @Override
    public String getName()
    {
        return "InitEmbeddedSynthStartupTask";
    }
}

/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2025 Jerome Lelasseux. All rights reserved.
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
package org.jjazz.musiccontrolactions.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrolactions.ui.QualityNotificationDialog;
import org.jjazz.outputsynth.spi.OutputSynthManager;
import org.jjazz.startup.spi.OnStartTask;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * Shows a dialog warning when music is going to be played and a low quality synth is being used.
 */
@ServiceProvider(service = OnStartTask.class)
public class RegisterLowQualityTask implements OnStartTask, PropertyChangeListener
{
    public static final int ONSTART_TASK_PRIORITY = 100;

    private static final Preferences prefs = NbPreferences.forModule(RegisterLowQualityTask.class);
    private static final String PREF_SHOW_NOTIFICATION = "ShowSynthQualityNotification";

    /**
     * Registers itself to listen for changes in MusicController's state.
     */
    @Override
    public void run() {
        var mc = MusicController.getInstance();
        mc.addPropertyChangeListener(this);
    }

    @Override
    public int getPriority() {
        return ONSTART_TASK_PRIORITY;
    }

    @Override
    public String getName() {
        return "RegisterLowQualityTask";
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        if (
                prefs.getBoolean(PREF_SHOW_NOTIFICATION, true)
                && MusicController.PROP_STATE.equals(event.getPropertyName())
                && event.getNewValue().equals(MusicController.State.PLAYING)
            )
        {
            if (isSynthLowQuality())
            {
                Runnable r = () -> 
                {
                    QualityNotificationDialog dlg = new QualityNotificationDialog(WindowManager.getDefault().getMainWindow(), true);
                    dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                    dlg.setVisible(true);
                    if (dlg.isDoNotShowAgain())
                    {
                        prefs.putBoolean(PREF_SHOW_NOTIFICATION, false);
                    }
                };
                SwingUtilities.invokeLater(r);
            }
        }
    }

    private boolean isSynthLowQuality()
    {
        // Check that fluidsynth in use
        var osm = OutputSynthManager.getDefault();
        var dos = osm.getDefaultOutputSynth();

        return !dos.getMidiSynth().getName().equalsIgnoreCase("FluidSynth");
    }
}

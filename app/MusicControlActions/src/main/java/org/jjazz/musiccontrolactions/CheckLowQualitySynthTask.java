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
package org.jjazz.musiccontrolactions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiDevice;
import javax.swing.SwingUtilities;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.startup.spi.OnStartTask;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * Shows a dialog warning when music is going to be played and a low quality synth is being used.
 */
@ServiceProvider(service = OnStartTask.class)
public class CheckLowQualitySynthTask implements OnStartTask, PropertyChangeListener
{

    public static final int ONSTART_TASK_PRIORITY = 100;

    private static final Preferences prefs = NbPreferences.forModule(CheckLowQualitySynthTask.class);
    private static final String PREF_SHOW_NOTIFICATION = "ShowSynthQualityNotification";

    /**
     * Registers itself to listen for changes in MusicController's state.
     */
    @Override
    public void run()
    {
        var mc = MusicController.getInstance();
        mc.addPropertyChangeListener(this);
    }

    @Override
    public int getPriority()
    {
        return ONSTART_TASK_PRIORITY;
    }

    @Override
    public String getName()
    {
        return "CheckLowQualitySynthTask";
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        if (prefs.getBoolean(PREF_SHOW_NOTIFICATION, true)
                && MusicController.PROP_STATE.equals(event.getPropertyName())
                && event.getNewValue().equals(MusicController.State.PLAYING))
        {

            var md = JJazzMidiSystem.getInstance().getDefaultOutDevice();

            if (isSynthLowQuality(md))
            {
                Runnable r = () -> 
                {
                    String mdName = JJazzMidiSystem.getInstance().getDeviceFriendlyName(md);
                    CheckLowQualitySynthDialog dlg = new CheckLowQualitySynthDialog(WindowManager.getDefault().getMainWindow(), true, mdName);
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

    private boolean isSynthLowQuality(MidiDevice md)
    {
        return md == JJazzMidiSystem.getInstance().getJavaInternalSynth()
                || md.getDeviceInfo().getName().toLowerCase().startsWith("microsoft");
    }
}

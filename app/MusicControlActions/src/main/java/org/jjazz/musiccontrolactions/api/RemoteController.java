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
package org.jjazz.musiccontrolactions.api;

import org.jjazz.musiccontrolactions.spi.RemoteActionProvider;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.swing.SwingUtilities;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.windows.OnShowing;

/**
 * Manage the Midi IN remote control playback operations.
 */
public class RemoteController
{

    public static final String PREF_ENABLED = "PrefEnabled";
    private static RemoteController INSTANCE = null;
    private final List<RemoteAction> remoteActions;
    private boolean isEnabled;

    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Preferences prefs = NbPreferences.forModule(RemoteController.class);
    private static final Logger LOGGER = Logger.getLogger(RemoteController.class.getSimpleName());

    static public RemoteController getInstance()
    {
        synchronized (RemoteController.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RemoteController();
            }
        }
        return INSTANCE;
    }

    private RemoteController()
    {
        isEnabled = prefs.getBoolean(PREF_ENABLED, false);


        // Get all the RemoteAction instances from providers
        remoteActions = new ArrayList<>();
        for (var rap : Lookup.getDefault().lookupAll(RemoteActionProvider.class))
        {
            remoteActions.addAll(rap.getRemoteActions());
        }


        JJazzMidiSystem.getInstance().getJJazzMidiInDevice().getTransmitter().setReceiver(new RemoteControlReceiver());
    }

    final public boolean isEnabled()
    {
        return isEnabled;
    }

    public void setEnabled(boolean b)
    {
        boolean old = isEnabled;
        if (old != b)
        {
            isEnabled = b;
            prefs.putBoolean(PREF_ENABLED, isEnabled);
            pcs.firePropertyChange(PREF_ENABLED, !isEnabled, isEnabled);
            LOGGER.log(Level.INFO, "setEnabled() enabled={0}", isEnabled);
        }
    }

    /**
     * Get all the RemoteAction instances.
     *
     * @return
     */
    public final List<RemoteAction> getRemoteActions()
    {
        return new ArrayList<>(remoteActions);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }
    // ==============================================================
    // Private methods
    // ==============================================================   

    // ==============================================================
    // Public classes
    // ==============================================================   
    @OnShowing
    public static class StartClass implements Runnable
    {

        @Override
        public void run()
        {
            // Create the instance
            RemoteController.getInstance();
        }

    }

    // ===========================================================================================
    // Private classes
    // ===========================================================================================
    private class RemoteControlReceiver implements Receiver
    {

        @Override
        public void send(MidiMessage msg, long timeStamp)
        {
            if (!isEnabled)
            {
                return;
            }

            for (RemoteAction ra : remoteActions)
            {
                if (ra.check(msg))
                {
                    Runnable r = () ->
                    {
                        var a = ra.getAction();
                        if (a.isEnabled())
                        {
                            ra.getAction().actionPerformed(null);
                        }
                    };
                    SwingUtilities.invokeLater(r);
                }
            }

        }

        @Override
        public void close()
        {
            // Nothing
        }

    }

}

/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.ui.musiccontrolactions.api;

import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.SwingUtilities;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.ui.musiccontrolactions.Play;
import org.jjazz.ui.musiccontrolactions.Stop;
import org.openide.awt.Actions;
import org.openide.util.NbPreferences;
import org.openide.windows.OnShowing;

/**
 * Manage the Midi IN remote control playback operations.
 */
public class RemoteController
{

    private static RemoteController INSTANCE = null;
    public final static String PREF_ENABLED = "RemoteEnabled";
    public final static String PREF_START_PAUSE_NOTE = "StartPauseNote";
    public final static String PREF_STOP_NOTE = "StopNote";

    // Use local variables to duplicate preference values in order to speed up processing in our Midi Receiver
    private boolean isEnabled;
    private int startPauseNote, stopNote;
    private final Play playAction;
    private final Stop stopAction;
    private static Preferences prefs = NbPreferences.forModule(RemoteController.class);
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
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
        startPauseNote = MidiUtilities.limit(prefs.getInt(PREF_START_PAUSE_NOTE, 24));
        stopNote = MidiUtilities.limit(prefs.getInt(PREF_STOP_NOTE, 26));

        LOGGER.info("RemoteController() enabled=" + isEnabled + " startPauseNote=" + startPauseNote + " stopNote=" + stopNote);

        playAction = (Play) Actions.forID("MusicControls", "org.jjazz.ui.musiccontrolactions.play");
        assert playAction != null;
        stopAction = (Stop) Actions.forID("MusicControls", "org.jjazz.ui.musiccontrolactions.stop");
        assert stopAction != null;


        JJazzMidiSystem.getInstance().getJJazzMidiInDevice().getTransmitter().setReceiver(new RemoteControlReceiver());
    }

    final public boolean isEnabled()
    {
        return isEnabled;
    }

    synchronized public void setEnabled(boolean b)
    {
        boolean old = isEnabled;
        if (old != b)
        {
            isEnabled = b;
            prefs.putBoolean(PREF_ENABLED, isEnabled);
            pcs.firePropertyChange(PREF_ENABLED, !isEnabled, isEnabled);
            LOGGER.info("setEnabled() enabled=" + isEnabled);
        }
    }

    public int getStartPauseNote()
    {
        return startPauseNote;
    }

    synchronized public void setStartPauseNote(int pitch)
    {
        int old = startPauseNote;
        if (old != pitch)
        {
            startPauseNote = MidiUtilities.limit(pitch);
            prefs.putInt(PREF_START_PAUSE_NOTE, startPauseNote);
            pcs.firePropertyChange(PREF_START_PAUSE_NOTE, old, startPauseNote);
            LOGGER.info("setStartNote() startPauseNote=" + startPauseNote);
        }
    }

    public int getStopNote()
    {
        return stopNote;
    }

    synchronized public void setStopNote(int pitch)
    {
        int old = stopNote;
        if (old != pitch)
        {
            stopNote = MidiUtilities.limit(pitch);
            prefs.putInt(PREF_STOP_NOTE, stopNote);
            pcs.firePropertyChange(PREF_STOP_NOTE, old, stopNote);
            LOGGER.info("setStartNote() stopNote=" + stopNote);
        }
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

            if (msg instanceof ShortMessage)
            {
                ShortMessage sm = (ShortMessage) msg;
                if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0)
                {
                    Runnable r = null;
                    int pitch = sm.getData1();
                    if (pitch == startPauseNote)
                    {
                        r = () -> playAction.actionPerformed(null);
                    } else if (pitch == stopNote)
                    {
                        r = () -> stopAction.setSelected(true);
                    }

                    if (r != null)
                    {
                        SwingUtilities.invokeLater(r);
                    }
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

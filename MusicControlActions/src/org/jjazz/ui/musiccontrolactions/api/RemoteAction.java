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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.Action;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.progress.BaseProgressUtils;
import org.netbeans.api.progress.ProgressRunnable;
import org.openide.awt.Actions;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

/**
 * A remote action stores the list of MidiMessages which can trigger an action.
 */
public class RemoteAction
{

    private final String actionId;
    private final String actionCategory;
    private List<MidiMessage> midiMessages;
    private final Action action;
    private int validIndex = 0;
    private boolean enabled = true;
    private static final Preferences prefs = NbPreferences.forModule(RemoteAction.class);
    private static final Logger LOGGER = Logger.getLogger(RemoteAction.class.getSimpleName());

    /**
     * Create a RemoteAction with no MidiMessage.
     *
     * @param actionCategory Must be a valid JJazzLab/Netbeans action category.
     * @param actionId Must be a valid JJazzLab/Netbeans action id.
     * @throws IllegalArgumentException If parameters do not represent a valid JJazzLab action.
     */
    public RemoteAction(String actionCategory, String actionId)
    {
        checkNotNull(actionId);
        checkNotNull(actionCategory);
        checkArgument(!actionId.isBlank(), "actionId=%s", actionId);
        checkArgument(!actionCategory.isBlank(), "actionCategory=%s", actionCategory);

        this.actionId = actionId;
        this.actionCategory = actionCategory;
        action = Actions.forID(actionCategory, actionId);
        if (action == null)
        {
            throw new IllegalArgumentException("No Netbeans action found for actionCategory=" + actionCategory + " actionId=" + actionId);
        }
    }

    /**
     * The associated action.
     *
     * @return Can't be null
     */
    public Action getAction()
    {
        return action;
    }

    /**
     * Save this RemoteAction as a preference.
     * <p>
     * Do nothing of this instance has no MidiMessages defined.
     */
    public void saveAsPreference()
    {
        if (midiMessages != null && !midiMessages.isEmpty())
        {
            prefs.put(getPrefMidiMessagesKey(actionCategory, actionId), MidiUtilities.saveMidiMessagesAsString(midiMessages));
            prefs.putBoolean(getPrefEnabledKey(actionCategory, actionId), enabled);
        }
    }

    /**
     * Try to create an instance from the saved preferences.
     *
     * @param actionCategory
     * @param actionId
     * @return Null if no instance found for the specified parameters.
     */
    static public RemoteAction loadFromPreference(String actionCategory, String actionId)
    {
        String s = prefs.get(getPrefMidiMessagesKey(actionCategory, actionId), null);
        if (s == null)
        {
            return null;
        }

        try
        {
            var mms = MidiUtilities.loadMidiMessagesFromString(s);      // Throws exceptions

            RemoteAction res = new RemoteAction(actionCategory, actionId);
            res.setMidiMessages(mms);
            res.setEnabled(prefs.getBoolean(getPrefEnabledKey(actionCategory, actionId), true));
            return res;

        } catch (ParseException | InvalidMidiDataException ex)
        {
            LOGGER.warning("loadFromPreference() Invalid save string: " + s);
            return null;
        }
    }

    /**
     * Start a Midi learn session.
     * <p>
     * Listen during timeOutMs Midi input to acquire MidiMessages that will trigger this action.
     *
     * @param timeOutMs In milliseconds.
     * @return True If new MidiMessages were learnt.
     */
    public boolean startMidiLearnSession(int timeOutMs)
    {

        // Prepare our receiver
        var jms = JJazzMidiSystem.getInstance();
        var transmitter = jms.getJJazzMidiInDevice().getTransmitter();
        var receiver = new MidiLearnReceiver();
        transmitter.setReceiver(receiver);

        // Show a progress dialog during timeOutMs
        ProgressRunnable<?> pr = ph ->
        {
            try
            {
                int steps = 4;
                ph.start(steps);
                while (steps > 0)
                {
                    Thread.sleep(timeOutMs / steps);
                    ph.progress(1);
                    steps--;
                }
                ph.finish();
            } catch (InterruptedException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
            }
            return null;
        };
        BaseProgressUtils.showProgressDialogAndRun(pr, ResUtil.getString(getClass(), "MidiLearnOn"), true);


        receiver.close();
        transmitter.close();

        var learntMessages = receiver.getLearntMessages();
        if (!learntMessages.isEmpty())
        {
            midiMessages = learntMessages;
            return true;
        }

        return false;
    }

    /**
     *
     * @return The control note pitch if MidiMessages correspond to a single Note_ON ShortMessage. -1 otherwise.
     * @see #setMidiMessages(int, int)
     */
    public int getControlNotePitch()
    {
        if (midiMessages == null || midiMessages.size() != 1)
        {
            return -1;
        }
        ShortMessage sm = MidiUtilities.getNoteOnMidiEvent(midiMessages.get(0));
        return sm == null ? -1 : sm.getData1();
    }

    /**
     *
     * @return The control note channel if MidiMessages correspond to a single Note_ON ShortMessage. -1 otherwise.
     * @see #setMidiMessages(int, int)
     */
    public int getControlNoteChannel()
    {
        if (midiMessages == null || midiMessages.size() != 1)
        {
            return -1;
        }
        ShortMessage sm = MidiUtilities.getNoteOnMidiEvent(midiMessages.get(0));
        return sm == null ? -1 : sm.getChannel();
    }

    public String getActionCategory()
    {
        return actionCategory;
    }

    public String getActionId()
    {
        return actionId;
    }

    public List<MidiMessage> getMidiMessages()
    {
        return midiMessages;
    }

    /**
     * Set the MidiMessages so that a single NOTE_ON with specified channel and pitch triggers the action.
     *
     * @param channel
     * @param pitch
     */
    public void setMidiMessages(int channel, int pitch)
    {
        setMidiMessages(noteOnMidiMessages(channel, pitch));
    }

    /**
     * Set the MidiMessages which trigger this action when received.
     * <p>
     *
     * @param messages
     */
    public synchronized void setMidiMessages(List<MidiMessage> messages)
    {
        this.midiMessages = messages;
        this.validIndex = 0;
    }

    public synchronized boolean isEnabled()
    {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Check if this MidiMessage should trigger this RemoteAction.
     * <p>
     * Comparison is done according to the following rules:<br>
     * - The number and the order of messages order must match.<br>
     * - Velocity is ignored on Note_ON/OFF messages comparison.<br>
     * <p>
     * Return false if RemoteAction is not enabled.
     *
     * @param mm
     * @return True if this message comparison is the last successful comparison with each of the reference MidiMessages of this
     * RemoteAction.
     */
    public boolean check(MidiMessage mm)
    {
        MidiMessage mmRef;
        synchronized (this)
        {
            if (!isEnabled())
            {
                return false;
            }

            if (midiMessages == null || midiMessages.isEmpty())
            {
                return false;
            }
            mmRef = midiMessages.get(validIndex);
        }


        if (compare(mmRef, mm))
        {
            validIndex++;
            if (validIndex == midiMessages.size())
            {
                validIndex = 0;
                return true;
            }
        } else if (validIndex > 0 && compare(midiMessages.get(0), mm))
        {
            validIndex = 1;
        } else
        {
            validIndex = 0;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return actionCategory + "#" + actionId + " msgs=" + midiMessages + " enabled=" + enabled;
    }

    // ================================================================================================
    // Private methods
    // ================================================================================================
    private boolean compare(MidiMessage mmRef, MidiMessage mm)
    {
        if (mmRef.getClass() != mm.getClass())
        {
            return false;
        }

        if (mmRef instanceof ShortMessage)
        {
            ShortMessage smRef = (ShortMessage) mmRef;
            ShortMessage sm = (ShortMessage) mm;
            if (smRef.getCommand() != sm.getCommand() || smRef.getChannel() != sm.getChannel())
            {
                return false;
            }

            if (smRef.getCommand() == ShortMessage.NOTE_ON)
            {
                // Ignore velocity in the comparison, except if it's a NOTE_ON with 0 velocity (NOTE_OFF equivalent)
                if (smRef.getData2() == 0)
                {
                    return smRef.getData1() == sm.getData1() && sm.getData2() == 0;
                } else
                {
                    return smRef.getData1() == sm.getData1();
                }
            } else if (smRef.getCommand() == ShortMessage.NOTE_OFF)
            {
                return smRef.getData1() == sm.getData1();
            }
            return false;
        } else
        {
            // Meta or SysEx messages
            return Arrays.equals(mmRef.getMessage(), mm.getMessage());
        }
    }

    static private String getPrefMidiMessagesKey(String actionCategory, String actionId)
    {
        return actionCategory + "#" + actionId;
    }

    static private String getPrefEnabledKey(String actionCategory, String actionId)
    {
        return getPrefMidiMessagesKey(actionCategory, actionId) + "_enabled";
    }


    static private List<MidiMessage> noteOnMidiMessages(int channel, int pitch)
    {
        return Arrays.asList(MidiUtilities.getNoteOnMessage(channel, pitch, 64));
    }

    // ================================================================================================
    // Inner classes
    // ================================================================================================
    private class MidiLearnReceiver implements Receiver
    {

        private boolean open = true;
        private List<MidiMessage> mms = new ArrayList<>();

        @Override
        public void send(MidiMessage msg, long timeStamp)
        {
            if (open)
            {
                mms.add(msg);
            }
        }

        public List<MidiMessage> getLearntMessages()
        {
            return mms;
        }

        @Override
        public void close()
        {
            open = false;
        }

    }

}

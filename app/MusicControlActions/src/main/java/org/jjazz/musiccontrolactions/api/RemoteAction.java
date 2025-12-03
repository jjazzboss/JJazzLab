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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.swing.Action;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.utilities.api.ResUtil;
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
    private List<MidiMessage> defaultMidiMessages;
    private final Action action;
    private int validIndex = 0;
    private boolean enabled = true;
    private static final Preferences prefs = NbPreferences.forModule(RemoteAction.class);
    private static final Logger LOGGER = Logger.getLogger(RemoteAction.class.getSimpleName());

    /**
     * Create a RemoteAction with no MidiMessage.
     *
     * @param actionCategory Must be a valid JJazzLab/Netbeans action category.
     * @param actionId       Must be a valid JJazzLab/Netbeans action id.
     * @throws IllegalArgumentException If parameters do not represent a valid JJazzLab action, or if the action does not have a NAME property defined.
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
        if (action == null || action.getValue(Action.NAME) == null
                || !(action.getValue(Action.NAME) instanceof String)
                || ((String) action.getValue(Action.NAME)).isBlank())
        {
            throw new IllegalArgumentException("No Netbeans action found for actionCategory=" + actionCategory + " actionId=" + actionId + " action=" + action);
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
            LOGGER.log(Level.WARNING, "loadFromPreference() Invalid save string: {0}", s);
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
                int steps = 5;
                ph.switchToDeterminate(steps);
                for (int i = 0; i < steps; i++)
                {
                    Thread.sleep(timeOutMs / steps);
                    ph.progress(i);
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
            simplify(learntMessages);
            midiMessages = learntMessages;
            return true;
        }

        return false;
    }

    /**
     *
     * @return The control note pitch if getMidiMessages() correspond to a single Note_ON ShortMessage. -1 otherwise.
     * @see #setMidiMessages(java.util.List) 
     */
    public int getControlNotePitch()
    {
        if (midiMessages == null || midiMessages.size() != 1)
        {
            return -1;
        }
        ShortMessage sm = MidiUtilities.getNoteOnShortMessage(midiMessages.get(0));
        return sm == null ? -1 : sm.getData1();
    }

    /**
     *
     * @return The control note channel if getMidiMessages() correspond to a single Note_ON ShortMessage. -1 otherwise.
     * @see #setMidiMessages(java.util.List) 
     */
    public int getControlNoteChannel()
    {
        if (midiMessages == null || midiMessages.size() != 1)
        {
            return -1;
        }
        ShortMessage sm = MidiUtilities.getNoteOnShortMessage(midiMessages.get(0));
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
     * Set the default Midi messages to be used when this RemoteAction is reset.
     *
     * @param messages Must be non null and non empty
     */
    public void setDefaultMidiMessages(List<MidiMessage> messages)
    {
        if (messages != null && !messages.isEmpty())
        {
            defaultMidiMessages = messages;
        }
    }

    /**
     * If valid default Midi messages are defined, use them as Midi messages.
     */
    public void reset()
    {
        if (defaultMidiMessages != null && !defaultMidiMessages.isEmpty())
        {
            midiMessages = new ArrayList<>(defaultMidiMessages);
        }
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
     * @return True if this message comparison is the last successful comparison with each of the reference MidiMessages of this RemoteAction.
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

    /**
     * Get the MidiMessages for a command which is a note pressed on the specified channel.
     *
     * @param channel
     * @param pitch
     * @return
     */
    static public List<MidiMessage> noteOnMidiMessages(int channel, int pitch)
    {
        return Arrays.asList(MidiUtilities.getNoteOnMessage(channel, pitch, 64));
    }

    // ================================================================================================
    // Private methods
    // ================================================================================================
    /**
     * Compare mm with the reference MidiMessage.
     *
     * @param mmRef
     * @param mm
     * @return
     */
    private boolean compare(MidiMessage mmRef, MidiMessage mm)
    {
        if (mmRef instanceof ShortMessage && mm instanceof ShortMessage)          // Can't just compare with == classes, because both JJazzLab and JDK use their own ShortMessage subclasses
        {
            ShortMessage smRef = (ShortMessage) mmRef;
            ShortMessage sm = (ShortMessage) mm;
            int refCommand = smRef.getCommand();
            int refStatus = smRef.getStatus();
            int refChannel = smRef.getChannel();
            int channel = sm.getChannel();
            int command = sm.getCommand();
            int status = sm.getStatus();


            // Special case if mmRef is a NoteON or NoteOFF
            if (MidiUtilities.getNoteOffShortMessage(smRef) != null)        // Note_OFF OR Note_ON with velocity=0
            {
                return MidiUtilities.getNoteOffShortMessage(sm) != null && sm.getData1() == smRef.getData1() && channel == refChannel;

            } else if (smRef.getCommand() == ShortMessage.NOTE_ON)      // Note_ON with velocity > 0
            {
                return MidiUtilities.getNoteOnShortMessage(sm) != null && sm.getData1() == smRef.getData1() && channel == refChannel;

            } else if (refCommand == ShortMessage.CONTROL_CHANGE
                    || refCommand == ShortMessage.PITCH_BEND)
            {
                // Use 2 data
                return command == refCommand && sm.getData1() == smRef.getData1() && sm.getData2() == smRef.getData2() && channel == refChannel;

            } else if (refCommand == ShortMessage.PROGRAM_CHANGE)
            {
                // Use 1 data
                return command == refCommand && sm.getData1() == smRef.getData1() && channel == refChannel;

            } else if (refStatus == ShortMessage.CONTINUE
                    || refStatus == ShortMessage.START
                    || refStatus == ShortMessage.STOP)
            {
                // Status message with no data
                return status == refStatus;

            } else if (refStatus == ShortMessage.SONG_SELECT)
            {
                // Status message with 1 data
                return status == refStatus && sm.getData1() == smRef.getData1();

            } else if (refStatus == ShortMessage.SONG_POSITION_POINTER)
            {
                // Status message with 2 data
                return status == refStatus && sm.getData1() == smRef.getData1() && sm.getData2() == smRef.getData2();
            }

            return false;

        } else if (mmRef instanceof SysexMessage && mm instanceof SysexMessage) // Can't just compare == classes, because both JJazzLab and JDK might use their own subclasses
        {
            return Arrays.equals(mmRef.getMessage(), mm.getMessage());

        } else if (mmRef instanceof MetaMessage && mm instanceof MetaMessage)  // Can't just compare == classes, because both JJazzLab and JDK might use their own subclasses
        {
            return Arrays.equals(mmRef.getMessage(), mm.getMessage());
        }

        return false;
    }

    static private String getPrefMidiMessagesKey(String actionCategory, String actionId)
    {
        return actionCategory + "#" + actionId;
    }

    static private String getPrefEnabledKey(String actionCategory, String actionId)
    {
        return getPrefMidiMessagesKey(actionCategory, actionId) + "_enabled";
    }

    /**
     * If there are only 2 events NoteON/NoteOff on a same pitch, remove the NoteOff.
     *
     * @param messages
     */
    private void simplify(List<MidiMessage> messages)
    {
        if (messages.size() == 2)
        {
            var mm0 = messages.get(0);
            var mm1 = messages.get(1);
            var sm0 = MidiUtilities.getNoteOnShortMessage(mm0);
            if (sm0 != null)
            {
                int pitch = sm0.getData1();
                var sm1 = MidiUtilities.getNoteOffShortMessage(mm1);
                if (sm1 != null && sm1.getData1() == pitch)
                {
                    messages.remove(1);
                }
            }
        }
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

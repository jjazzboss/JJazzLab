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
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import org.jjazz.midi.api.MidiUtilities;

/**
 * A remote action stores the list of MidiMessages which can trigger an action.
 */
public class RemoteAction
{

    private final String actionId;
    private final String actionCategory;
    private List<MidiMessage> midiMessages;
    private int controlNote = -1;
    private int controlNoteChannel = -1;
    private int validIndex = 0;
    private boolean enabled = true;


    /**
     * Create a RemoteAction with no MidiMessage.
     *
     * @param actionCategory Can't be blank
     * @param actionId Can't be blank
     */
    public RemoteAction(String actionCategory, String actionId)
    {
        checkNotNull(actionId);
        checkNotNull(actionCategory);
        checkArgument(!actionId.isBlank(), "actionId=%s", actionId);
        checkArgument(!actionCategory.isBlank(), "actionCategory=%s", actionCategory);

        this.actionId = actionId;
        this.actionCategory = actionCategory;
    }


    /**
     * Set MidiMessages to trigger the action when a NOTE_ON with pitch is received on the specified channel.
     *
     * @param channel
     * @param pitch
     */
    public void setControlNote(int channel, int pitch)
    {
        setMidiMessages(noteOnMidiMessages(channel, pitch));
        this.controlNoteChannel = channel;
        this.controlNote = pitch;
    }

    /**
     *
     * @return -1 if MidiMessages have not been set using setControlNote()
     * @see #setControlNote(int, int)
     */
    public int getControlNote()
    {
        return controlNote;
    }

    /**
     *
     * @return -1 if MidiMessages have not been set using setControlNote()
     * @see #setControlNote(int, int)
     */
    public int getControlNoteChannel()
    {
        return controlNoteChannel;
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
     * Set the MidiMessages to be recognized to trigger this action.
     * <p>
     * The method also resets the control note/channel to -1.
     *
     * @param messages
     */
    public synchronized void setMidiMessages(List<MidiMessage> messages)
    {
        this.midiMessages = messages;
        this.controlNote = -1;
        this.controlNoteChannel = -1;
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

    /**
     *
     * @return @see #loadFromString(java.lang.String)
     */
    public String saveAsString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(actionId);
        for (MidiMessage mm : midiMessages)
        {
            sb.append(":").append(MidiUtilities.saveMidiMessageAsString(mm));
        }
        return sb.toString();
    }

    /**
     *
     * @param s
     * @return
     * @throws java.text.ParseException @see #saveAsString()
     * @throws javax.sound.midi.InvalidMidiDataException
     * @see #saveAsString()
     */
    static public RemoteAction loadFromString(String s) throws ParseException, InvalidMidiDataException
    {
        String[] strs = s.split(":");
        if (strs.length < 2)
        {
            throw new ParseException("Invalid RemoteAction string '" + s + "'", 0);
        }

        String id = strs[0];

        List<MidiMessage> mms = new ArrayList<>();
        for (int i = 1; i < strs.length; i++)
        {
            mms.add(MidiUtilities.loadMidiMessageFromString(strs[i]));
        }

        RemoteAction res = new RemoteAction(id);
        res.setEnabled(false);
        res.setMidiMessages(mms);
        res.setEnabled(true);
        return res;
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


    static private List<MidiMessage> noteOnMidiMessages(int channel, int pitch)
    {
        return Arrays.asList(MidiUtilities.getNoteOnMessage(channel, pitch, 64));
    }
}

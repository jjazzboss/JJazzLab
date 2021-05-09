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
package org.jjazz.ui.keyboardcomponent;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import org.jjazz.harmony.Note;

/**
 * A Midi Receiver to show incoming notes on a Keyboard UI component.
 * <p>
 */
public class KeyboardMidiReceiver implements Receiver
{

    private KeyboardComponent keyboard;
    private boolean isOpen;
    private int pitchShift;

    public KeyboardMidiReceiver(KeyboardComponent kbd)
    {
        if (keyboard == null)
        {
            throw new NullPointerException("kbd");
        }
        keyboard = kbd;
    }

    public void open()
    {
        isOpen = true;
    }

    //--------------------------------------------------------------------
    // Implement the Receiver interface
    //--------------------------------------------------------------------
    /**
     * Close the Receiver : received MIDI notes are NOT shown on the keyboard.
     */
    @Override
    public void close()
    {
        isOpen = false;
    }

    /**
     * Show a received note ON/OFF on the PianoKeyboard.The way it is done depends on the lockMode.
     * @param msg
     * @param timestamp
     */
    @Override
    public void send(MidiMessage msg, long timestamp)
    {
        if (!isOpen)
        {
            return;
        }

        // Process only ShortMessages
        if (!(msg instanceof ShortMessage))
        {
            return;
        }

        ShortMessage sm = (ShortMessage) msg;
        int pitch;

        switch (sm.getCommand())
        {
            case ShortMessage.NOTE_ON:
                pitch = Math.min(sm.getData1() + pitchShift, Note.PITCH_MAX);
                pitch = Math.max(pitch, Note.PITCH_MIN);
                keyboard.setPressed(pitch, sm.getData2());
                break;

            case ShortMessage.NOTE_OFF:
                pitch = Math.min(sm.getData1() + pitchShift, Note.PITCH_MAX);
                pitch = Math.max(pitch, Note.PITCH_MIN);
                keyboard.setPressed(pitch, 0);
                break;

        }
    }
}

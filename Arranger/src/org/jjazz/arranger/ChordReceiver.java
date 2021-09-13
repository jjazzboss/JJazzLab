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
package org.jjazz.arranger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import org.jjazz.harmony.api.ChordSymbolFinder;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.MidiUtilities;


/**
 * A Midi receiver which updates the current list of notes ON.
 * <p>
 */
public class ChordReceiver implements Receiver
{

    public interface ChordListener
    {

        /**
         * One note ON has been added or removed.
         * <p>
         * Note: event handling should be as short as possible.
         *
         * @param notes
         */
        void chordChanged(List<Note> notes);
    }

    private List<ChordListener> listeners = new ArrayList<>();
    private List<Note> notes = new LinkedList<>();
    private static final Logger LOGGER = Logger.getLogger(ChordReceiver.class.getSimpleName());  //NOI18N

    public void reset()
    {
        notes.clear();
    }

    @Override
    public void send(MidiMessage msg, long timeStamp)
    {
        ShortMessage noteMsg = MidiUtilities.getNoteOnMidiEvent(msg);
        if (noteMsg != null)
        {
            // Note ON
            int pitch = noteMsg.getData1();
            int velocity = noteMsg.getData2();

            // Add new Note ordered by pitch
            Note newNote = new Note(pitch, 1, velocity);
            var lIt = notes.listIterator();
            while (lIt.hasNext())
            {
                if (pitch <= lIt.next().getPitch())
                {
                    lIt.previous();
                    break;
                }
            }
            lIt.add(newNote);

        } else if ((noteMsg = MidiUtilities.getNoteOffMidiEvent(msg)) != null)
        {
            // Note OFF
            int pitch = noteMsg.getData1();

            // Remove Note
            var it = notes.iterator();
            while (it.hasNext())
            {
                var n = it.next();
                if (n.getPitch() == pitch)
                {
                    it.remove();
                    break;
                }
            }
        }


        if (noteMsg != null)
        {
            // Chord was updated
            fireChordChanged(new ArrayList<>(notes));
        }

    }

    @Override
    public void close()
    {
        listeners.clear();
    }

    public void addChordListener(ChordListener listener)
    {
        if (!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    public void removeChordListener(ChordListener listener)
    {
        listeners.remove(listener);
    }

    private void fireChordChanged(List<Note> notes)
    {
        LOGGER.severe("fireChordChanged() notes=" + notes);
        listeners.forEach(l -> l.chordChanged(notes));
    }

}

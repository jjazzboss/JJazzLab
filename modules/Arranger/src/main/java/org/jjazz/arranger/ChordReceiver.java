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
package org.jjazz.arranger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiUtilities;
import org.openide.util.NbPreferences;


/**
 * A Midi receiver which updates the current list of notes ON.
 * <p>
 */
public class ChordReceiver implements Receiver
{

    public static final int DEFAULT_SPLIT_POINT_NOTE = 60; // E3
    private static final String PREF_SPLIT_NOTE = "SplitNote";

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
    private int splitNote;
    private final List<ChordListener> listeners = new ArrayList<>();
    private final List<Note> notes = new LinkedList<>();
    private static Preferences prefs = NbPreferences.forModule(JJazzMidiSystem.class);
    private static final Logger LOGGER = Logger.getLogger(ChordReceiver.class.getSimpleName());  

    public ChordReceiver()
    {
        // Restore the split note
        splitNote = prefs.getInt(PREF_SPLIT_NOTE, DEFAULT_SPLIT_POINT_NOTE);
        if (splitNote < 11 || splitNote > 127)
        {
            splitNote = DEFAULT_SPLIT_POINT_NOTE;
        }
    }

    public synchronized int getSplitNote()
    {
        return splitNote;
    }


    public synchronized void setSplitNote(int newSplitNote)
    {
        if (newSplitNote < this.splitNote)
        {
            for (var it = notes.iterator(); it.hasNext();)
            {
                if (it.next().getPitch() > newSplitNote)
                {
                    it.remove();
                }
            }
        }
        this.splitNote = newSplitNote;
        prefs.putInt(PREF_SPLIT_NOTE, this.splitNote);
    }

    public void reset()
    {
        notes.clear();
    }

    @Override
    public void send(MidiMessage msg, long timeStamp)
    {
        ShortMessage noteMsg = MidiUtilities.getNoteOnShortMessage(msg);
        if (noteMsg != null)
        {
            // Note ON
            int pitch = noteMsg.getData1();
            int velocity = noteMsg.getData2();

            if (pitch > getSplitNote())
            {
                noteMsg = null;
            } else
            {
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
            }
        } else if ((noteMsg = MidiUtilities.getNoteOffShortMessage(msg)) != null)
        {
            // Note OFF
            int pitch = noteMsg.getData1();

            if (pitch > getSplitNote())
            {
                noteMsg = null;
            } else
            {
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
        LOGGER.log(Level.FINE, "fireChordChanged() notes={0}", notes);
        listeners.forEach(l -> l.chordChanged(notes));
    }

}

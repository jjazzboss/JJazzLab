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
package org.jjazz.rhythmmusicgeneration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;
import org.jjazz.harmony.Chord;
import org.jjazz.harmony.Note;
import org.jjazz.midi.MidiConst;
import org.jjazz.util.Filter;

/**
 * A list of NoteEvents sorted by start position.
 */
public class Phrase implements Cloneable
{

    /**
     * NoteEvent client property set when new NoteEvents are created from existing ones.
     */
    public static final String PARENT_NOTE = "PARENT_NOTE";
    private final int channel;
    // We will make many inserts in the sorted list: linked list avoids the shifting of all subsequent elements.
    protected final LinkedList<NoteEvent> events = new LinkedList<>();
    private static final Logger LOGGER = Logger.getLogger(Phrase.class.getSimpleName());

    /**
     * Class used as return value type by getCrossingNotes().
     */
    public class NoteAndIndex
    {

        public NoteEvent noteEvent;
        public int index;
    }

    /**
     *
     * @param channel
     */
    public Phrase(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);
        }
        this.channel = channel;
    }

    @Override
    public Phrase clone()
    {
        Phrase p = new Phrase(channel);
        for (NoteEvent mne : events)
        {
            NoteEvent mne2 = mne.clone();
            p.add(mne2);
        }
        return p;
    }

    /**
     *
     * @return 1-16
     */
    public int getChannel()
    {
        return channel;
    }

    /**
     * The number of beats between start of first event and end of last event.
     *
     * @return 0 if size is less than 2
     */
    public float getSizeInBeats()
    {
        if (events.size() < 2)
        {
            return 0;
        }
        NoteEvent last = events.get(events.size() - 1);
        return last.getPositionInBeats() + last.getDurationInBeats() - getFirstEventPosition();
    }

    /**
     *
     * @return Null if phrase is empty.
     */
    public NoteEvent getHighestPitchNote()
    {
        NoteEvent res = null;
        for (NoteEvent event : events)
        {
            if (res == null || event.getPitch() > res.getPitch())
            {
                res = event;
            }
        }
        return res;
    }

    /**
     *
     * @return Null if phrase is empty.
     */
    public NoteEvent getLowestPitchNote()
    {
        NoteEvent res = null;
        for (NoteEvent event : events)
        {
            if (res == null || event.getPitch() < res.getPitch())
            {
                res = event;
            }
        }
        return res;
    }

    /**
     *
     * @return Null if phrase is empty.
     */
    public NoteEvent getHighestVelocityNote()
    {
        NoteEvent res = null;
        for (NoteEvent event : events)
        {
            if (res == null || event.getVelocity() > res.getVelocity())
            {
                res = event;
            }
        }
        return res;
    }

    /**
     *
     * @return 0 If phrase is empty.
     */
    public float getFirstEventPosition()
    {
        return events.isEmpty() ? 0 : events.get(0).getPositionInBeats();
    }

    /**
     *
     * @return 0 If phrase is empty.
     */
    public float getLastEventPosition()
    {
        return events.isEmpty() ? 0 : events.get(events.size() - 1).getPositionInBeats();
    }

    /**
     * Get the notes still ringing at specified position.
     * <p>
     *
     * @param posInBeats
     * @return The list of notes (with their index) whose startPos is strictly before posInBeats and endPos strictly after posInBeats
     */
    public List<NoteAndIndex> getCrossingNotes(float posInBeats)
    {
        ArrayList<NoteAndIndex> res = new ArrayList<>();
        for (int i = 0; i < events.size(); i++)
        {
            NoteEvent ne = events.get(i);
            float pos = ne.getPositionInBeats();
            if (pos >= posInBeats)
            {
                break;
            }
            if (pos + ne.getDurationInBeats() > posInBeats)
            {
                NoteAndIndex npi = new NoteAndIndex();
                npi.index = i;
                npi.noteEvent = ne;
                res.add(npi);
            }
        }
        return res;
    }

    /**
     * Add delta to the velocity of this Phrase notes.
     * <p>
     * Velocity is always maintained between 0 and 127. Notes of the returned phrase will have their PARENT_NOTE client property set to:<br>
     * - source note's PARENT_NOTE client property if this property is not null, or<br>
     * - the source note from this phrase
     *
     * @param delta eg -5 or +10
     * @return A new phrase
     */
    public Phrase getVelocityShiftedPhrase(int delta)
    {
        Phrase res = new Phrase(channel);
        for (NoteEvent ne : events)
        {
            int newVelocity = Math.min(ne.getVelocity() + delta, 127);
            newVelocity = Math.max(newVelocity, 0);
            NoteEvent tNe = new NoteEvent(ne, ne.getPitch(), ne.getDurationInBeats(), newVelocity);       // This clone also the clientProperties
            if (tNe.getClientProperty(PARENT_NOTE) == null)
            {
                tNe.putClientProperty(PARENT_NOTE, ne);         // If no previous PARENT_NOTE client property we can add one
            }
            res.add(tNe);
        }
        return res;
    }

    /**
     * Get a phrase with only the events accepted by the specified filter.
     *
     * @param f
     * @return
     */
    public Phrase getFilteredPhrase(Filter f)
    {
        Phrase res = new Phrase(channel);
        for (NoteEvent ne : this.events)
        {
            if (f.accept(f))
            {
                res.add(ne);
            }
        }
        return res;
    }

    /**
     * Make sure there is no note ringing after the specified position.
     * <p>
     * Notes starting after posInBeats are removed.<br>
     * If a note starts before posInBeats but is still ON beyond posInBeats, note duration is shortened to have Note OFF at posInBeats.
     *
     * @param posInBeats
     */
    public void silenceAfter(float posInBeats)
    {
        for (int i = events.size() - 1; i >= 0; i--)
        {
            NoteEvent ne = events.get(i);
            float pos = ne.getPositionInBeats();
            if (pos >= posInBeats)
            {
                events.remove(i);
            } else if (pos + ne.getDurationInBeats() > posInBeats)
            {
                float newDuration = posInBeats - pos;
                NoteEvent ne2 = new NoteEvent(ne, newDuration);
                events.set(i, ne2);
            }
        }
    }

    /**
     * Remove all events whose start position is before startPos, or equal/after endPos.
     * <p>
     * If a note is starting before startPos and ending after startPos: <br>
     * - if keepLeft is false, the note is removed.<br>
     * - if keepLeft is true, the note is replaced by a shorter identical one starting at startPos.<br>
     * If a note is starting before endPos and ending after endPos: <br>
     * - if cutRight is false, the note is not removed.<br>
     * - if cutRight is true, the note is replaced by a shorter identical that ends at endPos.<br>
     *
     * @param startPos
     * @param endPos
     * @param keepLeft
     * @param cutRight
     */
    public void slice(float startPos, float endPos, boolean keepLeft, boolean cutRight)
    {
        // Proceed in steps to speed up processing of the linkedlist
        ArrayList<NoteEvent> toBeRemoved = new ArrayList<>();
        ArrayList<NoteEvent> toBeAdded = new ArrayList<>();

        ListIterator<NoteEvent> it = events.listIterator();
        while (it.hasNext())
        {
            NoteEvent ne = it.next();
            float nePosFrom = ne.getPositionInBeats();
            float nePosTo = nePosFrom + ne.getDurationInBeats();
            if (nePosFrom < startPos)
            {
                it.remove();
                if (keepLeft && nePosTo > startPos)
                {
                    if (cutRight && nePosTo > endPos)
                    {
                        nePosTo = endPos;
                    }
                    float newDur = nePosTo - startPos;
                    NoteEvent newNe = new NoteEvent(ne, newDur, startPos);
                    toBeAdded.add(newNe);
                }
            } else if (nePosFrom < endPos)
            {
                if (cutRight && nePosTo > endPos)
                {
                    float newDur = endPos - nePosFrom;
                    NoteEvent newNe = new NoteEvent(ne, newDur, nePosFrom);
                    it.set(newNe);
                }
            } else
            {
                // nePosFrom is after endPost
                it.remove();
            }
        }

        // Add the new NoteEvents
        for (NoteEvent ne : toBeAdded)
        {
            add(ne);
        }

    }

    /**
     * Remove all events whose start position is equal/after startPos or before endPos.
     * <p>
     * If a note is starting before startPos and ending after startPos: <br>
     * - if cutLeft is false, the note is not removed.<br>
     * - if cutLeft is true, the note is replaced by a shorter identical that ends at startPos.<br>
     * If a note is starting before endPos and ending after endPos: <br>
     * - if keepRight is false, the note is removed.<br>
     * - if keepRight is true, the note is replaced by a shorter identical one starting at endPos.<br>
     *
     * @param startPos
     * @param endPos
     * @param cutLeft
     * @param keepRight
     */
    public void split(float startPos, float endPos, boolean cutLeft, boolean keepRight)
    {
        ArrayList<NoteEvent> toBeAdded = new ArrayList<>();

        ListIterator<NoteEvent> it = events.listIterator();
        while (it.hasNext())
        {
            NoteEvent ne = it.next();
            float nePosFrom = ne.getPositionInBeats();
            float nePosTo = nePosFrom + ne.getDurationInBeats();
            if (nePosFrom < startPos)
            {
                if (cutLeft && nePosTo > startPos)
                {
                    if (keepRight && nePosTo > endPos)
                    {
                        float newDur = nePosTo - endPos;
                        NoteEvent newNe = new NoteEvent(ne, newDur, endPos);
                        toBeAdded.add(newNe);
                    }
                    float newDur = startPos - nePosFrom;
                    NoteEvent newNe = new NoteEvent(ne, newDur, nePosFrom);
                    it.set(newNe);
                }
            } else if (nePosFrom < endPos)
            {
                it.remove();
                if (keepRight && nePosTo > endPos)
                {
                    float newDur = nePosTo - endPos;
                    NoteEvent newNe = new NoteEvent(ne, newDur, endPos);
                    toBeAdded.add(newNe);
                }
            } else
            {
                // nePosFrom is after endPost
                // Nothing
            }
        }

        // Add the new NoteEvents
        for (NoteEvent ne : toBeAdded)
        {
            add(ne);
        }
    }

    /**
     * Transpose all notes of t semitons.
     * <p>
     * Transposed notes are limited to pitch [0-127]. <br>
     * Notes of the returned phrase will have their PARENT_NOTE client property set to:<br>
     * - source note's PARENT_NOTE client property if this property is not null, or<br>
     * - the source note from this phrase
     *
     * @return The new transposed Phrase.
     */
    public Phrase getTransposedPhrase(int t)
    {
        Phrase mnp = new Phrase(channel);
        for (NoteEvent ne : events)
        {
            int newPitch = ne.getPitch() + t;
            if (newPitch < 0)
            {
                LOGGER.fine("getTransposed() pitch out of range note newPitch=" + newPitch + ". Changed to 0");
                newPitch = 0;
            } else if (newPitch > 127)
            {
                LOGGER.fine("getTransposed() pitch out of range note newPitch=" + newPitch + ". Changed to 127");
                newPitch = 127;
            }
            NoteEvent tNe = new NoteEvent(ne, newPitch);       // This clone also the clientProperties
            if (tNe.getClientProperty(PARENT_NOTE) == null)
            {
                tNe.putClientProperty(PARENT_NOTE, ne);         // If no previous PARENT_NOTE client property we can add one
            }
            mnp.add(tNe);
        }
        return mnp;
    }

    /**
     * Copy all the events of the specified phrase and add them to this phrase.
     *
     * @param phrase
     */
    public void add(Phrase phrase)
    {
        for (NoteEvent mne : phrase.getEvents())
        {
            NoteEvent mne2 = mne.clone();
            add(mne2);
        }
    }

    /**
     * Add a MidiNoteEvent.
     * <p>
     * The NoteEvent is added at the correct index depending on its position.
     *
     * @param mne
     */
    public void add(NoteEvent mne)
    {
        if (events.isEmpty())
        {
            events.add(mne);
        } else
        {
            // There are more chances to find the right place near the end...
            ListIterator<NoteEvent> it = events.listIterator(events.size());
            boolean beforeEvent = true;
            while (it.hasPrevious() && (beforeEvent = mne.isBefore(it.previous())))
            {
                // Nothing
            }
            if (!beforeEvent)
            {
                // We exited the loop because we're after the next event
                it.next();
            }
            it.add(mne);
        }
    }

    /**
     * Replace NoteEvent at specified index with NoteEvent ne.
     *
     * @param index
     * @param ne
     */
    public void replaceNote(int index, NoteEvent ne)
    {
        if (index < 0 || index >= events.size() || ne == null)
        {
            throw new IllegalArgumentException("index=" + index + " ne=" + ne);
        }
        events.set(index, ne);
    }

    /**
     * Remove a MidiNoteEvent.
     *
     * @param ne
     * @return
     */
    public boolean remove(NoteEvent ne)
    {
        return events.remove(ne);
    }

    /**
     * Create MidiEvents for each note and add it to the specified track.
     * <p>
     * Tick resolution used is MidiConst.PPQ_RESOLUTION.
     *
     * @param track
     */
    public void fillTrack(Track track)
    {
        for (NoteEvent ne : events)
        {
            for (MidiEvent me : ne.toMidiEvents(channel))
            {
                track.add(me);
            }
        }
    }

    /**
     * Get all NoteEvents sorted by startPosition.
     * <p>
     * Be careful: returned LinkedList is the internal data structure of the Phrase.
     *
     * @return
     */
    public LinkedList<NoteEvent> getEvents()
    {
        return events;
    }

    /**
     * Get all NoteEvents sorted by startPosition as a List.
     *
     * @return
     */
    public List<NoteEvent> getEventsAsList()
    {
        return new ArrayList<>(events);
    }

    /**
     * Get the events matching the specified relative pitch.
     *
     * @param relPitch
     * @return
     */
    public List<NoteEvent> getEvents(int relPitch)
    {
        ArrayList<NoteEvent> res = new ArrayList<>();
        for (NoteEvent ne : events)
        {
            if (ne.getRelativePitch() == relPitch)
            {
                res.add(ne);
            }
        }
        return res;
    }

    /**
     * Replace all events by the same events but with position shifted.
     *
     * @param shiftInBeats The value added to each event's position.
     * @throws IllegalArgumentException If an event's position become negative.
     */
    public void shiftEvents(float shiftInBeats)
    {
        for (int i = 0; i < events.size(); i++)
        {
            NoteEvent ne = events.get(i);
            float newPosInBeats = ne.getPositionInBeats() + shiftInBeats;
            if (newPosInBeats < 0)
            {
                throw new IllegalArgumentException("ne=" + ne + " shiftInBeats=" + shiftInBeats);
            }
            NoteEvent shiftedNe = new NoteEvent(ne, ne.getDurationInBeats(), newPosInBeats);
            events.set(i, shiftedNe);
        }
    }

    public boolean isEmpty()
    {
        return events.isEmpty();
    }

    /**
     * Get a chord made of all unique pitch NoteEvents present in the phrase.
     *
     * @return
     */
    public Chord getChord()
    {
        Chord c = new Chord(events);
        return c;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Phrase[ch=").append(channel).append("] size=").append(events.size()).append(" notes=").append(events);
//      if (events.size() >= 1)
//      {
//         sb.append(" ne[0]=").append(events.get(0));
//      }
//      if (events.size() > 1)
//      {
//         int lastIndex = events.size() - 1;
//         sb.append(" ... ne[").append(lastIndex).append("]=").append(events.get(lastIndex));
//      }
        return sb.toString();
    }

    public void dump()
    {
        LOGGER.info(toString());
        for (NoteEvent mne : events)
        {
            LOGGER.info(mne.toString());
        }
    }

    /**
     * Remove overlapped notes with identical pitch.
     * <p>
     * A note N1 is overlapped by N2 if N1's noteOn event occurs after N2's noteOn event and N1's noteOff event occurs before N2's noteOff
     * event.
     */
    public void removeOverlappedNotes()
    {
        for (Note n : getChord().getNotes())
        {
            ArrayList<NoteEvent> onNotes = new ArrayList<>();
            for (NoteEvent ne : events.toArray(new NoteEvent[0]))
            {
                if (ne.getPitch() == n.getPitch())
                {
                    float curStartPos = ne.getPositionInBeats();
                    float curEndPos = curStartPos + ne.getDurationInBeats();
                    boolean needRemove = false;
                    for (NoteEvent onNote : onNotes.toArray(new NoteEvent[0]))
                    {
                        float onNoteEndPos = onNote.getPositionInBeats() + onNote.getDurationInBeats();
                        if (onNoteEndPos < curStartPos)
                        {
                            // Remove noteOns which are now Off                     
                            onNotes.remove(onNote);
                        } else if (curEndPos <= onNoteEndPos)
                        {
                            // Cur note is overlapped !
                            needRemove = true;
                            break;
                        }
                    }
                    if (needRemove)
                    {
                        remove(ne);
                    } else
                    {
                        onNotes.add(ne);
                    }
                }
            }
        }
    }

    /**
     * Transpose notes (+/- octaves)whose pitch is above highLimit or below lowLimit.
     * <p>
     * Fixed new notes's PARENT_NOTE client property is preserved.
     *
     * @param lowLimit  There must be at least 1 octave between lowLimit and highLimit
     * @param highLimit There must be at least 1 octave between lowLimit and highLimit
     */
    public void limitPitchRange(int lowLimit, int highLimit)
    {
        if (lowLimit < 0 || highLimit > 127 || lowLimit > highLimit || highLimit - lowLimit < 11)
        {
            throw new IllegalArgumentException("lowLimit=" + lowLimit + " highLimit=" + highLimit);
        }
        for (int i = 0; i < events.size(); i++)
        {
            NoteEvent ne = events.get(i);
            int pitch = ne.getPitch();

            while (pitch < lowLimit)
            {
                pitch += 12;
            }
            while (pitch > highLimit)
            {
                pitch -= 12;
            }
            NoteEvent nne = new NoteEvent(ne, pitch);
            events.set(i, nne);
        }
    }

}

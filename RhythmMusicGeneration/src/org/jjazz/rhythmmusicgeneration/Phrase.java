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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jjazz.harmony.Chord;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiUtilities;
import org.jjazz.util.FloatRange;

/**
 * A list of NoteEvents sorted by start position.
 * <p>
 * Use addOrdered() to add a NoteEvent: this will ensure NoteEvents are kept ordered by position. Use of add() methods should be
 * used for optimization only and not change the NoteEvents order.
 * <p>
 * LinkedList implementation to speed up item insertion/remove rather than random access.
 */
public class Phrase extends LinkedList<NoteEvent>
{

    /**
     * NoteEvent client property set when new NoteEvents are created from existing ones.
     */
    public static final String PARENT_NOTE = "PARENT_NOTE";
    private final int channel;
    private static final Logger LOGGER = Logger.getLogger(Phrase.class.getSimpleName());

    /**
     * Create an empty phrase for the specific Midi channel.
     *
     * @param channel [0-15]
     */
    public Phrase(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);
        }
        this.channel = channel;
    }

    /**
     * Should be used only when caller is sure that added NoteEvent has the last position.
     * <p>
     * Otherwise use addOrdered().
     *
     * @param ne
     * @return
     */
    @Override
    public boolean add(NoteEvent ne)
    {
        return super.add(ne);
    }

    /**
     * Add a clone of each p's events to this phrase.
     *
     * @param p
     */
    public void add(Phrase p)
    {
        for (NoteEvent mne : p)
        {
            addOrdered(mne.clone());
        }
    }

    /**
     * Add a NoteEvent at the correct index depending on its position.
     * <p>
     * @param mne
     */
    public void addOrdered(NoteEvent mne)
    {
        if (isEmpty())
        {
            add(mne);

        } else
        {
            // There are more chances to find the right place near the end...
            ListIterator<NoteEvent> it = listIterator(size());
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
     * Use the specified Midi track content to add notes to this phrase.
     * <p>
     *
     * @param track
     * @param ppqResolution The resolution used by the track
     * @param srcChannels Add notes from these Midi channel(s) only. If null, all notes are added, whatever the channel.
     */
    public void add(Track track, int ppqResolution, Integer... srcChannels)
    {
        List<Integer> channels = null;
        if (srcChannels != null)
        {
            channels = Arrays.asList(srcChannels);
        }

        // Buffer for Note ONs waiting for their Note OFF
        var onNotes = new HashMap<Integer, NoteEvent>();
        float maxEndPosInBeats = 0;


        for (int i = 0; i < track.size(); i++)
        {
            MidiEvent me = track.get(i);
            MidiMessage mm = me.getMessage();
            long tick = me.getTick();

            if (mm instanceof ShortMessage)
            {
                ShortMessage sm = (ShortMessage) mm;
                if (channels != null && !channels.contains(sm.getChannel()))
                {
                    continue;
                }
                if (sm.getCommand() == ShortMessage.NOTE_OFF || (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0))
                {
                    // Note OFF
                    int pitch = sm.getData1();
                    NoteEvent ne = onNotes.get(pitch);
                    if (ne == null)
                    {
                        // Note OFF without Note ON : do nothing
                    } else
                    {
                        // Corresponding Note ON found, set duration and add note
                        float beat = Float.valueOf(tick) / ppqResolution;
                        maxEndPosInBeats = Math.max(maxEndPosInBeats, beat);
                        float duration = beat - ne.getPositionInBeats();
                        ne = new NoteEvent(ne, duration, ne.getPositionInBeats());
                        addOrdered(ne);
                        onNotes.remove(pitch);
                    }
                } else if (sm.getCommand() == ShortMessage.NOTE_ON)
                {
                    // Note ON
                    float beat = Float.valueOf(tick) / ppqResolution;
                    int pitch = sm.getData1();
                    int velocity = sm.getData2();
                    NoteEvent ne = new NoteEvent(sm.getData1(), 1, velocity, beat);    // fake duration=1
                    onNotes.put(pitch, ne);
                }
            }
        }


        if (!onNotes.isEmpty())
        {
            // Some Note OFF were missing
            // Create notes if possible using end of note = maxEndPosInBeats
            for (NoteEvent ne : onNotes.values())
            {
                if (ne.getPositionInBeats() < maxEndPosInBeats)
                {
                    float duration = maxEndPosInBeats - ne.getPositionInBeats();
                    ne = new NoteEvent(ne, duration, ne.getPositionInBeats());
                    addOrdered(ne);
                }
            }
        }
    }

    /**
     * A deep clone: returned phrase contains clones of the original NoteEvents.
     *
     * @return
     */
    @Override
    public Phrase clone()
    {
        return getFilteredPhrase(ne -> true);
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
     * Get the beat range from start of first note to end of last note.
     *
     * @return FloatRange.EMPTY_FLOAT_RANGE if phrase is empty.
     */
    public FloatRange getBeatRange()
    {
        if (isEmpty())
        {
            return FloatRange.EMPTY_FLOAT_RANGE;
        }
        float startPos = isEmpty() ? 0 : getFirst().getPositionInBeats();
        NoteEvent lastNote = getLast();
        FloatRange fr = new FloatRange(startPos, lastNote.getPositionInBeats() + lastNote.getDurationInBeats());
        return fr;
    }

    /**
     *
     * @return Null if phrase is empty.
     */
    public NoteEvent getHighestPitchNote()
    {
        return stream().max(Comparator.comparing(NoteEvent::getPitch)).orElse(null);
    }

    /**
     *
     * @return Null if phrase is empty.
     */
    public NoteEvent getLowestPitchNote()
    {
        return stream().min(Comparator.comparing(NoteEvent::getPitch)).orElse(null);
    }

    /**
     *
     * @return Null if phrase is empty.
     */
    public NoteEvent getHighestVelocityNote()
    {
        return stream().max(Comparator.comparing(NoteEvent::getVelocity)).orElse(null);
    }

    /**
     *
     * @return 0 If phrase is empty.
     */
    public float getLastEventPosition()
    {
        return isEmpty() ? 0 : getLast().getPositionInBeats();
    }

    /**
     * Get a new phrase from the NoteEvents who match the specified predicate.
     * <p>
     * New phrase contains clones of the filtered NoteEvents.
     *
     * @param predicate
     * @return
     */
    public Phrase getFilteredPhrase(Predicate<NoteEvent> predicate)
    {
        Phrase res = new Phrase(channel);
        stream().filter(predicate)
                .forEach(ne -> res.add(ne.clone()));      // Don't need addOrdered here
        return res;
    }

    /**
     * Return a new Phrase with filtered notes processed by the specified mapper.
     * <p>
     * Notes of the returned phrase will have their PARENT_NOTE client property set to:<br>
     * - source note's PARENT_NOTE client property if this property is not null, or<br>
     * - the source note from this phrase.
     *
     * @param tester
     * @param mapper
     * @return
     */
    public Phrase getProcessedPhrase(Predicate<NoteEvent> tester, Function<NoteEvent, NoteEvent> mapper)
    {
        Phrase res = new Phrase(channel);
        for (NoteEvent ne : this)
        {
            if (tester.test(ne))
            {
                NoteEvent newNe = mapper.apply(ne);
                newNe.setClientProperties(ne);
                if (newNe.getClientProperty(PARENT_NOTE) == null)
                {
                    newNe.putClientProperty(PARENT_NOTE, ne);         // If no previous PARENT_NOTE client property we can add one
                }
                res.addOrdered(newNe);
            }
        }
        return res;
    }

    /**
     * Modify this phrase with filtered notes processed by the specified mapper.
     * <p>
     *
     * @param tester
     * @param mapper The mapper must NOT change the position
     */
    public void processEvents(Predicate<NoteEvent> tester, Function<NoteEvent, NoteEvent> mapper)
    {
        for (var it = listIterator(); it.hasNext();)
        {
            NoteEvent ne = it.next();
            if (tester.test(ne))
            {
                NoteEvent newNe = mapper.apply(ne);
                newNe.setClientProperties(ne);
                it.set(newNe);
            }
        }
    }

    /**
     * Get a new phrase with notes velocity changed.
     * <p>
     * Velocity is always maintained between 0 and 127. Notes of the returned phrase will have their PARENT_NOTE client property
     * set to:<br>
     * - source note's PARENT_NOTE client property if this property is not null, or<br>
     * - the source note from this phrase
     *
     * @param f A function modifying the velocity.
     * @return A new phrase
     */
    public Phrase getVelocityProcessedPhrase(Function<Integer, Integer> f)
    {
        return getProcessedPhrase(ne -> true, ne ->
        {
            int v = MidiUtilities.limit(f.apply(ne.getVelocity()));
            NoteEvent newNe = new NoteEvent(ne, ne.getPitch(), ne.getDurationInBeats(), v);
            return newNe;
        });
    }

    /**
     * Change the velocity of all notes of this Phrase.
     * <p>
     * Velocity is always maintained between 0 and 127.
     *
     * @param f A function modifying the velocity.
     */
    public void processVelocity(Function<Integer, Integer> f)
    {
        processEvents(ne -> true, ne ->
        {
            int v = MidiUtilities.limit(f.apply(ne.getVelocity()));
            NoteEvent newNe = new NoteEvent(ne, ne.getPitch(), ne.getDurationInBeats(), v);
            return newNe;
        });
    }

    /**
     * Get a new phrase with all notes changed.
     * <p>
     * Pitch is always maintained between 0 and 127. Notes of the returned phrase will have their PARENT_NOTE client property set
     * to:<br>
     * - source note's PARENT_NOTE client property if this property is not null, or<br>
     * - the source note from this phrase
     *
     * @param f A function modifying the pitch.
     * @return A new phrase
     */
    public Phrase getPitchProcessedPhrase(Function<Integer, Integer> f)
    {
        return getProcessedPhrase(ne -> true, ne ->
        {
            int p = MidiUtilities.limit(f.apply(ne.getPitch()));
            NoteEvent newNe = new NoteEvent(ne, p, ne.getDurationInBeats(), ne.getVelocity());
            return newNe;
        });
    }

    /**
     * Change the pitch of all notes of this Phrase.
     * <p>
     * Pitch is always maintained between 0 and 127.
     *
     * @param f A function modifying the pitch.
     */
    public void processPitch(Function<Integer, Integer> f)
    {
        processEvents(ne -> true, ne ->
        {
            int p = MidiUtilities.limit(f.apply(ne.getPitch()));
            NoteEvent newNe = new NoteEvent(ne, p, ne.getDurationInBeats(), ne.getVelocity());
            return newNe;
        });
    }

    /**
     * Make sure there is no note ringing after the specified position.
     * <p>
     * Notes starting after posInBeats are removed.<br>
     * If a note starts before posInBeats but is still ON beyond posInBeats, note duration is shortened to have Note OFF at
     * posInBeats.
     *
     * @param posInBeats
     */
    public void silenceAfter(float posInBeats)
    {
        // Use an iterator to avoid using get(i) which is O(n) for a linkedlist
        ListIterator<NoteEvent> it = listIterator(size());
        while (it.hasPrevious())
        {
            NoteEvent ne = it.previous();
            float pos = ne.getPositionInBeats();
            if (pos >= posInBeats)
            {
                // Remove notes after posInBeats
                it.remove();
            } else if (pos + ne.getDurationInBeats() > posInBeats)
            {
                // Shorten notes before posInBeats but ending after posInBeats
                float newDuration = posInBeats - pos;
                NoteEvent ne2 = new NoteEvent(ne, newDuration);
                it.set(ne2);
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
        ArrayList<NoteEvent> toBeAdded = new ArrayList<>();

        ListIterator<NoteEvent> it = listIterator();
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
            addOrdered(ne);
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

        ListIterator<NoteEvent> it = listIterator();
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
            addOrdered(ne);
        }
    }

    /**
     * Get the NoteEvents whose start position is in the [posFrom:posTo] or [posFrom:posTo[ range.
     *
     * @param range
     * @param excludeUpperBound
     * @return
     */
    public List<NoteEvent> getNotes(FloatRange range, boolean excludeUpperBound)
    {
        var res = new ArrayList<NoteEvent>();
        for (NoteEvent ne : this)
        {
            if (range.contains(ne.getPositionInBeats(), excludeUpperBound))
            {
                res.add(ne);
            }
            if (ne.getPositionInBeats() > range.to)
            {
                break;
            }
        }
        return res;
    }

    /**
     * Get the notes still ringing at specified position.
     * <p>
     *
     * @param posInBeats
     * @param strict If true, notes starting or ending at posInBeats are excluded.
     * @return The list of notes whose startPos is before (or equals) posInBeats and endPos eafter (or equals) posInBeats
     */
    public List<NoteEvent> getCrossingNotes(float posInBeats, boolean strict)
    {
        ArrayList<NoteEvent> res = new ArrayList<>();
        var it = listIterator();
        while (it.hasNext())
        {
            NoteEvent ne = it.next();
            float pos = ne.getPositionInBeats();
            if ((strict && pos >= posInBeats) || (!strict && pos > posInBeats))
            {
                break;
            }
            if ((strict && pos + ne.getDurationInBeats() > posInBeats) || (!strict && pos + ne.getDurationInBeats() >= posInBeats))
            {
                res.add(ne);
            }
        }
        return res;
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
        for (NoteEvent ne : this)
        {
            for (MidiEvent me : ne.toMidiEvents(channel))
            {
                track.add(me);
            }
        }
    }

    /**
     * Replace all events by the same events but with position shifted.
     *
     * @param shiftInBeats The value added to each event's position.
     * @throws IllegalArgumentException If an event's position become negative.
     */
    public void shiftEvents(float shiftInBeats)
    {
        var it = listIterator();
        while (it.hasNext())
        {
            NoteEvent ne = it.next();
            float newPosInBeats = ne.getPositionInBeats() + shiftInBeats;
            if (newPosInBeats < 0)
            {
                throw new IllegalArgumentException("ne=" + ne + " shiftInBeats=" + shiftInBeats);
            }
            NoteEvent shiftedNe = new NoteEvent(ne, ne.getDurationInBeats(), newPosInBeats);
            it.set(shiftedNe);
        }
    }

    /**
     * Get a chord made of all unique pitch NoteEvents present in the phrase.
     *
     * @return
     */
    public Chord getChord()
    {
        Chord c = new Chord(this);
        return c;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Phrase[ch=").append(channel).append("] size=").append(size()).append(" notes=").append(super.toString());
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
        for (NoteEvent ne : this)
        {
            LOGGER.info(ne.toString());
        }
    }

    /**
     * Remove overlapped notes with identical pitch.
     * <p>
     * A note N1 is overlapped by N2 if N1's noteOn event occurs after N2's noteOn event and N1's noteOff event occurs before N2's
     * noteOff event.
     */
    public void removeOverlappedNotes()
    {
        // Get all the notes grouped per pitch
        HashMap<Integer, List<NoteEvent>> mapPitchNotes = new HashMap<>();
        for (NoteEvent ne : this)
        {
            int pitch = ne.getPitch();
            List<NoteEvent> nes = mapPitchNotes.get(pitch);
            if (nes == null)
            {
                nes = new ArrayList<>();
                mapPitchNotes.put(pitch, nes);
            }
            nes.add(ne);
        }

        // Search for overlapped notes
        HashSet<NoteEvent> overlappedNotes = new HashSet<>();
        for (Integer pitch : mapPitchNotes.keySet())
        {
            List<NoteEvent> notes = mapPitchNotes.get(pitch);
            if (notes.size() == 1)
            {
                continue;
            }
            ArrayList<NoteEvent> noteOnBuffer = new ArrayList<>();
            for (NoteEvent ne : notes)
            {
                FloatRange fr = ne.getBeatRange();
                boolean removed = false;
                Iterator<NoteEvent> itOn = noteOnBuffer.iterator();
                while (itOn.hasNext())
                {
                    NoteEvent noteOn = itOn.next();
                    FloatRange frOn = noteOn.getBeatRange();
                    if (frOn.to < fr.from)
                    {
                        // Remove noteOns which are now Off
                        itOn.remove();
                    } else if (frOn.to >= fr.to)
                    {
                        // Cur note is overlapped !
                        overlappedNotes.add(ne);
                        removed = true;
                        break;
                    }
                }
                if (!removed)
                {
                    noteOnBuffer.add(ne);
                }
            }
        }

        // Now remove the notes
        removeAll(overlappedNotes);
    }

    /**
     * Change the octave of notes whose pitch is above highLimit or below lowLimit.
     * <p>
     * Fixed new notes's PARENT_NOTE client property is preserved.
     *
     * @param lowLimit There must be at least 1 octave between lowLimit and highLimit
     * @param highLimit There must be at least 1 octave between lowLimit and highLimit
     */
    public void limitPitch(int lowLimit, int highLimit)
    {
        if (lowLimit < 0 || highLimit > 127 || lowLimit > highLimit || highLimit - lowLimit < 11)
        {
            throw new IllegalArgumentException("lowLimit=" + lowLimit + " highLimit=" + highLimit);
        }
        var it = listIterator();
        while (it.hasNext())
        {
            NoteEvent ne = it.next();
            int pitch = ne.getPitch();
            while (pitch < lowLimit)
            {
                pitch += 12;
            }
            while (pitch > highLimit)
            {
                pitch -= 12;
            }
            NoteEvent newNe = new NoteEvent(ne, pitch);
            it.set(newNe);
        }
    }

}

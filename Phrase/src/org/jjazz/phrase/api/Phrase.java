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
package org.jjazz.phrase.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jjazz.harmony.api.Chord;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.LongRange;

/**
 * A list of NoteEvents that are kept sorted by start position.
 * <p>
 * LinkedList implementation to speed up item insertion/remove rather than random access. Fire change events when modified (note
 * added/removed/moved/replaced).
 * <p>
 * WARNING: Direct add/remove/set methods have been secured to preserve the start position order. However when using an iterator
 * such as the one obtained via listIterator(), one could insert a wrongly placed NoteEvent using Iterator.set. So if you use such
 * method make sure you do not break the position order.
 * <p>
 */
public class Phrase extends LinkedList<NoteEvent> implements Serializable
{

    /**
     * Fired when a new NoteEvent replaced another one at the same position.
     * <p>
     * oldValue=old NoteEvent, newValue=new NoteEvent.
     */
    public static final String PROP_NOTE_SET = "PropNoteSet";
    /**
     * oldValue=index, newValue=added NoteEvent.
     */
    public static final String PROP_NOTE_ADDED = "PropNoteAdd";
    /**
     * oldValue=index, newValue=removed NoteEvent.
     */
    public static final String PROP_NOTE_REMOVED = "PropNoteRemove";
    /**
     * Fired when a NoteEvent was Phrase.move() was called.
     * <p>
     * oldValue=old NoteEvent, newValue=new NoteEvent
     */
    public static final String PROP_NOTE_MOVED = "PropNoteMoved";

    /**
     * NoteEvent client property set when new NoteEvents are created from existing ones.
     */
    public static final String PARENT_NOTE = "PARENT_NOTE";
    private final int channel;
    private final PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(Phrase.class.getSimpleName());

    /**
     *
     * @param channel
     */
    public Phrase(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   //NOI18N
        }
        this.channel = channel;
    }


    /**
     * Overridden to preserve order by position.
     *
     * @param ne
     * @return
     */
    @Override
    public boolean add(NoteEvent ne)
    {
        addOrdered(ne, true);
        return true;
    }

    /**
     * Overridden to check order by position is preserved.
     *
     * @param ne
     */
    @Override
    public void addFirst(NoteEvent ne)
    {
        if (!isEmpty() && ne.compareTo(get(0)) > 0)
        {
            throw new IllegalArgumentException("ne=" + ne + " this=" + this);
        }
        super.addFirst(ne);
        pcs.firePropertyChange(PROP_NOTE_ADDED, 0, ne);
    }

    /**
     * Overridden to check order by position is preserved.
     *
     * @param ne
     */
    @Override
    public void addLast(NoteEvent ne)
    {
        if (!isEmpty() && ne.compareTo(peekLast()) < 0)
        {
            throw new IllegalArgumentException("ne=" + ne + " this=" + this);
        }
        super.addLast(ne);
        pcs.firePropertyChange(PROP_NOTE_ADDED, size() - 1, ne);
    }

    /**
     * Overridden to check that order by position is preserved.
     *
     * @param index
     * @param ne
     */
    @Override
    public void add(int index, NoteEvent ne)
    {
        NoteEvent prev = index > 0 ? get(index - 1) : null;
        NoteEvent next = index < size() ? get(index) : null;
        if ((prev != null && prev.compareTo(ne) > 0)
                || (next != null && next.compareTo(ne) < 0))
        {
            throw new IllegalArgumentException("index=" + index + " ne=" + ne + " this=" + this);
        }
        super.add(index, ne);
        pcs.firePropertyChange(PROP_NOTE_ADDED, index, ne);
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
            addOrdered(mne.clone(), true);
        }
    }

    /**
     * Overridden to throw UnsupportedOperationException, can't be used since NoteEvents are ordered by position.
     *
     * @param index
     * @param c
     * @return
     */
    @Override
    public boolean addAll​(int index, Collection<? extends NoteEvent> c)
    {
        throw new UnsupportedOperationException("Can't be used: notes are ordered by position");
    }

    /**
     * Add a collection of NoteEvents.
     * <p>
     * Overridden to rely on addOrdered().
     *
     * @param c
     * @return
     */
    @Override
    public boolean addAll(Collection<? extends NoteEvent> c)
    {
        c.forEach(ne -> addOrdered(ne, true));
        return !c.isEmpty();
    }

    /**
     * Overridden to fire a change event.
     *
     * @return
     */
    @Override
    public NoteEvent poll()
    {
        return removeFirst();
    }

    /**
     * Overridden to fire a change event.
     *
     * @return
     */
    @Override
    public NoteEvent pollFirst()
    {
        return removeFirst();
    }

    /**
     * Overridden to fire a change event.
     *
     * @return
     */
    @Override
    public NoteEvent pollLast()
    {
        return removeLast();
    }


    public boolean remove(NoteEvent ne)
    {
        int index = indexOf(ne);
        if (index != -1)
        {
            remove(index);
        }
        return index != -1;
    }

    public boolean removeFirstOccurence(NoteEvent ne)
    {
        return remove(ne);
    }

    public boolean removeLastOccurence(NoteEvent ne)
    {
        int index = lastIndexOf(ne);
        if (index != -1)
        {
            remove(index);
        }
        return index != -1;
    }

    @Override
    public NoteEvent remove(int index)
    {
        var ne = super.remove(index);
        pcs.firePropertyChange(PROP_NOTE_REMOVED, index, ne);
        return ne;
    }

    @Override
    public NoteEvent removeFirst()
    {
        return isEmpty() ? null : remove(0);
    }

    @Override
    public NoteEvent removeLast()
    {
        return isEmpty() ? null : remove(size() - 1);
    }


    /**
     * Overridden to check that NoteEvent position is unchanged.
     * <p>
     * Fire a PROP_NOTE_SET change event.
     *
     * @param index
     * @param ne    Must have the same position that the existing NoteEvent
     */
    @Override
    public NoteEvent set(int index, NoteEvent ne)
    {
        NoteEvent oldNe = get(index);
        if (oldNe.getPositionInBeats() != ne.getPositionInBeats())
        {
            throw new IllegalArgumentException("index=" + index + " ne=" + ne + " oldNe=" + oldNe);
        }
        super.set(index, ne);
        pcs.firePropertyChange(PROP_NOTE_SET, oldNe, ne);
        return oldNe;
    }

    /**
     * Remove the specified NoteEvent and create a clone NoteEvent at specified position.
     * <p>
     * Fire a PROP_MOVED_EVENT change event.
     *
     * @param ne          Must belong to this Phrase.
     * @param newPosition
     * @return The created event at newPosition
     */
    public NoteEvent move(NoteEvent ne, float newPosition)
    {
        if (!contains(ne) || newPosition < 0)
        {
            throw new IllegalArgumentException("ne=" + ne + " this=" + this + " newPosition=" + newPosition);
        }

        if (ne.getPositionInBeats() == newPosition)
        {
            return ne.clone();
        }

        NoteEvent movedNe = ne.getCopyPos(newPosition);
        super.remove(ne);       // Don't fire a change event
        addOrdered(movedNe, false);     // Dont't fire a change event
        pcs.firePropertyChange(PROP_NOTE_MOVED, ne, movedNe);
        return movedNe;
    }

    /**
     * Add NoteEvents from a list of NOTE_ON/OFF Midi events at MidiConst.PPQ_RESOLUTION.
     * <p>
     * NOTE_ON events without a corresponding NOTE_OFF event are ignored.
     *
     * @param midiEvents       MidiEvents which are not ShortMessage.Note_ON/OFF are ignored. Must be ordered by tick position,
     *                         resolution must be MidiConst.PPQ_RESOLUTION.
     * @param posInBeatsOffset The position in natural beats of the first tick of the track.
     * @param ignoreChannel    If true, add also NoteEvents for MidiEvents which do not match this phrase channel.
     * @see MidiUtilities#getMidiEvents(javax.sound.midi.Track, java.util.function.Predicate, LongRange)
     * @see MidiConst#PPQ_RESOLUTION
     */
    public void add(List<MidiEvent> midiEvents, float posInBeatsOffset, boolean ignoreChannel)
    {

        // Build the NoteEvents
        MidiEvent[] lastNoteOn = new MidiEvent[128];
        for (MidiEvent me : midiEvents)
        {
            long tick = me.getTick();
            ShortMessage sm = MidiUtilities.getNoteShortMessage(me.getMessage());
            if (sm == null)
            {
                // It's not a note ON/OFF message
                continue;
            }

            int pitch = sm.getData1();
            int velocity = sm.getData2();
            int eventChannel = sm.getChannel();


            if (!ignoreChannel && channel != eventChannel)
            {
                // Different channel, ignore
                continue;
            }

            if (sm.getCommand() == ShortMessage.NOTE_ON && velocity > 0)
            {
                // NOTE_ON
                lastNoteOn[pitch] = me;

            } else
            {
                MidiEvent meOn = lastNoteOn[pitch];

                // NOTE_OFF
                if (meOn != null)
                {
                    // Create the NoteEvent
                    long tickOn = meOn.getTick();
                    ShortMessage smOn = (ShortMessage) meOn.getMessage();
                    float duration = ((float) tick - tickOn) / MidiConst.PPQ_RESOLUTION;
                    float posInBeats = posInBeatsOffset + ((float) tickOn / MidiConst.PPQ_RESOLUTION);
                    NoteEvent ne = new NoteEvent(pitch, duration, smOn.getData2(), posInBeats);
                    addOrdered(ne, true);

                    // Clean the last NoteOn
                    lastNoteOn[pitch] = null;
                } else
                {
                    // A note Off without a previous note On, do nothing
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
     * @param tester
     * @return
     */
    public Phrase getFilteredPhrase(Predicate<NoteEvent> tester)
    {
        Phrase res = new Phrase(channel);
        stream()
                .filter(tester)
                .forEach(ne -> res.add(ne.clone()));      // Don't need addOrdered here
        return res;
    }

    /**
     * Return a new Phrase with only filtered notes processed by the specified mapper.
     * <p>
     * Notes of the returned phrase will have their PARENT_NOTE client property set to:<br>
     * - source note's PARENT_NOTE client property if this property is not null, or<br>
     * - the source note from this phrase.
     *
     * @param tester
     * @param mapper
     * @return
     */
    public Phrase getFilteredAndMappedPhrase(Predicate<NoteEvent> tester, Function<NoteEvent, NoteEvent> mapper)
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
                res.addOrdered(newNe, false);
            }
        }
        return res;
    }

    /**
     * Change the Note attributes (pitch, duration, velocity) of the notes which satisfy the specified tester.
     * <p>
     *
     * @param tester Process the NoteEvent which satisfy this tester.
     * @param mapper The mapper must not change the NoteEvent position
     */
    public void processNote(Predicate<NoteEvent> tester, Function<NoteEvent, NoteEvent> mapper)
    {
        for (var it = listIterator(); it.hasNext();)
        {
            NoteEvent ne = it.next();
            if (tester.test(ne))
            {
                NoteEvent newNe = mapper.apply(ne);
                assert newNe.getPositionInBeats() == ne.getPositionInBeats() : "newNe=" + newNe + " ne=" + ne;
                newNe.setClientProperties(ne);
                it.set(newNe);  // Iterator.set() has direct access to LinkedList internal data, it will NOT use Phrase.set()
                pcs.firePropertyChange(PROP_NOTE_SET, ne, newNe);
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
        return getFilteredAndMappedPhrase(ne -> true, ne ->
        {
            int v = MidiUtilities.limit(f.apply(ne.getVelocity()));
            NoteEvent newNe = ne.getCopyVel(v);
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
        processNote(ne -> true, ne ->
        {
            int v = MidiUtilities.limit(f.apply(ne.getVelocity()));
            NoteEvent newNe = ne.getCopyVel(v);
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
        return getFilteredAndMappedPhrase(ne -> true, ne ->
        {
            int p = MidiUtilities.limit(f.apply(ne.getPitch()));
            NoteEvent newNe = ne.getCopyPitch(p);
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
        processNote(ne -> true, ne ->
        {
            int p = MidiUtilities.limit(f.apply(ne.getPitch()));
            NoteEvent newNe = ne.getCopyPitch(p);
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
        int index = size() - 1;
        while (it.hasPrevious())
        {
            NoteEvent ne = it.previous();
            float pos = ne.getPositionInBeats();
            if (pos >= posInBeats)
            {
                // Remove notes after posInBeats
                it.remove();
                pcs.firePropertyChange(PROP_NOTE_REMOVED, index, ne);
            } else if (pos + ne.getDurationInBeats() > posInBeats)
            {
                // Shorten notes before posInBeats but ending after posInBeats
                float newDuration = posInBeats - pos;
                NoteEvent ne2 = ne.getCopyDur(newDuration);
                it.set(ne2);      // Iterator.set() has direct access to LinkedList internal data, it will NOT use Phrase.set(), so careful when using it!
                pcs.firePropertyChange(PROP_NOTE_SET, ne, ne2);
            }
            index--;
        }
    }


    /**
     * Get a new phrase which keeps only the notes in the specified beat range, taking into account possible
     * live-played/non-quantized notes via the beatWindow parameter.
     * <p>
     * First, if beatWindow &gt; 0 then notes starting in the range [range.from-beatWindow; range.from[ are changed in the
     * returned phrase so they start at range.from, and notes starting in the range [range.to-beatWindow; range.to[ are removed.
     * <p>
     * Then, if a note is starting before startPos and ending after range.from: <br>
     * - if keepLeft is false, the note is removed<br>
     * - if keepLeft is true, the note is replaced by a shorter identical one starting at range.from
     * <p>
     * If a note is starting before range.to and ending after range.to: <br>
     * - if cutRight == 0 the note is not removed.<br>
     * - if cutRight == 1, the note is replaced by a shorter identical that ends at range.to.<br>
     * - if cutRight == 2, the note is removed<br>
     * <p>
     *
     * @param range
     * @param keepLeft
     * @param cutRight
     * @param beatWindow A tolerance window if this phrase contains live-played/non-quantized notes. Typical value is 0.1f.
     * @return
     * @see #silence(org.jjazz.util.api.FloatRange, boolean, boolean)
     */
    public Phrase getSlice(FloatRange range, boolean keepLeft, int cutRight, float beatWindow)
    {
        checkArgument(cutRight >= 0 && cutRight <= 2, "cutRight=%s", cutRight);
        checkArgument(beatWindow >= 0);

        Phrase res = new Phrase(channel);


        // Preprocess to accomodate for live playing / non-quantized notes
        List<NoteEvent> beatWindowProcessedNotes = new ArrayList<>();
        if (beatWindow > 0)
        {
            FloatRange frLeft = range.from - beatWindow > 0 ? new FloatRange(range.from - beatWindow, range.from) : null;
            FloatRange frRight = new FloatRange(range.to - beatWindow, range.to);

            ListIterator<NoteEvent> it = listIterator();
            while (it.hasNext())
            {
                var ne = it.next();
                var neBr = ne.getBeatRange();
                if (frLeft != null && frLeft.contains(neBr.from, true))
                {
                    if (frLeft.contains(neBr, false))
                    {
                        // Note is fully contained in the beatWindow! Probably a drums/perc note, move it
                        NoteEvent newNe = ne.getCopyPos(range.from);
                        res.addOrdered(newNe, false);
                    } else
                    {
                        // Note crosses range.from, make it start at range.from
                        float newDur = Math.max(neBr.to - range.from, 0.05f);
                        NoteEvent newNe = ne.getCopy(newDur, range.from);
                        res.addOrdered(newNe, false);
                    }
                    beatWindowProcessedNotes.add(ne);

                } else if (frRight.contains(neBr.from, true))
                {
                    // Remove the note
                    beatWindowProcessedNotes.add(ne);
                }
            }

        }


        // 
        ListIterator<NoteEvent> it = listIterator();
        while (it.hasNext())
        {
            NoteEvent ne = it.next();


            if (beatWindowProcessedNotes.contains(ne))
            {
                // It's already processed, skip
                continue;
            }

            float nePosFrom = ne.getPositionInBeats();
            float nePosTo = nePosFrom + ne.getDurationInBeats();


            if (nePosFrom < range.from)
            {
                // It starts before the slice zone, don't add, except if it overlaps the slice zone
                if (keepLeft && nePosTo > range.from)
                {
                    // It even goes beyond the slice zone!                                        
                    if (nePosTo > range.to)
                    {
                        switch (cutRight)
                        {
                            case 0:
                                // Add it but don't change its end point
                                break;
                            case 1:
                                // Make it shorter
                                nePosTo = range.to;
                                break;
                            case 2:
                                // Do not add
                                continue;
                            default:
                                throw new IllegalStateException("cutRight=" + cutRight);
                        }
                    }
                    float newDur = nePosTo - range.from;
                    NoteEvent newNe = ne.getCopy(newDur, range.from);
                    res.addOrdered(newNe, false);
                }
            } else if (nePosFrom < range.to)
            {
                // It starts in the slice zone, add it
                if (nePosTo <= range.to)
                {
                    // It ends in the slice zone, easy
                    res.addOrdered(ne, false);
                } else
                {
                    // It goes beyond the slice zone
                    switch (cutRight)
                    {
                        case 0:
                            // Add it anyway
                            res.addOrdered(ne, false);
                            break;
                        case 1:
                            // Add it but make it shorter
                            float newDur = range.to - nePosFrom;
                            NoteEvent newNe = ne.getCopyDur(newDur);
                            res.addOrdered(newNe, false);
                            break;
                        case 2:
                            // Do not add
                            break;
                        default:
                            throw new IllegalStateException("cutRight=" + cutRight);
                    }
                }
            } else
            {
                // It starts after the slice zone, do nothing
            }
        }


        return res;
    }


    /**
     * Remove all notes whose start position is in the specified beat range, taking into account possible
     * live-played/non-quantized notes via the beatWindow parameter.
     * <p>
     * If a note is starting before range.from and ending after range.from: <br>
     * - if cutLeft is false, the note is not removed.<br>
     * - if cutLeft is true, the note is replaced by a shorter identical that ends at range.from, except if the note starts in the
     * range [range.from-beatWindow;range.from[, then it's removed.<p>
     * If a note is starting before range.to and ending after range.to: <br>
     * - if keepRight is false, the note is removed, except if the note starts in the range [range.to-beatWindow;range.to[, then
     * it's replaced by a shorter identical one starting at range<br>
     * - if keepRight is true, the note is replaced by a shorter identical one starting at range.to<br>
     *
     * @param range
     * @param cutLeft
     * @param keepRight
     * @param beatWindow A tolerance window if this phrase contains live-played/non-quantized notes. Typical value is 0.1f.
     * @see #getSlice(org.jjazz.util.api.FloatRange, boolean, int, float)
     */
    public void silence(FloatRange range, boolean cutLeft, boolean keepRight, float beatWindow)
    {
        checkArgument(beatWindow >= 0);

        ArrayList<NoteEvent> toBeAdded = new ArrayList<>();

        FloatRange frLeft = FloatRange.EMPTY_FLOAT_RANGE;
        FloatRange frRight = FloatRange.EMPTY_FLOAT_RANGE;

        if (beatWindow > 0)
        {
            frLeft = range.from - beatWindow >= 0 ? new FloatRange(range.from - beatWindow, range.from) : FloatRange.EMPTY_FLOAT_RANGE;
            frRight = range.to - beatWindow >= range.from ? new FloatRange(range.to - beatWindow, range.to) : FloatRange.EMPTY_FLOAT_RANGE;
        }


        ListIterator<NoteEvent> it = listIterator();
        int index = 0;
        while (it.hasNext())
        {
            NoteEvent ne = it.next();
            float nePosFrom = ne.getPositionInBeats();
            float nePosTo = nePosFrom + ne.getDurationInBeats();

            if (nePosFrom < range.from)
            {
                if (nePosTo <= range.from)
                {
                    // Leave note unchanged

                } else if (cutLeft)
                {
                    // Replace the note by a shorter one, except if it's in the frLeft beat window
                    if (!frLeft.contains(nePosFrom, true))
                    {

                        // Replace
                        float newDur = range.from - nePosFrom;
                        NoteEvent newNe = ne.getCopyDur(newDur);
                        it.set(newNe);
                        pcs.firePropertyChange(PROP_NOTE_SET, ne, newNe);


                        // Special case if note was extending beyond range.to and keepRight is true, add a note after range
                        if (keepRight && nePosTo > range.to)
                        {
                            newDur = nePosTo - range.to;
                            newNe = ne.getCopy(newDur, range.to);
                            toBeAdded.add(newNe);
                        }
                    } else
                    {
                        // It's in the left beat window, directly remove the note
                        it.remove();
                        pcs.firePropertyChange(PROP_NOTE_REMOVED, index, ne);
                        index--;
                    }

                }
            } else if (nePosFrom < range.to)
            {
                // Remove the note
                it.remove();
                pcs.firePropertyChange(PROP_NOTE_REMOVED, index, ne);
                index--;

                // Re-add a note after range if required
                if (nePosTo > range.to && (keepRight || frRight.contains(nePosFrom, true)))
                {
                    float newDur = nePosTo - range.to;
                    NoteEvent newNe = ne.getCopy(newDur, range.to);
                    toBeAdded.add(newNe);
                }
            } else
            {
                // nePosFrom is after range.to
                // Nothing
            }

            index++;
        }

        // Add the new NoteEvents after range
        for (NoteEvent ne : toBeAdded)
        {
            addOrdered(ne, true);
        }
    }

    /**
     * Get the NoteEvents which match the tester and whose start position is in the [posFrom:posTo] or [posFrom:posTo[ range.
     *
     * @param tester
     * @param range
     * @param excludeUpperBound
     * @return
     */
    public List<NoteEvent> getNotes(Predicate<NoteEvent> tester, FloatRange range, boolean excludeUpperBound)
    {
        var res = new ArrayList<NoteEvent>();
        for (NoteEvent ne : this)
        {
            if (tester.test(ne) && range.contains(ne.getPositionInBeats(), excludeUpperBound))
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
     * @param strict     If true, notes starting or ending at posInBeats are excluded.
     * @return The list of notes whose startPos is before (or equals) posInBeats and range.to eafter (or equals) posInBeats
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
        toMidiEvents().forEach(me -> track.add(me));
    }

    /**
     * Get all the phrase notes as MidiEvents.
     * <p>
     * Tick resolution used is MidiConst.PPQ_RESOLUTION.
     *
     * @return Each note is converted into 1 MidiEvent for note ON, 1 for the note OFF
     */
    public List<MidiEvent> toMidiEvents()
    {
        List<MidiEvent> res = new ArrayList<>();
        for (NoteEvent ne : this)
        {
            for (MidiEvent me : ne.toMidiEvents(channel))
            {
                res.add(me);
            }
        }
        return res;
    }

    /**
     * Shift all events.
     * <p>
     * Fire PROP_NOTE_MOVED change events.
     *
     * @param shiftInBeats The value added to each event's position.
     * @throws IllegalArgumentException If an event's position become negative.
     */
    public void shiftAllEvents(float shiftInBeats)
    {
        if (shiftInBeats == 0)
        {
            return;
        }

        // Select head or tail processing to preserve position order
        if (shiftInBeats < 0)
        {
            var it = listIterator();
            while (it.hasNext())
            {
                NoteEvent ne = it.next();
                float newPosInBeats = ne.getPositionInBeats() + shiftInBeats;
                if (newPosInBeats < 0)
                {
                    throw new IllegalArgumentException("ne=" + ne + " shiftInBeats=" + shiftInBeats);   //NOI18N
                }
                NoteEvent shiftedNe = ne.getCopyPos(newPosInBeats);
                it.set(shiftedNe);
                pcs.firePropertyChange(PROP_NOTE_MOVED, ne, shiftedNe);
            }
        } else
        {
            var it = listIterator(size());
            while (it.hasPrevious())
            {
                NoteEvent ne = it.previous();
                float newPosInBeats = ne.getPositionInBeats() + shiftInBeats;
                NoteEvent shiftedNe = ne.getCopyPos(newPosInBeats);
                it.set(shiftedNe);
                pcs.firePropertyChange(PROP_NOTE_MOVED, ne, shiftedNe);
            }
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
        return sb.toString();
    }

    public void dump()
    {
        LOGGER.info(toString());   //NOI18N
        for (NoteEvent ne : this)
        {
            LOGGER.info(ne.toString());   //NOI18N
        }
    }

    /**
     * Get notes matching the specified tester and return them per pitch.
     *
     * @param tester
     * @return The matching notes grouped per pitch.
     */
    public Map<Integer, List<NoteEvent>> getNotesPerPitch(Predicate<NoteEvent> tester)
    {
        var resMap = new HashMap<Integer, List<NoteEvent>>();

        for (var ne : this)
        {
            if (tester.test(ne))
            {
                List<NoteEvent> nes = resMap.get(ne.getPitch());
                if (nes == null)
                {
                    nes = new ArrayList<>();
                    resMap.put(ne.getPitch(), nes);
                }
                nes.add(ne);
            }
        }

        return resMap;
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
        overlappedNotes.forEach(ne -> remove(ne));
    }

    /**
     * Change the octave of notes whose pitch is above highLimit or below lowLimit.
     * <p>
     *
     * @param lowLimit  There must be at least 1 octave between lowLimit and highLimit
     * @param highLimit There must be at least 1 octave between lowLimit and highLimit
     */
    public void limitPitch(int lowLimit, int highLimit)
    {
        if (lowLimit < 0 || highLimit > 127 || lowLimit > highLimit || highLimit - lowLimit < 11)
        {
            throw new IllegalArgumentException("lowLimit=" + lowLimit + " highLimit=" + highLimit);   //NOI18N
        }

        Predicate<NoteEvent> tester = ne -> ne.getPitch() < lowLimit || ne.getPitch() > highLimit;
        Function<NoteEvent, NoteEvent> mapper = ne ->
        {
            int pitch = ne.getPitch();
            while (pitch < lowLimit)
            {
                pitch += 12;
            }
            while (pitch > highLimit)
            {
                pitch -= 12;
            }
            return ne.getCopyPitch(pitch);
        };

        processNote(tester, mapper);

    }

    /**
     * Compare the specified phrase with this phrase, but tolerate slight differences in position and duration.
     *
     *
     * @param p
     * @param nearWindow Used to compare NoteEvents position and duration.
     * @return
     * @see NoteEvent#equalsNearPosition(org.jjazz.phrase.api.NoteEvent, float)
     */
    public boolean equalsNearPosition(Phrase p, float nearWindow)
    {
        checkNotNull(p);
        if (size() != p.size())
        {
            return false;
        }
        Iterator<NoteEvent> pIt = p.iterator();
        for (NoteEvent ne : this)
        {
            if (!pIt.next().equalsNearPosition(ne, nearWindow))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Save the specified Phrase as a string.
     * <p>
     * Examples: <br>
     * - "[8|NoteEventStr0|NoteEventStr1]" means a Phrase for channel 8 with 2 NoteEvents<br>
     * - "[0]" empty phrase on channel 0
     *
     * @param p
     * @return
     * @see loadAsString(String)
     */
    static public String saveAsString(Phrase p)
    {
        StringJoiner joiner = new StringJoiner("|", "[", "]");
        joiner.add(String.valueOf(p.getChannel()));
        p.forEach(ne -> joiner.add(NoteEvent.saveAsString(ne)));
        return joiner.toString();
    }

    /**
     * Create a Phrase from the specified string.
     * <p>
     * Example "[8|NoteEventStr0|NoteEventStr1]" means a Phrase for channel 8 with 2 NoteEvents.
     *
     * @param s
     * @return
     * @throws ParseException If s is not a valid string.
     * @see saveAsString(Phrase)
     */
    static public Phrase loadAsString(String s) throws ParseException
    {
        Phrase p = null;
        s = s.trim();
        if (s.length() >= 3 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']')    // minimum string is e.g. [2]
        {
            String[] strs = s.substring(1, s.length() - 1).split("\\|");
            try
            {
                int channel = Integer.parseInt(strs[0]);
                p = new Phrase(channel);
                for (int i = 1; i < strs.length; i++)
                {
                    NoteEvent ne = NoteEvent.loadAsString(strs[i]);
                    p.addOrdered(ne, false);
                }
            } catch (IllegalArgumentException | ParseException ex)       // Will catch NumberFormatException too
            {
                // Nothing
                LOGGER.warning("loadAsString() Catched ex=" + ex.getMessage());
            }

        }

        if (p == null)
        {
            throw new ParseException("Phrase.loadAsString() Invalid Phrase string s=" + s, 0);
        }
        return p;
    }


    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }
    // --------------------------------------------------------------------- 
    // Private methods
    // ---------------------------------------------------------------------


    /**
     * Add a NoteEvent at the correct index using NoteEvent natural ordering.
     * <p>
     * @param mne
     * @param fireAddEvent If true fire a PROP_NOTE_ADD change event.
     * @see NoteEvent#compareTo(org.jjazz.phrase.api.NoteEvent)
     */
    protected void addOrdered(NoteEvent mne, boolean fireAddEvent)
    {
        int res = Collections.binarySearch(this, mne);

        int index;
        if (res >= 0)
        {
            index = res;
            LOGGER.log(Level.FINE, "addOrdered() Inserting mne={0} but the same NoteEvent already exists at index={2}. this={1}", new Object[]
            {
                mne, this, index
            });
        } else
        {
            index = -(res + 1);
        }

        super.add(index, mne);

        if (fireAddEvent)
        {
            pcs.firePropertyChange(PROP_NOTE_ADDED, index, mne);
        }
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // ---------------------------------------------------------------------
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");


    }


    /**
     * Rely on loadFromString()/saveAsString() methods.
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -1823649110L;

        private final int spVERSION = 1;
        private final String spSaveString;

        private SerializationProxy(Phrase p)
        {
            spSaveString = saveAsString(p);
        }

        private Object readResolve() throws ObjectStreamException
        {
            Phrase p;
            try
            {
                p = loadAsString(spSaveString);
            } catch (ParseException ex)
            {
                throw new InvalidObjectException(ex.getMessage());
            }
            return p;
        }
    }
}

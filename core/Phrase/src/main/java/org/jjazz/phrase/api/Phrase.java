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
package org.jjazz.phrase.api;

import com.google.common.collect.HashBiMap;
import com.thoughtworks.xstream.XStream;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.harmony.api.Note.Accidental;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * A collection of NoteEvents that are kept sorted by start position.
 * <p>
 * Fire change events when modified, see the PROP_* values. Fire undoable events.
 * <p>
 */
public class Phrase implements Collection<NoteEvent>, SortedSet<NoteEvent>, NavigableSet<NoteEvent>, Serializable
{

    /**
     * newValue=collection of added NoteEvents.
     */
    public static final String PROP_NOTES_ADDED = "PropNoteAdd";
    /**
     * Same as PROP_NOTES_ADDED except this notifies an intermediate operation: a final non-adjusting operation will occur later.
     */
    public static final String PROP_NOTES_ADDED_ADJUSTING = "PropNoteAddAdjusting";
    /**
     * newValue=collection of removed NoteEvents.
     */
    public static final String PROP_NOTES_REMOVED = "PropNoteRemove";
    /**
     * Same as PROP_NOTES_REMOVED except this notifies an intermediate operation: a final non-adjusting operation will occur later.
     */
    public static final String PROP_NOTES_REMOVED_ADJUSTING = "PropNoteRemoveAdjusting";
    /**
     * newValue=a map with keys=OldNoteEvents and values=NewNoteEvents
     */
    public static final String PROP_NOTES_MOVED = "PropNoteMoved";
    /**
     * Same as PROP_NOTES_MOVED except this notifies an intermediate operation: a final non-adjusting operation will occur later.
     */
    public static final String PROP_NOTES_MOVED_ADJUSTING = "PropNoteMovedAdjusting";
    /**
     * Fired when a new NoteEvent replaced another one.
     * <p>
     * newValue=a map with keys=OldNoteEvents and values=NewNoteEvents
     */
    public static final String PROP_NOTES_REPLACED = "PropNoteReplaced";
    /**
     * Same as PROP_NOTES_REPLACED except this notifies an intermediate operation: a final non-adjusting operation will occur later.
     */
    public static final String PROP_NOTES_REPLACED_ADJUSTING = "PropNoteReplacedAdjusting";

    /**
     * NoteEvent client property set when new NoteEvents are created from existing ones.
     */
    public static final String PARENT_NOTE = "PARENT_NOTE";

    private final int channel;
    private final boolean isDrums;
    private final TreeSet<NoteEvent> noteEvents = new TreeSet<>();
    /**
     * The listeners for undoable edits in this LeadSheet.
     */
    protected transient List<UndoableEditListener> undoListeners = new ArrayList<>();
    private final PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(Phrase.class.getSimpleName());

    /**
     * Construct a new melodic phrase.
     *
     * @param channel
     */
    public Phrase(int channel)
    {
        this(channel, false);
    }

    /**
     * Construct a new melodic or drums phrase.
     *
     * @param channel
     * @param isDrums
     */
    public Phrase(int channel, boolean isDrums)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);
        }
        this.channel = channel;
        this.isDrums = isDrums;
    }

    /**
     * Relies on ==.
     * <p>
     * For comparison of the NoteEvent musical attributes, use equalsAsNoteNearPosition() with nearWindow=0.
     *
     * @param o
     * @return
     * @see #equalsAsNoteNearPosition(org.jjazz.phrase.api.Phrase, float)
     */
    @Override
    public boolean equals(Object o)
    {
        return this == o;
    }

    /**
     * Compare to a phrase using only NoteEvent.equalsAsNoteNearPosition() on each note.
     *
     * @param p
     * @param nearWindow Must be &gt;= 0
     * @return
     * @see NoteEvent#equalsAsNoteNearPosition(org.jjazz.phrase.api.NoteEvent, float)
     */
    public boolean equalsAsNoteNearPosition(Phrase p, float nearWindow)
    {
        boolean b = false;
        if (p.size() == size())
        {
            var pIt = p.iterator();
            b = stream().allMatch(ne -> pIt.next().equalsAsNoteNearPosition(ne, nearWindow));
        }
        return b;
    }


    /**
     * Check if p represents the same musical phrase than this phrase, indenpendantly of the key.
     * <p>
     * Compare the intervals and the note positions.
     *
     * @param p
     * @param checkNoteDuration If true check also that notes durations are equal +/- 2*nearWindow.
     * @param nearWindow        Accept note-to-note position difference of +/- nearWindow. Use 0 for a strict position comparison, or Float.MAX_VALUE to ignore
     *                          position (and possibly duration) differences.
     * @return
     * @see NoteEvent#equalsAsNoteNearPosition(org.jjazz.phrase.api.NoteEvent, float)
     */
    public boolean equalsAsIntervals(Phrase p, boolean checkNoteDuration, float nearWindow)
    {
        boolean b = false;
        if (p.size() == size())
        {
            b = true;
            NoteEvent lastNe = null, lastPNe = null;
            var pIt = p.iterator();
            for (var ne : this)
            {
                var pNe = pIt.next();
                if (!ne.isNear(pNe.getPositionInBeats(), nearWindow))
                {
                    b = false;
                    break;
                }
                if (checkNoteDuration && Math.abs(ne.getDurationInBeats() - pNe.getDurationInBeats()) > 2 * nearWindow)
                {
                    b = false;
                    break;
                }
                if (lastNe != null)
                {
                    if ((ne.getPitch() - lastNe.getPitch()) != (pNe.getPitch() - lastPNe.getPitch()))
                    {
                        b = false;
                        break;
                    }

                }
                lastNe = ne;
                lastPNe = pNe;
            }
        }

        return b;
    }


    /**
     * Check if the phrase is for non-melodic instruments.
     *
     * @return
     */
    public boolean isDrums()
    {
        return isDrums;
    }

    /**
     * Replace a NoteEvent by another one.
     * <p>
     * Same as removing old + adding new, except it only fires a PROP_NOTES_REPLACED change event.
     *
     * @param oldNe
     * @param newNe
     */
    public void replace(NoteEvent oldNe, NoteEvent newNe)
    {
        if (oldNe == newNe)
        {
            return;
        }
        replaceAll(Map.of(oldNe, newNe), false);
    }

    /**
     * Replace a NoteEvent by another one.
     * <p>
     * Same as removing old + adding new, except it only fires a PROP_NOTES_REPLACED or PROP_NOTES_REPLACED_ADJUSTING change event.
     *
     * @param oldNe
     * @param newNe
     * @param isAdjusting If true fire a PROP_NOTES_REPLACED_ADJUSTING instead of PROP_NOTES_REPLACED
     */
    public void replace(NoteEvent oldNe, NoteEvent newNe, boolean isAdjusting)
    {
        if (oldNe == newNe)
        {
            return;
        }
        replaceAll(Map.of(oldNe, newNe), isAdjusting);
    }

    /**
     * Replace NoteEvents.
     * <p>
     * Same as removing old + adding new, except it only fires a PROP_NOTES_REPLACED or PROP_NOTES_REPLACED_ADJUSTING change event.
     *
     * @param mapOldNew   A map where keys=oldNoteEvents and values=newNoteEvents
     * @param isAdjusting If true fire a PROP_NOTES_REPLACED_ADJUSTING instead of PROP_NOTES_REPLACED
     */
    public void replaceAll(Map<NoteEvent, NoteEvent> mapOldNew, boolean isAdjusting)
    {
        replaceOrMoveAll(mapOldNew, false, isAdjusting);
    }


    /**
     * Move a NoteEvent.
     * <p>
     * Same as removing the old one + adding copy at new position , except it only fires a PROP_NOTES_MOVED.
     *
     * @param ne     Must belong to this phrase
     * @param newPos
     * @return The new NoteEvent at newPos. Return ne if position unchanged.
     */
    public NoteEvent move(NoteEvent ne, float newPos)
    {
        if (Float.compare(ne.getPositionInBeats(), newPos) == 0)
        {
            return ne;
        }
        return moveAll(Map.of(ne, newPos), false).iterator().next();
    }

    /**
     * Move a NoteEvent.
     * <p>
     * Same as removing the old one + adding copy at new position , except it only fires a PROP_NOTES_MOVED or PROP_NOTES_MOVED_ADJUSTING
     *
     * @param ne          Must belong to this phrase
     * @param newPos
     * @param isAdjusting If true fire a PROP_NOTES_MOVED_ADJUSTING instead of PROP_NOTES_MOVED
     * @return The new NoteEvent at newPos. Return ne if position unchanged.
     */
    public NoteEvent move(NoteEvent ne, float newPos, boolean isAdjusting)
    {
        if (Float.compare(ne.getPositionInBeats(), newPos) == 0)
        {
            return ne;
        }
        return moveAll(Map.of(ne, newPos), isAdjusting).iterator().next();
    }

    /**
     * Move NoteEvents.
     * <p>
     * Same as removing the old one + adding copy at new position , except it only fires a PROP_NOTES_MOVED or PROP_NOTES_MOVED_ADJUSTING change event.
     *
     * @param mapNoteNewPos A map where keys=NoteEvents and values=new positions
     * @param isAdjusting   If true fire a PROP_NOTES_MOVED_ADJUSTING instead of PROP_NOTES_MOVED
     * @return The created NoteEvents at new positions.
     */
    public Set<NoteEvent> moveAll(Map<NoteEvent, Float> mapNoteNewPos, boolean isAdjusting)
    {
        var res = new HashSet<NoteEvent>();
        var mapOldNew = new HashMap<NoteEvent, NoteEvent>();

        mapNoteNewPos.keySet().forEach(ne -> 
        {
            var newNe = ne.setPosition(mapNoteNewPos.get(ne), true);
            mapOldNew.put(ne, newNe);
            res.add(newNe);
        });
        replaceOrMoveAll(mapOldNew, true, isAdjusting);
        return res;
    }

    /**
     * Clone NoteEvents from the specified Phrase and add them to this Phrase.
     *
     * @param p
     */
    public void add(Phrase p)
    {
        add(p, false);
    }

    /**
     * Add a Phrase from the specified Phrase.
     *
     * @param p
     * @param doNotCloneNotes If true directly add the NoteEvents without cloning them -so client properties might be changed.
     */
    public void add(Phrase p, boolean doNotCloneNotes)
    {
        Collection<NoteEvent> notes = p;
        if (!doNotCloneNotes)
        {
            notes = p.stream()
                    .map(ne -> ne.clone())
                    .toList();
        }
        addAll(notes);
    }

    /**
     * A deep clone: returned phrase contains clones of the original NoteEvents.
     *
     * @return
     */
    @Override
    public Phrase clone()
    {
        var p = new Phrase(getChannel(), isDrums());
        for (var ne : this)
        {
            p.add(ne.clone());
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
     * Notes are sorted by position.
     *
     * @return
     */
    public List<NoteEvent> getNotes()
    {
        return new ArrayList<>(noteEvents);
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
        var subSet = subSet(range, excludeUpperBound);
        var res = subSet.stream()
                .filter(ne -> tester.test(ne))
                .toList();
        return res;
    }


    /**
     * Get the beat range from start of first note to end of last note.
     *
     * @return FloatRange.EMPTY_FLOAT_RANGE if phrase is empty.
     */
    public FloatRange getNotesBeatRange()
    {
        if (isEmpty())
        {
            return FloatRange.EMPTY_FLOAT_RANGE;
        }
        float startPos = first().getPositionInBeats();
        NoteEvent lastNote = last();
        FloatRange fr = new FloatRange(startPos, lastNote.getPositionInBeats() + lastNote.getDurationInBeats());
        return fr;
    }


    /**
     * Check that the note is valid before adding it to the Phrase.
     * <p>
     * Default implements return true, but subclasses might override to do some sanity checks.
     *
     * @param ne
     * @return
     */
    protected boolean canAddNote(NoteEvent ne)
    {
        return true;
    }

    /**
     * Get a clone of this Phrase with only filtered notes processed by the specified mapper.
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
        Phrase res = clone();           // Use clone()+clear() so that method also works for Phrase subclasses
        res.clear();

        for (NoteEvent ne : this)
        {
            if (tester.test(ne))
            {
                NoteEvent newNe = mapper.apply(ne);
                if (newNe != ne)
                {
                    newNe.getClientProperties().set(ne.getClientProperties());
                    if (newNe.getClientProperties().get(PARENT_NOTE) == null)
                    {
                        newNe.getClientProperties().put(PARENT_NOTE, ne);         // If no previous PARENT_NOTE client property we can add one
                    }
                }
                res.add(newNe);
            }
        }
        return res;
    }

    /**
     * Transform the notes which satisfy the specified tester.
     * <p>
     * Once the mapper has produced a new NoteEvent, the old one is removed and the new one is added. Fire the PROP_NOTES_REPLACED change event.
     *
     * @param tester Process the NoteEvent which satisfy this tester.
     * @param mapper Transform each NoteEvent.
     */
    public void processNotes(Predicate<NoteEvent> tester, Function<NoteEvent, NoteEvent> mapper)
    {
        Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
        for (var ne : this.toArray(NoteEvent[]::new))
        {
            if (tester.test(ne))
            {
                NoteEvent newNe = mapper.apply(ne);
                if (newNe != ne)
                {
                    mapOldNew.put(ne, newNe);
                }
            }
        }
        replaceAll(mapOldNew, false);
    }

    /**
     * Get a new phrase with notes velocity changed.
     * <p>
     * Velocity is always maintained between 0 and 127. Notes of the returned phrase will have their PARENT_NOTE client property set to:<br>
     * - source note's PARENT_NOTE client property if this property is not null, or<br>
     * - the source note from this phrase
     *
     * @param f A function modifying the velocity.
     * @return A new phrase
     */
    public Phrase getProcessedPhraseVelocity(Function<Integer, Integer> f)
    {
        return getProcessedPhrase(ne -> true, ne -> 
        {
            int v = MidiConst.clamp(f.apply(ne.getVelocity()));
            NoteEvent newNe = ne.setVelocity(v, true);
            return newNe;
        });
    }

    /**
     * Change the velocity of all notes of this Phrase.
     * <p>
     * Velocity is always maintained between 0 and 127. Fire the PROP_NOTES_REPLACED change event.
     *
     * @param f A function modifying the velocity.
     */
    public void processVelocity(Function<Integer, Integer> f)
    {
        processNotes(ne -> true, ne -> 
        {
            int v = MidiConst.clamp(f.apply(ne.getVelocity()));
            NoteEvent newNe = ne.setVelocity(v, true);
            return newNe;
        });
    }

    /**
     * Get a new phrase with all notes changed.
     * <p>
     * Pitch is always maintained between 0 and 127. Notes of the returned phrase will have their PARENT_NOTE client property set to:<br>
     * - source note's PARENT_NOTE client property if this property is not null, or<br>
     * - the source note from this phrase
     *
     * @param f A function modifying the pitch.
     * @return A new phrase
     */
    public Phrase getProcessedPhrasePitch(Function<Integer, Integer> f)
    {
        return getProcessedPhrase(ne -> true, ne -> 
        {
            int p = MidiConst.clamp(f.apply(ne.getPitch()));
            NoteEvent newNe = ne.setPitch(p, true);
            return newNe;
        });
    }

    /**
     * Change the pitch of all notes of this Phrase.
     * <p>
     * Pitch is always maintained between 0 and 127. Fire the PROP_NOTES_REPLACED change event.
     *
     * @param f A function modifying the pitch.
     */
    public void processPitch(Function<Integer, Integer> f)
    {
        processNotes(ne -> true, ne -> 
        {
            int p = MidiConst.clamp(f.apply(ne.getPitch()));
            NoteEvent newNe = ne.setPitch(p, true);
            return newNe;
        });
    }

    /**
     *
     * @return 0 If phrase is empty.
     */
    public float getLastEventPosition()
    {
        return isEmpty() ? 0 : last().getPositionInBeats();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        String drums = isDrums ? " dr]" : "]";
        sb.append("Phrase[ch=").append(channel).append(drums).append(" sz=").append(size()).append(" notes=").append(getNotes().toString());
        return sb.toString();
    }

    /**
     * A String like "A3 Bb3 C4".
     *
     * @param showPos Show notes with beat position, e.g. "A3[2.3]"
     * @return
     */
    public String toStringSimple(boolean showPos)
    {
        StringJoiner joiner = new StringJoiner("  ");
        DecimalFormat df = new DecimalFormat("#.##");
        for (var n : this)
        {
            String s = n.toPianoOctaveString() + (showPos ? "[" + df.format(n.getPositionInBeats()) + "]" : "");
            joiner.add(s);
        }
        return joiner.toString();
    }

    public String toStringOneNotePerLine()
    {
        StringJoiner joiner = new StringJoiner("\n");
        forEach(ne -> joiner.add(ne.toString()));
        return joiner.toString();
    }

    public void dump()
    {
        LOGGER.info(toString());
        LOGGER.info(toStringOneNotePerLine());
    }

    /**
     * Check if propertyName corresponds to one of the adjusting events PROP_NOTES_ADDED_ADJUSTING, PROP_NOTES_REMOVED_ADJUSTING etc.
     *
     * @param propertyName
     * @return
     */
    static public boolean isAdjustingEvent(String propertyName)
    {
        return propertyName.equals(PROP_NOTES_ADDED_ADJUSTING)
                || propertyName.equals(PROP_NOTES_REMOVED_ADJUSTING)
                || propertyName.equals(PROP_NOTES_MOVED_ADJUSTING)
                || propertyName.equals(PROP_NOTES_REPLACED_ADJUSTING);
    }

    // --------------------------------------------------------------------- 
    // Collection interface
    // ---------------------------------------------------------------------

    /**
     * Add a NoteEvent.
     * <p>
     * Fire a PROP_NOTES_ADDED event.
     *
     * @param ne
     * @return False if ne was already part of this Phrase.
     */
    @Override
    public boolean add(NoteEvent ne)
    {
        return add(ne, false);
    }

    /**
     * Add a NoteEvent.
     * <p>
     * Fire a PROP_NOTES_ADDED or PROP_NOTES_ADDED_ADJUSTING event.
     *
     * @param ne
     * @param isAdjusting
     * @return
     */
    public boolean add(NoteEvent ne, boolean isAdjusting)
    {
        checkAddNote(ne);

        var res = noteEvents.add(ne);
        if (res)
        {
            String PROP = isAdjusting ? PROP_NOTES_ADDED_ADJUSTING : PROP_NOTES_ADDED;
            String PROP_UNDO = isAdjusting ? PROP_NOTES_REMOVED_ADJUSTING : PROP_NOTES_REMOVED;

            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Add note")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "add.undoBody() ne={0}", ne);
                    noteEvents.remove(ne);
                    pcs.firePropertyChange(PROP_UNDO, null, Arrays.asList(ne));
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "add.redoBody() ne={0}", ne);
                    noteEvents.add(ne);
                    pcs.firePropertyChange(PROP, null, Arrays.asList(ne));
                }
            };

            fireUndoableEditHappened(edit);
            pcs.firePropertyChange(PROP, null, Arrays.asList(ne));

        } else
        {
            LOGGER.log(Level.WARNING, "add() ne={0} already present. Phrase={1}, ignoring", new Object[]
            {
                ne, this
            });
        }
        return res;
    }

    /**
     * Add several NoteEvents.
     * <p>
     * Fire a PROP_NOTES_ADDED_ADJUSTING or PROP_NOTES_ADDED change event.
     *
     * @param collection
     * @param isAdjusting
     * @return
     */
    public boolean addAll(java.util.Collection<? extends NoteEvent> collection, boolean isAdjusting)
    {
        if (collection.isEmpty())
        {
            return false;
        }

        String PROP = isAdjusting ? PROP_NOTES_ADDED_ADJUSTING : PROP_NOTES_ADDED;
        String PROP_UNDO = isAdjusting ? PROP_NOTES_REMOVED_ADJUSTING : PROP_NOTES_REMOVED;

        boolean res = false;

        List<NoteEvent> addedList = new ArrayList<>();
        for (var ne : collection)
        {
            checkAddNote(ne);
            if (noteEvents.add(ne))
            {
                res = true;
                addedList.add(ne);
            }
        }

        if (res)
        {
            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Add notes")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "add.undoBody() addedList={0}", addedList);
                    noteEvents.removeAll(addedList);
                    pcs.firePropertyChange(PROP_UNDO, null, addedList);
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "add.redoBody() addedList={0}", addedList);
                    noteEvents.addAll(addedList);
                    pcs.firePropertyChange(PROP, null, addedList);
                }
            };

            fireUndoableEditHappened(edit);
            pcs.firePropertyChange(PROP, null, addedList);
        }

        return res;
    }

    /**
     * Add several NoteEvents.
     * <p>
     * Fire a PROP_NOTES_ADDED event.
     *
     * @param collection
     * @return True if at least one element was added.
     */
    @Override
    public boolean addAll(java.util.Collection<? extends NoteEvent> collection)
    {
        return addAll(collection, false);
    }

    /**
     * Remove a NoteEvent.
     * <p>
     * Fire a PROP_NOTES_REMOVED event
     *
     * @param o
     * @return The removed NoteEvent
     */
    @Override
    public boolean remove(Object o)
    {
        return remove(o, false);
    }

    /**
     * Remove a NoteEvent.
     * <p>
     * Fire a PROP_NOTES_REMOVED or PROP_NOTES_REMOVED_ADJUSTING event.
     *
     * @param o
     * @param isAdjusting
     * @return
     */
    public boolean remove(Object o, boolean isAdjusting)
    {
        if (o instanceof NoteEvent ne && noteEvents.remove(ne))
        {
            String PROP = isAdjusting ? PROP_NOTES_REMOVED_ADJUSTING : PROP_NOTES_REMOVED;
            String PROP_UNDO = isAdjusting ? PROP_NOTES_ADDED_ADJUSTING : PROP_NOTES_ADDED;

            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Remove note")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "remove.undoBody() ne={0}", ne);
                    noteEvents.add(ne);
                    pcs.firePropertyChange(PROP_UNDO, null, Arrays.asList(ne));
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "remove.redoBody() ne={0}", ne);
                    noteEvents.remove(ne);
                    pcs.firePropertyChange(PROP, null, Arrays.asList(ne));
                }
            };

            fireUndoableEditHappened(edit);

            pcs.firePropertyChange(PROP, null, Arrays.asList(ne));
            return true;
        }
        return false;
    }

    @Override
    public void clear()
    {
        removeAll(new ArrayList<>(noteEvents));
    }

    /**
     * Remove elements.
     * <p>
     * Fire a PROP_NOTES_REMOVED change event.
     *
     * @param collection
     * @return True if at least one element was removed.
     */
    @Override
    public boolean removeAll(java.util.Collection<?> collection)
    {
        return removeAll(collection, false);
    }

    public boolean removeAll(java.util.Collection<?> collection, boolean isAdjusting)
    {
        if (collection.isEmpty())
        {
            return false;
        }

        String PROP = isAdjusting ? PROP_NOTES_REMOVED_ADJUSTING : PROP_NOTES_REMOVED;
        String PROP_UNDO = isAdjusting ? PROP_NOTES_ADDED_ADJUSTING : PROP_NOTES_ADDED;

        boolean res = false;


        List<NoteEvent> removedList = new ArrayList<>();
        for (var o : collection)
        {
            if (o instanceof NoteEvent ne && noteEvents.remove(ne))
            {
                res = true;
                removedList.add(ne);
            }
        }


        if (res)
        {
            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Remove notes")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "add.undoBody() removedList={0}", removedList);
                    noteEvents.addAll(removedList);
                    pcs.firePropertyChange(PROP_UNDO, null, removedList);
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "add.redoBody() removedList={0}", removedList);
                    noteEvents.removeAll(removedList);
                    pcs.firePropertyChange(PROP, null, removedList);
                }
            };

            fireUndoableEditHappened(edit);
            pcs.firePropertyChange(PROP, null, removedList);
        }

        return res;
    }

    @Override
    public int size()
    {
        return noteEvents.size();
    }

    @Override
    public boolean isEmpty()
    {
        return noteEvents.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return noteEvents.contains(o);
    }

    @Override
    public Iterator<NoteEvent> iterator()
    {
        // Decorate the real iterator to fire an event when removing
        return decorateIteratorRemove(noteEvents.iterator());
    }


    @Override
    public Object[] toArray()
    {
        return noteEvents.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return noteEvents.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return noteEvents.containsAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        var toBeRemoved = noteEvents.stream()
                .filter(ne -> !c.contains(ne))
                .toList();
        return removeAll(toBeRemoved);
    }

    /**
     * Shift all events.
     * <p>
     *
     * @param shiftInBeats            The value added to each event's position.
     * @param handleNegativePositions If true reset negative note positions to 0
     * @throws IllegalArgumentException If handleNegativePositions is false and a note's position becomes negative.
     */
    public void shiftAllEvents(float shiftInBeats, boolean handleNegativePositions)
    {
        if (shiftInBeats == 0)
        {
            return;
        }

        Map<NoteEvent, Float> toBeMoved = new HashMap<>();

        // Select head or tail processing to facilitate preservation of position order
        if (shiftInBeats < 0)
        {
            for (var ne : this)
            {
                float newPosInBeats = ne.getPositionInBeats() + shiftInBeats;
                if (handleNegativePositions && newPosInBeats < 0)
                {
                    newPosInBeats = 0;
                }
                if (newPosInBeats < 0)
                {
                    throw new IllegalArgumentException("ne=" + ne + " shiftInBeats=" + shiftInBeats);
                }
                toBeMoved.put(ne, newPosInBeats);
            }
        } else
        {
            var it = descendingIterator();
            while (it.hasNext())
            {
                NoteEvent ne = it.next();
                float newPosInBeats = ne.getPositionInBeats() + shiftInBeats;
                toBeMoved.put(ne, newPosInBeats);
            }
        }

        moveAll(toBeMoved, false);

    }


    // --------------------------------------------------------------------- 
    // SortedSet interface
    // ---------------------------------------------------------------------
    @Override
    public Comparator<? super NoteEvent> comparator()
    {
        return noteEvents.comparator();
    }

    /**
     *
     * @return Return value is unmodifiable.
     */
    @Override
    public SortedSet<NoteEvent> subSet(NoteEvent fromElement, NoteEvent toElement)
    {
        return Collections.unmodifiableSortedSet(noteEvents.subSet(fromElement, toElement));
    }

    /**
     *
     * @return Return value is unmodifiable.
     */
    @Override
    public SortedSet<NoteEvent> headSet(NoteEvent toElement)
    {
        return Collections.unmodifiableSortedSet(noteEvents.headSet(toElement));
    }

    /**
     *
     * @return Return value is unmodifiable.
     */
    @Override
    public SortedSet<NoteEvent> tailSet(NoteEvent fromElement)
    {
        return Collections.unmodifiableSortedSet(noteEvents.tailSet(fromElement));
    }

    @Override
    public NoteEvent first()
    {
        return noteEvents.first();
    }

    @Override
    public NoteEvent last()
    {
        return noteEvents.last();
    }

    // --------------------------------------------------------------------- 
    // NavigableSet interface
    // ---------------------------------------------------------------------
    @Override
    public NoteEvent lower(NoteEvent e)
    {
        return noteEvents.lower(e);
    }

    @Override
    public NoteEvent floor(NoteEvent e)
    {
        return noteEvents.floor(e);
    }

    @Override
    public NoteEvent ceiling(NoteEvent e)
    {
        return noteEvents.ceiling(e);
    }

    @Override
    public NoteEvent higher(NoteEvent e)
    {
        return noteEvents.higher(e);
    }

    @Override
    public NoteEvent pollFirst()
    {
        if (isEmpty())
        {
            return null;
        }
        var ne = first();
        remove(ne);
        return ne;
    }

    @Override
    public NoteEvent pollLast()
    {
        if (isEmpty())
        {
            return null;
        }
        var ne = last();
        remove(ne);
        return ne;
    }

    /**
     *
     * @return Return value is unmodifiable.
     */
    @Override
    public NavigableSet<NoteEvent> descendingSet()
    {
        return noteEvents.descendingSet();
    }

    /**
     *
     * @return Return value is unmodifiable.
     */
    @Override
    public Iterator<NoteEvent> descendingIterator()
    {
        return decorateIteratorRemove(noteEvents.descendingIterator());
    }

    /**
     * A subset of all notes starting between the specified notes.
     *
     * @return Return value is unmodifiable.
     */
    @Override
    public NavigableSet<NoteEvent> subSet(NoteEvent fromElement, boolean fromInclusive, NoteEvent toElement, boolean toInclusive)
    {
        return Collections.unmodifiableNavigableSet(noteEvents.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    /**
     * A subset of all notes starting in the specified range.
     *
     * @param range
     * @param excludeUpperBound
     * @return Return value is unmodifiable.
     */
    public NavigableSet<NoteEvent> subSet(FloatRange range, boolean excludeUpperBound)
    {
        return Collections.unmodifiableNavigableSet(noteEvents.subSet(getFloorNote(range.from), true, getCeilNote(range.to),
                excludeUpperBound));
    }


    /**
     *
     * @return Return value is unmodifiable.
     */
    @Override
    public NavigableSet<NoteEvent> headSet(NoteEvent toElement, boolean inclusive)
    {
        return Collections.unmodifiableNavigableSet(noteEvents.headSet(toElement, inclusive));
    }

    /**
     *
     * @return Return value is unmodifiable.
     */
    @Override
    public NavigableSet<NoteEvent> tailSet(NoteEvent fromElement, boolean inclusive)
    {
        return Collections.unmodifiableNavigableSet(noteEvents.tailSet(fromElement, inclusive));
    }


    /**
     * Save the specified Phrase as a string.
     * <p>
     * Examples: <br>
     * - "[8|NoteEventStr0|NoteEventStr1]" means a meodic Phrase for channel 8 with 2 NoteEvents<br>
     * - "[drums_9|NoteEventStr0]" means a drums Phrase for channel 9 with 1 NoteEvent<br>
     * - "[0]" empty phrase on channel 0
     *
     * @param p
     * @return
     * @see loadAsString(String)
     */
    static public String saveAsString(Phrase p)
    {
        StringJoiner joiner = new StringJoiner("|", "[", "]");
        String drums = p.isDrums() ? "drums_" : "";
        joiner.add(drums + String.valueOf(p.getChannel()));
        p.forEach(ne -> joiner.add(NoteEvent.saveAsString(ne)));
        return joiner.toString();
    }

    /**
     * Create a Phrase from the specified string.
     * <p>
     * Example: "[8|NoteEventStr0|NoteEventStr1]" means a meodic Phrase for channel 8 with 2 NoteEvents.<br>
     * Example: "[drums_8|NoteEventStr0|NoteEventStr1]" means a drums Phrase for channel 8 with 2 NoteEvents.
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
                boolean drums = false;
                if (strs[0].startsWith("drums_"))
                {
                    drums = true;
                    strs[0] = strs[0].substring(6);
                }
                int channel = Integer.parseInt(strs[0]);
                p = new Phrase(channel, drums);
                for (int i = 1; i < strs.length; i++)
                {
                    NoteEvent ne = NoteEvent.loadAsString(strs[i]);
                    p.add(ne);
                }
            } catch (IllegalArgumentException | ParseException ex)       // Will catch NumberFormatException too
            {
                // Nothing
                LOGGER.log(Level.WARNING, "loadAsString() Catched ex={0}", ex.getMessage());
            }

        }

        if (p == null)
        {
            throw new ParseException("Phrase.loadAsString() Invalid Phrase string s=" + s, 0);
        }
        return p;
    }

    /**
     * Create a NoteEvent limit to be used as a floor/min position as per NoteEvent.compareTo().
     * <p>
     * For use with the subSet(), tailSet() etc. methods.
     *
     * @param pos
     * @return
     */
    static public NoteEvent getFloorNote(float pos)
    {
        return new NoteEvent(0, 0.000001f, 0, pos, Accidental.FLAT);
    }

    /**
     * Create a NoteEvent limit to be used as a ceil/max position as per NoteEvent.compareTo().
     * <p>
     * For use with the subSet(), tailSet() etc. methods.
     *
     * @param pos
     * @return
     */
    static public NoteEvent getCeilNote(float pos)
    {
        return new NoteEvent(127, 10000f, 127, pos, Accidental.FLAT);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        if (edit == null)
        {
            throw new IllegalArgumentException("edit=" + edit);
        }
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        for (UndoableEditListener l : undoListeners.toArray(UndoableEditListener[]::new))
        {
            l.undoableEditHappened(event);
        }
    }

    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
    }
    // --------------------------------------------------------------------- 
    // Private methods
    // ---------------------------------------------------------------------


    /**
     * Do the moveAll or replaceAll operation.
     *
     * @param mapOldNew
     * @param move
     * @param isAdjusting
     */
    private void replaceOrMoveAll(Map<NoteEvent, NoteEvent> mapOldNew, boolean move, boolean isAdjusting)
    {
        if (mapOldNew.isEmpty())
        {
            return;
        }

        String PROP;
        if (isAdjusting)
        {
            PROP = move ? PROP_NOTES_MOVED_ADJUSTING : PROP_NOTES_REPLACED_ADJUSTING;
        } else
        {
            PROP = move ? PROP_NOTES_MOVED : PROP_NOTES_REPLACED;
        }


        final var biMapOldNew = HashBiMap.create(mapOldNew);

        // Change state
        for (var oldNe : mapOldNew.keySet())
        {
            var newNe = mapOldNew.get(oldNe);
            if (noteEvents.remove(oldNe))
            {
                checkAddNote(newNe);
                if (!noteEvents.add(newNe))
                {
                    throw new IllegalArgumentException("newNe=" + newNe + " already belongs to this phrase=" + this);
                }
            } else
            {
                throw new IllegalArgumentException("oldNe=" + oldNe + " does not belong to this phrase=" + this);
            }
        }


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Replace/Move notes")
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "replaceOrMoveImpl.undoBody() mapOldNew={0}", mapOldNew);
                var map = biMapOldNew.inverse();
                for (var oldNe : map.keySet())
                {
                    var newNe = map.get(oldNe);
                    noteEvents.remove(oldNe);
                    noteEvents.add(newNe);
                }
                pcs.firePropertyChange(PROP, null, map);
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "replaceOrMoveImpl.redoBody() mapOldNew={0}", mapOldNew);
                var map = biMapOldNew;
                for (var oldNe : map.keySet())
                {
                    var newNe = map.get(oldNe);
                    noteEvents.remove(oldNe);
                    noteEvents.add(newNe);
                }
                pcs.firePropertyChange(PROP, null, map);
            }
        };

        fireUndoableEditHappened(edit);

        pcs.firePropertyChange(PROP, null, biMapOldNew);
    }

    /**
     * Decorate the specified Iterator to make sure remove() fires a change event.
     *
     * @param it
     * @return
     */
    private Iterator<NoteEvent> decorateIteratorRemove(final Iterator<NoteEvent> it)
    {
        var res = new Iterator<NoteEvent>()
        {
            NoteEvent lastNext;

            @Override
            public boolean hasNext()
            {
                return it.hasNext();
            }

            @Override
            public NoteEvent next()
            {
                lastNext = it.next();
                return lastNext;
            }

            @Override
            public void remove()
            {
                it.remove();


                if (lastNext != null)
                {
                    NoteEvent ne = lastNext;

                    // Create the undoable event
                    UndoableEdit edit = new SimpleEdit("IteratorRemove note " + ne)
                    {
                        @Override
                        public void undoBody()
                        {
                            LOGGER.log(Level.FINER, "IteratorRemove.undoBody() ne={0}", ne);
                            noteEvents.add(ne);
                            pcs.firePropertyChange(PROP_NOTES_ADDED, null, Arrays.asList(ne));
                        }

                        @Override
                        public void redoBody()
                        {
                            LOGGER.log(Level.FINER, "IteratorRemove.redoBody() ne={0}", ne);
                            noteEvents.remove(ne);
                            pcs.firePropertyChange(PROP_NOTES_REMOVED, null, Arrays.asList(ne));
                        }
                    };

                    fireUndoableEditHappened(edit);

                    pcs.firePropertyChange(PROP_NOTES_REMOVED, null, Arrays.asList(ne));
                }
            }
        };
        return res;
    }

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files
                    xstream.alias("Phrase", Phrase.class);
                    xstream.alias("PhraseSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                }

                case MIDIMIX_LOAD ->
                {
                    // Nothing
                }
                case MIDIMIX_SAVE ->
                {
                    // Nothing
                }
                default -> throw new AssertionError(instanceId.name());
            }
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

    private void checkAddNote(NoteEvent ne)
    {
        if (!canAddNote(ne))
        {
            throw new IllegalArgumentException("ne=" + ne + " this=" + this);
        }
    }


    /**
     * Rely on loadFromString()/saveAsString() methods.
     * <p>
     * spVERSION2 introduces XStream aliases (XStreamConfig)
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -1823649110L;

        private int spVERSION = 2;          // Do not make final!
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

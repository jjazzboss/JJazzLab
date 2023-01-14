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

import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
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
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.util.api.FloatRange;

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
    public static final String PROP_NOTE_ADDED = "PropNoteAdd";
    /**
     * newValue=collection of removed NoteEvents.
     */
    public static final String PROP_NOTE_REMOVED = "PropNoteRemove";
    /**
     * oldValue=old NoteEvent at old position, value=new NoteEvent at new position.
     */
    public static final String PROP_NOTE_MOVED = "PropNoteMoved";
    /**
     * Fired when a new NoteEvent replaced another one.
     * <p>
     * oldValue=old NoteEvent, newValue=new NoteEvent.
     */
    public static final String PROP_NOTE_REPLACED = "PropNoteReplaced";
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
            throw new IllegalArgumentException("channel=" + channel);   //NOI18N
        }
        this.channel = channel;
        this.isDrums = isDrums;
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
     * Same as removing old + adding new, except it only fires a PROP_NOTE_REPLACED change event.
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

        if (noteEvents.remove(oldNe))
        {
            checkAddNote(newNe);
            if (!noteEvents.add(newNe))
            {
                throw new IllegalArgumentException("newNe=" + newNe + " already belongs to this phrase=" + this);
            }


            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Replace note " + oldNe + " by " + newNe)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "Replace.undoBody() oldNe={0} newNe={1}", new Object[]
                    {
                        newNe, oldNe
                    });   //NOI18N
                    noteEvents.remove(newNe);
                    noteEvents.add(oldNe);
                    pcs.firePropertyChange(PROP_NOTE_REPLACED, newNe, oldNe);
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "Replace.redoBody() oldNe={0} newNe={1}", new Object[]
                    {
                        oldNe, newNe
                    });   //NOI18N
                    noteEvents.remove(oldNe);
                    noteEvents.add(newNe);
                    pcs.firePropertyChange(PROP_NOTE_REPLACED, oldNe, newNe);
                }
            };

            fireUndoableEditHappened(edit);
            pcs.firePropertyChange(PROP_NOTE_REPLACED, oldNe, newNe);

        } else
        {
            throw new IllegalArgumentException("oldNe=" + oldNe + " does not belong to this phrase=" + this);
        }
    }

    /**
     * Move a NoteEvent.
     * <p>
     * Same as removing the old one + adding copy at new position , except it only fires a PROP_NOTE_MOVED change event.
     *
     * @param ne     Must belong to this phrase
     * @param newPos
     * @return The new NoteEvent at newPos. Return ne if position unchanged.
     */
    public NoteEvent move(NoteEvent ne, float newPos)
    {
        if (ne.getPositionInBeats() == newPos)
        {
            return ne;
        }

        NoteEvent newNe = ne.getCopyPos(newPos);

        if (noteEvents.remove(ne))
        {
            checkAddNote(newNe);
            if (!noteEvents.add(newNe))
            {
                throw new IllegalArgumentException("newNe=" + newNe + " already belongs to this phrase=" + this);
            }

            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Move note " + ne + " to " + newNe)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "Move.undoBody() oldNe={0} ne={1}", new Object[]
                    {
                        newNe, ne
                    });   //NOI18N
                    noteEvents.remove(newNe);
                    noteEvents.add(ne);
                    pcs.firePropertyChange(PROP_NOTE_MOVED, newNe, ne);
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "Move.redoBody() oldNe={0} newNe={1}", new Object[]
                    {
                        ne, newNe
                    });   //NOI18N
                    noteEvents.remove(ne);
                    noteEvents.add(newNe);
                    pcs.firePropertyChange(PROP_NOTE_MOVED, ne, newNe);
                }
            };

            fireUndoableEditHappened(edit);
            pcs.firePropertyChange(PROP_NOTE_MOVED, ne, newNe);
        } else
        {
            throw new IllegalArgumentException("ne=" + ne + " does not belong to this phrase=" + this);
        }

        return newNe;
    }

    /**
     * Add a phrase, ie clone and add each NoteEvent from the specified Phrase.
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
    protected void add(Phrase p, boolean doNotCloneNotes)
    {
        List<NoteEvent> notes;
        if (doNotCloneNotes)
        {
            notes = p.getNotes();
        } else
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
        return getProcessedPhrase(ne -> true, ne -> ne.clone());
    }

    /**
     * Compare the notes (pitch, velocity), but tolerate slight differences in position and duration.
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
            if (!pIt.next().equalsAsNoteNearPosition(ne, nearWindow))
            {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @return 1-16
     */
    public int getChannel()
    {
        return channel;
    }


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
    public FloatRange getBeatRange()
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
     * Default implement return true, but subclasses might override to do some sanity checks.
     *
     * @param ne
     * @return
     */
    protected boolean canAddNote(NoteEvent ne)
    {
        return true;
    }

    /**
     * Get a new Phrase with only filtered notes processed by the specified mapper.
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
        Phrase res = new Phrase(channel, isDrums);
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
                res.add(newNe);
            }
        }
        return res;
    }

    /**
     * Transform the notes which satisfy the specified tester.
     * <p>
     * Once the mapper has produced a new NoteEvent, the old one is removed and the new one is added. Fire the PROP_NOTE_REPLACED
     * change event.
     *
     *
     * @param tester Process the NoteEvent which satisfy this tester.
     * @param mapper Transform each NoteEvent.
     */
    public void processNotes(Predicate<NoteEvent> tester, Function<NoteEvent, NoteEvent> mapper)
    {
        for (var ne : this.toArray(NoteEvent[]::new))
        {
            if (tester.test(ne))
            {
                NoteEvent newNe = mapper.apply(ne);
                replace(ne, newNe);
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
    public Phrase getProcessedPhraseVelocity(Function<Integer, Integer> f)
    {
        return getProcessedPhrase(ne -> true, ne ->
        {
            int v = MidiUtilities.limit(f.apply(ne.getVelocity()));
            NoteEvent newNe = ne.getCopyVel(v);
            return newNe;
        });
    }

    /**
     * Change the velocity of all notes of this Phrase.
     * <p>
     * Velocity is always maintained between 0 and 127. Fire the PROP_NOTE_REPLACED change event.
     *
     * @param f A function modifying the velocity.
     */
    public void processVelocity(Function<Integer, Integer> f)
    {
        processNotes(ne -> true, ne ->
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
    public Phrase getProcessedPhrasePitch(Function<Integer, Integer> f)
    {
        return getProcessedPhrase(ne -> true, ne ->
        {
            int p = MidiUtilities.limit(f.apply(ne.getPitch()));
            NoteEvent newNe = ne.getCopyPitch(p);
            return newNe;
        });
    }

    /**
     * Change the pitch of all notes of this Phrase.
     * <p>
     * Pitch is always maintained between 0 and 127. Fire the PROP_NOTE_REPLACED change event.
     *
     * @param f A function modifying the pitch.
     */
    public void processPitch(Function<Integer, Integer> f)
    {
        processNotes(ne -> true, ne ->
        {
            int p = MidiUtilities.limit(f.apply(ne.getPitch()));
            NoteEvent newNe = ne.getCopyPitch(p);
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
        sb.append("Phrase[ch=").append(channel).append("] size=").append(size()).append(" notes=").append(getNotes().toString());
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

    // --------------------------------------------------------------------- 
    // Collection interface
    // ---------------------------------------------------------------------
    /**
     * Add a NoteEvent.
     *
     * @param ne
     * @return False if ne was already part of this Phrase.
     */
    @Override
    public boolean add(NoteEvent ne)
    {
        checkAddNote(ne);

        var res = noteEvents.add(ne);
        if (res)
        {
            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Add note " + ne)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "add.undoBody() ne={0}", ne);   //NOI18N
                    noteEvents.remove(ne);
                    pcs.firePropertyChange(PROP_NOTE_REMOVED, null, Arrays.asList(ne));
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "add.redoBody() ne={0}", ne);   //NOI18N
                    noteEvents.add(ne);
                    pcs.firePropertyChange(PROP_NOTE_ADDED, null, Arrays.asList(ne));
                }
            };

            fireUndoableEditHappened(edit);
            pcs.firePropertyChange(PROP_NOTE_ADDED, null, Arrays.asList(ne));

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
     *
     * @param collection
     * @return
     */
    @Override
    public boolean addAll(java.util.Collection<? extends NoteEvent> collection)
    {
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
            UndoableEdit edit = new SimpleEdit("Add notes " + addedList)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "add.undoBody() addedList={0}", addedList);   //NOI18N
                    noteEvents.removeAll(addedList);
                    pcs.firePropertyChange(PROP_NOTE_REMOVED, null, addedList);
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "add.redoBody() addedList={0}", addedList);   //NOI18N
                    noteEvents.addAll(addedList);
                    pcs.firePropertyChange(PROP_NOTE_ADDED, null, addedList);
                }
            };

            fireUndoableEditHappened(edit);
            pcs.firePropertyChange(PROP_NOTE_ADDED, null, addedList);
        }

        return res;
    }


    @Override
    public boolean remove(Object o)
    {
        if (o instanceof NoteEvent ne && noteEvents.remove(ne))
        {

            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Remove note " + ne)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "remove.undoBody() ne={0}", ne);   //NOI18N
                    noteEvents.add(ne);
                    pcs.firePropertyChange(PROP_NOTE_ADDED, null, Arrays.asList(ne));
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "remove.redoBody() ne={0}", ne);   //NOI18N
                    noteEvents.remove(ne);
                    pcs.firePropertyChange(PROP_NOTE_REMOVED, null, Arrays.asList(ne));
                }
            };

            fireUndoableEditHappened(edit);

            pcs.firePropertyChange(PROP_NOTE_REMOVED, null, Arrays.asList(ne));
            return true;
        }
        return false;
    }

    @Override
    public void clear()
    {
        removeAll(noteEvents);
    }

    @Override
    public boolean removeAll(java.util.Collection<?> collection)
    {
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
            UndoableEdit edit = new SimpleEdit("Remove notes " + removedList)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "add.undoBody() removedList={0}", removedList);   //NOI18N
                    noteEvents.addAll(removedList);
                    pcs.firePropertyChange(PROP_NOTE_ADDED, null, removedList);
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "add.redoBody() removedList={0}", removedList);   //NOI18N
                    noteEvents.removeAll(removedList);
                    pcs.firePropertyChange(PROP_NOTE_REMOVED, null, removedList);
                }
            };

            fireUndoableEditHappened(edit);
            pcs.firePropertyChange(PROP_NOTE_REMOVED, null, removedList);
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
     * @param shiftInBeats The value added to each event's position.
     * @throws IllegalArgumentException If an event's position become negative.
     */
    public void shiftAllEvents(float shiftInBeats)
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
                if (newPosInBeats < 0)
                {
                    throw new IllegalArgumentException("ne=" + ne + " shiftInBeats=" + shiftInBeats);   //NOI18N
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


        toBeMoved.keySet().forEach(ne -> move(ne, toBeMoved.get(ne)));

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
     *
     * @return Return value is unmodifiable.
     */
    @Override
    public NavigableSet<NoteEvent> subSet(NoteEvent fromElement, boolean fromInclusive, NoteEvent toElement, boolean toInclusive)
    {
        return Collections.unmodifiableNavigableSet(noteEvents.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    /**
     * A subset of all notes in the specified range.
     *
     * @param range
     * @param excludeUpperBound
     * @return Return value is unmodifiable.
     */
    public NavigableSet<NoteEvent> subSet(FloatRange range, boolean excludeUpperBound)
    {
        return Collections.unmodifiableNavigableSet(noteEvents.subSet(getFloorNote(range.from), true, getCeilNote(range.to), excludeUpperBound));
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
                LOGGER.warning("loadAsString() Catched ex=" + ex.getMessage());
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
        return new NoteEvent(0, 0.000001f, 0, pos);
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
        return new NoteEvent(127, 10000f, 127, pos);
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
            throw new IllegalArgumentException("edit=" + edit);   //NOI18N
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
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        undoListeners.remove(l);
    }
    // --------------------------------------------------------------------- 
    // Private methods
    // ---------------------------------------------------------------------


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
                            LOGGER.log(Level.FINER, "IteratorRemove.undoBody() ne={0}", ne);   //NOI18N
                            noteEvents.add(ne);
                            pcs.firePropertyChange(PROP_NOTE_ADDED, null, Arrays.asList(ne));
                        }

                        @Override
                        public void redoBody()
                        {
                            LOGGER.log(Level.FINER, "IteratorRemove.redoBody() ne={0}", ne);   //NOI18N
                            noteEvents.remove(ne);
                            pcs.firePropertyChange(PROP_NOTE_REMOVED, null, Arrays.asList(ne));
                        }
                    };

                    fireUndoableEditHappened(edit);

                    pcs.firePropertyChange(PROP_NOTE_REMOVED, null, Arrays.asList(ne));
                }
            }
        };
        return res;
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

/*
 *
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *   This file is part of the JJazzLab software.
 *
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3)
 *   as published by the Free Software Foundation, either version 3 of the License,
 *   or (at your option) any later version.
 *
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 *   Contributor(s):
 *
 */
package org.jjazz.phrase.api;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.utilities.api.FloatRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Phrase.java core behaviour.
 */
public class PhraseTest
{

    // Reusable phrase and notes, reset before each test. Variable index = position in beats.
    private Phrase p;
    private NoteEvent ne0, ne1, ne2;

    public PhraseTest()
    {
    }

    @BeforeEach
    public void setUp()
    {
        p = new Phrase(0);
        ne0 = new NoteEvent(60, 1f, 64, 0f);   // C4, pos=0
        ne1 = new NoteEvent(64, 1f, 64, 1f);   // E4, pos=1
        ne2 = new NoteEvent(62, 1f, 64, 2f);   // D4, pos=2
    }

    // -------------------------------------------------------------------------
    // 1. Sort ordering invariant
    // -------------------------------------------------------------------------

    @Test
    public void testSortOrder_insertOutOfOrder()
    {
        // Insert in reverse position order; iteration must always be ascending
        p.add(ne2);   // pos=2
        p.add(ne0);   // pos=0
        p.add(ne1);   // pos=1

        var notes = p.getNotes();
        assertEquals(3, notes.size());
        assertEquals(0f, notes.get(0).getPositionInBeats(), "First note must be at pos=0");
        assertEquals(1f, notes.get(1).getPositionInBeats(), "Second note must be at pos=1");
        assertEquals(2f, notes.get(2).getPositionInBeats(), "Third note must be at pos=2");
    }

    // -------------------------------------------------------------------------
    // 2. shiftAllEvents
    // -------------------------------------------------------------------------

    @Test
    public void testShiftAllEvents_positiveShift()
    {
        p.add(ne0);   // pos=0
        p.add(ne1);   // pos=1
        p.shiftAllEvents(2f, false);

        var notes = p.getNotes();
        assertEquals(2f, notes.get(0).getPositionInBeats(), 0.0001f);
        assertEquals(3f, notes.get(1).getPositionInBeats(), 0.0001f);
    }

    @Test
    public void testShiftAllEvents_negativeWithHandle_clampsToZero()
    {
        // pos=0.5f shifted by -1 → clamped to 0, not negative
        p.add(new NoteEvent(60, 1f, 64, 0.5f));
        p.shiftAllEvents(-1f, true);

        assertEquals(0f, p.first().getPositionInBeats(), 0.0001f);
    }

    @Test
    public void testShiftAllEvents_negativeWithoutHandle_throws()
    {
        p.add(new NoteEvent(60, 1f, 64, 0.5f));
        assertThrows(IllegalArgumentException.class, () -> p.shiftAllEvents(-1f, false));
    }

    @Test
    public void testShiftAllEvents_zeroShift_isNoop()
    {
        p.add(ne0);
        p.shiftAllEvents(0f, false);
        assertEquals(0f, p.first().getPositionInBeats(), 0.0001f);
    }

    // -------------------------------------------------------------------------
    // 3. replace and move
    // -------------------------------------------------------------------------

    @Test
    public void testReplace_swapsNote_phraseStaysSorted()
    {
        p.add(ne0);   // pos=0
        p.add(ne2);   // pos=2
        // Replace ne2 (pos=2) with a note at pos=0.5 — should land between ne0 and old ne2
        NoteEvent replacement = new NoteEvent(65, 1f, 80, 0.5f);
        p.replace(ne2, replacement);

        assertFalse(p.contains(ne2), "Old note must be removed");
        assertTrue(p.contains(replacement), "New note must be present");
        assertEquals(2, p.size());
        assertEquals(0f, p.getNotes().get(0).getPositionInBeats(), 0.0001f);
        assertEquals(0.5f, p.getNotes().get(1).getPositionInBeats(), 0.0001f);
    }

    @Test
    public void testMove_returnsNewNote_oldNoteGone()
    {
        p.add(ne0);   // pos=0
        NoteEvent moved = p.move(ne0, 5f);

        assertFalse(p.contains(ne0), "Original note must be gone after move");
        assertTrue(p.contains(moved), "Moved note must be in phrase");
        assertEquals(5f, moved.getPositionInBeats(), 0.0001f);
        // Pitch and velocity must be preserved
        assertEquals(ne0.getPitch(), moved.getPitch());
        assertEquals(ne0.getVelocity(), moved.getVelocity());
    }

    @Test
    public void testMove_samePosition_returnsIdenticalNote()
    {
        p.add(ne0);
        NoteEvent result = p.move(ne0, ne0.getPositionInBeats());
        assertSame(ne0, result, "No move needed: original note must be returned as-is");
    }

    // -------------------------------------------------------------------------
    // 4. replaceAll — batch without positional collision
    // -------------------------------------------------------------------------

    @Test
    public void testReplaceAll_swapPositions_noCollision()
    {
        // Swap ne0 (pos=0) and ne2 (pos=2) positions — a single-pass approach
        // would collide when adding to the TreeSet; two-pass must handle this
        p.add(ne0);
        p.add(ne2);
        NoteEvent ne0Moved = ne0.setPosition(2f, false);
        NoteEvent ne2Moved = ne2.setPosition(0f, false);

        p.replaceAll(Map.of(ne0, ne0Moved, ne2, ne2Moved), false);

        assertEquals(2, p.size());
        assertEquals(0f, p.getNotes().get(0).getPositionInBeats(), 0.0001f);
        assertEquals(62, p.getNotes().get(0).getPitch(), "D4 should now be first");
        assertEquals(2f, p.getNotes().get(1).getPositionInBeats(), 0.0001f);
        assertEquals(60, p.getNotes().get(1).getPitch(), "C4 should now be second");
    }

    // -------------------------------------------------------------------------
    // 5. PropertyChange events
    // -------------------------------------------------------------------------

    @Test
    public void testPropertyChange_add_firesAddedEvent()
    {
        List<PropertyChangeEvent> events = new ArrayList<>();
        p.addPropertyChangeListener(events::add);

        p.add(ne0);

        assertEquals(1, events.size());
        assertEquals(Phrase.PROP_NOTES_ADDED, events.get(0).getPropertyName());
        @SuppressWarnings("unchecked")
        var added = (Collection<NoteEvent>) events.get(0).getNewValue();
        assertTrue(added.contains(ne0));
    }

    @Test
    public void testPropertyChange_remove_firesRemovedEvent()
    {
        p.add(ne0);
        List<PropertyChangeEvent> events = new ArrayList<>();
        p.addPropertyChangeListener(events::add);

        p.remove(ne0);

        assertEquals(1, events.size());
        assertEquals(Phrase.PROP_NOTES_REMOVED, events.get(0).getPropertyName());
        @SuppressWarnings("unchecked")
        var removed = (Collection<NoteEvent>) events.get(0).getNewValue();
        assertTrue(removed.contains(ne0));
    }

    @Test
    public void testPropertyChange_replace_firesReplacedEvent()
    {
        p.add(ne0);
        List<PropertyChangeEvent> events = new ArrayList<>();
        p.addPropertyChangeListener(events::add);

        NoteEvent newNe = new NoteEvent(65, 1f, 80, 3f);
        p.replace(ne0, newNe);

        assertEquals(1, events.size());
        assertEquals(Phrase.PROP_NOTES_REPLACED, events.get(0).getPropertyName());
    }

    @Test
    public void testPropertyChange_move_firesMovedEvent()
    {
        p.add(ne0);
        List<PropertyChangeEvent> events = new ArrayList<>();
        p.addPropertyChangeListener(events::add);

        p.move(ne0, 5f);

        assertEquals(1, events.size());
        assertEquals(Phrase.PROP_NOTES_MOVED, events.get(0).getPropertyName());
    }

    // -------------------------------------------------------------------------
    // 6. Undo/Redo
    // -------------------------------------------------------------------------

    @Test
    public void testUndo_add()
    {
        var um = new JJazzUndoManager();
        p.addUndoableEditListener(um);

        um.startCEdit("add");
        p.add(ne0);
        um.endCEdit("add");

        assertEquals(1, p.size());
        um.undo();
        assertEquals(0, p.size(), "Undo must remove the added note");
        um.redo();
        assertEquals(1, p.size(), "Redo must re-add the note");
    }

    @Test
    public void testUndo_remove()
    {
        p.add(ne0);
        var um = new JJazzUndoManager();
        p.addUndoableEditListener(um);

        um.startCEdit("remove");
        p.remove(ne0);
        um.endCEdit("remove");

        assertEquals(0, p.size());
        um.undo();
        assertEquals(1, p.size(), "Undo must restore the removed note");
        assertEquals(ne0.getPitch(), p.first().getPitch());
    }

    @Test
    public void testUndo_replace()
    {
        p.add(ne0);
        var um = new JJazzUndoManager();
        p.addUndoableEditListener(um);

        NoteEvent newNe = new NoteEvent(65, 1f, 80, 3f);
        um.startCEdit("replace");
        p.replace(ne0, newNe);
        um.endCEdit("replace");

        assertFalse(p.contains(ne0));
        assertTrue(p.contains(newNe));

        um.undo();
        assertTrue(p.contains(ne0), "Undo must restore original note");
        assertFalse(p.contains(newNe), "Undo must remove replacement note");
    }

    @Test
    public void testUndo_move()
    {
        p.add(ne0);
        var um = new JJazzUndoManager();
        p.addUndoableEditListener(um);

        um.startCEdit("move");
        NoteEvent moved = p.move(ne0, 5f);
        um.endCEdit("move");

        assertEquals(5f, p.first().getPositionInBeats(), 0.0001f);
        um.undo();
        assertEquals(1, p.size());
        assertEquals(0f, p.first().getPositionInBeats(), 0.0001f, "Undo must restore original position");
    }

    // -------------------------------------------------------------------------
    // 7. clone independence
    // -------------------------------------------------------------------------

    @Test
    public void testClone_equalContent()
    {
        p.add(ne0);
        p.add(ne2);
        Phrase clone = p.clone();
        assertTrue(p.equalsAsNoteNearPosition(clone, 0f), "Clone must have identical notes");
    }

    @Test
    public void testClone_independence()
    {
        p.add(ne0);
        Phrase clone = p.clone();

        // Modifying the clone must not affect original
        clone.add(new NoteEvent(70, 1f, 64, 9f));
        assertEquals(1, p.size(), "Original phrase must be unaffected by clone modification");
        assertEquals(2, clone.size());
    }

    // -------------------------------------------------------------------------
    // 8. subSet(FloatRange, excludeUpperBound)
    // -------------------------------------------------------------------------

    @Test
    public void testSubSet_floatRange_excludeUpperBound()
    {
        p.add(ne0);   // pos=0
        p.add(ne1);   // pos=1
        p.add(ne2);   // pos=2

        // Range [0, 2) — excludes pos=2
        var sub = p.subSet(new FloatRange(0f, 2f), true);
        assertEquals(2, sub.size(), "Should include pos=0 and pos=1 only");
        assertFalse(sub.stream().anyMatch(ne -> ne.getPositionInBeats() == 2f));
    }

    @Test
    public void testSubSet_floatRange_includeUpperBound()
    {
        p.add(ne0);   // pos=0
        p.add(ne1);   // pos=1
        p.add(ne2);   // pos=2

        // Range [0, 2] — includes pos=2
        var sub = p.subSet(new FloatRange(0f, 2f), false);
        assertEquals(3, sub.size(), "Should include all three notes");
    }

    // -------------------------------------------------------------------------
    // 9. equalsAsNoteNearPosition boundary cases
    // -------------------------------------------------------------------------

    @Test
    public void testEqualsAsNoteNearPosition_differentSize_returnsFalse()
    {
        p.add(ne0);
        Phrase other = new Phrase(0);
        assertFalse(p.equalsAsNoteNearPosition(other, 0f));
    }

    @Test
    public void testEqualsAsNoteNearPosition_differentPitch_returnsFalse()
    {
        p.add(ne0);   // pitch=60
        Phrase other = new Phrase(0);
        other.add(new NoteEvent(61, 1f, 64, 0f));  // pitch=61, same pos+dur+vel
        assertFalse(p.equalsAsNoteNearPosition(other, 0f));
    }

    @Test
    public void testEqualsAsNoteNearPosition_positionWithinWindow_returnsTrue()
    {
        p.add(new NoteEvent(60, 1f, 64, 1.0f));
        Phrase other = new Phrase(0);
        other.add(new NoteEvent(60, 1f, 64, 1.05f));  // 0.05 beats difference

        assertTrue(p.equalsAsNoteNearPosition(other, 0.1f), "Should match within nearWindow=0.1");
        assertFalse(p.equalsAsNoteNearPosition(other, 0.04f), "Should not match with nearWindow=0.04");
    }

    @Test
    public void testEqualsAsNoteNearPosition_differentDuration_returnsFalse()
    {
        p.add(new NoteEvent(60, 1f, 64, 0f));
        Phrase other = new Phrase(0);
        other.add(new NoteEvent(60, 2f, 64, 0f));  // different duration
        assertFalse(p.equalsAsNoteNearPosition(other, 0f));
    }

    // -------------------------------------------------------------------------
    // 10. getNotesBeatRange and getLastEventPosition
    // -------------------------------------------------------------------------

    @Test
    public void testGetNotesBeatRange_emptyPhrase()
    {
        assertEquals(FloatRange.EMPTY_FLOAT_RANGE, p.getNotesBeatRange());
    }

    @Test
    public void testGetNotesBeatRange_singleNote()
    {
        // Note at pos=3, dur=2 → range should be [3, 5]
        p.add(new NoteEvent(60, 2f, 64, 3f));
        FloatRange range = p.getNotesBeatRange();
        assertEquals(3f, range.from, 0.0001f);
        assertEquals(5f, range.to, 0.0001f);
    }

    @Test
    public void testGetNotesBeatRange_multipleNotes()
    {
        p.add(new NoteEvent(60, 1f, 64, 1f));   // [1, 2]
        p.add(new NoteEvent(62, 3f, 64, 4f));   // [4, 7] — latest end
        p.add(new NoteEvent(64, 1f, 64, 2f));   // [2, 3]
        FloatRange range = p.getNotesBeatRange();
        assertEquals(1f, range.from, 0.0001f, "Range must start at first note");
        assertEquals(7f, range.to, 0.0001f, "Range must end at last note-off");
    }

    @Test
    public void testGetLastEventPosition_emptyPhrase()
    {
        assertEquals(0f, p.getLastEventPosition(), 0.0001f);
    }

    @Test
    public void testGetLastEventPosition_multipleNotes()
    {
        p.add(ne0);   // pos=0
        p.add(ne2);   // pos=2
        p.add(ne1);   // pos=1
        assertEquals(2f, p.getLastEventPosition(), 0.0001f, "Last event position must be the highest start position");
    }
}

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
package org.jjazz.midimix;

import java.beans.PropertyChangeEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.SongImpl;
import org.jjazz.song.api.SongPropertyChangeEvent;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.*;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

public class MidiMixImplTest
{

    private static final String UNDO_EDIT = "UT-edit";
    MidiMixImpl midiMix;
    SongImpl song;
    ChordLeadSheet cls;
    SongStructure sgs;
    Rhythm r44sgs;
    /**
     * All 4/4 rhythms sorted by decrescending number of rhythm voices.
     */
    static List<Rhythm> r44s;
    static DefaultRhythmDatabase rdb;
    JJazzUndoManager undoManager;
    private List<RhythmVoice> baselineRhythmVoices;

    static
    {
        Utilities.setLoggingFormat(null);
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeAll
    public static void setUpClass(TestInfo testInfo) throws Exception
    {
        System.out.println("\n" + testInfo.getDisplayName() + "     ########################\n");
        rdb = DefaultRhythmDatabase.getInstance(NbPreferences.forModule(MidiMixImplTest.class));
        rdb.addRhythmsFromRhythmProviders(false, true, false);
        System.out.println(rdb.toStatsString());
        assert !rdb.getRhythms().isEmpty();

        r44s = new ArrayList<>();
        var ri44s = rdb.getRhythms(TimeSignature.FOUR_FOUR);
        ri44s.sort((ri1, ri2) -> Integer.compare(ri2.rvInfos().size(), ri1.rvInfos().size()));
        for (var ri44 : ri44s)
        {
            try
            {
                r44s.add(rdb.getRhythmInstance(ri44));
            } catch (UnavailableRhythmException ex)
            {
                Exceptions.printStackTrace(ex);
                assert false;
            }
        }
    }

    @AfterAll
    public static void tearDownClass() throws Exception
    {
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws UnsupportedEditException, ParseException
    {
        System.out.println(testInfo.getDisplayName() + " ------");

        var sf = SongFactory.getDefault();
        cls = new ChordLeadSheetImpl("section1", TimeSignature.FOUR_FOUR, 12);
        sgs = sf.createSongStructure(cls);
        song = (SongImpl) sf.createSong("Test-Song", sgs);      // No user phrase
        r44sgs = sgs.getSongPart(0).getRhythm();

        midiMix = (MidiMixImpl) MidiMixManager.getDefault().findMix(song);
        baselineRhythmVoices = midiMix.getRhythmVoices();


        undoManager = new JJazzUndoManager();
        song.addUndoableEditListener(undoManager);
        midiMix.addUndoableEditListener(undoManager);
        JJazzUndoManagerFinder.getDefault().put(song, undoManager);
        JJazzUndoManagerFinder.getDefault().put(midiMix, undoManager);


        undoManager.startCEdit(UNDO_EDIT);      // So that each test will be undoable
    }

    @AfterEach
    public void tearDown()
    {
        if (undoManager.getCurrentCEditName() == null)
        {
            return;
        }
        undoManager.endCEdit(UNDO_EDIT);

        undoAll();
        redoAll();
        undoAll();

        assertEquals(baselineRhythmVoices, midiMix.getRhythmVoices(), "Expected MidiMix to be restored to baseline state");
    }


    @Test
    public void testAddRhythm_FiresPropChannelInstrumentMixesWithAdditions() throws Exception
    {
        var events = new ArrayList<PropertyChangeEvent>();
        midiMix.addPropertyChangeListener(events::add);

        var r = getTestRhythm(2);
        var expectedNbVoices = r.getRhythmVoices().size();

        midiMix.addRhythm(r);

        var mixEvents = events.stream()
                .filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName()))
                .toList();

        assertEquals(1, mixEvents.size(), "Expected exactly 1 " + MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES + " event");
        assertTrue(mixEvents.get(0) instanceof SongPropertyChangeEvent);
        var ev = (SongPropertyChangeEvent) mixEvents.get(0);

        assertFalse(ev.isUndoOrRedo(), "Unexpected undo/redo marker");
        assertNull(ev.getNewValue());

        @SuppressWarnings("unchecked")
        var changes = (List<MidiMix.InsMixChange>) ev.getOldValue();
        assertNotNull(changes);
        assertEquals(expectedNbVoices, changes.size(), "Unexpected number of InsMixChange entries");
        assertTrue(changes.stream().allMatch(c -> c.oldInsMix() == null && c.newInsMix() != null), "Expected only additions");

        assertTrue(midiMix.getUniqueRhythms().contains(r), "MidiMix should contain the added rhythm");
        assertEquals(expectedNbVoices, midiMix.getUsedChannels(r).size(), "Unexpected number of used channels for rhythm");
    }

    @Test
    public void testAddRhythmWithNotEnoughMidiChannels() throws Exception
    {
        var events = new ArrayList<PropertyChangeEvent>();
        midiMix.addPropertyChangeListener(events::add);

        int count = 0;
        for (var r : r44s)
        {
            if (r == r44sgs)
            {
                continue;
            }
            if (midiMix.getUnusedChannels().size() < r.getRhythmVoices().size())
            {
                assertThrows(UnsupportedEditException.class, () -> midiMix.addRhythm(r));
                break;
            } else
            {
                midiMix.addRhythm(r);
                count++;
            }
        }

        var mixEvents = events.stream()
                .filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName()))
                .toList();

        assertEquals(count, mixEvents.size());
    }


    @Test
    public void testAddRhythm_UndoRedo_FiresPropChannelInstrumentMixes() throws Exception
    {
        var events = new ArrayList<PropertyChangeEvent>();
        midiMix.addPropertyChangeListener(events::add);
        // System.out.println("   midiMix before=" + midiMix.toDumpString());

        var r = r44s.getLast();
        var expectedNbVoices = r.getRhythmVoices().size();

        midiMix.addRhythm(r);
        // System.out.println("   midiMix after=" + midiMix.toDumpString());

        // Close compound edit so undo/redo works
        undoManager.endCEdit(UNDO_EDIT);

        // Undo addRhythm => removals
        events.clear();
        undoManager.undo();
        var undoEvents = events.stream()
                .filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName()))
                .toList();

        assertEquals(1, undoEvents.size(), "Expected exactly 1 undo " + MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES + " event");
        assertTrue(undoEvents.get(0) instanceof SongPropertyChangeEvent);
        var undoEv = (SongPropertyChangeEvent) undoEvents.get(0);
        assertTrue(undoEv.isUndo());

        @SuppressWarnings("unchecked")
        var undoChanges = (List<MidiMix.InsMixChange>) undoEv.getOldValue();
        assertNotNull(undoChanges);
        assertEquals(expectedNbVoices, undoChanges.size());
        assertTrue(undoChanges.stream().allMatch(c -> c.oldInsMix() != null && c.newInsMix() == null), "Expected only removals on undo");

        // Redo addRhythm => additions
        events.clear();
        undoManager.redo();
        var redoEvents = events.stream()
                .filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName()))
                .toList();

        assertEquals(1, redoEvents.size(), "Expected exactly 1 redo " + MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES + " event");
        assertTrue(redoEvents.get(0) instanceof SongPropertyChangeEvent);
        var redoEv = (SongPropertyChangeEvent) redoEvents.get(0);
        assertTrue(redoEv.isRedo());

        @SuppressWarnings("unchecked")
        var redoChanges = (List<MidiMix.InsMixChange>) redoEv.getOldValue();
        assertNotNull(redoChanges);
        assertEquals(expectedNbVoices, redoChanges.size());
        assertTrue(redoChanges.stream().allMatch(c -> c.oldInsMix() == null && c.newInsMix() != null), "Expected only additions on redo");

        // Reopen compound edit for tearDown
        undoManager.startCEdit(UNDO_EDIT);
    }


    @Test
    public void testRemoveRhythm_FiresPropChannelInstrumentMixesAndUndoRedo() throws Exception
    {
        var events = new ArrayList<PropertyChangeEvent>();
        midiMix.addPropertyChangeListener(events::add);

        var r = getTestRhythm(2);
        var expectedNbVoices = r.getRhythmVoices().size();

        // Add rhythm in its own compound edit
        midiMix.addRhythm(r);
        undoManager.endCEdit(UNDO_EDIT);

        // Remove rhythm in its own compound edit
        undoManager.startCEdit(UNDO_EDIT);
        events.clear();
        midiMix.removeRhythm(r);

        var removeEvents = events.stream()
                .filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName()))
                .toList();

        assertEquals(1, removeEvents.size(), "Expected exactly 1 " + MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES + " event");
        assertTrue(removeEvents.get(0) instanceof SongPropertyChangeEvent);
        var removeEv = (SongPropertyChangeEvent) removeEvents.get(0);
        assertFalse(removeEv.isUndoOrRedo());

        @SuppressWarnings("unchecked")
        var removeChanges = (List<MidiMix.InsMixChange>) removeEv.getOldValue();
        assertNotNull(removeChanges);
        assertEquals(expectedNbVoices, removeChanges.size());
        assertTrue(removeChanges.stream().allMatch(c -> c.oldInsMix() != null && c.newInsMix() == null), "Expected only removals");

        undoManager.endCEdit(UNDO_EDIT);

        // Undo removeRhythm => additions
        events.clear();
        undoManager.undo();
        var undoEvents = events.stream()
                .filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName()))
                .toList();
        assertEquals(1, undoEvents.size());
        assertTrue(undoEvents.get(0) instanceof SongPropertyChangeEvent);
        var undoEv = (SongPropertyChangeEvent) undoEvents.get(0);
        assertTrue(undoEv.isUndo());

        @SuppressWarnings("unchecked")
        var undoChanges = (List<MidiMix.InsMixChange>) undoEv.getOldValue();
        assertEquals(expectedNbVoices, undoChanges.size());
        assertTrue(undoChanges.stream().allMatch(c -> c.oldInsMix() == null && c.newInsMix() != null), "Expected only additions on undo");

        // Redo removeRhythm => removals
        events.clear();
        undoManager.redo();
        var redoEvents = events.stream()
                .filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName()))
                .toList();
        assertEquals(1, redoEvents.size());
        assertTrue(redoEvents.get(0) instanceof SongPropertyChangeEvent);
        var redoEv = (SongPropertyChangeEvent) redoEvents.get(0);
        assertTrue(redoEv.isRedo());

        @SuppressWarnings("unchecked")
        var redoChanges = (List<MidiMix.InsMixChange>) redoEv.getOldValue();
        assertEquals(expectedNbVoices, redoChanges.size());
        assertTrue(redoChanges.stream().allMatch(c -> c.oldInsMix() != null && c.newInsMix() == null), "Expected only removals on redo");

        // Reopen compound edit for tearDown
        undoManager.startCEdit(UNDO_EDIT);
    }


    @Test
    public void testAddUserChannel_UndoRedo_FiresPropChannelInstrumentMixes() throws Exception
    {
        var events = new ArrayList<PropertyChangeEvent>();
        midiMix.addPropertyChangeListener(events::add);

        String name = "UT-user1";
        midiMix.addUserChannel(name, false);

        var addEvents = events.stream()
                .filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName()))
                .toList();
        assertEquals(1, addEvents.size());

        @SuppressWarnings("unchecked")
        var addChanges = (List<MidiMix.InsMixChange>) addEvents.get(0).getOldValue();
        assertEquals(1, addChanges.size());
        assertTrue(addChanges.get(0).oldInsMix() == null && addChanges.get(0).newInsMix() != null);
        assertEquals(name, addChanges.get(0).rv().getName());
        assertTrue(addChanges.get(0).rv() instanceof UserRhythmVoice);

        // Close compound edit so undo/redo works
        undoManager.endCEdit(UNDO_EDIT);

        // Undo addUserChannel => removal
        events.clear();
        undoManager.undo();
        var undoEvents = events.stream().filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName())).toList();
        assertEquals(1, undoEvents.size());
        assertTrue(((SongPropertyChangeEvent) undoEvents.get(0)).isUndo());

        @SuppressWarnings("unchecked")
        var undoChanges = (List<MidiMix.InsMixChange>) undoEvents.get(0).getOldValue();
        assertEquals(1, undoChanges.size());
        assertTrue(undoChanges.get(0).oldInsMix() != null && undoChanges.get(0).newInsMix() == null);

        // Redo addUserChannel => addition
        events.clear();
        undoManager.redo();
        var redoEvents = events.stream().filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName())).toList();
        assertEquals(1, redoEvents.size());
        assertTrue(((SongPropertyChangeEvent) redoEvents.get(0)).isRedo());

        @SuppressWarnings("unchecked")
        var redoChanges = (List<MidiMix.InsMixChange>) redoEvents.get(0).getOldValue();
        assertEquals(1, redoChanges.size());
        assertTrue(redoChanges.get(0).oldInsMix() == null && redoChanges.get(0).newInsMix() != null);

        // Reopen compound edit for tearDown
        undoManager.startCEdit(UNDO_EDIT);
    }


    @Test
    public void testRemoveUserChannel_UndoRedo_FiresPropChannelInstrumentMixes() throws Exception
    {
        var events = new ArrayList<PropertyChangeEvent>();
        midiMix.addPropertyChangeListener(events::add);

        String name = "UT-user2";

        // Add in its own compound edit
        midiMix.addUserChannel(name, false);
        undoManager.endCEdit(UNDO_EDIT);

        // Remove in its own compound edit
        undoManager.startCEdit(UNDO_EDIT);
        events.clear();
        midiMix.removeUserChannel(name);

        var removeEvents = events.stream().filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName())).toList();
        assertEquals(1, removeEvents.size());

        @SuppressWarnings("unchecked")
        var removeChanges = (List<MidiMix.InsMixChange>) removeEvents.get(0).getOldValue();
        assertEquals(1, removeChanges.size());
        assertTrue(removeChanges.get(0).oldInsMix() != null && removeChanges.get(0).newInsMix() == null);
        assertEquals(name, removeChanges.get(0).rv().getName());

        undoManager.endCEdit(UNDO_EDIT);

        // Undo removeUserChannel => addition
        events.clear();
        undoManager.undo();
        var undoEvents = events.stream().filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName())).toList();
        assertEquals(1, undoEvents.size());
        assertTrue(((SongPropertyChangeEvent) undoEvents.get(0)).isUndo());

        @SuppressWarnings("unchecked")
        var undoChanges = (List<MidiMix.InsMixChange>) undoEvents.get(0).getOldValue();
        assertEquals(1, undoChanges.size());
        assertTrue(undoChanges.get(0).oldInsMix() == null && undoChanges.get(0).newInsMix() != null);

        // Redo removeUserChannel => removal
        events.clear();
        undoManager.redo();
        var redoEvents = events.stream().filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName())).toList();
        assertEquals(1, redoEvents.size());
        assertTrue(((SongPropertyChangeEvent) redoEvents.get(0)).isRedo());

        @SuppressWarnings("unchecked")
        var redoChanges = (List<MidiMix.InsMixChange>) redoEvents.get(0).getOldValue();
        assertEquals(1, redoChanges.size());
        assertTrue(redoChanges.get(0).oldInsMix() != null && redoChanges.get(0).newInsMix() == null);

        // Reopen compound edit for tearDown
        undoManager.startCEdit(UNDO_EDIT);
    }


    @Test
    public void testSetInstrumentMix_UndoRedo_FiresPropChannelInstrumentMixes() throws Exception
    {
        var events = new ArrayList<PropertyChangeEvent>();
        midiMix.addPropertyChangeListener(events::add);

        int channel = getFirstUsedChannelWithNonDelegateRhythmVoice();
        var rv = midiMix.getRhythmVoice(channel);
        assertNotNull(rv);

        InstrumentMix oldInsMix = midiMix.getInstrumentMix(channel);
        assertNotNull(oldInsMix);
        InstrumentMix newInsMix = new InstrumentMix(oldInsMix);

        midiMix.setInstrumentMix(channel, rv, newInsMix);

        var setEvents = events.stream().filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName())).toList();
        assertEquals(1, setEvents.size());

        @SuppressWarnings("unchecked")
        var changes = (List<MidiMix.InsMixChange>) setEvents.get(0).getOldValue();
        assertEquals(1, changes.size());
        assertSame(oldInsMix, changes.get(0).oldInsMix());
        assertSame(newInsMix, changes.get(0).newInsMix());
        assertSame(rv, changes.get(0).rv());

        // Close compound edit so undo/redo works
        undoManager.endCEdit(UNDO_EDIT);

        // Undo => swap back
        events.clear();
        undoManager.undo();
        var undoEvents = events.stream().filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName())).toList();
        assertEquals(1, undoEvents.size());
        assertTrue(((SongPropertyChangeEvent) undoEvents.get(0)).isUndo());

        @SuppressWarnings("unchecked")
        var undoChanges = (List<MidiMix.InsMixChange>) undoEvents.get(0).getOldValue();
        assertEquals(1, undoChanges.size());
        assertSame(newInsMix, undoChanges.get(0).oldInsMix());
        assertSame(oldInsMix, undoChanges.get(0).newInsMix());

        // Redo => apply new
        events.clear();
        undoManager.redo();
        var redoEvents = events.stream().filter(e -> MidiMix.PROP_CHANNEL_INSTRUMENT_MIXES.equals(e.getPropertyName())).toList();
        assertEquals(1, redoEvents.size());
        assertTrue(((SongPropertyChangeEvent) redoEvents.get(0)).isRedo());

        @SuppressWarnings("unchecked")
        var redoChanges = (List<MidiMix.InsMixChange>) redoEvents.get(0).getOldValue();
        assertEquals(1, redoChanges.size());
        assertSame(oldInsMix, redoChanges.get(0).oldInsMix());
        assertSame(newInsMix, redoChanges.get(0).newInsMix());

        // Reopen compound edit for tearDown
        undoManager.startCEdit(UNDO_EDIT);
    }


    @Test
    public void testSetRhythmVoice_UndoRedo_FiresPropRhythmVoice() throws Exception
    {
        var events = new ArrayList<PropertyChangeEvent>();
        midiMix.addPropertyChangeListener(events::add);

        int channel = getFirstUsedChannelWithNonDelegateRhythmVoice();
        var oldRv = midiMix.getRhythmVoice(channel);
        assertNotNull(oldRv);

        var newRv = oldRv.getCopy(oldRv.getContainer());
        midiMix.setRhythmVoice(oldRv, newRv);

        var setEvents = events.stream().filter(e -> MidiMix.PROP_RHYTHM_VOICE.equals(e.getPropertyName())).toList();
        assertEquals(1, setEvents.size());
        var ev = (SongPropertyChangeEvent) setEvents.get(0);
        assertSame(oldRv, ev.getOldValue());
        assertSame(newRv, ev.getNewValue());

        // Close compound edit so undo/redo works
        undoManager.endCEdit(UNDO_EDIT);

        // Undo => newRv -> oldRv
        events.clear();
        undoManager.undo();
        var undoEvents = events.stream().filter(e -> MidiMix.PROP_RHYTHM_VOICE.equals(e.getPropertyName())).toList();
        assertEquals(1, undoEvents.size());
        var undoEv = (SongPropertyChangeEvent) undoEvents.get(0);
        assertTrue(undoEv.isUndo());
        assertSame(newRv, undoEv.getOldValue());
        assertSame(oldRv, undoEv.getNewValue());

        // Redo => oldRv -> newRv
        events.clear();
        undoManager.redo();
        var redoEvents = events.stream().filter(e -> MidiMix.PROP_RHYTHM_VOICE.equals(e.getPropertyName())).toList();
        assertEquals(1, redoEvents.size());
        var redoEv = (SongPropertyChangeEvent) redoEvents.get(0);
        assertTrue(redoEv.isRedo());
        assertSame(oldRv, redoEv.getOldValue());
        assertSame(newRv, redoEv.getNewValue());

        // Reopen compound edit for tearDown
        undoManager.startCEdit(UNDO_EDIT);
    }


    // =========================================================================================================
    // Helper methods
    // =========================================================================================================
    private int getFirstUsedChannelWithNonDelegateRhythmVoice()
    {
        for (var ch : midiMix.getUsedChannels())
        {
            var rv = midiMix.getRhythmVoice(ch);
            if (rv != null && !(rv instanceof RhythmVoiceDelegate))
            {
                return ch;
            }
        }
        throw new IllegalStateException("No used channel with a non-delegate RhythmVoice found");
    }

    private Rhythm getTestRhythm(int minNbVoices) throws Exception
    {
        for (var ri : rdb.getRhythms())
        {
            try
            {
                var r = rdb.getRhythmInstance(ri);
                if (r.getRhythmVoices().size() >= minNbVoices)
                {
                    return r;
                }
            } catch (Exception ex)
            {
                // Ignore and try next rhythm
            }
        }
        throw new IllegalStateException("No rhythm with at least " + minNbVoices + " voices found in test database");
    }

    private void redoAll()
    {
        while (undoManager.canRedo())
        {
            undoManager.redo();
        }
    }

    private void undoAll()
    {
        while (undoManager.canUndo())
        {
            undoManager.undo();
        }
    }


}

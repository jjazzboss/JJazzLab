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
package org.jjazz.song;

import java.beans.PropertyChangeEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.song.api.Song;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.*;

public class SongImplTest
{

    private static final String UNDO_EDIT = "UT-edit";
    SongImpl song;
    ChordLeadSheet cls;
    SongStructure sgs;
    static DefaultRhythmDatabase rdb;
    JJazzUndoManager undoManager;
    private TestInfo testInfo;

    static
    {
        Utilities.setLoggingFormat(null);
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeAll
    public static void setUpClass(TestInfo testInfo) throws Exception
    {
        System.out.println("\n" + testInfo.getDisplayName() + "     ########################\n");
        rdb = (DefaultRhythmDatabase) RhythmDatabase.getDefault();
        rdb.addRhythmsFromRhythmProviders(false, true, false);
        System.out.println(rdb.toStatsString());
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
        cls = sf.createSampleChordLeadSheet("section1", 12);
        sgs = sf.createSongStructure(cls);
        song = (SongImpl) sf.createSong("Test-Song", sgs);      // No user phrase

        undoManager = new JJazzUndoManager();
        JJazzUndoManagerFinder.getDefault().put(cls, undoManager);
        JJazzUndoManagerFinder.getDefault().put(sgs, undoManager);
        song.addUndoableEditListener(undoManager);
        JJazzUndoManagerFinder.getDefault().put(song, undoManager);


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

        assertEquals(0, song.getUserPhraseNames().size(), "Expected 0 user phrases, found userPhraseNames=" + song.getUserPhraseNames());
    }

    @Test
    public void testSetUserPhraseAddAndReplace() throws UnsupportedEditException
    {
        Song testedSong = song;
        Phrase phrase1 = new Phrase(0);
        Phrase phrase2 = new Phrase(1);

        testedSong.setUserPhrase("phrase1", phrase1);
        assertEquals(1, testedSong.getUserPhraseNames().size());
        assertSame(phrase1, testedSong.getUserPhrase("phrase1"));

        testedSong.setUserPhrase("phrase1", phrase2);
        assertEquals(1, testedSong.getUserPhraseNames().size());
        assertSame(phrase2, testedSong.getUserPhrase("phrase1"));
    }

    @Test
    public void testRenameUserPhrase() throws UnsupportedEditException
    {
        Song testedSong = song;
        Phrase phrase = new Phrase(0);

        testedSong.setUserPhrase("phrase1", phrase);
        testedSong.renameUserPhrase("phrase1", "phrase2");

        assertNull(testedSong.getUserPhrase("phrase1"));
        assertSame(phrase, testedSong.getUserPhrase("phrase2"));
        assertEquals(1, testedSong.getUserPhraseNames().size());
    }

    @Test
    public void testRemoveUserPhrase() throws UnsupportedEditException
    {
        Song testedSong = song;
        Phrase phrase = new Phrase(0);

        testedSong.setUserPhrase("phrase1", phrase);
        Phrase removed = testedSong.removeUserPhrase("phrase1");

        assertSame(phrase, removed);
        assertNull(testedSong.getUserPhrase("phrase1"));
        assertTrue(testedSong.getUserPhraseNames().isEmpty());
        assertNull(testedSong.removeUserPhrase("missing"));
    }

    @Test
    public void testUserPhraseEventsAddAndRemove() throws UnsupportedEditException
    {
        Song testedSong = song;
        Phrase phrase = new Phrase(0);
        List<PropertyChangeEvent> events = new ArrayList<>();
        testedSong.addPropertyChangeListener(events::add);

        testedSong.setUserPhrase("phrase1", phrase);
        var addEvent = getSinglePropertyEvent(events, Song.PROP_USER_PHRASE);
        assertSame(phrase, addEvent.getOldValue());
        assertEquals("phrase1", addEvent.getNewValue());

        events.clear();
        testedSong.removeUserPhrase("phrase1");
        var removeEvent = getSinglePropertyEvent(events, Song.PROP_USER_PHRASE);
        assertEquals("phrase1", removeEvent.getOldValue());
        assertSame(phrase, removeEvent.getNewValue());
    }

    @Test
    public void testUserPhraseEventsAddAndRemoveUndoRedo() throws UnsupportedEditException
    {
        Song testedSong = song;
        Phrase phrase = new Phrase(0);
        List<PropertyChangeEvent> events = new ArrayList<>();
        testedSong.addPropertyChangeListener(events::add);

        testedSong.setUserPhrase("phrase1", phrase);
        testedSong.removeUserPhrase("phrase1");
        undoManager.endCEdit(UNDO_EDIT);

        events.clear();
        undoManager.undo();
        assertUserPhraseAddAndRemoveEvents(events, phrase, "phrase1");
        assertNull(testedSong.getUserPhrase("phrase1"));

        events.clear();
        undoManager.redo();
        assertUserPhraseAddAndRemoveEvents(events, phrase, "phrase1");
        assertNull(testedSong.getUserPhrase("phrase1"));
    }

    @Test
    public void testUserPhraseNameEventOnRename() throws UnsupportedEditException
    {
        Song testedSong = song;
        List<PropertyChangeEvent> events = new ArrayList<>();
        testedSong.addPropertyChangeListener(events::add);
        testedSong.setUserPhrase("phrase1", new Phrase(0));

        events.clear();
        testedSong.renameUserPhrase("phrase1", "phrase2");
        var renameEvent = getSinglePropertyEvent(events, Song.PROP_USER_PHRASE_NAME);
        assertEquals("phrase1", renameEvent.getOldValue());
        assertEquals("phrase2", renameEvent.getNewValue());
    }

    @Test
    public void testUserPhraseContentEventsReplaceAndModify() throws UnsupportedEditException
    {
        Song testedSong = song;
        Phrase oldPhrase = new Phrase(0);
        Phrase newPhrase = new Phrase(1);
        List<PropertyChangeEvent> events = new ArrayList<>();
        testedSong.addPropertyChangeListener(events::add);
        testedSong.setUserPhrase("phrase1", oldPhrase);

        events.clear();
        testedSong.setUserPhrase("phrase1", newPhrase);
        var replaceEvent = getSinglePropertyEvent(events, Song.PROP_USER_PHRASE_CONTENT);
        assertSame(oldPhrase, replaceEvent.getOldValue());
        assertEquals("phrase1", replaceEvent.getNewValue());

        events.clear();
        newPhrase.add(new NoteEvent(60, 1f, 100, 0f));
        var modifyEvent = getSinglePropertyEvent(events, Song.PROP_USER_PHRASE_CONTENT);
        assertNull(modifyEvent.getOldValue());
        assertEquals("phrase1", modifyEvent.getNewValue());
    }


    // =========================================================================================================
    // Helper methods
    // =========================================================================================================
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

    private PropertyChangeEvent getSinglePropertyEvent(List<PropertyChangeEvent> events, String propertyName)
    {
        var matchingEvents = events.stream()
                .filter(e -> propertyName.equals(e.getPropertyName()))
                .toList();
        assertEquals(1, matchingEvents.size(), "events=" + events);
        return matchingEvents.get(0);
    }

    private void assertUserPhraseAddAndRemoveEvents(List<PropertyChangeEvent> events, Phrase phrase, String phraseName)
    {
        var userPhraseEvents = events.stream()
                .filter(e -> Song.PROP_USER_PHRASE.equals(e.getPropertyName()))
                .toList();
        assertEquals(2, userPhraseEvents.size(), "events=" + events);
        assertTrue(userPhraseEvents.stream().anyMatch(e -> e.getOldValue() == phrase && phraseName.equals(e.getNewValue())), "events=" + events);
        assertTrue(userPhraseEvents.stream().anyMatch(e -> phraseName.equals(e.getOldValue()) && e.getNewValue() == phrase), "events=" + events);
    }

}

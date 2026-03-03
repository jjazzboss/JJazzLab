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
package org.jjazz.song.api;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.event.UndoableEditListener;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.StringProperties;

/**
 * A song instance: the top-level data model.
 * <p>
 * Contains a chord leadsheet, a song structure, a few parameters (name, tempo, etc.), optional user phrases and client properties. Song, ChordLeadSheet and
 * SongStructure instances can be created using the SongFactory.
 * <p>
 * When updated Song fires SongPropertyChangeEvents.
 */
public interface Song
{

    public static final String SONG_EXTENSION = "sng";
    /**
     * Default song comments.
     */
    public static final String DEFAULT_COMMENTS = ResUtil.getString(Song.class, "EDIT_ME");
    String PROP_USER_PHRASE_NAME = "PROP_USER_PHRASE_NAME";
    String PROP_NAME = "PROP_NAME";
    String PROP_COMMENTS = "PROP_COMMENTS";
    String PROP_TAGS = "PROP_TAGS";
    String PROP_TEMPO = "PROP_TEMPO";
    /**
     * A user phrase was added or removed.
     * <p>
     * If a user phrase is removed: oldValue=name_of_removed_phrase and newValue=removed_phrase.<br>
     * If a user phrase is added, oldValue=added_phrase and newValue=name_of_new_phrase<br>
     */
    String PROP_USER_PHRASE = "PROP_USER_PHRASE";
    /**
     * A user phrase was modified (some notes changed) or replaced by another one.
     * <p>
     * oldValue=old_phrase if phrase was replaced, or null if it was modified<br>
     * newValue=name_of_phrase
     */
    String PROP_USER_PHRASE_CONTENT = "PROP_USER_PHRASE_CONTENT";
    /**
     * Fired when the close() method is called.
     */
    String PROP_CLOSED = "PROP_CLOSED";
    /**
     * Fired after the song is modified (oldValue=false, newValue=true), or saved (oldValue=true, newValue=false), or Song.setSaveNeeded(false) is called
     * (oldValue=null, newValue=false).
     * <p>
     * For modification tracking see also ClsChangeEvent/ClsActionEvent, Sgs/ChangeEvent/SgsActionEvent, SongEvents.
     */
    String PROP_MODIFIED_OR_SAVED_OR_RESET = "PROP_MODIFIED_OR_SAVED_OR_RESET";
    static final Logger LOGGER = Logger.getLogger(Song.class.getSimpleName());


    void addPropertyChangeListener(PropertyChangeListener l);

    void addPropertyChangeListener(String propertyName, PropertyChangeListener l);

    void addUndoableEditListener(UndoableEditListener l);

    /**
     * Mark song instance as closed, it should not be used anymore.
     * <p>
     * Fires a PROP_CLOSED property change event.
     *
     * @param releaseRhythmResources True if the method should also call releaseResources() for each used rhythm.
     */
    void close(boolean releaseRhythmResources);

    ChordLeadSheet getChordLeadSheet();

    StringProperties getClientProperties();

    /**
     * The comments associated to this song.
     *
     * @return Can be an empty String.
     */
    String getComments();

    /**
     * Get a deep copy of the song and its components.
     * <p>
     * You might want to register
     *
     * @param disableSongInternalUpdates If true the returned instance will have internal consistency updates disabled, e.g. changing a section in the
     *                                   ChordLeadSheet won't impact the SongStructure. For special purposes only, this can lead to inconsistent Song states.
     * @return
     */
    Song getDeepCopy(boolean disableSongInternalUpdates);

    /**
     * The file where this song is stored.
     *
     * @return Can be null for example if it's a builtin song or created programmatically.
     */
    File getFile();

    /**
     * The song name.
     *
     * @return
     */
    String getName();

    /**
     * Convenience method which delegates to getSongStructure().getSizeInBars().
     *
     * @return
     */
    int getSize();

    SongStructure getSongStructure();

    /**
     * @return List can be empty if not tags. Tags are lowercase.
     */
    List<String> getTags();

    /**
     * Get the preferred tempo for this song.
     *
     * @return
     */
    int getTempo();

    /**
     * Get the user phrase associated to specified name.
     * <p>
     * Returned phrase might be longer than the song.
     * <p>
     *
     * @param name
     * @return Null if no phrase associated to name. The Phrase channel should be ignored.
     * @see #setUserPhrase(java.lang.String, org.jjazz.phrase.api.Phrase)
     */
    Phrase getUserPhrase(String name);

    /**
     * Get all the names of the user phrases.
     *
     * @return Can't be null.
     */
    Set<String> getUserPhraseNames();

    /**
     *
     * @return True if close() has been called.
     */
    boolean isClosed();

    /**
     * @return True if song has some unsaved changes.
     */
    boolean isSaveNeeded();

    void removePropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(String propertyName, PropertyChangeListener l);

    void removeUndoableEditListener(UndoableEditListener l);

    /**
     * Remove the user phrase associated to name.
     * <p>
     * Fires a PROP_USER_PHRASE event.
     *
     * @param name
     * @return The removed phrase or null
     * @see #PROP_USER_PHRASE
     */
    Phrase removeUserPhrase(String name);

    /**
     * Rename a user phrase.
     * <p>
Fires a PROP_USER_PHRASE_NAME change event.
     *
     * @param name    Must be the name of an existing phrase
     * @param newName
     */
    void renameUserPhrase(final String name, final String newName);

    /**
     * Save this song to a file (XML format).
     * <p>
     * If isCopy is false: song's file and name are updated from songFile, and a PROP_MODIFIED_OR_SAVED_OR_RESET property change event is fired with
     * oldValue=true and newValue=false.<br>
     * If isCopy is true: only the file write is performed, song state and events are unchanged.
     *
     * @param songFile
     * @param isCopy   If true, just write the file without updating this song instance (name, file, save-needed state, events).
     * @throws java.io.IOException
     * @see getFile()
     */
    void saveToFile(File songFile, boolean isCopy) throws IOException;

    /**
     * Same as saveToFile but notify user if a problem occurred.
     * <p>
     *
     * @param f
     * @param isCopy
     * @return False if problem
     */
    boolean saveToFileNotify(File f, boolean isCopy);

    /**
     * Set the comments.
     * <p>
     * Fires the PROP_COMMENTS change event. This operation is not undoable.
     *
     * @param newComments
     */
    void setComments(final String newComments);

    /**
     * Set the file from which the song can be read/written.
     *
     * @param f Can be null.
     */
    void setFile(File f);

    /**
     * Set the song name.
     * <p>
     * Fires a PROP_NAME property change event. This operation is not undoable.
     *
     * @param newName A non-empty string.
     */
    void setName(final String newName);

    /**
     * Set or reset the "save needed" status, i.e if song has some unsaved changes or not.
     * <p>
     * Fires a PROP_MODIFIED_OR_SAVED_OR_RESET change event with the relevant values.
     *
     * @param b
     */
    void setSaveNeeded(boolean b);

    /**
     * Set the list of String tags associated to this song, e.g. "rock", "dance-oriented", etc...
     * <p>
     * Fires a PROP_TAGS property change events. This operation is not undoable.
     *
     * @param newTags Must not be null but can be an empty list. Tags are space-trimmed and converted to lower case.
     */
    void setTags(List<String> newTags);

    /**
     * Set the preferred tempo for this song.
     * <p>
     * Fires a PROP_TEMPO property change event. This operation is not undoable.
     *
     * @param newTempo
     */
    void setTempo(final int newTempo);

    /**
     * Set the user phrase for the specified name.
     * <p>
     * Fires a PROP_USER_PHRASE change event if name was new, or a PROP_USER_PHRASE_CONTENT change event if name was existing. DO nothing if song has 0 bars (no
     * song parts).
     * <p>
     * This song will listen to Phrase p's changes and fire a PROP_MODIFIED_OR_SAVED_OR_RESET change event when a non-adjusting change is made.
     * <p>
     * @param name Can't be blank.
     * @param p    Can't be null. No defensive copy is done, p is directly reused. No control is done on the phrase consistency Vs the song.
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException If user phrase could not be added, typically because there is no more available MIDI
     *                                                               channel
     * @see #PROP_USER_PHRASE
     * @see #PROP_USER_PHRASE_CONTENT
     * @see #removeUserPhrase(java.lang.String)
     */
    void setUserPhrase(String name, Phrase p) throws UnsupportedEditException;

}

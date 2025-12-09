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
package org.jjazz.songeditormanager.spi;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.openide.util.Lookup;

/**
 * The manager in charge of creating song editors.
 * <p>
 * You can register to get some change events (opened, saved, closed) from any song managed by this object.
 */
public interface SongEditorManager
{

    /**
     * This property change event is fired when a song is closed.
     * <p>
     * NewValue is the song object. Note that a Song object also fires a Closed event when it is closed by the SongEditorManager.
     */
    String PROP_SONG_CLOSED = "SongClosed";
    /**
     * This property change event is fired when a song is opened.
     * <p>
     * NewValue is the song object.
     */
    String PROP_SONG_OPENED = "SongOpened";
    /**
     * This property change event is fired when a song is saved.
     * <p>
     * NewValue is the song object. This is just a forward of a Song Saved event.
     */
    String PROP_SONG_SAVED = "SongSaved";


    /**
     * Get the first implementation available in the global lookup.
     *
     * @return Can't be null
     */
    static public SongEditorManager getDefault()
    {
        var res = Lookup.getDefault().lookup(SongEditorManager.class);
        if (res == null)
        {
            throw new IllegalArgumentException("No SongEditorManager implementation found");
        }
        return res;
    }

    /**
     * Show a song in the application.
     * <p>
     * - Creates undomanager, creates and shows the required editors.<br>
     * - Opens the song memo if used by song, and open its possible links if makeActive is true. <br>
     * - Opens the PianoRollEditor if there is a user track.
     * <p>
     * If song is already shown in an editor, just makes its TopComponent active.
     *
     * @param song
     * @param makeActive If true try to make the song musically active, see ActiveSongManager.
     * @param savable    If true, song will appear as modified/savable (save button enabled).
     */
    void showSong(final Song song, boolean makeActive, boolean savable);

    /**
     * Load a song from a file and show it.
     * <p>
     * Load the song from file, fix the MidiMix if required, and call showSong(song, makeActive).
     *
     * @param f
     * @param makeActive
     * @param updateLastSongDirectory If true and the file is not already shown, update the LastSongDirectory in FileDirectoryManager.
     * @return The created song from file f
     * @throws org.jjazz.song.api.SongCreationException
     */
    Song showSong(File f, boolean makeActive, boolean updateLastSongDirectory) throws SongCreationException;

    /**
     * Programmatically close all the editors associated to a song.
     *
     * @param song
     * @param enforce If true, close the song unconditionnaly (disable user prompt if song is not saved)
     * @return True if all editors were successfully closed, false if user blocked the closing due to unsaved song
     */
    boolean closeSong(Song song, boolean enforce);

    /**
     * Get the editors currently opened for the specified song.
     *
     * @param s
     * @return Can be null.
     */
    SongEditorSet getSongEditorSet(Song s);

    /**
     * Get all the currently opened songs.
     *
     * @return
     */
    List<Song> getOpenedSongs();

    /**
     * Open (or show) the song's PianoRollEditor and set it up to edit a custom phrase of a SongPart.
     *
     * @param song
     * @param midiMix
     * @param spt     The SongPart for which a phrase is customized
     * @param rv      The RhythmVoice for which we customize the phrase
     * @param p       The customized phrase
     *
     * @return
     */
    PianoRollEditorTopComponent showPianoRollEditorForSptCustomPhrase(Song song, MidiMix midiMix, SongPart spt, RhythmVoice rv, Phrase p);

    /**
     * Open (or show) the song's PianoRollEditor and set it up to edit a user track associated to userRhythmVoice.
     *
     * @param song
     * @param midiMix
     * @param userRhythmVoice
     * @return
     */
    PianoRollEditorTopComponent showPianoRollEditorForUserTrack(Song song, MidiMix midiMix, UserRhythmVoice userRhythmVoice);

    /**
     * Set JJazzLab so that it open last opened songs upon startup.
     *
     * @param b
     */
    void setOpenLastFilesUponStartup(boolean b);

    /**
     *
     * @return True if JJazzLab must open last opened songs upon startup.
     */
    boolean isOpenLastFilesUponStartup();

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);

    //=============================================================================
    // Inner classes
    //============================================================================= 
    /**
     * The set of editors for a given song.
     */
    static public class SongEditorSet
    {

        private final CL_EditorTopComponent tcCle;
        private final SS_EditorTopComponent tcSse;
        private PianoRollEditorTopComponent tcPre;


        public SongEditorSet(CL_EditorTopComponent tcCle, SS_EditorTopComponent tcSse)
        {
            Preconditions.checkNotNull(tcCle);
            Preconditions.checkNotNull(tcSse);
            this.tcCle = tcCle;
            this.tcSse = tcSse;
        }


        public void setPianoRollEditor(PianoRollEditorTopComponent tc)
        {
            tcPre = tc;
        }

        public PianoRollEditorTopComponent getPianoRollTc()
        {
            return tcPre;
        }

        public CL_EditorTopComponent getCL_EditorTc()
        {
            return tcCle;
        }

        public SS_EditorTopComponent getSS_EditorTc()
        {
            return tcSse;
        }

    }
}

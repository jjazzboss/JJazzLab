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
package org.jjazz.notesviewer.spi;

import javax.swing.Icon;
import javax.swing.JComponent;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;

/**
 * A service provider to show notes with a graphical UI, such as a keyboard or a guitar.
 * <p>
 * There are 2 modes: one to show backing track notes in real time, the other to show static harmony notes.
 */
public interface NotesViewer
{

    public enum Mode
    {
        ShowBackingTrack, // Show incoming notes in real time
        ShowSelection        // Show static harmony notes
    };

    /**
     * Get the visual component which shows the notes.
     *
     * @return
     */
    JComponent getComponent();

    /**
     * A one-line description of this viewer.
     * <p>
     * Used by UI to describe this viewer, e.g. as a tooltip.
     *
     * @return
     */
    String getDescription();

    /**
     * A 16x16 icon representing this viewer.
     * <p>
     * Used by the framework to build a UI which lets the user select the active NotesViewer.
     *
     * @return
     */
    Icon getIcon();

    /**
     * Provide additional context info to the viewer.
     *
     * @param song Can be null.
     * @param midiMix Can be null.
     * @param rv Can be null. E.g. used to adapt keyboard size/pitch depending on instrument (bass Vs piano)
     */
    void setContext(Song song, MidiMix midiMix, RhythmVoice rv);

    /**
     * Set the operating mode.
     *
     * @param mode
     */
    void setMode(Mode mode);

    /**
     * Get the current operating mode.
     *
     * @return
     */
    Mode getMode();

    /**
     * A note ON arrived.
     *
     * @param pitch
     * @param velocity
     * @throws IllegalStateException If getMode() is not ShowBackingTrack
     */
    void realTimeNoteOn(int pitch, int velocity);

    /**
     * A note OFF arrived.
     *
     * @param pitch
     * @throws IllegalStateException If getMode() is not ShowBackingTrack
     */
    void realTimeNoteOff(int pitch);

    void releaseAllNotes();

    /**
     * Represent the notes of the specified chord symbol.
     *
     * @param cliCs
     * @throws IllegalStateException If getMode() is not ShowSelection
     */
    void showChordSymbolNotes(CLI_ChordSymbol cliCs);

    /**
     * Represent the notes of the specified scale.
     *
     * @param scale
     * @throws IllegalStateException If getMode() is not ShowSelection
     */
    void showScaleNotes(StandardScaleInstance scale);

    void cleanup();

    /**
     * If disabled the viewer should ignore the commands and show a disabled UI.
     *
     * @param b
     */
    void setEnabled(boolean b);
}

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
import org.jjazz.harmony.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.midimix.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;

/**
 * A service provider to show notes (played notes in real time, chord symbol or scale notes, voicings, etc.) with a graphical UI,
 * such as a keyboard or a guitar.
 */
public interface NotesViewer
{

    public enum DisplayMode
    {
        RealTimeNotes, // Show incoming notes in real time
        HarmonyNotes        // Show static harmony notes
    };

    /**
     * Get the visual component which shows the notes.
     *
     * @return
     */
    JComponent getComponent();

    /**
     * A 16x16 icon to represent this viewer.
     *
     * @return
     */
    Icon getIcon();

    /**
     *
     * @param song
     * @param midiMix
     */
    void setContext(Song song, MidiMix midiMix);

    /**
     * Set the display mode.
     *
     * @param mode
     * @param rv The RhythmVoice type for which we receive real time notes. Ignored if mode != RealTimeNotes.
     */
    void setMode(DisplayMode mode, RhythmVoice rv);

    /**
     * Get the current mode.
     *
     * @return
     */
    DisplayMode getMode();

    /**
     * A note ON arrived.
     *
     * @param pitch
     * @throws IllegalStatException If getMode() is not RealTimeNotes
     */
    void realTimeNoteOn(int pitch, int velocity);

    /**
     * A note OFF arrived.
     *
     * @param pitch
     * @throws IllegalStatException If getMode() is not RealTimeNotes
     */
    void realTimeNoteOff(int pitch);

    void releaseAllNotes();

    /**
     * Represent the notes of the specified chord symbol.
     *
     * @param cliCs
     * @throws IllegalStateException If getMode() is not HarmonyNotes
     */
    void showChordSymbolNotes(CLI_ChordSymbol cliCs);

    /**
     * Represent the notes of the specified scale.
     *
     * @param scale
     * @throws IllegalStateException If getMode() is not HarmonyNotes
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

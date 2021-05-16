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
package org.jjazz.realtimeviewer;

import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import org.jjazz.harmony.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.midimix.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.ui.keyboardcomponent.KeyboardRange;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.realtimeviewer.spi.NotesViewer;
import org.jjazz.ui.keyboardcomponent.KeyboardComponent;

/**
 * A NotesViewer based on a keyboard.
 */
@ServiceProvider(service = NotesViewer.class)
public class PianoNotesViewer implements NotesViewer
{

    private DisplayMode mode;
    private Song song;
    private MidiMix midiMix;
    private RhythmVoice rhythmVoice;
    private CLI_ChordSymbol cliChordSymbol;
    private StandardScaleInstance scaleInstance;
    private PianoNotesViewerComponent component;
    private KeyboardComponent keyboard;
    private boolean realTimeNotesEnabled = true;
    private static final Logger LOGGER = Logger.getLogger(NotesViewerPanel.class.getSimpleName());

    @Override
    public JComponent getComponent()
    {
        var c = new PianoNotesViewerComponent();
        keyboard = c.getKeyboard();
        return c;
    }

    @Override
    public Icon getIcon()
    {
        return new ImageIcon(getClass().getResource("resources/PianoSize.png"));
    }

    @Override
    public void setContext(Song song, MidiMix midiMix)
    {
        this.song = song;
        this.midiMix = midiMix;
    }

    @Override
    public void setMode(DisplayMode mode, RhythmVoice rv)
    {
        this.mode = mode;
        if (mode.equals(DisplayMode.RealTimeNotes) && rv == null)
        {
            throw new IllegalArgumentException("mode=" + mode + " rv=" + rv);
        }
        this.rhythmVoice = rv;
        updateKeyboardSize();
    }

    @Override
    public DisplayMode getMode()
    {
        return mode;
    }

    @Override
    public void realTimeNoteOn(int pitch, int velocity)
    {
        if (realTimeNotesEnabled)
        {
            keyboard.setPressed(pitch, velocity, null);
        }
    }

    @Override
    public void realTimeNoteOff(int pitch)
    {
        if (realTimeNotesEnabled)
        {
            keyboard.setReleased(pitch);
        }
    }

    @Override
    public void releaseAllNotes()
    {
        keyboard.releaseAllNotes();
    }

    @Override
    public void showChordSymbolNotes(CLI_ChordSymbol cliCs)
    {
        
    }

    @Override
    public void showScaleNotes(StandardScaleInstance scale)
    {
        
    }

    @Override
    public void cleanup()
    {
        // Nothing
    }

    @Override
    public void setEnabled(boolean b)
    {
        
    }

    // =================================================================================
    // Private methods
    // =================================================================================     
    private void updateKeyboardSize()
    {
        boolean b = realTimeNotesEnabled;
        realTimeNotesEnabled = false;
        
        
        KeyboardRange kbdRange = keyboard.getKeyboardRange();
        switch (mode)
        {
            case RealTimeNotes:

                if (rhythmVoice.isDrums())
                {
                    kbdRange = KeyboardRange.DRUMS_KEYS;
                } else if (rhythmVoice.getType().equals(RhythmVoice.Type.BASS))
                {
                    kbdRange = KeyboardRange.BASS_KEYS;
                } else
                {
                    kbdRange = KeyboardRange._61_KEYS;
                }
                break;
            case HarmonyNotes:
                kbdRange = KeyboardRange._61_KEYS;
                break;
            default:
                throw new AssertionError(mode.name());
        }

        keyboard.setKeyboardRange(kbdRange);

        
        realTimeNotesEnabled = b;
    }

}

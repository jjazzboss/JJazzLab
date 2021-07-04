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
package org.jjazz.notesviewer.piano;

import java.awt.Color;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.ui.keyboardcomponent.api.KeyboardRange;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.notesviewer.spi.NotesViewer;
import org.jjazz.ui.keyboardcomponent.api.KeyboardComponent;
import org.jjazz.ui.keyboardcomponent.api.PianoKey;
import org.jjazz.util.api.ResUtil;

/**
 * A NotesViewer based on a keyboard.
 */
@ServiceProvider(service = NotesViewer.class, position = 0)
public class PianoNotesViewer implements NotesViewer
{

    final private static String ICON_PATH = "resources/KeyboardIcon.png";
    final private static Icon ICON = new ImageIcon(PianoNotesViewer.class.getResource(ICON_PATH));
    private static final Color CHORD_COLOR = new Color(0, 128, 192);
    private static final Color SCALE_COLOR = new Color(187, 187, 187);
    private Mode mode;
    private Song song;
    private MidiMix midiMix;
    private RhythmVoice rhythmVoice;
    private StandardScaleInstance scaleInstance;
    private PianoNotesViewerComponent component;
    private boolean realTimeNotesEnabled = true;
    private static final Logger LOGGER = Logger.getLogger(PianoNotesViewer.class.getSimpleName());

    @Override
    public PianoNotesViewerComponent getComponent()
    {
        if (component == null)
        {
            component = new PianoNotesViewerComponent(this);
        }
        return component;
    }

    @Override
    public Icon getIcon()
    {
        return ICON;
    }

    @Override
    public String getDescription()
    {
        return ResUtil.getString(getClass(), "PianoViewerDesc");
    }

    @Override
    public void setContext(Song song, MidiMix midiMix, RhythmVoice rv)
    {
        this.song = song;
        this.midiMix = midiMix;
        this.rhythmVoice = rv;
        updateKeyboardSize();
    }

    @Override
    public void setMode(Mode mode)
    {
        if (mode == null)
        {
            throw new NullPointerException("mode");
        }

        this.mode = mode;
        releaseAllNotes();
        updateKeyboardSize();
    }

    @Override
    public Mode getMode()
    {
        return mode;
    }

    @Override
    public void realTimeNoteOn(int pitch, int velocity)
    {
        if (realTimeNotesEnabled)
        {
            getKeyboard().setPressed(pitch, velocity, null);
        }
    }

    @Override
    public void realTimeNoteOff(int pitch)
    {
        if (realTimeNotesEnabled)
        {
            getKeyboard().setReleased(pitch);
        }
    }

    @Override
    public void releaseAllNotes()
    {
        getKeyboard().reset();
    }

    @Override
    public void showChordSymbolNotes(CLI_ChordSymbol cliCs)
    {
        KeyboardComponent keyboard = getKeyboard();
        int cPitch = keyboard.getRange().getCentralC();
        ExtChordSymbol ecs = cliCs.getData();
        if (ecs.getRootNote().getRelativePitch() > 7)
        {
            cPitch -= 12;
        }
        for (Note n : ecs.getChord().getNotes())
        {
            keyboard.setPressed(cPitch + n.getPitch(), 127, CHORD_COLOR);
        }
    }

    @Override
    public void showScaleNotes(StandardScaleInstance ssi)
    {
        KeyboardComponent keyboard = getKeyboard();
        var scaleRelPitches = ssi.getRelativePitches();
        for (PianoKey key : keyboard.getPianoKeys())
        {
            if (scaleRelPitches.contains(key.getPitch() % 12))
            {
                key.setMarked(SCALE_COLOR);
            }
        }
    }

    @Override
    public void cleanup()
    {
        // Nothing
    }

    @Override
    public void setEnabled(boolean b)
    {
        getComponent().setEnabled(b);
        realTimeNotesEnabled = b;
    }

    // =================================================================================
    // Private methods
    // =================================================================================     
    private void updateKeyboardSize()
    {
        boolean b = realTimeNotesEnabled;
        realTimeNotesEnabled = false;

        KeyboardComponent keyboard = getKeyboard();
        KeyboardRange kbdRange;
        switch (mode)
        {
            case ShowBackingTrack:

                if (rhythmVoice != null && rhythmVoice.isDrums())
                {
                    kbdRange = KeyboardRange.DRUMS_KEYS;
                } else if (rhythmVoice != null && rhythmVoice.getType().equals(RhythmVoice.Type.BASS))
                {
                    kbdRange = KeyboardRange.BASS_KEYS;
                } else
                {
                    kbdRange = KeyboardRange._61_KEYS;
                }
                break;
            case ShowSelection:
                kbdRange = KeyboardRange._37_KEYS;
                break;
            default:
                throw new AssertionError(mode.name());
        }

        keyboard.setKeyboardRange(kbdRange);

        realTimeNotesEnabled = b;
    }

    private KeyboardComponent getKeyboard()
    {
        return getComponent().getKeyboard();
    }

}

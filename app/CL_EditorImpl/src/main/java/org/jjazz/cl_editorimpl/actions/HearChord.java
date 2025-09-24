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
package org.jjazz.cl_editorimpl.actions;

import org.jjazz.cl_editor.api.CL_ContextActionListener;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice.Type;
import org.jjazz.song.api.Song;
import org.jjazz.testplayerservice.spi.TestPlayer;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.hearchord")
@ActionRegistration(displayName = "unused", lazy = false) // lazy is false to show the accelerator key in the menu
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 415),
        })
public final class HearChord extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("M");
    private final String undoText = ResUtil.getString(getClass(), "CTL_HearChord");
    private Lookup context;
    private CL_ContextActionSupport cap;
    private static final Logger LOGGER = Logger.getLogger(HearChord.class.getSimpleName());

    public HearChord()
    {
        this(Utilities.actionsGlobalContext());
    }

    public HearChord(Lookup context)
    {
        this.context = context;

        // Help class to get notified of selection change in the current leadsheet editor
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);


        // As lazy=false above, need to set action properties to have the correct display in the menu
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KEYSTROKE);


        // Update enabled state
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();

        TestPlayer tp = TestPlayer.getDefault();
        int channel = findChannel(editor.getSongModel());
        Phrase p = createChordSamplePhrase(selection.getSelectedChordSymbols(), channel);
        try
        {
            tp.playTestNotes(p, null);
        } catch (MusicGenerationException ex)
        {
            LOGGER.log(Level.WARNING, "actionPerformed() Unexpected error playing chord notes. ex={0}", ex.getMessage());
        }
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        setEnabled(selection.isItemSelected() && (selection.getSelectedItems().get(0) instanceof CLI_ChordSymbol));
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new HearChord(context);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing
    }


    /**
     * Find a relevant melodic channel used by the current song.
     *
     * @param song
     * @return
     */
    static public int findChannel(Song song)
    {
        int res = 0;
        MidiMix mm;
        try
        {
            mm = MidiMixManager.getDefault().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            LOGGER.log(Level.WARNING, "findChannel() Unexpected error while getting MidiMix. ex={0}", ex.getMessage());
            return 0;
        }

        var rvs = mm.getRhythmVoices();

        // Search first for a piano
        var rvTarget = rvs.stream()
                .filter(rv -> !rv.isDrums()
                && mm.getInstrumentMix(rv).getInstrument().getSubstitute() != null
                && mm.getInstrumentMix(rv).getInstrument().getSubstitute().getFamily() == InstrumentFamily.Piano)
                .findFirst()
                .orElse(null);

        // Find first chord instrument
        if (rvTarget == null)
        {
            rvs.stream()
                    .filter(rv -> !rv.isDrums() && (rv.getType() == Type.CHORD1 || rv.getType() == Type.CHORD2))
                    .findFirst()
                    .orElse(null);
        }
        // Find first non drums channel
        if (rvTarget == null)
        {
            rvs.stream()
                    .filter(rv -> !rv.isDrums())
                    .findFirst()
                    .orElse(null);
        }
        if (rvTarget != null)
        {
            res = mm.getChannel(rvTarget);
        }

        return res;
    }

    /**
     * Create a Phrase to hear the chord(s).
     * <p>
     * A 120 tempo is assumed.
     *
     * @param cliCsList
     * @param channel
     * @return
     */
    static public Phrase createChordSamplePhrase(List<CLI_ChordSymbol> cliCsList, int channel)
    {
        // Create a phrase with all selected chord symbols
        Phrase p = new Phrase(channel);
        float beatAdvance = 0.5f;
        float beat = 0;

        for (CLI_ChordSymbol cliCs : cliCsList)
        {
            var ecs = cliCs.getData();

            var rootRelPitch = ecs.getRootNote().getRelativePitch();
            var rootPitch = rootRelPitch < 7 ? rootRelPitch + 60 : rootRelPitch + 38;  // 4th or 5th octave
            var rootNote = new Note(rootPitch);
            List<Integer> chordPitches = new ArrayList<>(ecs.getChord().getNotes().stream()
                    .map(n -> rootNote.getUpperPitch(n.getRelativePitch(), true))
                    .toList());
            Collections.sort(chordPitches);

            // play arpeggio
            for (var pitch : chordPitches)
            {
                var ne = new NoteEvent(pitch, beatAdvance, 80, beat);
                p.add(ne);
                beat += beatAdvance;
            }

            // then strike chord
            beat += 2 * beatAdvance;
            for (int pitch : chordPitches)
            {
                var ne = new NoteEvent(pitch, 4 * beatAdvance, 75, beat);
                p.add(ne);
                beat += beatAdvance / 5;
            }

            beat += 3 * beatAdvance;
        }

        return p;
    }
}

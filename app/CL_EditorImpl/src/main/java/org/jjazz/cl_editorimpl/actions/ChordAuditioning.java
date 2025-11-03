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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.harmony.api.ChordSymbol;
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

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.chordauditioning")
@ActionRegistration(displayName = "unused", lazy = false) // lazy is false to show the accelerator key in the menu
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 2010),
            @ActionReference(path = "Shortcuts", name="M")
        })
public final class ChordAuditioning extends CL_ContextAction
{
    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("M");
    private static final Logger LOGGER = Logger.getLogger(ChordAuditioning.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_ChordAuditioning"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();

        TestPlayer tp = TestPlayer.getDefault();
        int channel = findChannel(editor.getSongModel());
        var csList = selection.getSelectedChordSymbols().stream()
                .map(cliCs -> cliCs.getData())
                .toList();
        Phrase p = createChordSamplePhrase(csList, channel);
        try
        {
            tp.playTestNotes(p, null);
        } catch (MusicGenerationException ex)
        {
            LOGGER.log(Level.WARNING, "actionPerformed() Unexpected error playing chord notes. ex={0}", ex.getMessage());
        }
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        setEnabled(selection.isChordSymbolSelected());
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
     * Create a Phrase to hear the specified chords.
     * <p>
     * A 120 tempo is assumed.
     *
     * @param csList
     * @param channel
     * @return
     */
    static public Phrase createChordSamplePhrase(List<? extends ChordSymbol> csList, int channel)
    {
        // Create a phrase with all selected chord symbols
        Phrase p = new Phrase(channel);
        float beatAdvance = 0.5f;
        float beat = 0;

        for (var cs : csList)
        {
            var rootRelPitch = cs.getRootNote().getRelativePitch();
            var rootPitch = rootRelPitch < 7 ? rootRelPitch + 60 : rootRelPitch + 38;  // 4th or 5th octave
            var rootNote = new Note(rootPitch);
            List<Integer> chordPitches = new ArrayList<>(cs.getChord().getNotes().stream()
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

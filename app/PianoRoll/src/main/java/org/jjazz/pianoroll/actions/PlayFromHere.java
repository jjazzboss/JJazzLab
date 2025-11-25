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
package org.jjazz.pianoroll.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import javax.sound.midi.Sequencer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdateProviderSongSession;
import org.jjazz.outputsynth.api.FixMidiMix;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.songcontext.api.SongContext;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Action to play from the loop zone, or the bar of the first selected note, or the first visible bar.
 */
public class PlayFromHere extends AbstractAction
{

    public static final String ACTION_ID = "PlayFromHere";
    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_SPACE);
    private final PianoRollEditor editor;

    private static final Logger LOGGER = Logger.getLogger(PlayFromHere.class.getSimpleName());

    public PlayFromHere(PianoRollEditor editor)
    {
        this.editor = editor;

    }


    @Override
    public void actionPerformed(ActionEvent e)
    {

        // Song must be active !
        ActiveSongManager asm = ActiveSongManager.getDefault();
        if (asm.getActiveSong() != editor.getSong())
        {
            String msg = ResUtil.getString(getClass(), "PlayEditor.ERR_NotActive");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        var mc = MusicController.getInstance();
        mc.stop();


        UpdatableSongSession session = null;
        try
        {
            int startBar = computeStartBar();
            MidiMix midiMix = PianoRollEditorTopComponent.get(editor.getSong()).getMidiMix();
            SongContext context = new SongContext(editor.getSong(), midiMix);
            FixMidiMix.checkAndPossiblyFix(context.getMidiMix(), true);

            // Create the session
            var dynSession = UpdateProviderSongSession.getSession(context, PlaybackSession.Context.SONG);
            session = UpdatableSongSession.getSession(dynSession);
            mc.setPlaybackSession(session, false); // can raise MusicGenerationException            


            // Play it
            mc.play(startBar);
            PlaybackSettings.getInstance().setLoopCount(Sequencer.LOOP_CONTINUOUSLY);

        } catch (MusicGenerationException ex)
        {
            if (session != null)
            {
                session.close();
            }
            if (ex.getMessage() != null)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
            return;
        }
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
    private int computeStartBar()
    {
        int res;
        int startBar = editor.getRulerStartBar();

        IntRange loopZone = editor.getLoopZone();
        if (loopZone != null)
        {
            res = loopZone.from + startBar;
        } else
        {
            var nes = editor.getSelectedNoteEvents();
            if (!nes.isEmpty())
            {
                res = editor.toPosition(nes.getFirst().getPositionInBeats()).getBar();
            } else
            {
                var br = editor.getVisibleBarRange();
                res = br.isEmpty() ? 0 : br.from;
            }
            res += startBar;
        }

        return res;
    }
}

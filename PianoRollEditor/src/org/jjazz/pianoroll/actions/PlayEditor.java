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
package org.jjazz.pianoroll.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;
import javax.sound.midi.Sequencer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSessionOnePlay;
import org.jjazz.musiccontrol.api.playbacksession.UpdateProviderSongSession;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Action to force the play of the editor phrase in loop mode.
 */
public class PlayEditor extends AbstractAction
{

    public static final String ACTION_ID = "PlayEditor";
    public static final String KEYBOARD_SHORTCUT = "ctrl alt SPACE";
    private final PianoRollEditorTopComponent topComponent;

    private static final Logger LOGGER = Logger.getLogger(PlayEditor.class.getSimpleName());

    public PlayEditor(PianoRollEditorTopComponent topComponent)
    {
        this.topComponent = topComponent;


        // UI settings for the FlatToggleButton
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("resources/PlayEditor-OFF.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "PlayEditorTooltip") + " (" + KEYBOARD_SHORTCUT + ")");
        putValue("hideActionText", true);


        topComponent.getEditor().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KEYBOARD_SHORTCUT),
                PlayEditor.ACTION_ID);
        topComponent.getEditor().getActionMap().put(PlayEditor.ACTION_ID, this);
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {

        // Song must be active !
        ActiveSongManager asm = ActiveSongManager.getInstance();
        if (asm.getActiveSong() != topComponent.getSong())
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
            IntRange barRange = topComponent.getBarRange();
            SongContext context = new SongContext(topComponent.getSong(), topComponent.getMidiMix(), barRange);

            // Check that all listeners are OK to start playback     
            PlaybackSettings.getInstance().firePlaybackStartVetoableChange(context);  // can raise PropertyVetoException


            // Create the session
            var dynSession = UpdateProviderSongSession.getSession(context);
            session = new UpdatableSongSessionOnePlay(dynSession);
            mc.setPlaybackSession(session, false); // can raise MusicGenerationException            

            // Play it
            mc.play(barRange.from);
            PlaybackSettings.getInstance().setLoopCount(Sequencer.LOOP_CONTINUOUSLY);

        } catch (MusicGenerationException | PropertyVetoException ex)
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
}

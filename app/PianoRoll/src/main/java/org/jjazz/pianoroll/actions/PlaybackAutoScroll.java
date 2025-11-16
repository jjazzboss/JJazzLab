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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.harmony.api.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListenerAdapter;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.ToggleAction;

/**
 * Action to toggle the playback auto-scroll.
 * <p>
 * The action also is responsible to move the playback point when playback is ON.
 */
public class PlaybackAutoScroll extends ToggleAction implements PropertyChangeListener
{

    public static final String ACTION_ID = "PlaybackAutoScroll";
    public static final String KEYBOARD_SHORTCUT = "A";
    private final PianoRollEditor editor;
    private MusicListener musicListener;
    private static final Logger LOGGER = Logger.getLogger(PlaybackAutoScroll.class.getSimpleName());

    public PlaybackAutoScroll(PianoRollEditor editor)
    {
        super(editor.isPlaybackAutoScrollEnabled());

        this.editor = editor;

        // UI settings for the FlatToggleButton
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("resources/PlaybackAutoScrollOFF.png")));
        setSelectedIcon(new ImageIcon(getClass().getResource("resources/PlaybackAutoScrollON.png")));
        // putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));                                   
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "PlaybackAutoScrollToolip") + " (" + KEYBOARD_SHORTCUT + ")");
        putValue("hideActionText", true);


        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KEYBOARD_SHORTCUT), PlaybackAutoScroll.ACTION_ID);
        editor.getActionMap().put(PlaybackAutoScroll.ACTION_ID, this);


        listenToTheMusic();


        editor.addPropertyChangeListener(this);
        
        
        var mc = MusicController.getInstance();        
        musicListener.enabled = mc.isPlaying() || mc.isPaused();
    }


    @Override
    public void selectedStateChanged(boolean b)
    {
        editor.setPlaybackAutoScrollEnabled(b);
    }


    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "propertyChange() e={0}", e);

        var mc = MusicController.getInstance();

        if (e.getSource() == mc)
        {
            if (e.getPropertyName().equals(MusicController.PROP_STATE))
            {
                switch (mc.getState())
                {
                    case DISABLED, STOPPED ->
                    {
                        musicListener.enabled = false;
                        editor.showPlaybackPoint(-1f);
                    }
                    case PAUSED, PLAYING -> musicListener.enabled = true;
                    default -> throw new AssertionError(mc.getState().name());

                }
            }
        } else if (e.getSource() == editor)
        {
            if (e.getPropertyName().equals(PianoRollEditor.PROP_EDITOR_ALIVE))
            {
                stopListeningToTheMusic();
                editor.removePropertyChangeListener(this);
            }
        }
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================
    private void listenToTheMusic()
    {
        musicListener = new MusicListener();
        var mc = MusicController.getInstance();
        mc.addPlaybackListener(musicListener);
        mc.addPropertyChangeListener(this);
    }

    private void stopListeningToTheMusic()
    {
        var mc = MusicController.getInstance();
        mc.removePlaybackListener(musicListener);
        mc.removePropertyChangeListener(this);
    }

    // ====================================================================================
    // Inner classes
    // ====================================================================================

    private class MusicListener extends PlaybackListenerAdapter
    {

        boolean enabled;

        public MusicListener()
        {
            super(EnumSet.of(PlaybackSession.Context.SONG));
        }
        
        @Override
        public void beatChanged(Position oldPos, Position newPos, float newPosInBeats)
        {
            // newPos is a song/ruler position, not a phrase position
            if (!enabled || !editor.getRulerBarRange().contains(newPos.getBar()))
            {
                if (editor.getPlaybackPointPosition() >= 0)
                {
                    editor.showPlaybackPoint(-1);
                }
                return;
            }

            editor.showPlaybackPoint(editor.toPositionInBeats(newPos.getMoved(-editor.getRulerStartBar(), 0)));
        }

    }
}

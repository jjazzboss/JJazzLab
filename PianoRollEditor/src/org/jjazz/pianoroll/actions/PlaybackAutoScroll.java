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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListenerAdapter;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.ui.utilities.api.ToggleAction;
import org.jjazz.util.api.ResUtil;

/**
 * Action to toggle the playback auto-scroll.
 * <p>
 * The action also is responsible to move the playback point when playback is ON.
 */
public class PlaybackAutoScroll extends ToggleAction implements PropertyChangeListener
{

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
        // putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));   //NOI18N                                
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "PlaybackAutoScrollToolip"));
        putValue("hideActionText", true);


        // Keyboard shortcut
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("A"), "PlaybackAutoScroll");   //NOI18N
        editor.getActionMap().put("PlaybackAutoScroll", this);   //NOI18N


        listenToTheMusic();


        editor.addPropertyChangeListener(PianoRollEditor.PROP_EDITOR_ALIVE, e ->
        {
            if (e.getNewValue().equals(false))
            {
                stopListeningToTheMusic();
            }
        });

        musicListener.enabled = switch (MusicController.getInstance().getState())
        {
            case PAUSED, PLAYING ->
                true;
            default ->
                false;
        };
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        setSelected(!isSelected());
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
                    case DISABLED:
                    case STOPPED:
                        musicListener.enabled = false;
                        editor.showPlaybackPoint(-1f);
                        break;

                    case PAUSED:
                    case PLAYING:
                        musicListener.enabled = true;
                        break;
                    default:
                        throw new AssertionError(mc.getState().name());

                }
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

        @Override
        public void beatChanged(Position oldPos, Position newPos)
        {

            if (!enabled || !editor.getBarRange().contains(newPos.getBar()))
            {
                return;
            }

            editor.showPlaybackPoint(editor.toPositionInBeats(newPos));
        }

    }
}

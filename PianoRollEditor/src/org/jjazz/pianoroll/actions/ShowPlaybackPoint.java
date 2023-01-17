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
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListenerAdapter;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.ui.utilities.api.ToggleAction;
import org.jjazz.util.api.ResUtil;

/**
 * Action to toggle the showing of the playback point.
 */
public class ShowPlaybackPoint extends ToggleAction implements PropertyChangeListener
{

    private final PianoRollEditor editor;
    private MusicListener musicListener;
    private static final Logger LOGGER = Logger.getLogger(ShowPlaybackPoint.class.getSimpleName());

    public ShowPlaybackPoint(PianoRollEditor editor)
    {
        super(editor.isSnapEnabled());

        this.editor = editor;

        // UI settings for the FlatToggleButton
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("resources/PlaybackPointOFF.png")));
        setSelectedIcon(new ImageIcon(getClass().getResource("resources/PlaybackPointON.png")));
        // putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));   //NOI18N                                
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "PlaybackPointTooltip"));
        putValue("hideActionText", true);


        // Keyboard shortcut
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("P"), "ShowPlaybackPoint");   //NOI18N
        editor.getActionMap().put("ShowPlaybackPoint", this);   //NOI18N

    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        setSelected(!isSelected());
    }

    @Override
    public void selectedStateChanged(boolean b)
    {
        if (b)
        {
            listenToTheMusic();
        } else
        {
            stopListeningToTheMusic();
            editor.showPlaybackPoint(-1f);
        }
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
                        editor.showPlaybackPoint(-1f);
                        break;
                    case PAUSED:
                        break;
                    case PLAYING:
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
        if (musicListener == null)
        {
            musicListener = new MusicListener();
        }
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

        @Override
        public void beatChanged(Position oldPos, Position newPos)
        {

            if (!editor.getBarRange().contains(newPos.getBar()))
            {
                return;
            }
            
            int relBar = newPos.getBar() - editor.getStartBarIndex();
            float relPos = relBar * editor.getModel().getTimeSignature().getNbNaturalBeats() + newPos.getBeat();
            float pos = editor.getBeatRange().from + relPos;
            
            editor.showPlaybackPoint(pos);
        }

    }
}

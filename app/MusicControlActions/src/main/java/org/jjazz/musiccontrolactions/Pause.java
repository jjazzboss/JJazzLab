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
package org.jjazz.musiccontrolactions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.flatcomponents.api.FlatToggleButton;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.actions.BooleanStateAction;

/**
 * Show/hide the pauseback point in editors during song pauseback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.pause")
@ActionRegistration(displayName = "#CTL_Pause", lazy = false)
@ActionReferences(
        {
            // 
        })
public class Pause extends BooleanStateAction implements PropertyChangeListener, LookupListener
{

    private Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private static final Logger LOGGER = Logger.getLogger(Pause.class.getSimpleName());

    public Pause()
    {
        setBooleanState(false);

//        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PauseButtonBorder-24x24.png")));
//        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PauseButtonBorderOn-24x24.png")));
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PauseButton-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PauseButtonOn-24x24.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_PauseTooltip"));
        putValue("hideActionText", true);

        // Listen to pausebackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);

        // Listen to the Midi active song changes
        ActiveSongManager.getDefault().addPropertyListener(this);

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        updateEnabledAndSelectedState();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        setSelected(!getBooleanState());
    }

    public synchronized void setSelected(boolean newState)
    {
        if (newState == getBooleanState())
        {
            return;
        }

        setBooleanState(newState);      // Notify all listeners eg UI button        

        MusicController mc = MusicController.getInstance();
        MusicController.State playBackState = mc.getState();
        LOGGER.log(Level.FINE, "buttonStateChanged() newState={0} playBackState={1}", new Object[]
        {
            newState, playBackState
        });
        switch (playBackState)
        {
            case STOPPED:
            case DISABLED:
                if (newState)
                {
                    // Can't pause if already stopped, revert back
                    setBooleanState(!newState);
                } else
                {
                    // Nothing
                }
                break;
            case PAUSED:
                if (newState)
                {
                    // Nothing
                } else
                {
                    // Restart playback
                    assert currentSong != null; // Otherwise button should be disabled   
                    try
                    {
                        mc.resume();
                    } catch (MusicGenerationException ex)
                    {
                        if (ex.getMessage() != null)
                        {
                            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                            DialogDisplayer.getDefault().notify(d);
                        }
                        setBooleanState(!newState);
                    }
                }
                break;
            case PLAYING:
                if (newState)
                {
                    // Pause playback, might actually be equivalent to a stop() if song was modified
                    mc.pause();
                } else
                {
                    // Nothing
                }
                break;
            default:
                throw new IllegalArgumentException("playBackState=" + playBackState + " newState=" + newState);
        }
    }

    @Override
    public synchronized void resultChanged(LookupEvent ev)
    {
        int i = 0;
        Song newSong = null;
        for (Song s : lookupResult.allInstances())
        {
            newSong = s;
            i++;
        }
        assert i < 2 : "i=" + i + " lookupResult.allInstances()=" + lookupResult.allInstances();
        if (newSong != null)
        {
            // Current song has changed
            if (currentSong != null)
            {
                // Listen to song close event
                currentSong.removePropertyChangeListener(this);
            }
            currentSong = newSong;
            currentSong.addPropertyChangeListener(this);
            updateEnabledAndSelectedState();
        } else
        {
            // Do nothing : pause is still using the last valid song
        }
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(), "CTL_Pause");
    }

    @Override
    public HelpCtx getHelpCtx()
    {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public Component getToolbarPresenter()
    {
        return new FlatToggleButton(this);
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    @Override
    public synchronized void propertyChange(PropertyChangeEvent evt)
    {
        MusicController mc = MusicController.getInstance();
        if (evt.getSource() == mc)
        {
            switch (evt.getPropertyName())
            {
                case MusicController.PROP_STATE, MusicController.PROP_PLAYBACK_SESSION -> updateEnabledAndSelectedState();
            }
        } else if (evt.getSource() == ActiveSongManager.getDefault())
        {
            if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                updateEnabledAndSelectedState();
            }
        } else if (evt.getSource() == currentSong)
        {
            if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                currentSongClosed();
            }
        }
    }

    // ======================================================================
    // Private methods
    // ======================================================================   

    private void updateEnabledAndSelectedState()
    {
        MusicController mc = MusicController.getInstance();
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
        boolean b = (currentSong != null && currentSong == activeSong)
                && (mc.isPlaying() || mc.isPaused())
                && mc.getPlaybackSession().getContext() == PlaybackSession.Context.SONG;
        setEnabled(b);
        setBooleanState(mc.isPaused());
    }


    private void currentSongClosed()
    {
        currentSong.removePropertyChangeListener(this);
        currentSong = null;
        updateEnabledAndSelectedState();
    }

}

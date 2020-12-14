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
package org.jjazz.ui.musiccontrolactions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.song.api.Song;
import org.jjazz.ui.flatcomponents.FlatToggleButton;
import org.jjazz.util.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.actions.BooleanStateAction;
import org.openide.util.actions.Presenter;

/**
 * Show/hide the stopback point in editors during song stopback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.stop")
@ActionRegistration(displayName = "#CTL_Stop", lazy = false)
@ActionReferences(
        {
            // 
        })
public class Stop extends BooleanStateAction implements PropertyChangeListener, LookupListener, Presenter.Toolbar
{

    private Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private static final Logger LOGGER = Logger.getLogger(Stop.class.getSimpleName());

    public Stop()
    {
        setBooleanState(true);

        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/StopButtonBorderOff-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/StopButtonBorderOn-24x24.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_StopTooltip"));
        putValue("hideActionText", true);

        // Listen to stopbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);

        // Listen to the Midi active song changes
        ActiveSongManager.getInstance().addPropertyListener(this);

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        currentSongChanged();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        setSelected(!getBooleanState());
    }

    public void setSelected(boolean newState)
    {
        if (newState == getBooleanState())
        {
            return;
        }

        setBooleanState(newState);      // Notify all listeners eg UI button                

        MusicController mc = MusicController.getInstance();
        MusicController.State playBackState = mc.getState();
        LOGGER.fine("setSelected() newState=" + newState + " playBackState=" + playBackState);   //NOI18N
        switch (playBackState)
        {
            case PAUSED:
            case PLAYING:
                if (newState)
                {
                    mc.stop();
                } else
                {
                    // Nothing
                }
                break;
            case STOPPED:
            case DISABLED:
                if (newState)
                {
                    // Nothing
                } else
                {
                    // Revert 
                    setBooleanState(!newState);
                }
                break;
            default:
                throw new IllegalArgumentException("playBackState=" + playBackState + " newState=" + newState);   //NOI18N
        }
    }

    @Override
    public void resultChanged(LookupEvent ev)
    {
        int i = 0;
        Song newSong = null;
        for (Song s : lookupResult.allInstances())
        {
            newSong = s;
            i++;
        }
        assert i < 2 : "i=" + i + " lookupResult.allInstances()=" + lookupResult.allInstances();   //NOI18N
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
            currentSongChanged();
        } else
        {
            // Do nothing : stoper is still using the last valid song
        }
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(), "CTL_Stop");
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

    @Override
    public String toString()
    {
        return "state=" + getBooleanState();
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        MusicController mc = MusicController.getInstance();
        if (evt.getSource() == mc)
        {
            if (evt.getPropertyName() == MusicController.PROP_STATE)
            {
                playbackStateChanged();
            }
        } else if (evt.getSource() == ActiveSongManager.getInstance())
        {
            if (evt.getPropertyName() == ActiveSongManager.PROP_ACTIVE_SONG)
            {
                activeSongChanged();
            }
        } else if (evt.getSource() == currentSong)
        {
            if (evt.getPropertyName() == Song.PROP_CLOSED)
            {
                currentSongClosed();
            }
        }
    }

    // ======================================================================
    // Private methods
    // ======================================================================   
    private void activeSongChanged()
    {
        currentSongChanged();    // Enable/Disable components            
    }

    private void currentSongChanged()
    {
        MusicController mc = MusicController.getInstance();
        Song activeSong = ActiveSongManager.getInstance().getActiveSong();
        boolean b = (currentSong != null && currentSong == activeSong);
        setEnabled(b);
    }

    private void currentSongClosed()
    {
        currentSong.removePropertyChangeListener(this);
        currentSong = null;
        currentSongChanged();
    }

    private void playbackStateChanged()
    {
        MusicController mc = MusicController.getInstance();
        LOGGER.fine("playbackStateChanged() actionState=" + getBooleanState() + " mc.getPlaybackState()=" + mc.getState());   //NOI18N
        setEnabled(!mc.getState().equals(MusicController.State.DISABLED));
        setBooleanState(mc.getState() == MusicController.State.STOPPED);
    }

}

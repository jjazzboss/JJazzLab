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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.song.api.Song;
import org.jjazz.flatcomponents.api.FlatToggleButton;
import org.jjazz.musiccontrolactions.api.RemoteAction;
import org.jjazz.musiccontrolactions.spi.RemoteActionProvider;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.actions.BooleanStateAction;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.ServiceProvider;

/**
 * Show/hide the stopback point in editors during song stopback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.stop")
@ActionRegistration(displayName = "#CTL_Stop", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Shortcuts", name = "S-SPACE")
        })
public class Stop extends BooleanStateAction implements PropertyChangeListener, LookupListener, Presenter.Toolbar
{

    private Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private static final Logger LOGGER = Logger.getLogger(Stop.class.getSimpleName());

    public Stop()
    {
        setBooleanState(true);

        putValue(Action.NAME, "Stop");        // For our RemoteActionProvider
//        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/StopButtonBorderOff-24x24.png")));
//        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/StopButtonBorderOn-24x24.png")));
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/StopButton-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/StopButtonOn-24x24.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_StopTooltip"));
        putValue("hideActionText", true);

        // Listen to stopbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);

        // Listen to the Midi active song changes
        ActiveSongManager.getDefault().addPropertyListener(this);

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

    public synchronized void setSelected(boolean newState)
    {
        if (newState == getBooleanState())
        {
            return;
        }

        setBooleanState(newState);      // Notify all listeners eg UI button                

        MusicController mc = MusicController.getInstance();
        MusicController.State playBackState = mc.getState();
        LOGGER.log(Level.FINE, "setSelected() newState={0} playBackState={1}", new Object[]{newState, playBackState});   
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
    public synchronized void propertyChange(PropertyChangeEvent evt)
    {
        MusicController mc = MusicController.getInstance();
        if (evt.getSource() == mc)
        {
            if (evt.getPropertyName() == MusicController.PROP_STATE)
            {
                playbackStateChanged();
            }
        } else if (evt.getSource() == ActiveSongManager.getDefault())
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
    // Inner classes
    // ======================================================================   

    @ServiceProvider(service = RemoteActionProvider.class)
    public static class StopRemoteActionProvider implements RemoteActionProvider
    {

        @Override
        public List<RemoteAction> getRemoteActions()
        {
            RemoteAction ra = RemoteAction.loadFromPreference("MusicControls", "org.jjazz.musiccontrolactions.stop");
            if (ra == null)
            {
                ra = new RemoteAction("MusicControls", "org.jjazz.musiccontrolactions.stop");
                ra.setMidiMessages(RemoteAction.noteOnMidiMessages(0, 26));
            }
            ra.setDefaultMidiMessages(RemoteAction.noteOnMidiMessages(0, 26));
            return Arrays.asList(ra);
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
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
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
        LOGGER.log(Level.FINE, "playbackStateChanged() actionState={0} mc.getPlaybackState()={1}", new Object[]{getBooleanState(),
            mc.getState()});   
        setEnabled(!mc.getState().equals(MusicController.State.DISABLED));
        setBooleanState(mc.isStopped());
    }


}

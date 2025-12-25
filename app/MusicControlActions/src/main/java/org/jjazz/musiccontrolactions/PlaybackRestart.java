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

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.song.api.Song;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.musiccontrolactions.api.RemoteAction;
import org.jjazz.musiccontrolactions.spi.RemoteActionProvider;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 * Make playback jump to song's start.
 * <p>
 * Action is enabled when playback is on.<p>
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.playbacktohead")
@ActionRegistration(displayName = "#CTL_RestartPlayback", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Shortcuts", name = "F3")
        })
public class PlaybackRestart extends AbstractAction implements PropertyChangeListener, LookupListener
{

    private Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private static final Logger LOGGER = Logger.getLogger(PlaybackRestart.class.getSimpleName());

    public PlaybackRestart()
    {
        putValue(Action.NAME, ResUtil.getString(getClass(), "CTL_RestartPlayback"));         // For our RemovoteActionProvider
//        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PreviousSongpart-24x24.png")));
//        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_PlaybackToPreviousSongPartTooltip"));
//        putValue("hideActionText", true);

        // Listen to playbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);

        // Listen to the Midi active song changes
        ActiveSongManager.getDefault().addPropertyListener(this);

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        setEnabled(false);              // By default
        resultChanged(null);            // Might enable the action
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var mc = MusicController.getInstance();
        int startBar = mc.getPlaybackSession().getBarRange().from;

        if (mc.isPaused())
        {
            mc.changePausedBar(startBar);

        } else
        {
            // Playing

            // Restart the MusicController on next songpart
            mc.pause();
            try
            {
                mc.play(startBar);
            } catch (MusicGenerationException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }
    }


    // ======================================================================
    // LookupListener interface
    // ======================================================================  
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
            currentSong = newSong;
            updateEnabledState();
        } else
        {
            // Do nothing : player is still using the last valid song
        }
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
                case MusicController.PROP_STATE, MusicController.PROP_PLAYBACK_SESSION -> updateEnabledState();
                default ->
                {
                    // Nothing
                }
            }
        } else if (evt.getSource() == ActiveSongManager.getDefault())
        {
            if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                updateEnabledState();
            }
        }
    }
    // ======================================================================
    // Inner classes
    // ======================================================================   

    @ServiceProvider(service = RemoteActionProvider.class)
    public static class PlaybackToHeadRemoteActionProvider implements RemoteActionProvider
    {

        @Override
        public List<RemoteAction> getRemoteActions()
        {
            RemoteAction ra = RemoteAction.loadFromPreference("MusicControls", "org.jjazz.musiccontrolactions.playbacktohead");
            if (ra == null)
            {
                ra = new RemoteAction("MusicControls", "org.jjazz.musiccontrolactions.playbacktohead");
                ra.setMidiMessages(RemoteAction.noteOnMidiMessages(0, 28));
            }
            ra.setDefaultMidiMessages(RemoteAction.noteOnMidiMessages(0, 28));
            return Arrays.asList(ra);
        }
    }

    //=====================================================================================
    // Private methods
    //=====================================================================================   

    private void updateEnabledState()
    {
        MusicController mc = MusicController.getInstance();
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
        boolean b = (currentSong != null && currentSong == activeSong)
                && (mc.isPlaying() || mc.isPaused())
                && mc.getPlaybackSession().getContext() == PlaybackSession.Context.SONG;
        setEnabled(b);
    }

}

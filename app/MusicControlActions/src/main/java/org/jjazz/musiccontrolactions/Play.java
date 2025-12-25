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
import javax.sound.midi.MidiUnavailableException;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.UpdateProviderSongSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.flatcomponents.api.FlatToggleButton;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrolactions.api.FixMissingSectionStartChord;
import org.jjazz.musiccontrolactions.api.RemoteAction;
import org.jjazz.musiccontrolactions.spi.RemoteActionProvider;
import org.jjazz.outputsynth.api.FixMidiMix;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
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
import org.openide.util.lookup.ServiceProvider;

/**
 * Show/hide the playback point in editors during song playback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.play")
@ActionRegistration(displayName = "#CTL_Play", lazy = false)
@ActionReferences(
        {
            // 
            @ActionReference(path = "Shortcuts", name = "SPACE")
        })
public class Play extends BooleanStateAction implements PropertyChangeListener, LookupListener
{

    private final Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private static final Logger LOGGER = Logger.getLogger(Play.class.getSimpleName());

    public Play()
    {
        setBooleanState(false);

        putValue(Action.NAME, "Play/Pause");        // For our RemoteActionProvider
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlayButton-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlayButtonOn-24x24-orange.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_PlayToolTip"));
        putValue("hideActionText", true);

        // Listen to playbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);

        // Listen to the Midi active song changes
        ActiveSongManager.getDefault().addPropertyListener(this);

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        currentSongChanged();
    }

    /**
     * Just toggle the selected state.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        setSelected(!getBooleanState());
    }

    public synchronized void setSelected(boolean newState)
    {
        if (newState == getBooleanState() || currentSong == null)
        {
            return;
        }

        setBooleanState(newState);      // Notify all listeners eg UI button                

        MusicController mc = MusicController.getInstance();
        MusicController.State playBackState = mc.getState();

        LOGGER.log(Level.FINE, "setSelected() newState={0} playBackState={1}", new Object[]
        {
            newState, playBackState
        });

        switch (playBackState)
        {
            case PAUSED:
                if (newState)
                {
                    // Start from last position
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
                        return;
                    }
                } else
                {
                    // Nothing
                }
                break;
            case STOPPED:
                if (newState)
                {
                    // Start playback from initial position   
                    UpdatableSongSession session = null;
                    try
                    {
                        MidiMix midiMix = MidiMixManager.getDefault().findMix(currentSong);      // Can raise MidiUnavailableException
                        SongContext context = new SongContext(currentSong, midiMix);

                        new FixMissingSectionStartChord(context).autofix();
                        FixMidiMix.checkAndPossiblyFix(midiMix, true);

                        // Prepare the session
                        UpdateProviderSongSession dynSession = UpdateProviderSongSession.getSession(context, PlaybackSession.Context.SONG);
                        session = UpdatableSongSession.getSession(dynSession);
                        mc.setPlaybackSession(session, false);  // Can generate MusicGenerationException


                        // Start sequencer
                        mc.play(0);


                        // Log the song play event        
                        Analytics.setPropertiesOnce(Analytics.buildMap("First Play", Analytics.toStdDateTimeString()));
                        Analytics.incrementProperties("Nb Play", 1);
                        var mapParams = Analytics.buildMap("Bar Range", context.getBarRange().toString(), "Rhythms", Analytics.toStrList(context.getUniqueRhythms()));
                        Analytics.logEvent("Play", mapParams);

                    } catch (MusicGenerationException | MidiUnavailableException ex)
                    {
                        if (session != null)
                        {
                            session.close();
                        }

                        if (ex.getMessage() != null)
                        {
                            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                            DialogDisplayer.getDefault().notify(d);
                        }

                        setBooleanState(!newState);
                        return;
                    }


                } else
                {
                    // Nothing
                }
                break;

            case PLAYING:
                if (newState)
                {
                    // Nothing
                } else
                {
                    // Pause playback, might actually be equivalent to a stop() if song was modified
                    mc.pause();
                }
                break;
            case DISABLED:
                if (newState)
                {
                    // Can't play if disabled, revert back
                    setBooleanState(!newState);
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
            currentSong = newSong;
            currentSongChanged();
        } else
        {
            // Do nothing : player is still using the last valid song
        }
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(), "CTL_Play");
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
        }
    }
    // ======================================================================
    // Inner classes
    // ======================================================================   

    @ServiceProvider(service = RemoteActionProvider.class)
    public static class PlayRemoteActionProvider implements RemoteActionProvider
    {

        @Override
        public List<RemoteAction> getRemoteActions()
        {
            RemoteAction ra = RemoteAction.loadFromPreference("MusicControls", "org.jjazz.musiccontrolactions.play");
            if (ra == null)
            {
                ra = new RemoteAction("MusicControls", "org.jjazz.musiccontrolactions.play");
                ra.setMidiMessages(RemoteAction.noteOnMidiMessages(0, 24));
            }
            ra.setDefaultMidiMessages(RemoteAction.noteOnMidiMessages(0, 24));
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
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
        boolean b = (currentSong != null && currentSong == activeSong);
        setEnabled(b);
    }

    private void playbackStateChanged()
    {
        MusicController mc = MusicController.getInstance();
        LOGGER.log(Level.FINE, "playbackStateChanged() actionState={0} mc.getPlaybackState()={1}", new Object[]
        {
            getBooleanState(),
            mc.getState()
        });
        setEnabled(!mc.getState().equals(MusicController.State.DISABLED));
        setBooleanState(mc.isPlaying());
    }

}

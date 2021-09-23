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
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.MusicController.State;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.ui.flatcomponents.api.FlatToggleButton;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.BooleanStateAction;

/**
 * Show/hide the pauseback point in editors during song pauseback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.pause")
@ActionRegistration(displayName = "#CTL_Pause", lazy = false)
@ActionReferences(
        {
            // 
        })
@NbBundle.Messages(
        {

        })
public class Pause extends BooleanStateAction implements PropertyChangeListener, LookupListener
{

    private Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private static final Logger LOGGER = Logger.getLogger(Pause.class.getSimpleName());

    public Pause()
    {
        setBooleanState(false);

//        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PauseButtonBorder-24x24.png")));
//        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PauseButtonBorderOn-24x24.png")));
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PauseButton-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PauseButtonOn-24x24.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_PauseTooltip"));
        putValue("hideActionText", true);

        // Listen to pausebackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);

        // Listen to the Midi active song changes
        ActiveSongManager.getInstance().addPropertyListener(this);

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
        LOGGER.fine("buttonStateChanged() newState=" + newState + " playBackState=" + playBackState);   //NOI18N
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
                    assert currentSong != null; // Otherwise button should be disabled   //NOI18N
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
                throw new IllegalArgumentException("playBackState=" + playBackState + " newState=" + newState);   //NOI18N
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
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                updateEnabledAndSelectedState();
            }
        } else if (evt.getSource() == ActiveSongManager.getInstance())
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
        Song activeSong = ActiveSongManager.getInstance().getActiveSong();
        boolean b = (currentSong != null && currentSong == activeSong);

        MusicController mc = MusicController.getInstance();
        b &= !mc.isArrangerPlaying() && (mc.getState().equals(MusicController.State.PLAYING) || mc.getState().equals(MusicController.State.PAUSED));
        setEnabled(b);
        setBooleanState(mc.getState().equals(MusicController.State.PAUSED));
    }


    private void currentSongClosed()
    {
        currentSong.removePropertyChangeListener(this);
        currentSong = null;
        updateEnabledAndSelectedState();
    }

}

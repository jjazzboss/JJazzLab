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
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.harmony.api.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListenerAdapter;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.flatcomponents.api.FlatToggleButton;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.ss_editor.api.SS_Editor;
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
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.ResUtil;

/**
 * Show/hide the playback point in editors during song playback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.showplaybackpoint")
@ActionRegistration(displayName = "#CTL_ShowPlaybackPoint", lazy = false)
@ActionReferences(
        {
            // 
        })
public class ShowPlaybackPoint extends BooleanStateAction implements PropertyChangeListener, LookupListener, Presenter.Toolbar
{

    private final Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private CL_Editor currentCL_Editor;
    private SS_Editor currentRL_Editor;
    private final Position newSgsPos = new Position();
    private boolean playbackListenerDisabled;
    private static final Logger LOGGER = Logger.getLogger(ShowPlaybackPoint.class.getSimpleName());

    public ShowPlaybackPoint()
    {
        setSelected(true);
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource(
                "/org/jjazz/musiccontrolactions/resources/PlaybackPointOFF-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource(
                "/org/jjazz/musiccontrolactions/resources/PlaybackPointON-24x24.png")));
        putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource(
                "/org/jjazz/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_ShowPlaybackTooltip"));
        putValue("hideActionText", true);


        // Listen to playbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);
        MusicController.getInstance().addPlaybackListener(new SongListener());

        // Listen to the Midi active song changes
        ActiveSongManager.getDefault().addPropertyListener(this);

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        currentSongChanged();
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
            // Do nothing : keep using the last valid song
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        setSelected(!getBooleanState());
    }

    public void setSelected(boolean b)
    {
        if (b == getBooleanState())
        {
            return;
        }
        setBooleanState(b);  // Notify action listeners
        updateShowing();
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(), "CTL_ShowPlaybackPoint");
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
    public void propertyChange(PropertyChangeEvent evt)
    {
        MusicController mc = MusicController.getInstance();
        if (evt.getSource() == mc)
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                updateShowing();
                updateEnabled();
            }
        } else if (evt.getSource() == ActiveSongManager.getDefault())
        {
            if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                activeSongChanged();
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
    /**
     * Convert a position from a SongStructure into the corresponding ChordLeadSheetPosition.
     * <p>
     * Positions will differ as soon as SongParts are duplicated or their order was changed.
     *
     * @param sgsPos
     * @return Can be null if sgsPos is not a valid bar in the songstructure
     */
    private Position convertToClsPosition(Position sgsPos)
    {
        Position clsPos = new Position(sgsPos);
        SongPart spt = currentSong.getSongStructure().getSongPart(sgsPos.getBar());
        if (spt == null)
        {
            return null;
        }
        int sectionBarIndex = spt.getParentSection().getPosition().getBar();
        clsPos.setBar(sectionBarIndex + sgsPos.getBar() - spt.getStartBarIndex());
        return clsPos;
    }

    private void activeSongChanged()
    {
        updateEnabled();
    }

    private void currentSongChanged()
    {
        updateEnabled();
    }

    private void currentSongClosed()
    {
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
        LOGGER.log(Level.FINE, "currentSongClosed() currentSong={0} activeSong={1}", new Object[]
        {
            currentSong.getName(),
            activeSong != null ? activeSong.getName() : "null"
        });
        if (currentSong == activeSong)
        {
            hidePlaybackPoint();
        }
        currentSong.removePropertyChangeListener(this);
        currentSong = null;
        currentSongChanged();
    }

    /**
     * Set the enabled state depending on:
     * <p>
     * - current and active song <br>
     * - playback state <br>
     * - if song was modified during playback
     * <p>
     */
    private void updateEnabled()
    {
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
        boolean currentIsActive = (currentSong != null) && (currentSong == activeSong);
        MusicController.State state = MusicController.getInstance().getState();
        switch (state)
        {
            case PLAYING, PAUSED -> setEnabled(currentIsActive && !playbackListenerDisabled);
            case STOPPED -> setEnabled(currentIsActive);
            case DISABLED -> setEnabled(false);
            default -> throw new IllegalStateException("state=" + state);
        }
    }

    /**
     * Show or hide the playback point in the editors depending on:
     * <p>
     * - MusicController state <br>
     * - this action enabled state <br>
     * - boolean state <br>
     * - if song was modified during playback
     */
    private void updateShowing()
    {
        MusicController.State state = MusicController.getInstance().getState();
        if (playbackListenerDisabled)
        {
            switch (state)
            {
                case PLAYING, PAUSED -> hidePlaybackPoint();
                case STOPPED, DISABLED -> playbackListenerDisabled = false;
                default -> throw new IllegalStateException(
                            "state=" + state + " currentCL_Editor=" + currentCL_Editor + " songWasModified=" + playbackListenerDisabled + " isEnabled()=" + isEnabled() + " getBooleanState()=" + getBooleanState());
            }
        } else
        {
            switch (state)
            {
                case PLAYING, PAUSED ->
                {
                    if (currentCL_Editor != null && getBooleanState() == false)
                    {
                        hidePlaybackPoint();
                    } else if (currentCL_Editor == null && isEnabled() && getBooleanState() == true)
                    {
                        showPlaybackPoint();
                    }
                }
                case STOPPED, DISABLED ->
                {
                    if (currentCL_Editor != null)
                    {
                        hidePlaybackPoint();
                    }
                }
                default -> throw new IllegalStateException(
                            "state=" + state + " currentCL_Editor=" + currentCL_Editor + " songWasModified=" + playbackListenerDisabled + " isEnabled()=" + isEnabled() + " getBooleanState()=" + getBooleanState());
            }
        }
    }

    private void showPlaybackPoint()
    {
        if (currentCL_Editor == null)
        {
            MusicController mc = MusicController.getInstance();
            var sem = SongEditorManager.getDefault();
            currentCL_Editor = sem.getSongEditorSet(currentSong).getCL_EditorTc().getEditor();
            currentRL_Editor = sem.getSongEditorSet(currentSong).getSS_EditorTc().getEditor();
            newSgsPos.set(mc.getCurrentBeatPosition());
            Position newClsPos = convertToClsPosition(newSgsPos);
            if (newClsPos != null)
            {
                currentCL_Editor.showPlaybackPoint(true, newClsPos);
            }
            currentRL_Editor.showPlaybackPoint(true, newSgsPos);
        } else
        {
            throw new IllegalStateException("currentCL_Editor is not null. currentSong=" + currentSong.getName());
        }
    }

    private void hidePlaybackPoint()
    {
        if (currentCL_Editor != null)
        {
            currentCL_Editor.showPlaybackPoint(false, null);
            currentRL_Editor.showPlaybackPoint(false, null);
        }
        currentCL_Editor = null;
        currentRL_Editor = null;
    }

    // ===========================================================================================================
    // Inner classes
    // ===========================================================================================================
    
    private class SongListener extends PlaybackListenerAdapter
    {

        public SongListener()
        {
            super(EnumSet.of(PlaybackSession.Context.SONG));
        }

        @Override
        public void enabledChanged(boolean b)
        {
            // LOGGER.info("PlaybackListener.enabledChanged() -- b="+b);
            playbackListenerDisabled = !b;
            updateShowing();
            updateEnabled();
        }

        @Override
        public void beatChanged(final Position oldPos, final Position newPos, float newPosInBeats)
        {
            if (currentCL_Editor != null)
            {
                newSgsPos.set(newPos);
                Position newClsPos = convertToClsPosition(newSgsPos);
                if (newClsPos != null)
                {
                    currentCL_Editor.showPlaybackPoint(true, newClsPos);
                }
                currentRL_Editor.showPlaybackPoint(true, newSgsPos);
            }
        }
    }
}

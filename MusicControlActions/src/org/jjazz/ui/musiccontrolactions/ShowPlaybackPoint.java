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
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.event.SectionMovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemAddedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemBarShiftedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemMovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemRemovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.musiccontrol.PlaybackListener;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.RpChangedEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptRenamedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
import org.jjazz.song.api.Song;
import org.jjazz.songeditormanager.SongEditorManager;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.flatcomponents.FlatToggleButton;
import org.jjazz.ui.ss_editor.api.SS_Editor;
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
import org.openide.util.actions.Presenter;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;

/**
 * Show/hide the playback point in editors during song playback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.showplaybackpoint")
@ActionRegistration(displayName = "#CTL_ShowPlaybackPoint", lazy = false)
@ActionReferences(
        {
            // 
        })
@NbBundle.Messages(
        {
            "CTL_ShowPlaybackPoint=Show playback point",
            "CTL_ShowPlaybackTooltip=Highlight the bar being played"
        })
public class ShowPlaybackPoint extends BooleanStateAction implements PropertyChangeListener, LookupListener, Presenter.Toolbar, ClsChangeListener, SgsChangeListener, PlaybackListener
{

    private final Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private CL_Editor currentCL_Editor;
    private SS_Editor currentRL_Editor;
    private final Position newSgsPos = new Position();
    private boolean songWasModifiedDuringPlayback;
    private static final Logger LOGGER = Logger.getLogger(ShowPlaybackPoint.class.getSimpleName());

    public ShowPlaybackPoint()
    {
        setSelected(true);
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointOFF-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointON-24x24.png")));
//      putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointOff-20x20.png")));
//      putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointOn-20x20.png")));
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ShowPlaybackTooltip());
        putValue("hideActionText", true);
        // Listen to playbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);
        MusicController.getInstance().addPlaybackListener(this);
        // Listen to the Midi active song changes
        ActiveSongManager.getInstance().addPropertyListener(this);
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
                currentSong.getChordLeadSheet().removeClsChangeListener(this);
                currentSong.getSongStructure().removeSgsChangeListener(this);
            }
            currentSong = newSong;
            currentSong.addPropertyChangeListener(this);
            currentSong.getChordLeadSheet().addClsChangeListener(this);
            currentSong.getSongStructure().addSgsChangeListener(this);
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
        return Bundle.CTL_ShowPlaybackPoint();
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
    // ClsChangeListener interface
    // ======================================================================  
    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        if (songWasModifiedDuringPlayback)
        {
            return;
        }
        if (event instanceof SizeChangedEvent)
        {
            songWasModifiedDuringPlayback = true;
        } else if (event instanceof ItemAddedEvent)
        {
            songWasModifiedDuringPlayback = !(event.getItem() instanceof CLI_ChordSymbol);
        } else if (event instanceof ItemRemovedEvent)
        {
            songWasModifiedDuringPlayback = !(event.getItem() instanceof CLI_ChordSymbol);
        } else if (event instanceof ItemChangedEvent)
        {
            songWasModifiedDuringPlayback = !(event.getItem() instanceof CLI_ChordSymbol);
        } else if (event instanceof ItemMovedEvent)
        {
            songWasModifiedDuringPlayback = !(event.getItem() instanceof CLI_ChordSymbol);
        } else if (event instanceof SectionMovedEvent)
        {
            songWasModifiedDuringPlayback = true;
        } else if (event instanceof ItemBarShiftedEvent)
        {
            songWasModifiedDuringPlayback = true;
        }
        updateShowing();
        updateEnabled();
    }

    // ======================================================================
    // SgsChangeListener interface
    // ======================================================================  
    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {
        if (songWasModifiedDuringPlayback)
        {
            return;
        }
        LOGGER.fine("songStructureChanged() e=" + e);
        if (e instanceof SptRemovedEvent)
        {
            songWasModifiedDuringPlayback = true;
        } else if (e instanceof SptAddedEvent)
        {
            songWasModifiedDuringPlayback = true;
        } else if (e instanceof SptReplacedEvent)
        {
            // Nothing
        } else if (e instanceof SptResizedEvent)
        {
            songWasModifiedDuringPlayback = true;
        } else if (e instanceof SptRenamedEvent)
        {
            // Nothing
        } else if (e instanceof RpChangedEvent)
        {
            // Nothing
        }
        updateShowing();
        updateEnabled();
    }

    // ======================================================================
    // Playbackistener interface
    // ======================================================================  
    @Override
    public void beatChanged(final Position oldPos, final Position newPos)
    {
        // Changes can be generated outside the EDT
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                if (currentCL_Editor != null)
                {
                    newSgsPos.set(newPos);
                    currentCL_Editor.showPlaybackPoint(true, convertToClsPosition(newSgsPos));
                    currentRL_Editor.showPlaybackPoint(true, newSgsPos);
                }
            }
        };
        org.jjazz.ui.utilities.Utilities.invokeLaterIfNeeded(run);
    }

    @Override
    public void barChanged(int oldBar, int newBar)
    {
        // Nothing
    }

    @Override
    public void midiActivity(int channel, long tick)
    {
        // Nothing
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
            if (evt.getPropertyName() == MusicController.PROP_PLAYBACK_STATE)
            {
                updateShowing();
                updateEnabled();
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
    /**
     * Convert a position from a SongStructure into the corresponding ChordLeadSheetPosition.
     * <p>
     * Positions will differ as soon as SongParts are duplicated or their order was changed.
     *
     * @param sgsPos
     * @return
     */
    private Position convertToClsPosition(Position sgsPos)
    {
        Position clsPos = new Position(sgsPos);
        SongPart spt = currentSong.getSongStructure().getSongPart(sgsPos.getBar());
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
        Song activeSong = ActiveSongManager.getInstance().getActiveSong();
        LOGGER.fine("currentSongClosed() currentSong=" + currentSong.getName() + " activeSong=" + (activeSong != null ? activeSong.getName() : "null"));
        if (currentSong == activeSong)
        {
            hidePlaybackPoint();
        }
        currentSong.removePropertyChangeListener(this);
        currentSong.getChordLeadSheet().removeClsChangeListener(this);
        currentSong.getSongStructure().removeSgsChangeListener(this);
        currentSong = null;
        currentSongChanged();
    }

    /**
     *
     * Set the enabled state depending on:
     * <p>
     * - current and active song <br>
     * - playback state <br>
     * - if song was modified during playback
     * <p>
     */
    private void updateEnabled()
    {
        Song activeSong = ActiveSongManager.getInstance().getActiveSong();
        boolean currentIsActive = (currentSong != null) && (currentSong == activeSong);
        MusicController.State state = MusicController.getInstance().getPlaybackState();
        switch (state)
        {
            case PLAYBACK_STARTED:
            case PLAYBACK_PAUSED:
                setEnabled(currentIsActive && !songWasModifiedDuringPlayback);
                break;
            case PLAYBACK_STOPPED:
                setEnabled(currentIsActive);
                break;
            default:
                throw new IllegalStateException("state=" + state);
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
        MusicController.State state = MusicController.getInstance().getPlaybackState();
        if (songWasModifiedDuringPlayback)
        {
            switch (state)
            {
                case PLAYBACK_STARTED:
                case PLAYBACK_PAUSED:
                    hidePlaybackPoint();
                    break;
                case PLAYBACK_STOPPED:
                    songWasModifiedDuringPlayback = false;
                    break;
                default:
                    throw new IllegalStateException("state=" + state + " currentCL_Editor=" + currentCL_Editor + " songWasModified=" + songWasModifiedDuringPlayback + " isEnabled()=" + isEnabled() + " getBooleanState()=" + getBooleanState());
            }
        } else
        {
            switch (state)
            {
                case PLAYBACK_STARTED:
                case PLAYBACK_PAUSED:
                    if (currentCL_Editor != null && getBooleanState() == false)
                    {
                        hidePlaybackPoint();
                    } else if (currentCL_Editor == null && isEnabled() && getBooleanState() == true)
                    {
                        showPlaybackPoint();
                    }
                    break;
                case PLAYBACK_STOPPED:
                    if (currentCL_Editor != null)
                    {
                        hidePlaybackPoint();
                    }
                    break;
                default:
                    throw new IllegalStateException("state=" + state + " currentCL_Editor=" + currentCL_Editor + " songWasModified=" + songWasModifiedDuringPlayback + " isEnabled()=" + isEnabled() + " getBooleanState()=" + getBooleanState());
            }
        }
    }

    private void showPlaybackPoint()
    {
        if (currentCL_Editor == null)
        {
            MusicController mc = MusicController.getInstance();
            currentCL_Editor = SongEditorManager.getInstance().getEditors(currentSong).getTcCle().getCL_Editor();
            currentRL_Editor = SongEditorManager.getInstance().getEditors(currentSong).getTcRle().getSS_Editor();
            newSgsPos.set(mc.getBeatPosition());
            currentCL_Editor.showPlaybackPoint(true, convertToClsPosition(newSgsPos));
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
}

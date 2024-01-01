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
import java.util.logging.Logger;
import javax.sound.midi.Sequencer;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.song.api.Song;
import org.jjazz.flatcomponents.api.FlatToggleButton;
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

/**
 * Toggle loop sound during playback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.loop")
@ActionRegistration(displayName = "#CTL_Loop", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Shortcuts", name = "L")
        })
public class Loop extends BooleanStateAction implements PropertyChangeListener, LookupListener
{

    private Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private static final Logger LOGGER = Logger.getLogger(Loop.class.getSimpleName());

    public Loop()
    {
        setBooleanState(false);
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/Loop-OFF-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/Loop-ON-24x24.png")));
        putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/LoopDisabled-24x24.png")));                   
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_LoopTooltip"));
        putValue("hideActionText", true);

        // Listen to loopbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);
        PlaybackSettings.getInstance().addPropertyChangeListener(this);

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
            // Do nothing : looper is still using the last valid song
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert currentSong != null; // Otherwise button should be disabled   
        setSelected(!getBooleanState());
    }

    public void setSelected(boolean b)
    {
        if (b == getBooleanState())
        {
            return;
        }
        PlaybackSettings.getInstance().setLoopCount(b ? Sequencer.LOOP_CONTINUOUSLY : 0);
        setBooleanState(b);
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(), "CTL_Loop");
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
        if (evt.getSource() == MusicController.getInstance())
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                playbackStateChanged();
            }
        } else if (evt.getSource() == PlaybackSettings.getInstance())
        {
            if (evt.getPropertyName().equals(PlaybackSettings.PROP_LOOPCOUNT))
            {
                int nbLoops = (int) evt.getNewValue();
                setBooleanState(nbLoops == Sequencer.LOOP_CONTINUOUSLY);
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
    private void activeSongChanged()
    {
        currentSongChanged();    // Enable/Disable components            
    }

    private void currentSongChanged()
    {
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
        boolean b = (currentSong != null) && (currentSong == activeSong);
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
        // Nothing
    }

}

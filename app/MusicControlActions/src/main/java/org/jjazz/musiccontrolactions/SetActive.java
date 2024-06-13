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
import javax.sound.midi.MidiUnavailableException;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.song.api.Song;
import org.jjazz.flatcomponents.api.FlatToggleButton;
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
import static org.openide.util.actions.BooleanStateAction.PROP_BOOLEAN_STATE;

/**
 * Toggle the active state of the song.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.setactive")
@ActionRegistration(displayName = "#CTL_SetActive", lazy = false)
@ActionReferences(
        {
            // @ActionReference(path = "Actions/CL_EditorTopComponent", position = 100),  # Moved to action SetActivePopupAction to fix Issue #311
            @ActionReference(path = "Shortcuts", name = "O")
        })
public class SetActive extends BooleanStateAction implements PropertyChangeListener, LookupListener
{

    private final Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private MidiMix currentMidiMix;
    private static final Logger LOGGER = Logger.getLogger(SetActive.class.getSimpleName());

    public SetActive()
    {
        setBooleanState(false);

        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/Off-24x18.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/On-24x18.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_SetActiveToolTip"));
        putValue("hideActionText", true);

        // Listen to the Midi active song changes
        ActiveSongManager.getDefault().addPropertyListener(this);

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        updateEnabledAndSelected();
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
            try
            {
                currentMidiMix = MidiMixManager.getDefault().findMix(currentSong);
            } catch (MidiUnavailableException ex)
            {
                // We should never be here
                throw new IllegalStateException("Unexpected MidiUnavailableException", ex);
            }
            currentSong.addPropertyChangeListener(this);
            updateEnabledAndSelected();
        } else
        {
            // Do nothing : setactive is still using the last valid song
        }

        // LOGGER.severe("resultChanged() currentSong=" + currentSong);
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
        if (b)
        {
            // Check song is activable before validating the state change
            String err = ActiveSongManager.getDefault().isActivable(currentSong);
            if (err != null)
            {
                // We don't change the state but still we need to fire a true=>false propertychange event to tell
                // the associated UI component which called actionPerformed()) to revert.
                firePropertyChange(PROP_BOOLEAN_STATE, true, false);
                NotifyDescriptor d = new NotifyDescriptor.Message(err, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return;
            } else
            {
                ActiveSongManager.getDefault().setActive(currentSong, currentMidiMix);
            }
        } else
        {
            ActiveSongManager.getDefault().setActive(null, null);

            // Nothing : don't try to activate another open song

        }
        setBooleanState(b);  // Notify action listeners
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(), "CTL_SetActive");
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
        if (evt.getSource() == ActiveSongManager.getDefault())
        {
            if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                updateEnabledAndSelected();    // Enable/Disable and select/unselect button    
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
 

    private void updateEnabledAndSelected()
    {
        setEnabled(currentSong != null);
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
        boolean b = (currentSong != null) && (currentSong == activeSong);
        setBooleanState(b);
    }

    private void currentSongClosed()
    {
        currentSong.removePropertyChangeListener(this);
        currentSong = null;
        currentMidiMix = null;
        updateEnabledAndSelected();
    }


}

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
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.musiccontrol.ClickManager;
import org.jjazz.musiccontrol.ClickManager.PrecountMode;
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

/**
 * Toggle click precount before playback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.precount")
@ActionRegistration(displayName = "#CTL_Precount", lazy = false)
@ActionReferences(
        {
            // 
        })
public class Precount extends BooleanStateAction implements PropertyChangeListener, LookupListener
{

    private Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private static final Logger LOGGER = Logger.getLogger(Precount.class.getSimpleName());

    public Precount()
    {
        setBooleanState(false);
        ClickManager cm = ClickManager.getInstance();
        cm.addPropertyChangeListener(this);
        updateUI(cm.getClickPrecountMode());
        putValue("hideActionText", true);

        setSelected(cm.isClickPrecountEnabled());

        // Listen to the Midi active song changes
        ActiveSongManager.getInstance().addPropertyListener(this);

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        currentSongChanged();
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        boolean shift = (ae.getModifiers() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
        if (shift)
        {
            // If shift used just change the precount mode
            ClickManager cm = ClickManager.getInstance();
            PrecountMode mode = cm.getClickPrecountMode();
            switch (mode)
            {
                case ONE_BAR:
                    cm.setClickPrecountMode(PrecountMode.TWO_BARS);
                    break;
                case TWO_BARS:
                    cm.setClickPrecountMode(PrecountMode.AUTO);
                    break;
                case AUTO:
                    cm.setClickPrecountMode(PrecountMode.ONE_BAR);
                    break;
                default:
                    throw new AssertionError(mode.name());
            }
        } else
        {
            setSelected(!getBooleanState());
        }
    }

    public void setSelected(boolean b)
    {
        if (b == getBooleanState())
        {
            return;
        }
        ClickManager cm = ClickManager.getInstance();
        cm.setClickPrecountEnabled(b);
        setBooleanState(b);  // Notify action listeners
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
            // Do nothing : still using the last valid song
        }
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(), "CTL_Precount");
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
        ClickManager cm = ClickManager.getInstance();
        if (evt.getSource() == cm)
        {
            if (evt.getPropertyName().equals(ClickManager.PROP_CLICK_PRECOUNT_ENABLED))
            {
                setBooleanState((boolean) evt.getNewValue());
            } else if (evt.getPropertyName().equals(ClickManager.PROP_CLICK_PRECOUNT_MODE))
            {
                updateUI(cm.getClickPrecountMode());
            }
        } else if (evt.getSource() == ActiveSongManager.getInstance())
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
        Song activeSong = ActiveSongManager.getInstance().getActiveSong();
        boolean b = (currentSong != null) && (currentSong == activeSong);
        setEnabled(b);
    }

    private void currentSongClosed()
    {
        currentSong.removePropertyChangeListener(this);
        currentSong = null;
        currentSongChanged();
    }

    private void updateUI(ClickManager.PrecountMode mode)
    {
        switch (mode)
        {
            case ONE_BAR:
                putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precount1-OFF-24x24.png")));
                putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precount1-ON-24x24.png")));
                putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_Precount1Bar"));
                break;
            case TWO_BARS:
                putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precount2-OFF-24x24.png")));
                putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precount2-ON-24x24.png")));
                putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_Precount2Bar"));
                break;
            case AUTO:
                putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precountA-OFF-24x24.png")));
                putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precountA-ON-24x24.png")));
                putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_PrecountBarAuto"));
                break;
            default:
                throw new AssertionError(mode.name());

        }

    }

}

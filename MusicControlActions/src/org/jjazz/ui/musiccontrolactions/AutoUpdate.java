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
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.MusicController.State;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.jjazz.ui.flatcomponents.api.FlatToggleButton;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.actions.BooleanStateAction;

/**
 * Enable/disable the auto-update mode.
 * <p>
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.autoupdate")
@ActionRegistration(displayName = "NOT USED", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ExtendedToolbar", position = 20)
        })
public class AutoUpdate extends BooleanStateAction implements PropertyChangeListener
{

    private UpdatableSongSession currentUpdatableSession;
    private static final Logger LOGGER = Logger.getLogger(AutoUpdate.class.getSimpleName());

    public AutoUpdate()
    {
        LOGGER.fine("AutoUpdate() -- ");

        setBooleanState(PlaybackSettings.getInstance().isAutoUpdateEnabled());

        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource(
                "/org/jjazz/ui/musiccontrolactions/resources/AutoUpdate-OFF-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource(
                "/org/jjazz/ui/musiccontrolactions/resources/AutoUpdate-ON-24x24.png")));
        putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource(
                "/org/jjazz/ui/musiccontrolactions/resources/AutoUpdate-disabled-24x24.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_AutoUpdateTooltip"));
        putValue("hideActionText", true);

        MusicController.getInstance().addPropertyChangeListener(this);


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
        PlaybackSettings.getInstance().setAutoUpdateEnabled(b);

        setBooleanState(b);  // Notify action listeners
    }

    @Override
    public String getName()
    {
        return "not used";
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
        var mc = MusicController.getInstance();

        if (evt.getSource() == currentUpdatableSession)
        {
            if (evt.getPropertyName().equals(UpdatableSongSession.PROP_ENABLED))
            {
                if (!currentUpdatableSession.isEnabled())
                {
                    setEnabled(false);
                }
            }
        } else if (evt.getSource() == MusicController.getInstance())
        {
            if (evt.getPropertyName().equals(MusicController.PROP_PLAYBACK_SESSION))
            {
                // Unregister previous session
                if (currentUpdatableSession != null)
                {
                    currentUpdatableSession.removePropertyChangeListener(this);
                }

                currentUpdatableSession = null;       // By default


                PlaybackSession newSession = mc.getPlaybackSession();
                if (newSession instanceof UpdatableSongSession uss)
                {
                    currentUpdatableSession = uss;
                    currentUpdatableSession.addPropertyChangeListener(this);
                    setEnabled(currentUpdatableSession.isEnabled());
                }
            }

            if (currentUpdatableSession == null)
            {
                // Unknow session, autoupdate has no meaning                 
                setEnabled(false);
            }

        } else if (evt.getPropertyName().equals(MusicController.PROP_STATE))
        {
            State newState = (State) evt.getNewValue();
            State oldState = (State) evt.getOldValue();

            switch (newState)
            {
                case DISABLED:
                    break;
                case PAUSED:
                case STOPPED:
                    if (currentUpdatableSession != null)
                    {
                        setEnabled(true);
                    }
                    break;
                case PLAYING:
                    break;
                default:
                    throw new AssertionError(newState.name());

            }
        }
    }

    // ======================================================================
    // Private methods
    // ======================================================================   
}

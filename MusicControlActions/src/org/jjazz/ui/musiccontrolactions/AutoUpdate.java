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
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.playbacksession.DynamicSongSession;
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

    private DynamicSongSession currentDynamicSession;
    private static final Logger LOGGER = Logger.getLogger(AutoUpdate.class.getSimpleName());

    public AutoUpdate()
    {
        LOGGER.fine("AutoUpdate() -- ");
        
        setBooleanState(PlaybackSettings.getInstance().isAutoUpdateEnabled());

        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/AutoUpdate-OFF-24x24.png")));     //NOI18N
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/AutoUpdate-ON-24x24.png")));   //NOI18N
        putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/AutoUpdate-disabled-24x24.png")));   //NOI18N
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_AutoUpdateTooltip"));   //NOI18N
        putValue("hideActionText", true);       //NOI18N

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
        return "not used";       //NOI18N
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

        if (evt.getSource() == currentDynamicSession)
        {
            if (evt.getPropertyName().equals(DynamicSongSession.PROP_UPDATES_ENABLED))
            {
                if (!currentDynamicSession.isUpdatable())
                {
                    setEnabled(false);
                }
            }
        } else if (evt.getSource() == MusicController.getInstance())
        {
            if (evt.getPropertyName().equals(MusicController.PROP_PLAYBACK_SESSION))
            {
                // Unregister previous session
                if (currentDynamicSession != null)
                {
                    currentDynamicSession.removePropertyChangeListener(this);
                }

                currentDynamicSession = null;       // By default


                // If it's an Updatable+Dynamic session, we can work
                PlaybackSession session = mc.getPlaybackSession();
                if (session instanceof UpdatableSongSession)
                {
                    PlaybackSession baseSession = ((UpdatableSongSession) session).getBaseSession();
                    if (baseSession instanceof DynamicSongSession)
                    {
                        currentDynamicSession = (DynamicSongSession) baseSession;
                        currentDynamicSession.addPropertyChangeListener(this);
                        setEnabled(currentDynamicSession.isUpdatable());
                    }
                }

                if (currentDynamicSession == null)
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
                        if (currentDynamicSession != null)
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
    }

    // ======================================================================
    // Private methods
    // ======================================================================   

    private void updateEnabled()
    {
        boolean b = false;

        if (ActiveSongManager.getInstance().getActiveSong() != null)
        {
            switch (MusicController.getInstance().getState())
            {
                case DISABLED:
                    break;
                case STOPPED:
                    b = true;
                    break;
                case PAUSED:
                    break;
                case PLAYING:
                    PlaybackSession session = MusicController.getInstance().getPlaybackSession();
                    break;
                default:        // DISABLED
                // Nothing                
            }
        }
        setEnabled(b);
    }

}

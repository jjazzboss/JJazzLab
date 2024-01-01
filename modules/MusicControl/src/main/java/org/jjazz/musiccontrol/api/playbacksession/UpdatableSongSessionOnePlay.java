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
package org.jjazz.musiccontrol.api.playbacksession;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.util.EnumSet;
import org.jjazz.musiccontrol.api.MusicController;

/**
 * A special UpdatableSongSession, which accepts an UpdateProvider as base session, and which closes itself after being played.
 * <p>
 * This prevents the session to be automatically updated after session is played. 
 */
public class UpdatableSongSessionOnePlay extends UpdatableSongSession
{
    /**
     * Create an UpdatableSongSession to enable updates of the specified BaseSongSession.
     *
     * @param session Must be an UpdateProvider instance
     */
    public UpdatableSongSessionOnePlay(BaseSongSession session)
    {
        super(session);
        Preconditions.checkNotNull(session);
        Preconditions.checkArgument(session instanceof UpdateProvider);


        MusicController.getInstance().addPropertyChangeListener(this);
    }

    @Override
    public void close()
    {
        super.close();
        MusicController.getInstance().removePropertyChangeListener(this);
    }
    
        @Override
    public String toString()
    {
        return "UpdatableSongSessionOnePlay=[" + super.toString() + "]";
    }

    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        super.propertyChange(evt);


        var mc = MusicController.getInstance();
        if (evt.getSource() == mc)
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                var oldState = (MusicController.State) evt.getOldValue();
                var newState = (MusicController.State) evt.getNewValue();


                if (EnumSet.of(MusicController.State.PLAYING, MusicController.State.PAUSED).contains(oldState)
                        && EnumSet.of(MusicController.State.STOPPED, MusicController.State.DISABLED).contains(newState))
                {
                    close();
                }
            }
        }
    }
}

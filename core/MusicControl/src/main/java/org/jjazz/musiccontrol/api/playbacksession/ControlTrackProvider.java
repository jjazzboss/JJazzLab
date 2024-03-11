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

import org.jjazz.musiccontrol.api.ControlTrack;

/**
 * PlaybackSession additional capability: the sequence contains a control track.
 * <p>
 */
public interface ControlTrackProvider
{

    /**
     * Session must fire this property change event with newValue=false when control track info becomes disabled (it is enabled by
     * default).
     */
    public static final String ENABLED_STATE = "PropControlTrackProviderEnabledState";

    /**
     * get the control track.
     *
     * @return Null if no contral track available.
     */
    ControlTrack getControlTrack();

}

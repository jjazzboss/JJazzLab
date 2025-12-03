/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.musiccontrol.api.playbacksession;

import java.awt.event.ActionListener;

/**
 * Base configuration parameters of PlaybackSession implementations.
 *
 * @param includeClickTrack    If true add the click track, and its muted/unmuted state will depend on the PlaybackSettings
 * @param includePrecountTrack If true add the precount track, and loopStartTick will depend on the PlaybackSettings
 * @param includeControlTrack  if true add a control track (beat positions + chord symbol markers)
 * @param loopCount            See Sequencer.setLoopCount(). Use PLAYBACK_SETTINGS_LOOP_COUNT to rely on the PlaybackSettings instance value.
 * @param endOfPlaybackAction  Action executed when playback is stopped. Can be null.
 */
public record SessionConfig(boolean includeClickTrack,
        boolean includePrecountTrack,
        boolean includeControlTrack,
        int loopCount,
        ActionListener endOfPlaybackAction)
        {

 

    /**
     * Create a default config which includes all tracks, loopCount uses PlaybackSettings, and no endOfPlaybackAction.
     */
    public SessionConfig()
    {
        this(true, true, true, BaseSongSession.PLAYBACK_SETTINGS_LOOP_COUNT, null);
    }

}

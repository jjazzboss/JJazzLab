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
package org.jjazz.mixconsole;

import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * The user actions on a MixChannelPanel that can not be handled by the MixChannelPanel itself.
 */
public interface MixChannelPanelController
{

    /**
     * User has changed the channel id.
     *
     * @param channel
     * @param newChannelId
     */
    void editChannelId(int channel, String newChannelId);

    /**
     * User has changed the channel name (only for user channels).
     *
     * @param channel
     * @param newChannelName
     */
    void editChannelName(int channel, String newChannelName);

    /**
     * Open the editor to edit the specified user phrase.
     *
     * @param userRhythmVoice
     */
    void editUserPhrase(UserRhythmVoice userRhythmVoice);

    /**
     * User asked to close a MixChannelPanel.
     * @param userRhythmVoice
     */
    void removeUserPhrase(UserRhythmVoice userRhythmVoice);

    /**
     * Clone a rhythm track in a new user track.
     *
     * @param rhythmVoice
     */
    void cloneRhythmTrackAsUserTrack(RhythmVoice rhythmVoice);

    /**
     * User wants to edit settings of our MixChannelPanel.
     * @param channel
     */
    void editSettings(int channel);

    /**
     * User wants to edit the MixChannelPanel instrument.
     * @param channel
     */
    void editInstrument(int channel);

    /**
     * User wants to use the next instrument after current one.
     * @param channel
     */
    void editNextInstrument(int channel);

    /**
     * User wants to use the previous instrument after current one.
     * @param channel
     */
    void editPreviousInstrument(int channel);

}

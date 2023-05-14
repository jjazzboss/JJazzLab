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
package org.jjazz.mixconsole;

import org.jjazz.midimix.api.UserRhythmVoice;

/**
 * The user actions on a MixChannelPanel that can not be handled by the MixChannelPanel itself.
 */
public interface MixChannelPanelController
{

    /**
     * User has changed the channel id.
     */
    void editChannelId(int channel, String newChannelId);

    /**
     * User has changed the channel name (only for user channels).
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
     */
    void removeUserPhrase(UserRhythmVoice userRhythmVoice);

    /**
     * User wants to edit settings of our MixChannelPanel.
     */
    void editSettings(int channel);

    /**
     * User wants to edit the MixChannelPanel instrument.
     */
    void editInstrument(int channel);

    /**
     * User wants to use the next instrument after current one.
     */
    void editNextInstrument(int channel);

    /**
     * User wants to use the previous instrument after current one.
     */
    void editPreviousInstrument(int channel);

}

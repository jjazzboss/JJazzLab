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
package org.jjazz.ui.mixconsole;

/**
 * The user actions on a MixChannelPanel that can not be handled by the MixChannelPanel itself.
 */
public interface MixChannelPanelController
{

    /**
     * User has changed the channel id.
     */
    public void editChannel(int channel, String newChannelId);

    /**
     * User asked to close our MixChannelPanel.
     */
    public void editClose(int channel);

    /**
     * User wants to edit settings of our MixChannelPanel.
     */
    public void editSettings(int channel);

    /**
     * User wants to edit the MixChannelPanel instrument.
     */
    public void editInstrument(int channel);

    /**
     * User wants to use the next instrument after current one.
     */
    public void editNextInstrument(int channel);

    /**
     * User wants to use the previous instrument after current one.
     */
    public void editPreviousInstrument(int channel);

}

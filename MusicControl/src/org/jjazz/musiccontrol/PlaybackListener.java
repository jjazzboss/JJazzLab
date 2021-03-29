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
package org.jjazz.musiccontrol;

import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

/**
 * Listener of events occuring during song playback.
 * <p>
 * There can be many events fired during a playback, so event handling must be time-efficient. Events are fired out of the Event
 * Dispatching Thread, so event handlers must use SwingUtilities.invokeLater() to trigger any UI-related work.
 */
public interface PlaybackListener
{

    void beatChanged(Position oldPos, Position newPos);

    void barChanged(int oldBar, int newBar);

    void chordSymbolChanged(String chordSymbol);

    /**
     * Indicates some musical activity on specified channel at specified time.
     * <p>
     * Should be used only for non-accurate "musical activity " indicators.
     *
     * @param channel
     * @param tick
     */
    void midiActivity(int channel, long tick);
}

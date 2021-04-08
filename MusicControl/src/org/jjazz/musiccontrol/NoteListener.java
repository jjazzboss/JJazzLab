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

/**
 * A listener to Note ON/OFF events fired by the MusicController.
 * <p>
 * Event handling must be time-efficient. Events are fired by the MusicController out of the Event Dispatching Thread, so event
 * handlers must use SwingUtilities.invokeLater() to trigger any UI-related work.
 * <p>
 * MusicController will fire the events taking into account the user-defined output synth latency.
 */
public interface NoteListener
{

    /**
     * A note ON event has occured.
     * <p>
     *
     * @param channel
     * @param pitch
     * @param velocity
     * @param tick The approximate tick at which the event occured. Can be -1 if no tick available.
     */
    void noteOn(long tick, int channel, int pitch, int velocity);

    /**
     * A note OFF event has occured.
     * <p>
     *
     * @param channel
     * @param pitch
     * @param tick The approximate tick at which the event occured. Can be -1 if no tick available.
     */
    void noteOff(long tick, int channel, int pitch);
}

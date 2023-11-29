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
package org.jjazz.musiccontrol.api;

/**
 * A listener to Note ON/OFF events fired by the MusicController.
 * <p>
 * MusicController will fire the events taking into account the user-defined output synth latency. Events are fired by the
 * MusicController on the Event Dispatching Thread, event handling must be time-efficient.
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
     * @param tick The approximate tick at which the event occured. Can be -1 if no tick information available.
     */
    void noteOn(long tick, int channel, int pitch, int velocity);

    /**
     * A note OFF event has occured.
     * <p>
     *
     * @param channel
     * @param pitch
     * @param tick The approximate tick at which the event occured. Can be -1 if no tick information available.
     */
    void noteOff(long tick, int channel, int pitch);
}

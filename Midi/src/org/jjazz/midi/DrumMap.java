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
package org.jjazz.midi;

import java.util.List;

/**
 * Defines a drum map: associate a percussion name to a note pitch.
 * <p>
 * 36=Kick, 37=Rimshot, 38=Snare, etc.
 */
public interface DrumMap
{

    /**
     * The name of the DrumMap.
     *
     * @return
     */
    public String getName();

    /**
     * An optional default drum map to be used as a replacement if this DrumMap could not be used for some reason.
     *
     * @return Can be null.
     */
    public DrumMap getDefaultDrumMap();

    /**
     * The note name, e.g. "Kick" for the given pitch.
     *
     * @param pitch
     * @return Can be null if pitch is not used by this DrumMap.
     */
    public String getNoteName(int pitch);

    /**
     * The pitch corresponding to the note name.
     *
     * @param noteName
     * @return -1 if noteName is not used by this DrumMap.
     */
    public int getNotePitch(String noteName);

    /**
     * Get the typical notes used for a rhythmic accent with this DrumMap.
     * <p>
     * Usually contains at least kicks/snares/crash or splash cymbals etc.
     *
     * @return Can be an empty list.
     */
    public List<Integer> getAccentNotes();

}

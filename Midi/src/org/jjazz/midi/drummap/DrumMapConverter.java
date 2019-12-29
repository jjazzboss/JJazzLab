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
package org.jjazz.midi.drummap;

import org.jjazz.midi.DrumMap;

/**
 * Map notes from a source DrumMap to a destination DrumMap.
 */
public interface DrumMapConverter
{

    String getConverterId();

    /**
     * Return true if this mapper accepts map as a source DrumMap.
     *
     * @param dMap
     * @return
     */
    boolean acceptSrcDrumMap(DrumMap dMap);

    /**
     * Return true if this mapper accepts map as a destination DrumMap.
     *
     * @param dMap
     * @return
     */
    boolean acceptDestDrumMap(DrumMap dMap);

    /**
     * Map srcPitch from srcMap into the corresponding note in destMap.
     *
     * @param srcMap
     * @param srcPitch
     * @param destMap
     * @return -1 If srcPitch could not be mapped.
     */
    int mapNote(DrumMap srcMap, int srcPitch, DrumMap destMap);
}

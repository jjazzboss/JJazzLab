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
package org.jjazz.songstructure.api.event;

import org.jjazz.util.SmallMap;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

public class SptResizedEvent extends SgsChangeEvent
{

    private SmallMap<SongPart, Integer> mapOldSptSize;

    /**
     * Some SongParts have been resized.
     *
     * @param src
     * @param mapOldSptSize The old size of each resized RhythPart.
     */
    public SptResizedEvent(SongStructure src, SmallMap<SongPart, Integer> mapOldSptSize)
    {
        super(src, mapOldSptSize.getKeys());
        this.mapOldSptSize = mapOldSptSize;
    }

    /**
     * @return A map with the old size of each resized SongPart.
     */
    public SmallMap<SongPart, Integer> getMapOldSptSize()
    {
        return mapOldSptSize;
    }
}

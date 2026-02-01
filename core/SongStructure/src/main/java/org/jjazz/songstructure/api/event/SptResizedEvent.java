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
package org.jjazz.songstructure.api.event;

import java.util.IdentityHashMap;
import java.util.Map;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * Song parts were resized.
 */
public class SptResizedEvent extends SgsChangeEvent
{

    public record Resizing(int oldSize, int newSize)
            {

    }
    private final Map<SongPart, Resizing> mapSptResizing;

    /**
     * Some SongParts have been resized.
     *
     * @param src
     * @param mapSptResizing A map providing the resizing for each SongPart
     */
    public SptResizedEvent(SongStructure src, Map<SongPart, Resizing> mapSptResizing)
    {
        super(src, mapSptResizing.keySet());
        this.mapSptResizing = new IdentityHashMap<>(mapSptResizing);
    }


    public int getOldSize(SongPart spt)
    {
        return mapSptResizing.get(spt).oldSize();
    }

    public int getNewSize(SongPart spt)
    {
        return mapSptResizing.get(spt).newSize();
    }
}

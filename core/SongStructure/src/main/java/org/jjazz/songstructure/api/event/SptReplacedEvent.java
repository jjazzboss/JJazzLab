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

import java.util.ArrayList;
import java.util.List;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * A SongPart has been replaced with another one with same size/startBarIndex.
 */
public class SptReplacedEvent extends SgsChangeEvent
{

    private final ArrayList<SongPart> newSpts = new ArrayList<>();

    public SptReplacedEvent(SongStructure src, List<SongPart> oldSpts, List<SongPart> newSpts)
    {
        super(src, oldSpts);
        this.newSpts.addAll(newSpts);
        SgsChangeEvent.sortSongParts(this.newSpts);
    }

    /**
     * @return The songpart to replace the source song part, ordered by startBarIndex (like oldSpts)
     */
    public List<SongPart> getNewSpts()
    {
        return newSpts;
    }
}

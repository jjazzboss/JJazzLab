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

public class SptRenamedEvent extends SgsChangeEvent
{

    public record Renaming(String oldName, String newName)
            {

    }
    private final Map<SongPart, Renaming> mapSptRenaming;

    /**
     * Some SongParts names have been changed
     *
     * @param src
     * @param mapSptRenaming Provide the old and new name for each renamed SongPart
     */
    public SptRenamedEvent(SongStructure src, Map<SongPart, Renaming> mapSptRenaming)
    {
        super(src, mapSptRenaming.keySet());        
        this.mapSptRenaming = new IdentityHashMap<>(mapSptRenaming);
    }

    public String getOldName(SongPart spt)
    {
        return mapSptRenaming.get(spt).oldName();
    }

    public String getNewName(SongPart spt)
    {
        return mapSptRenaming.get(spt).newName();
    }

}

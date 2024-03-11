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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * Describe a change in a SongStructure.
 */
public class SgsChangeEvent
{

    private final SongStructure source;
    /**
     * Ordered list.
     */
    private final ArrayList<SongPart> songParts = new ArrayList<>();

    public SgsChangeEvent(SongStructure src)
    {
        this(src, new ArrayList<SongPart>());
    }

    public SgsChangeEvent(SongStructure src, SongPart spt)
    {
        this(src, Arrays.asList(spt));
    }

    /**
     *
     * @param src
     * @param spts
     */
    public SgsChangeEvent(SongStructure src, Collection<SongPart> spts)
    {
        if (src == null || spts == null)
        {
            throw new IllegalArgumentException("src=" + src + " spts=" + spts);   
        }
        source = src;
        songParts.addAll(spts);
        sortSongParts(songParts);
    }

    /**
     * @return The first SongPart, i.e. which has the lowest startBarIndex. Can be null.
     */
    public SongPart getSongPart()
    {
        return songParts.isEmpty() ? null : songParts.get(0);
    }

    /**
     *
     * @return A list of SongParts ordered by startBarIndex (lowest first). Can be empty.
     */
    public List<SongPart> getSongParts()
    {
        return songParts;
    }

    public SongStructure getSource()
    {
        return source;
    }

    public static void sortSongParts(List<SongPart> spts)
    {
        Collections.sort(spts, new Comparator<SongPart>()
        {
            @Override
            public int compare(SongPart spt1, SongPart spt2)
            {
                return spt1.getStartBarIndex() - spt2.getStartBarIndex();
            }
        }
        );
    }
}

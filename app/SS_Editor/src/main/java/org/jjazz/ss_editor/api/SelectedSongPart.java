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
package org.jjazz.ss_editor.api;

import java.util.Objects;
import org.jjazz.songstructure.api.SongPart;

/**
 * A selected SongPart in the SongStructure editor.
 * <p>
 */
public record SelectedSongPart(SongPart songPart) implements Comparable<SelectedSongPart>
        {

    public SelectedSongPart
    {
        Objects.requireNonNull(songPart);
    }

    /**
     * Relies on songPart identity only because used in a selection lookup.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == null || this.getClass() != o.getClass())
        {
            return false;
        }
        SelectedSongPart s = (SelectedSongPart) o;
        return s.songPart == songPart;
    }

    /**
     * Relies on songPart identity only because used in a selection lookup.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 53 * hash + System.identityHashCode(songPart);
        return hash;
    }

    @Override
    public String toString()
    {
        return "selSpt(" + songPart.toString() + ")";
    }

    @Override
    public int compareTo(SelectedSongPart other)
    {
        Objects.requireNonNull(other);
        int res = Integer.compare(songPart().getStartBarIndex(), other.songPart().getStartBarIndex());
        return res;
    }
}

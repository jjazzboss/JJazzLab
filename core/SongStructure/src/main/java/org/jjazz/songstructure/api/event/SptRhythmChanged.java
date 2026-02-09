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

import java.util.List;
import java.util.Objects;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * SongParts got their rhythm changed, possibly their parent section too.
 * <p>
 * getSongParts() will return oldSpts.
 */
public class SptRhythmChanged extends SgsChangeEvent
{

    private final List<SongPart> newSpts;
    private final Rhythm rhythm;

    /**
     *
     * @param src
     * @param r             The new rhythm
     * @param oldSptsCopies A copy of each SongPart from newSpts before their rhythm was changed (possibly parentSection too)
     * @param newSpts       The changed SongParts
     */
    public SptRhythmChanged(SongStructure src, Rhythm r, List<SongPart> oldSptsCopies, List<SongPart> newSpts)
    {
        super(src, oldSptsCopies);
        Objects.requireNonNull(newSpts);
        this.rhythm = r;
        this.newSpts = sortSongParts(newSpts);
    }

    public Rhythm getNewRhythm()
    {
        return rhythm;
    }


    /**
     * @return The updated SongParts, ordered by startBarIndex
     */
    public List<SongPart> getNewSpts()
    {
        return newSpts;
    }
}

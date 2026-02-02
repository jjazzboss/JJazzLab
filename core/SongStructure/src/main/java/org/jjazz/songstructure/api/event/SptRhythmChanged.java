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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * A SongPart has been replaced by its clone but with a different rhythm.
 * <p>
 * getSongParts() will return oldSpts.
 */
public class SptRhythmChanged extends SgsChangeEvent
{

    private final ArrayList<SongPart> newSpts = new ArrayList<>();
    private final Rhythm rhythm;

    public SptRhythmChanged(SongStructure src, Rhythm r, List<SongPart> oldSpts, List<SongPart> newSpts)
    {
        super(src, oldSpts);
        Preconditions.checkArgument(newSpts.stream().allMatch(spt -> spt.getRhythm() == r), "r=%s newSpts=%s", newSpts);

        this.rhythm = r;
        this.newSpts.addAll(newSpts);
        SgsChangeEvent.sortSongParts(this.newSpts);
    }

    public Rhythm getNewRhythm()
    {
        return rhythm;
    }


    /**
     * @return The replacing SongParts, ordered by startBarIndex (like oldSpts but with the new rhythm)
     */
    public List<SongPart> getNewSpts()
    {
        return newSpts;
    }
}

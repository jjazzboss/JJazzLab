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
 * SongParts got their rhythm and/or parent section changed.
 * <p>
 * getSongParts() will return the updated SongParts (newSpts).
 * <p>
 * Note: when this event is used to precheck the change, olsSptCopies and newSpts will both be the SongParts to be modified.
 */
public class SptRhythmChangedEvent extends SgsChangeEvent
{

    private final List<SongPart> oldSptsCopies;
    private final Rhythm rhythm;

    /**
     * Create the event.
     * <p>
     * Note: when this event is used to precheck the change, olsSptCopies and newSpts will both be the SongParts to be modified.
     *
     * @param src
     * @param r             The new rhythm. Can be null if only parent section was changed.
     * @param oldSptsCopies A copy of each SongPart before their rhythm was changed (possibly parentSection too)
     * @param newSpts       The updated SongParts
     */
    public SptRhythmChangedEvent(SongStructure src, Rhythm r, List<SongPart> oldSptsCopies, List<SongPart> newSpts)
    {
        super(src, newSpts);
        Objects.requireNonNull(newSpts);
        this.rhythm = r;
        this.oldSptsCopies = sortSongParts(oldSptsCopies);
    }

    /**
     *
     * @return Can be null if only the parent section was changed
     */
    public Rhythm getNewRhythm()
    {
        return rhythm;
    }


    /**
     * @return Copies of the updated SongParts before the rhythm and/or parent section was updated.
     */
    public List<SongPart> getOldSptsCopies()
    {
        return oldSptsCopies;
    }
}

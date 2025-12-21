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
package org.jjazz.songcontext.api;

import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.IntRange;

/**
 * A SongContext with only one SongPart.
 */
public class SongPartContext extends SongContext
{

    /**
     * Create a SongPartContext.
     *
     * @param s
     * @param mix
     * @param spt
     */
    public SongPartContext(Song s, MidiMix mix, SongPart spt)
    {
        super(s, mix, spt.getBarRange());
    }

    /**
     * Create a SongPartContext.
     *
     * @param s
     * @param mix
     * @param bars The range must be contained in only one SongPart
     * @throws IllegalArgumentException If the bar range spans over 2 or more SongParts.
     */
    public SongPartContext(Song s, MidiMix mix, IntRange bars)
    {
        super(s, mix, bars);
        if (getSongParts().size() != 1)
        {
            throw new IllegalArgumentException("song=" + s + " mix=" + mix + " bars=" + bars);
        }
    }

    public SongPart getSongPart()
    {
        return getSongParts().get(0);
    }

    @Override
    public String toString()
    {
        return "SongPartContext[sg=" + getSong().getName() + ", " + getMidiMix() + ", " + getSongPart() + ", rg=" + getBarRange() + "]";
    }
}

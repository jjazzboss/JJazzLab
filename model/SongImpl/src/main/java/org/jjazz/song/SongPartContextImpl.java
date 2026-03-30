/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.song;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongPartContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.IntRange;

/**
 * SongPartContext implementation.
 */
public class SongPartContextImpl extends SongContextImpl implements SongPartContext
{

    public SongPartContextImpl(Song s, MidiMix mix, SongPart spt)
    {
        this(s, mix, spt, spt.getBarRange());
    }


    public SongPartContextImpl(Song s, MidiMix mix, SongPart spt, IntRange br)
    {
        Objects.requireNonNull(spt);
        Objects.requireNonNull(br);
        Preconditions.checkArgument(s.getSongStructure().getSongParts().contains(spt), "s=%s spt=%s", s, spt);
        Preconditions.checkArgument(spt.getBarRange().contains(br), "spt=%s br=%s", spt, br);
        super(s, mix, br, br.from);
    }


    @Override
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

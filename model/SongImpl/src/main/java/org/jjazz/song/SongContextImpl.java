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
package org.jjazz.song;

import java.util.Objects;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongContext;
import org.jjazz.utilities.api.IntRange;


/**
 * SongContext implementation.
 * <p>
 */
public class SongContextImpl implements SongContext
{

    private final Song song;
    private final MidiMix midiMix;
    private final IntRange barRange;


    /**
     * Create a SongContext object for whole or part of a song.
     *
     * @param s        Should be effectively immutable
     * @param mm       Should be effectively immutable
     * @param barRange If null, the range will represent the whole song from first to last bar.
     */
    public SongContextImpl(Song s, MidiMix mm, IntRange barRange)
    {
        Objects.requireNonNull(s);
        Objects.requireNonNull(mm);

        song = s;
        this.midiMix = mm;

        int sizeInBars = s.getSongStructure().getSizeInBars();
        if (sizeInBars == 0)
        {
            this.barRange = IntRange.EMPTY_RANGE;
        } else if (barRange == null)
        {
            this.barRange = new IntRange(0, sizeInBars - 1);
        } else if (barRange.to > sizeInBars - 1)
        {
            throw new IllegalArgumentException("s=" + s + " sizeInBars=" + sizeInBars + " mix=" + mm + " barRange=" + barRange);
        } else
        {
            this.barRange = barRange;
        }
    }

    /**
     * Create a SongContext which reuse sgContext's Song and MidiMix, but with the specified range.
     *
     * @param sgContext Should be effectively immutable
     * @param newRange
     */
    public SongContextImpl(SongContext sgContext, IntRange newRange)
    {
        this(sgContext.getSong(), sgContext.getMidiMix(), newRange);
    }


    @Override
    public Song getSong()
    {
        return song;
    }

    @Override
    public MidiMix getMidiMix()
    {
        return midiMix;
    }

    @Override
    public IntRange getBarRange()
    {
        return barRange;
    }


    @Override
    public SongContext clone()
    {
        return new SongContextImpl(this, getBarRange());
    }

    /**
     * Thread-safe deep copy implementation.
     *
     * @return
     */
    @Override
    public SongContext getDeepCopy()
    {
        SongImpl songImpl = (SongImpl) song;
        return songImpl.performReadAPImethod(() -> 
        {
            Song songCopy = song.getDeepCopy(false);
            MidiMix mixCopy = midiMix.getDeepCopy(songCopy);
            return new SongContextImpl(songCopy, mixCopy, barRange);
        });
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 97 * hash + System.identityHashCode(this.song);
        hash = 97 * hash + Objects.hashCode(this.midiMix);
        hash = 97 * hash + Objects.hashCode(this.barRange);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof SongContextImpl other)
        {
            if (this == other)
            {
                return true;
            }
            if (this.song != other.song)
            {
                return false;
            }
            if (!Objects.equals(this.midiMix, other.midiMix))
            {
                return false;
            }
            if (!Objects.equals(this.barRange, other.barRange))
            {
                return false;
            }
            return true;
        } else
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        return "SongContext[song=" + song.getName() + ", " + midiMix + ", range=" + barRange + "]";
    }


    // ============================================================================================
    // Private methods
    // ============================================================================================   
}

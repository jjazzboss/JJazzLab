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
import org.jjazz.midimix.spi.MidiMixManager;
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
    private final int loopRestartBar;


    /**
     * Create a SongContext object for whole or part of a song.
     *
     * @param s              Should be effectively immutable
     * @param mm             Should be effectively immutable
     * @param barRange       If null, the used bar range will represent the whole song from first to last bar.
     * @param loopRestartBar Must be within barRange
     */
    public SongContextImpl(Song s, MidiMix mm, IntRange barRange, int loopRestartBar)
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
            throw new IllegalArgumentException(
                    "s=" + s + " sizeInBars=" + sizeInBars + " mix=" + mm + " barRange=" + barRange + " loopRestartBar=" + loopRestartBar);
        } else
        {
            this.barRange = barRange;
        }
        if (!this.barRange.contains(loopRestartBar))
        {
            throw new IllegalArgumentException(
                    "s=" + s + " sizeInBars=" + sizeInBars + " mix=" + mm + " barRange=" + barRange + " loopRestartBar=" + loopRestartBar);
        }
        this.loopRestartBar = loopRestartBar;
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
    public int getLoopRestartBar()
    {
        return loopRestartBar;
    }

    @Override
    public SongContext clone()
    {
        return new SongContextImpl(this.getSong(), this.getMidiMix(), this.getBarRange(), this.getLoopRestartBar());
    }

    /**
     * Thread-safe deep copy implementation.
     *
     * @return
     */
    @Override
    public SongContext getDeepCopy(boolean disableInternalUpdates)
    {
        SongImpl songImpl = (SongImpl) song;
        return songImpl.performReadAPImethod(() -> 
        {
            Song songCopy = song.getDeepCopy(disableInternalUpdates);
            MidiMix mixCopy = MidiMixManager.getDefault().getDeepCopy(midiMix, disableInternalUpdates ? null : songCopy);
            return new SongContextImpl(songCopy, mixCopy, barRange, loopRestartBar);
        });
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 71 * hash + System.identityHashCode(this.song);
        hash = 71 * hash + System.identityHashCode(this.midiMix);
        hash = 71 * hash + Objects.hashCode(this.barRange);
        hash = 71 * hash + this.loopRestartBar;
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
            if (this.midiMix != other.midiMix)
            {
                return false;
            }
            if (!Objects.equals(this.barRange, other.barRange))
            {
                return false;
            }
            return this.loopRestartBar == other.loopRestartBar;
        } else
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        var s1 = "SongContext[song=" + song.getName() + ", " + midiMix + ", barRange=" + barRange;
        String s2 = loopRestartBar == barRange.from ? "" : ", loopRestartBar=" + loopRestartBar;
        return s1 + s2 + "]";
    }


    // ============================================================================================
    // Private methods
    // ============================================================================================   
}

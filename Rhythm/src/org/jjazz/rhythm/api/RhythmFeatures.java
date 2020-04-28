/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3)
 *  as published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 *
 *  Contributor(s):
 */
package org.jjazz.rhythm.api;

import java.util.Objects;

/**
 * Rhythm general features.
 */
public class RhythmFeatures
{
    private Feel feel;
    private Beat beat;
    private MusicalGenre genre;
    private TempoRange tempoRange;
    private Intensity intensity;

    /**
     * Construct an object with all default values.
     * <p>
     * Values are set to UNKNOWN, and ALL_TEMPO for TempoRange.
     * <p>
     */
    public RhythmFeatures()
    {
        this(Feel.UNKNOWN, Beat.UNKNOWN, MusicalGenre.UNKNOWN, TempoRange.ALL_TEMPO, Intensity.UNKNOWN);
    }


    public RhythmFeatures(Feel f, Beat b, MusicalGenre g, TempoRange rg, Intensity i)
    {
        if (f == null || b == null || g == null || rg == null || i == null)
        {
            throw new NullPointerException("f=" + f + " b=" + b + " g=" + g + " tg=" + rg + " i=" + i);
        }
        feel = f;
        beat = b;
        genre = g;
        tempoRange = rg;
        intensity = i;
    }

    /**
     *
     * @return Default to UNKNOWN.
     */
    public Feel getFeel()
    {
        return feel;
    }

    /**
     *
     * @return Default to UNKNOWN.
     */
    public Intensity getIntensity()
    {
        return intensity;
    }

    /**
     *
     * @return Default to UNKNOWN.
     */
    public Beat getBeat()
    {
        return beat;
    }

    /**
     *
     * @return Default to TempoRange.ALL_TEMPO.
     */
    public TempoRange getTempoRange()
    {
        return tempoRange;
    }

    /**
     *
     * @return Default to UNKNOWN.
     */
    public MusicalGenre getGenre()
    {
        return genre;
    }


    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 11 * hash + Objects.hashCode(this.feel);
        hash = 11 * hash + Objects.hashCode(this.beat);
        hash = 11 * hash + Objects.hashCode(this.genre);
        hash = 11 * hash + Objects.hashCode(this.tempoRange);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final RhythmFeatures other = (RhythmFeatures) obj;
        if (this.feel != other.feel)
        {
            return false;
        }
        if (this.beat != other.beat)
        {
            return false;
        }
        if (this.genre != other.genre)
        {
            return false;
        }
        if (!Objects.equals(this.tempoRange, other.tempoRange))
        {
            return false;
        }
        return true;
    }


}

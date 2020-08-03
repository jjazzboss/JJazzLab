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

import java.io.Serializable;
import java.util.Objects;

/**
 * Rhythm general features.
 */
public class RhythmFeatures implements Serializable
{

    private static final long serialVersionUID = 1223380872L;
    private Feel feel;
    private Beat beat;
    private Genre genre;
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
        this(Feel.UNKNOWN, Beat.UNKNOWN, Genre.UNKNOWN, TempoRange.ALL_TEMPO, Intensity.UNKNOWN);
    }


    public RhythmFeatures(Feel f, Beat b, Genre g, TempoRange rg, Intensity i)
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
     * Compute a matching score between this RhythmFeatures and rf.
     * <p>
     * Score calculation on each variable:<br>
     * - Add 0 point if both values are UNKNOWN (or ALL_TEMPO_RANGE)<br>
     * - Add 70 points if genre values match  <br>
     * - Add 30 points if 2 values match (other than genre)<br>
     * - Add 10 points if 1 value matches with UNKNOWN <br>
     *
     * @param rf
     * @return The matching score
     */
    public int getMatchingScore(RhythmFeatures rf)
    {
        int score = 0;
        if (feel == Feel.UNKNOWN && rf.getFeel() == Feel.UNKNOWN)
        {
            // Nothing
        } else if (feel == Feel.UNKNOWN || rf.getFeel() == Feel.UNKNOWN)
        {
            score += 10;
        } else if (feel == rf.getFeel())
        {
            score += 30;
        }
        if (beat == Beat.UNKNOWN && rf.getBeat() == Beat.UNKNOWN)
        {
            // Nothing
        } else if (beat == Beat.UNKNOWN || rf.getBeat() == Beat.UNKNOWN)
        {
            score += 10;
        } else if (beat == rf.getBeat())
        {
            score += 30;
        }
        if (genre == Genre.UNKNOWN && rf.getGenre() == Genre.UNKNOWN)
        {
            // Nothing
        } else if (genre == Genre.UNKNOWN || rf.getGenre() == Genre.UNKNOWN)
        {
            score += 10;
        } else if (genre == rf.getGenre())
        {
            score += 70;
        }
        if (intensity == Intensity.UNKNOWN && rf.getIntensity() == Intensity.UNKNOWN)
        {
            // Nothing
        } else if (intensity == Intensity.UNKNOWN || rf.getIntensity() == Intensity.UNKNOWN)
        {
            score += 10;
        } else if (intensity == rf.getIntensity())
        {
            score += 30;
        }
        if (tempoRange == TempoRange.ALL_TEMPO && rf.tempoRange == TempoRange.ALL_TEMPO)
        {
            // Nothing
        } else
        {
            score += (int) (tempoRange.computeSimilarityLevel(rf.tempoRange) * 30);
        }

        return score;
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
    public Genre getGenre()
    {
        return genre;
    }


    /**
     * Try to set features from a rhythm name.
     * <p>
     * Use featureValues to fix some values in the returned RhythmFeatures.
     *
     * @param name eg "Bossa Nova"
     * @param fixedValues If Intensity object use it for intensity, if Feel object use it for feel, etc.
     * @return If no guess all features will be UNKNOWN or TempoRange.ALL_TEMPO
     */
    static public RhythmFeatures guessFeatures(String name, Object... fixedValues)
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("rName=" + name + " fixedValues=" + fixedValues);
        }


        Feel f = Feel.UNKNOWN;
        Genre g = Genre.UNKNOWN;
        Intensity i = Intensity.UNKNOWN;
        TempoRange tr = TempoRange.ALL_TEMPO;
        Beat b = Beat.UNKNOWN;


        if (name.toLowerCase().contains("bossa"))
        {
            g = Genre.LATIN;
            f = Feel.BINARY;
            b = Beat.EIGHT;
        } else if (name.toLowerCase().contains("funk"))
        {
            g = Genre.FUNK;
            f = Feel.BINARY;
            b = Beat.SIXTEEN;
        }

        // Override values
        f = getFixedValue(Feel.class, fixedValues) == null ? f : getFixedValue(Feel.class, fixedValues);
        g = getFixedValue(Genre.class, fixedValues) == null ? g : getFixedValue(Genre.class, fixedValues);
        b = getFixedValue(Beat.class, fixedValues) == null ? b : getFixedValue(Beat.class, fixedValues);
        tr = getFixedValue(TempoRange.class, fixedValues) == null ? tr : getFixedValue(TempoRange.class, fixedValues);
        i = getFixedValue(Intensity.class, fixedValues) == null ? i : getFixedValue(Intensity.class, fixedValues);

        return new RhythmFeatures(f, b, g, tr, i);
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.feel);
        hash = 37 * hash + Objects.hashCode(this.beat);
        hash = 37 * hash + Objects.hashCode(this.genre);
        hash = 37 * hash + Objects.hashCode(this.tempoRange);
        hash = 37 * hash + Objects.hashCode(this.intensity);
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


    static private <T> T getFixedValue(Class<T> c, Object... values)
    {
        for (Object value : values)
        {
            if (value != null && c.isInstance(value))
            {
                @SuppressWarnings("unchecked")
                T res = (T) value;
                return res;
            }
        }
        return null;
    }

}

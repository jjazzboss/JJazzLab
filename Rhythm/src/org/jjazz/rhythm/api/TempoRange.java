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

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * A range of tempo.
 */
public final class TempoRange implements Cloneable, Serializable
{

    private static final long serialVersionUID = -4112098277726L;
    // Standard tempo ranges 
    public static final TempoRange ALL_TEMPO = new TempoRange(TempoRange.TEMPO_MIN, TempoRange.TEMPO_MAX, "All Tempo");
    public static final TempoRange VERY_SLOW = new TempoRange(TempoRange.TEMPO_MIN, 59, "Very Slow");
    public static final TempoRange SLOW = new TempoRange(60, 89, "Slow");
    public static final TempoRange MEDIUM_SLOW = new TempoRange(90, 109, "Medium Slow");
    public static final TempoRange MEDIUM = new TempoRange(110, 139, "Medium");
    public static final TempoRange MEDIUM_FAST = new TempoRange(140, 179, "Medium Fast");
    public static final TempoRange FAST = new TempoRange(180, 219, "Fast");
    public static final TempoRange VERY_FAST = new TempoRange(220, TempoRange.TEMPO_MAX, "Very Fast");
    // Const
    public static final int TEMPO_MIN = 10;
    public static final int TEMPO_STD = 120;
    public static final int TEMPO_MAX = 400;
    // Variables
    /**
     * Minimum recommanded tempo.
     */
    private int min = TEMPO_MIN;
    /**
     * Maximum recommanded tempo.
     */
    private int max = TEMPO_MAX;
    /**
     * TempoRange name.
     */
    private String name;
    private static final Logger LOGGER = Logger.getLogger(TempoRange.class.getSimpleName());

    static public List<TempoRange> getStandardTempoRanges()
    {
        ArrayList<TempoRange> trs = new ArrayList<>();
        Collections.addAll(trs, VERY_SLOW, SLOW, MEDIUM_SLOW, MEDIUM, MEDIUM_FAST, FAST, VERY_FAST, ALL_TEMPO);
        return trs;
    }

    /**
     * @param min
     * @param max
     * @param name
     */
    public TempoRange(int min, int max, String name)
    {
        if (!checkTempo(min) || !checkTempo(max) || min > max || name == null)
        {
            throw new IllegalArgumentException(" min=" + min + " max=" + max + " name=" + name);
        }
        this.min = min;
        this.max = max;
        this.name = name;
    }

    public TempoRange(TempoRange tr)
    {
        this.min = tr.min;
        this.max = tr.max;
        this.name = tr.name;
    }

    /**
     * @param tempo
     * @return True if tempo is included in the bounds of this TempoRange.
     */
    public boolean contains(int tempo)
    {
        return (tempo >= min && tempo <= max);
    }

    public int getMax()
    {
        return max;
    }

    public int getMin()
    {
        return min;
    }

    public String getName()
    {
        return name;
    }

    /**
     * @param o
     * @return True if both TempoRanges share same min and max values (name is not used).
     */
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof TempoRange)
        {
            TempoRange tr = (TempoRange) o;
            return min == tr.min && max == tr.max;
        } else
        {
            return false;
        }
    }

    /**
     * Use only min and max values, name is not used.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 23 * hash + this.min;
        hash = 23 * hash + this.max;
        return hash;
    }

    @Override
    public String toString()
    {
        return name + "[" + min + "-" + max + "]";
    }

    /**
     * Compute a percentage that say how similar are this object's tempo bounds with tr's tempo bounds.
     * <p>
     * Return value = (tempo range of the intersection of both objects)/(tempo range of the union of both objects) <br>
     * Examples: this = [60,80], tr=[90,100] =&gt; return value = 0 this = [60,80], tr=[70,90] =&gt; return value = 10/30 = 0.33 this =
     * [60,80], tr=[58,85] =&gt; return value = 20/27 = 0.74 this = [60,80], tr=[60,80] =&gt; return value = 20/20 = 1
     *
     * @param tr TempoRange
     * @return A value between 0 and 1.
     */
    public float computeSimilarityLevel(TempoRange tr)
    {
        float inter = 0;
        float union = 1;
        if (tr == null)
        {
            throw new NullPointerException("tr=" + tr);
        }
        if (min <= tr.min && max >= tr.min)
        {
            // e.g. this=[10, 20] tr=[15, 25]
            inter = Math.min(max, tr.max) - tr.min + 1;
            union = Math.max(max, tr.max) - min + 1;
        } else if (tr.min <= min && tr.max >= min)
        {
            // e.g. this=[20, 30] tr=[10, 25]
            inter = Math.min(max, tr.max) - min + 1;
            union = Math.max(max, tr.max) - tr.min + 1;
        }
        return inter / union;
    }

    public static boolean checkTempo(int t)
    {
        if ((t < TEMPO_MIN) || (t > TEMPO_MAX))
        {
            return false;
        } else
        {
            return true;
        }
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // ---------------------------------------------------------------------
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 37861L;
        private final int spVERSION = 1;
        private final int spMin;
        private final int spMax;
        private final String spName;

        private SerializationProxy(TempoRange tr)
        {
            spMin = tr.min;
            spMax = tr.max;
            spName = tr.name;
        }

        private Object readResolve() throws ObjectStreamException
        {
            TempoRange tr = new TempoRange(spMin, spMax, spName);
            return tr;
        }
    }
}

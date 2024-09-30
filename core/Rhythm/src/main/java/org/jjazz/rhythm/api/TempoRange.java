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
package org.jjazz.rhythm.api;

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * A range of tempo.
 */
public final class TempoRange implements Cloneable, Serializable
{

    private static final long serialVersionUID = -4112098277726L;
    // Standard tempo ranges 
    public static final TempoRange ALL_TEMPO = new TempoRange(TempoRange.TEMPO_MIN, TempoRange.TEMPO_MAX, "All Tempo");
    public static final TempoRange SLOW = new TempoRange(50, 90, "Slow");
    public static final TempoRange MEDIUM_SLOW = new TempoRange(75, 115, "Medium Slow");
    public static final TempoRange MEDIUM = new TempoRange(90, 135, "Medium");
    public static final TempoRange MEDIUM_FAST = new TempoRange(125, 180, "Medium Fast");
    public static final TempoRange FAST = new TempoRange(160, 250, "Fast");
    // Const
    public static final int TEMPO_MIN = 20;
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
        return List.of(SLOW, MEDIUM_SLOW, MEDIUM, MEDIUM_FAST, FAST);
    }

    static public TempoRange getStandardTempoRange(int tempo)
    {
        Preconditions.checkArgument(TempoRange.checkTempo(tempo), "tempo=", tempo);

        // As standard tempo ranges overlap, use a custom order
        for (var tr : List.of(MEDIUM, MEDIUM_SLOW, MEDIUM_FAST, SLOW, FAST))
        {
            if (tr.contains(tempo))
            {
                return tr;
            }
        }
        throw new IllegalStateException("tempo=" + tempo);
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
     * Examples: this = [60,80], tr=[90,100] =&gt; return value = 0 this = [60,80], tr=[70,90] =&gt; return value = 10/30 = 0.33 this = [60,80], tr=[58,85]
     * =&gt; return value = 20/27 = 0.74 this = [60,80], tr=[60,80] =&gt; return value = 20/20 = 1
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

    /**
     * Try to guess a standard TempoRange from the parameters.
     *
     * @param g
     * @param text Typically a style name, eg "BossaNova.s626.prs"
     * @return
     */
    public static TempoRange guess(Genre g, String text)
    {
        Objects.requireNonNull(g);
        Objects.requireNonNull(text);

        var res = switch (g)
        {
            case SAMBA, SOUL, RnB, HIP_HOP, BLUES, REGGAE, CLASSICAL ->
                TempoRange.MEDIUM_SLOW;
            case UNKNOWN ->
                TempoRange.ALL_TEMPO;
            default ->
            {
                if (containsOneOf(text.toLowerCase(), "fast", "upte", "up-te", "up "))
                {
                    yield TempoRange.FAST;
                    
                } else if (containsOneOf(text.toLowerCase(), "slow", "bld", "ballad"))
                {
                    yield TempoRange.SLOW;
                    
                } else
                {
                    yield TempoRange.MEDIUM;
                }
            }
        };
        return res;
    }

    static private boolean containsOneOf(String text, CharSequence... strs)
    {
        for (var str : strs)
        {
            if (text.contains(str))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files
                    xstream.alias("TempoRange", TempoRange.class);
                    xstream.alias("TempoRangeSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spMin");
                    xstream.useAttributeFor(SerializationProxy.class, "spMax");
                    xstream.useAttributeFor(SerializationProxy.class, "spName");
                }

                case MIDIMIX_LOAD ->
                {
                    // Nothing
                }
                case MIDIMIX_SAVE ->
                {
                    // Nothing
                }
                default -> throw new AssertionError(instanceId.name());
            }
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

    /**
     * Serialization proxy.
     * <p>
     * spVERSION2 introduces XStream aliases (XStreamConfig)
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 37861L;
        private int spVERSION = 2;          // Do not make final!
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

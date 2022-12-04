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
package org.jjazz.leadsheet.chordleadsheet.api.item;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A filter used to decide whether the alternate data of a CLI_ChordSymbol should be used.
 * <p>
 * This is an immutable class.
 */
public class AltDataFilter implements Serializable
{

    private final List<String> values;
    private Random random;

    public enum Random
    {
        RANDOM_RARE(0.3d), RANDOM(0.5d), RANDOM_OFTEN(0.7d);
        double threshold;

        private Random(double t)
        {
            threshold = t;
        }

        public double getThreshold()
        {
            return threshold;
        }
    };

    /**
     * Create a filter which accept a string if it is one of the specified values.
     *
     * @param values Must be a non empty list
     */
    public AltDataFilter(List<String> values)
    {
        if (values == null || values.isEmpty())
        {
            throw new IllegalArgumentException("values=" + values);   //NOI18N
        }

        this.values = new ArrayList<>(values);
        random = null;
    }

    /**
     * Create a filter which just accepts strings randomly.
     *
     * @param r
     */
    public AltDataFilter(Random r)
    {
        if (r == null)
        {
            throw new IllegalArgumentException("r=" + r);   //NOI18N
        }
        random = r;
        values = null;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.values);
        hash = 71 * hash + Objects.hashCode(this.random);
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
        final AltDataFilter other = (AltDataFilter) obj;
        if (!Objects.equals(this.values, other.values))
        {
            return false;
        }
        if (this.random != other.random)
        {
            return false;
        }
        return true;
    }

    /**
     * True if filter accepts strings randomly.
     *
     * @return
     */
    public boolean isRandom()
    {
        return random != null;
    }

    /**
     * Get the list of valid strings.
     *
     * @return Null if this filter is configured as a random filter.
     */
    public List<String> getValues()
    {
        return isRandom() ? null : new ArrayList<>(values);
    }

    /**
     * Check if we accept the specified string.
     *
     * @param str
     * @return
     */
    public boolean accept(String str)
    {
        if (str == null)
        {
            throw new NullPointerException("str");   //NOI18N
        }
        if (random != null)
        {
            double x = Math.random();
            return x <= random.getThreshold();
        } else
        {
            return values.contains(str);
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

        private static final long serialVersionUID = 12973626811L;
        private final int spVERSION = 1;
        private final List<String> spValues;
        private Random spRandom;

        private SerializationProxy(AltDataFilter f)
        {
            spRandom = f.random;
            spValues = f.values;        // Ok because f.values can't be changed
        }

        private Object readResolve() throws ObjectStreamException
        {
            AltDataFilter f;
            if (spValues != null)
            {
                f = new AltDataFilter(spValues);
            } else
            {
                f = new AltDataFilter(spRandom);
            }
            return f;
        }
    }

}

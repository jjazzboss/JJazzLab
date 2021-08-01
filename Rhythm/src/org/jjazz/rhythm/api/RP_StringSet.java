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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RpEnumerable;

/**
 * A RhythmParemeter composed of a set of strings.
 */
public class RP_StringSet implements RhythmParameter<Set<String>>, RpEnumerable<Set<String>>
{

    public static final int MAX_SET_SIZE = 10;
    private String id;
    private String displayName;
    private String description;
    private Set<String> defaultValue;
    private Set<String> minValue;
    private Set<String> maxValue;
    private List<String> possibleValues;
    protected static final Logger LOGGER = Logger.getLogger(RP_StringSet.class.getName());

    /**
     * @param id
     * @param name The name of the RhythmParameter.
     * @param description
     * @param defaultValue All members of the Set must be one of the possibleValues
     * @param possibleValues String[] The possible values which can be used in a Set (max MAX_SET_SIZE). By convention, min value
     * is set to the 1st possible value, max value to the last one. The empty string must not be one of the possible values.
     */
    public RP_StringSet(String id, String name, String description, Set<String> defaultValue, String... possibleValues)
    {
        if (id == null || name == null || defaultValue == null || possibleValues == null || possibleValues.length == 0 || possibleValues.length > MAX_SET_SIZE)
        {
            throw new IllegalArgumentException( //NOI18N
                    "id=" + id + " name=" + name + " defaultVal=" + defaultValue + " possibleValues=" + Arrays.asList(possibleValues));
        }
        this.id = id;
        this.displayName = name;
        this.description = description;
        this.possibleValues = Arrays.asList(possibleValues);
        if (this.possibleValues.indexOf("") != -1)
        {
            throw new IllegalArgumentException("n=" + name + " defaultVal=" + defaultValue + " possibleValues=" + this.possibleValues);   //NOI18N
        }
        this.defaultValue = defaultValue;
        if (!isValidValue(defaultValue))
        {
            throw new IllegalArgumentException("n=" + name + " defaultVal=" + defaultValue + " possibleValues=" + this.possibleValues);   //NOI18N
        }
        this.minValue = new HashSet<>();
        this.maxValue = new HashSet<>(this.possibleValues);

    }

    @Override
    public boolean equals(Object o)
    {
        boolean b = false;
        if (o instanceof RP_StringSet)
        {
            RP_StringSet rp = (RP_StringSet) o;
            b = id.equals(rp.id)
                    && displayName.equalsIgnoreCase(rp.displayName)
                    && description.equals(rp.description)
                    && defaultValue.equals(rp.defaultValue)
                    && possibleValues.equals(rp.possibleValues);
        }
        return b;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.id);
        hash = 67 * hash + Objects.hashCode(this.displayName.toLowerCase());
        hash = 67 * hash + Objects.hashCode(this.description);
        hash = 67 * hash + Objects.hashCode(this.defaultValue);
        hash = 67 * hash + Objects.hashCode(this.possibleValues);
        return hash;
    }

    @Override
    public final String getId()
    {
        return id;
    }

    @Override
    public final Set<String> getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * True if all elements of value are one of the possible values, or if value is empty.
     * <p>
     *
     * @param value
     * @return
     */
    @Override
    public final boolean isValidValue(Set<String> value)
    {
        boolean b = true;
        if (!value.isEmpty())
        {
            for (String s : value)
            {
                if (possibleValues.indexOf(s) == -1)
                {
                    b = false;
                    break;
                }
            }
        }
        return b;
    }

    /**
     * Calculate a Set from p as produced by calculatePercentage().
     * <p>
     *
     * @param p [0-1]
     * @return
     */
    @Override
    public final Set<String> calculateValue(double p)
    {
        if (p < 0 || p > 1)
        {
            throw new IllegalArgumentException("p=" + p);   //NOI18N
        }
        HashSet<String> set = new HashSet<>();
        long pBin = Math.round(10000 * p);
        for (int i = 0; i < possibleValues.size(); i++)
        {
            if ((pBin & 1) == 1)
            {
                set.add(possibleValues.get(i));
            }
            pBin >>= 1;

            if (pBin == 0)
            {
                // No need to loop further
                break;
            }
        }
//      LOGGER.severe("calculateValue() possibleValues="+possibleValues);
//      LOGGER.severe("calculateValue() p="+(Math.round(10000 * p))+" set="+set);
        return set;
    }

    /**
     * A unique value for a set.
     * <p>
     * 1/ Construct and int value : each bit correspond to the presence of not of the possibleValues. <br>
     * 2/ Divide this value by 10000 to make sure it's between 0 and 1.
     *
     * @return [0-1]
     */
    @Override
    public final double calculatePercentage(Set<String> value)
    {
        if (!isValidValue(value))
        {
            throw new IllegalArgumentException("value=" + value + " possibleValues=" + possibleValues);   //NOI18N
        }
        double p = 0;
        for (String s : value)
        {
            int index = possibleValues.indexOf(s);
            assert index != -1 : "s=" + s;   //NOI18N
            p += Math.pow(2, index);
        }
        if (p > 10000)
        {
            throw new IllegalStateException("p=" + p + " value=" + value);   //NOI18N
        }
//      LOGGER.severe("calculatePercentage() value="+value+" p="+p);
        return p / 10000;
    }

    @Override
    public final String getDescription()
    {
        return description;
    }

    /**
     * @return A set with all the possible items.
     */
    @Override
    public final Set<String> getMaxValue()
    {
        return maxValue;
    }

    /**
     * @return An empty set.
     */
    @Override
    public final Set<String> getMinValue()
    {
        return minValue;
    }

    @Override
    public final String getDisplayName()
    {
        return displayName;
    }

    @Override
    public final Set<String> getNextValue(Set<String> value)
    {
        if (!isValidValue(value))
        {
            throw new IllegalArgumentException("value=" + value + " this=" + this);   //NOI18N
        }
        double maxPercentage = (Math.pow(2, possibleValues.size()) - 1) / 10000d;
        double p = calculatePercentage(value);
        p += (1 / 10000d);
        if (p > (maxPercentage + 0.000001d))      // Because 0.0002 + 0.0001 = 0.000300000003!!!
        {
            p = 0;
        }
        Set<String> nextValue = calculateValue(p);
        return nextValue;
    }

    @Override
    public final Set<String> getPreviousValue(Set<String> value)
    {
        if (!isValidValue(value))
        {
            throw new IllegalArgumentException("value=" + value + " this=" + this);   //NOI18N
        }
        double maxPercentage = (Math.pow(2, possibleValues.size()) - 1) / 10000d;
        double p = this.calculatePercentage(value);
        p -= (1 / 10000d);
        if (p < -0.0000001d)        // See double rounding error in getNextValue()
        {
            p = maxPercentage;
        }
        Set<String> previousValue = calculateValue(p);
        return previousValue;
    }

    /**
     * Return a single-element list containing one set with all possible values.
     * <p>
     * Note: this does not return all the possible combinations of the set.
     *
     * @return
     */
    @Override
    public final List<Set<String>> getPossibleValues()
    {
        return Arrays.asList(getMaxValue());
    }

    /**
     *
     * @param value
     * @return A string with format "[v1,v2,v5]"
     */
    @Override
    public String valueToString(Set<String> value)
    {
        String s = null;
        if (isValidValue(value))
        {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (String v : value)
            {
                if (!first)
                {
                    sb.append(",");
                }
                sb.append(v);
                first = false;
            }
            sb.append("]");
            s = sb.toString();
        }
        return s;
    }

    @Override
    public Set<String> stringToValue(String s)
    {
        Set<String> res = null;
        if (s != null && "[]".equals(s.trim()))
        {
            res = new HashSet<>();
        } else if (s != null && s.length() > 1)
        {
            res = new HashSet<>();
            String[] strs = s.substring(1, s.length() - 1).split(",");
            for (String str : strs)
            {
                if (!possibleValues.contains(str))
                {
                    res = null;
                    break;
                }
                res.add(str);
            }
        }
        return res;
    }

    @Override
    public String getValueDescription(Set<String> value)
    {
        return null;
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }

    // =============================================================================================
    // Private methods
    // =============================================================================================
}

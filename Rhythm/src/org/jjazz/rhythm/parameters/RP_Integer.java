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
package org.jjazz.rhythm.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A RhythmParemeter representing positive integer values.
 */
public class RP_Integer implements RhythmParameter<Integer>
{

    private String id;
    private String displayName;
    private String description;
    private int defaultValue;
    private int minValue;
    private int maxValue;
    private ArrayList<Integer> possibleValues;
    /**
     * Step used to increase/decrease the value.
     */
    private int step;

    protected static final Logger LOGGER = Logger.getLogger(RP_Integer.class.getName());

    public RP_Integer(String id, String name, String description, int defaultVal, int minValue, int maxValue, int step)
    {
        if (id == null || name == null || description == null || name.length() == 0
                || minValue > maxValue
                || (step == 0 && minValue != maxValue)
                || step < 0
                || step > (maxValue - minValue))
        {
            throw new IllegalArgumentException(
                    "id=" + id + " n=" + name + " defaultVal=" + defaultVal + " min=" + minValue + " max=" + maxValue + " st=" + step);
        }
        this.id = id;
        this.displayName = name;
        this.description = description;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        if (!isValidValue(defaultVal))
        {
            throw new IllegalArgumentException(
                    "n=" + name + " defaultVal=" + defaultVal + " min=" + minValue + " max=" + maxValue + " st=" + step);
        }
        this.defaultValue = defaultVal;
    }

    @Override
    public final boolean equals(Object o)
    {
        boolean b = false;
        if (o instanceof RP_Integer)
        {
            RP_Integer rp = (RP_Integer) o;
            b = id.equals(rp.id)
                    && displayName.equals(rp.displayName)
                    && minValue == rp.minValue
                    && maxValue == rp.maxValue
                    && step == rp.step
                    && defaultValue == rp.defaultValue
                    && description.equals(rp.description);
        }
        return b;
    }

    @Override
    public final int hashCode()
    {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.displayName);
        hash = 97 * hash + Objects.hashCode(this.description);
        hash = 97 * hash + this.defaultValue;
        hash = 97 * hash + this.minValue;
        hash = 97 * hash + this.maxValue;
        hash = 97 * hash + this.step;
        return hash;
    }

    public final int getStep()
    {
        return step;
    }

    @Override
    public final String getDescription()
    {
        return description;
    }

    @Override
    public final Integer getMaxValue()
    {
        return maxValue;
    }

    @Override
    public final Integer getMinValue()
    {
        return minValue;
    }

    @Override
    public final String getId()
    {
        return id;
    }

    @Override
    public final String getDisplayName()
    {
        return displayName;
    }

    @Override
    public final Integer getNextValue(Integer value)
    {
        int newValue = value + step;
        return (newValue > maxValue) ? minValue : newValue;
    }

    @Override
    public final Integer getPreviousValue(Integer value)
    {
        int newValue = value - step;
        return (newValue < minValue) ? maxValue : newValue;
    }

    @Override
    public final Integer getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public final boolean isValidValue(Integer value)
    {
        if (value < minValue || value > maxValue)
        {
            return false;
        }
        if (step == 0)
        {
            return value == minValue;
        }
        return (value - minValue) % step == 0;
    }

    @Override
    public final Integer calculateValue(double y)
    {
        if (y < 0 || y > 1)
        {
            throw new IllegalArgumentException("y=" + y);
        }
        if (step == 0)
        {
            return minValue;
        }
        int res = minValue + (int) Math.round(y * (maxValue - minValue));
        if (!isValidValue(res))
        {
            int mod = res % step;
            res = mod < (step / 2d) ? res - mod : res - mod + step;
        }
        return res;
    }

    @Override
    public final double calculatePercentage(Integer value)
    {
        if (!isValidValue(value))
        {
            throw new IllegalArgumentException("value=" + value);
        }
        if (step == 0)
        {
            return 0;
        }
        double v = (value - minValue) / ((double) maxValue - minValue);
        v = Math.min(1, v);
        v = Math.max(0, v);
        return v;
    }

    @Override
    public final String toString()
    {
        return getDisplayName();
    }

    @Override
    public final List<Integer> getPossibleValues()
    {
        if (possibleValues == null)
        {
            possibleValues = new ArrayList<>();
            int count = 1000;
            Integer value = minValue;
            do
            {
                possibleValues.add(value);
                value = this.getNextValue(value);
                count--;
            } while (value != minValue && count > 0);
        }
        return possibleValues;
    }

    @Override
    public String valueToString(Integer value)
    {
        String s = null;
        if (isValidValue(value))
        {
            s = String.valueOf(value);
        }
        return s;
    }

    @Override
    public Integer stringToValue(String s)
    {
        Integer value = null;
        try
        {
            Integer v = Integer.valueOf(s);
            if (isValidValue(v))
            {
                value = v;
            }
        } catch (NumberFormatException ex)
        {
            // Do nothing
        }
        return value;
    }
}

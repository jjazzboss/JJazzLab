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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A RhythmParemeter representing positive integer values.
 */
public class RP_Integer implements RhythmParameter<Integer>, RpEnumerable<Integer>, Cloneable
{

    private final String id;
    private final String displayName;
    private final String description;
    private final int defaultValue;
    private final int minValue;
    private final int maxValue;
    private ArrayList<Integer> possibleValues;      // Lazy initialized in getPossibleValues()
    /**
     * Step used to increase/decrease the value.
     */
    private final int step;
    private final boolean primary;

    protected static final Logger LOGGER = Logger.getLogger(RP_Integer.class.getName());

    /**
     * Create a RP_Integer.Created instance is primary by default.
     *
     *
     * @param id
     * @param name
     * @param description
     * @param defaultVal
     * @param minValue
     * @param maxValue
     * @param step
     * @param isPrimary
     */
    public RP_Integer(String id, String name, String description, boolean isPrimary, int defaultVal, int minValue, int maxValue, int step)
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
        this.primary = isPrimary;
        if (!isValidValue(defaultVal))
        {
            throw new IllegalArgumentException(
                    "n=" + name + " defaultVal=" + defaultVal + " min=" + minValue + " max=" + maxValue + " st=" + step);
        }
        this.defaultValue = defaultVal;
    }

    @Override
    public RP_Integer getCopy(Rhythm r)
    {
        return new RP_Integer(id, displayName, description, primary, defaultValue, minValue, maxValue, step);
    }

    @Override
    public boolean isPrimary()
    {
        return primary;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.id);
        hash = 43 * hash + Objects.hashCode(this.displayName);
        hash = 43 * hash + Objects.hashCode(this.description);
        hash = 43 * hash + this.defaultValue;
        hash = 43 * hash + this.minValue;
        hash = 43 * hash + this.maxValue;
        hash = 43 * hash + this.step;
        hash = 43 * hash + (this.primary ? 1 : 0);
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
        final RP_Integer other = (RP_Integer) obj;
        if (this.defaultValue != other.defaultValue)
        {
            return false;
        }
        if (this.minValue != other.minValue)
        {
            return false;
        }
        if (this.maxValue != other.maxValue)
        {
            return false;
        }
        if (this.step != other.step)
        {
            return false;
        }
        if (this.primary != other.primary)
        {
            return false;
        }
        if (!Objects.equals(this.id, other.id))
        {
            return false;
        }
        if (!Objects.equals(this.displayName, other.displayName))
        {
            return false;
        }
        return Objects.equals(this.description, other.description);
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
    public String saveAsString(Integer value)
    {
        String s = null;
        if (isValidValue(value))
        {
            s = String.valueOf(value);
        }
        return s;
    }

    @Override
    public Integer loadFromString(String s)
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

    @Override
    public String getValueDescription(Integer value)
    {
        return null;
    }

    @Override
    public boolean isCompatibleWith(RhythmParameter<?> rp)
    {
        return rp instanceof RP_Integer && rp.getId().equals(getId());
    }

    @Override
    public <T> Integer convertValue(RhythmParameter<T> rp, T value)
    {
        Preconditions.checkArgument(isCompatibleWith(rp), "rp=%s is not compatible with this=%s", rp, this);
        Preconditions.checkNotNull(value);

        RP_Integer rpInt = (RP_Integer) rp;
        Integer intValue = (Integer) value;

        // Convert via the percentage value
        double percentage = rpInt.calculatePercentage(intValue);
        Integer res = calculateValue(percentage);
        return res;
    }

    @Override
    public String getDisplayValue(Integer value)
    {
        return value.toString();
    }
}

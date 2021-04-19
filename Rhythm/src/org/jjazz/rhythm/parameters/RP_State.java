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

import org.jjazz.rhythm.api.Enumerable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.RhythmParameter;

/**
 * A RhythmParemeter whose value can be some specified strings.
 */
public class RP_State implements RhythmParameter<String>, Enumerable<String>
{

    private String id;
    private String displayName;
    private String description;
    private String defaultValue;
    private String minValue;
    private String maxValue;
    private String[] possibleValues;   
    protected static final Logger LOGGER = Logger.getLogger(RP_State.class.getName());

    /**
     * @param id
     * @param name The name of the RhythmParameter.
     * @param defaultValue String The default value.
     * @param possibleValues String[] The possible values for this parameter. By convention, min value is set to the 1st possible
     * value, max value to the last one.
     */
    public RP_State(String id, String name, String description, String defaultValue, String... possibleValues)
    {
        if (id == null || name == null || defaultValue == null || possibleValues == null || possibleValues.length == 0)
        {
            throw new IllegalArgumentException( //NOI18N
                    "id=" + id + " name=" + name + " defaultVal=" + defaultValue + " possibleValues=" + Arrays.asList(possibleValues));
        }
        this.id = id;
        this.displayName = name;
        this.description = description;
        this.possibleValues = possibleValues;
        this.minValue = possibleValues[0];
        this.maxValue = possibleValues[possibleValues.length - 1];
        if (indexOf(defaultValue) == -1)
        {
            throw new IllegalArgumentException("n=" + name + " defaultVal=" + defaultValue + " possibleValues=" + Arrays.asList(possibleValues));   //NOI18N
        }
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean equals(Object o)
    {
        boolean b = false;
        if (o instanceof RP_State)
        {
            RP_State rp = (RP_State) o;
            b = id.equals(rp.id)
                    && displayName.equalsIgnoreCase(rp.displayName)
                    && description.equals(rp.description)
                    && defaultValue.equals(rp.defaultValue)
                    && Arrays.deepEquals(possibleValues, rp.possibleValues);
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
        hash = 67 * hash + Arrays.deepHashCode(this.possibleValues);
        return hash;
    }

    @Override
    public final String getId()
    {
        return id;
    }

    @Override
    public final String getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public final boolean isValidValue(String value)
    {
        return indexOf(value) != -1;
    }

    @Override
    public final String calculateValue(double y)
    {
        if (y < 0 || y > 1)
        {
            throw new IllegalArgumentException("y=" + y);   //NOI18N
        }
        int index = (int) Math.round(y * (possibleValues.length - 1));
        return possibleValues[index];
    }

    @Override
    public final double calculatePercentage(String value)
    {
        if (!isValidValue(value))
        {
            throw new IllegalArgumentException("value=" + value + " this=" + this);   //NOI18N
        }
        double index = indexOf(value);
        return index / (possibleValues.length - 1);
    }

    @Override
    public final String getDescription()
    {
        return description;
    }

    @Override
    public final String getMaxValue()
    {
        return maxValue;
    }

    @Override
    public final String getMinValue()
    {
        return minValue;
    }

    @Override
    public final String getDisplayName()
    {
        return displayName;
    }

    @Override
    public final String getNextValue(String value)
    {
        int valueIndex = indexOf(value);
        if (valueIndex == -1)
        {
            throw new IllegalArgumentException("value=" + value);   //NOI18N
        }
        return possibleValues[(valueIndex >= possibleValues.length - 1) ? 0 : valueIndex + 1];

    }

    @Override
    public final String getPreviousValue(String value)
    {
        int valueIndex = indexOf(value);
        if (valueIndex == -1)
        {
            throw new IllegalArgumentException("value=" + value);   //NOI18N
        }
        return possibleValues[(valueIndex == 0) ? possibleValues.length - 1 : valueIndex - 1];
    }

    @Override
    public final List<String> getPossibleValues()
    {
        return Arrays.asList(possibleValues);
    }

    @Override
    public String valueToString(String value)
    {
        String s = null;
        if (isValidValue(value))
        {
            s = value;
        }
        return s;
    }

    @Override
    public String stringToValue(String s)
    {
        return valueToString(s);
    }

    @Override
    public String getValueDescription(String value)
    {
        return null;
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }

    private int indexOf(String s)
    {
        for (int i = 0; i < possibleValues.length; i++)
        {
            if (possibleValues[i].equals(s))
            {
                return i;
            }
        }
        return -1;
    }

}

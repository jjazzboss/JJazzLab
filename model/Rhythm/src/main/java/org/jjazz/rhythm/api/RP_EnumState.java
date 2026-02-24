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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A variant of RP_State built from an enum value.
 *
 * @param <E>
 */
public class RP_EnumState<E extends Enum<E>> implements RhythmParameter<E>, RpEnumerable<E>, Cloneable
{

    private final String id;
    private final String displayName;
    private final String description;
    private final E defaultValue;
    private final List<E> possibleValues;
    private final Class<E> enumClass;
    private final boolean primary;
    protected static final Logger LOGGER = Logger.getLogger(RP_EnumState.class.getName());

    /**
     * Create a RP_EnumState RhythmParameter.
     * <p>
     * The possible values are the possible enum values. The min value if the first value of the enum, the max value the last one.
     * <p>
     *
     * @param id
     * @param name         The name of the RhythmParameter.
     * @param description
     * @param isPrimary
     * @param enumClass
     * @param defaultValue E The default value.
     */
    public RP_EnumState(String id, String name, String description, boolean isPrimary, Class<E> enumClass, E defaultValue)
    {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(enumClass);
        Preconditions.checkArgument(EnumSet.allOf(enumClass).contains(defaultValue), "enumClass=%s defaultValue=%s", enumClass, defaultValue);

        this.id = id;
        this.displayName = name;
        this.description = description;
        this.enumClass = enumClass;
        this.possibleValues = Collections.unmodifiableList(new ArrayList<>(EnumSet.allOf(enumClass)));
        this.defaultValue = defaultValue;
        this.primary = isPrimary;
    }

    @Override
    public RP_EnumState<E> getCopy(Rhythm r)
    {
        return new RP_EnumState(id, displayName, description, primary, enumClass, defaultValue);
    }

    @Override
    public boolean isPrimary()
    {
        return primary;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 61 * hash + Objects.hashCode(this.id);
        hash = 61 * hash + Objects.hashCode(this.displayName);
        hash = 61 * hash + Objects.hashCode(this.description);
        hash = 61 * hash + Objects.hashCode(this.defaultValue);
        hash = 61 * hash + Objects.hashCode(this.enumClass);
        hash = 61 * hash + (this.primary ? 1 : 0);
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
        final RP_EnumState<?> other = (RP_EnumState<?>) obj;
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
        if (!Objects.equals(this.description, other.description))
        {
            return false;
        }
        if (!Objects.equals(this.defaultValue, other.defaultValue))
        {
            return false;
        }
        return Objects.equals(this.enumClass, other.enumClass);
    }


    @Override
    public final String getId()
    {
        return id;
    }

    @Override
    public final E getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public final boolean isValidValue(E value)
    {
        return possibleValues.indexOf(value) != -1;
    }

    @Override
    public final E calculateValue(double y)
    {
        Preconditions.checkArgument(y < 0 || y > 1, "y=%s", y);
        int index = (int) Math.round(y * (possibleValues.size() - 1));
        return possibleValues.get(index);
    }

    @Override
    public final double calculatePercentage(E value)
    {
        Preconditions.checkArgument(isValidValue(value), "value=%s", value);
        double index = possibleValues.indexOf(value);
        return index / (possibleValues.size() - 1);
    }

    @Override
    public final String getDescription()
    {
        return description;
    }

    @Override
    public final E getMaxValue()
    {
        return possibleValues.getLast();
    }

    @Override
    public final E getMinValue()
    {
        return possibleValues.getFirst();
    }

    @Override
    public final String getDisplayName()
    {
        return displayName;
    }

    @Override
    public final E getNextValue(E value)
    {
        int valueIndex = possibleValues.indexOf(value);
        if (valueIndex == -1)
        {
            throw new IllegalArgumentException("value=" + value);
        }
        int index = (valueIndex >= possibleValues.size() - 1) ? 0 : valueIndex + 1;
        return possibleValues.get(index);

    }

    @Override
    public final E getPreviousValue(E value)
    {
        int valueIndex = possibleValues.indexOf(value);
        if (valueIndex == -1)
        {
            throw new IllegalArgumentException("value=" + value);
        }
        int index = (valueIndex == 0) ? possibleValues.size() - 1 : valueIndex - 1;
        return possibleValues.get(index);
    }

    @Override
    public final List<E> getPossibleValues()
    {
        return possibleValues;
    }

    @Override
    public String saveAsString(E value)
    {
        String s = null;
        if (isValidValue(value))
        {
            s = value.toString();
        }
        return s;
    }

    @Override
    public E loadFromString(String s)
    {
        E res = possibleValues.stream()
                .filter( v -> v.toString().equals(s))
                .findAny()
                .orElse(null);
        return res;
    }

    @Override
    public String getValueDescription(E value)
    {
        return null;
    }

    @Override
    public boolean isCompatibleWith(RhythmParameter<?> rp)
    {
        return rp instanceof RP_EnumState && rp.getId().equals(getId());
    }

    @Override
    public <T> E convertValue(RhythmParameter<T> rp, T value)
    {
        Preconditions.checkArgument(isCompatibleWith(rp), "rp=%s is not compatible with this=%s", rp, this);
        Preconditions.checkNotNull(value);

        RP_EnumState rpEnumState = (RP_EnumState) rp;

        double percentage = rpEnumState.calculatePercentage((Enum) value);
        E res = calculateValue(percentage);
        return res;
    }

    @Override
    public String getDisplayValue(E value)
    {
        return isValidValue(value) ? value.toString() : "";
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }

}

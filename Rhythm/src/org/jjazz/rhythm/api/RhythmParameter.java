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

import java.util.List;

/**
 * Define a parameter that influences the way a Rhythm generates music.
 * <p>
 * A RhythmParameter implementation can have additional capabilities by implementing additional interfaces, such as RpEnumerable,
 * RpCustomEditorProvider, RpViewerRendererFactory, etc.
 * <p>
 *
 * @param <E> The type of value of this RhythmParameter. E.toString() should return a short (max ~30 characters) user-readable
 * string.
 *
 */
public interface RhythmParameter<E>
{

    /**
     * Helper method to check compatibility of 2 Rps.
     *
     * @param <T>
     * @param <S>
     * @param rp1
     * @param rp2
     * @return True if Rps have the same id, or the same DisplayName (ignoring case) with the same value class.
     */
    static public <T, S> boolean checkCompatibility(RhythmParameter<T> rp1, RhythmParameter<S> rp2)
    {
        return rp1.getId().equals(rp2.getId())
                || (rp1.getDisplayName().equalsIgnoreCase(rp2.getDisplayName()) && rp1.getDefaultValue().getClass() == rp2.getDefaultValue().getClass());
    }

    /**
     * Return the first compatible RhythmParameter in rps compatible with rp.
     *
     * @param rps
     * @param rp
     * @return Can be null.
     */
    static public RhythmParameter<?> findFirstCompatibleRp(List<? extends RhythmParameter<?>> rps, RhythmParameter<?> rp)
    {
        for (RhythmParameter<?> rpi : rps)
        {
            if (checkCompatibility(rp, rpi))
            {
                return rpi;
            }
        }
        return null;
    }

    /**
     * @return A unique identifier. Usually the english name.
     */
    String getId();

    /**
     * @return The localized display name of the rhythm parameter.
     */
    String getDisplayName();

    /**
     * The description of this rhythm parameter.
     *
     * @return
     */
    String getDescription();

    /**
     * Provide an optional description or help text associated to the specified value.
     *
     * @param value
     * @return Can be null.
     */
    String getValueDescription(E value);

    /**
     * @return Object The default value.
     */
    E getDefaultValue();

    /**
     * Try to convert the specified RhythmParameter value to a string.
     *
     * @param value
     * @return Can be null if value is invalid or RhytmParameter does not have this capability.
     */
    String valueToString(E value);

    /**
     * Try to convert the specified string to a RhythmParameter value.
     *
     * @param s A string produced by valueToString().
     * @return Can be null if conversion failed.
     */
    E stringToValue(String s);

    /**
     * @param value
     * @return True is value is valid.
     */
    boolean isValidValue(E value);

    /**
     * Clone the specified value.
     * <p>
     * The default implementation just return value, which is fine is E is an immutable class. If E is mutable, this method must
     * be overridden.
     *
     * @param value
     * @return A copy of the specified value.
     */
    default E cloneValue(E value)
    {
        return value;
    }

}

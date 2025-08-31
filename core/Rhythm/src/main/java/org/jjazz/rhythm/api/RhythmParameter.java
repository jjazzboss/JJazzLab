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

import java.util.List;

/**
 * Define a parameter that influences the way a Rhythm generates music.
 * <p>
 * A RhythmParameter implementation can have additional capabilities by implementing additional interfaces, such as RpEnumerable, RpCustomEditorFactory,
 * RpViewerRendererFactory, etc.
 * <p>
 *
 * @param <E> The type of value of this RhythmParameter. E.toString() should return a short (max ~30 characters) user-readable string. Always prefer an
 *            immutable value class, but if it's not possible E must implement the MutableRpValue interface.
 * @see MutableRpValue
 */
public interface RhythmParameter<E>
{


    /**
     * Return the first RhythmParameter in rps compatible with rp.
     *
     * @param rps
     * @param rp
     * @return Null if no compatible RhythmParameter found
     */
    static public RhythmParameter<?> findFirstCompatibleRp(List<? extends RhythmParameter<?>> rps, RhythmParameter<?> rp)
    {
        return rps.stream()
                .filter(rpi -> rpi.isCompatibleWith(rp))
                .findFirst().orElse(null);
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
     * Get a short String representation of the value.
     *
     * @param value
     * @return Can be an empty String.
     */
    String getDisplayValue(E value);

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
     * @return Can be null if value is invalid.
     * @see loadFromString(String)
     */
    String saveAsString(E value);

    /**
     * Try to convert the specified string to a RhythmParameter value.
     *
     * @param s A string produced by valueToString().
     * @return Can be null if conversion failed.
     * @see saveAsString(E)
     */
    E loadFromString(String s);

    /**
     * @param value
     * @return True is value is valid.
     */
    boolean isValidValue(E value);

    /**
     * Clone the specified value.
     * <p>
     * The default implementation just return value, which is fine if E is an immutable class. If E is mutable, this method must be overridden.
     *
     * @param value
     * @return A copy of the specified value.
     */
    default E cloneValue(E value)
    {
        return value;
    }

    /**
     * Indicate if rp is compatible with this RhythmParameter.
     * <p>
     * NOTE: if rp1 is compatible with rp2, then rp2 must be compatible with rp1 as well.
     *
     * @param rp
     * @return True if a rp's value can be converted to a value for this RhythmParameter.
     */
    boolean isCompatibleWith(RhythmParameter<?> rp);


    /**
     * Convert the value of a compatible RhythmParameter to a value for this RhythmParameter.
     *
     * @param <T>     A RhythmParameter value
     * @param rp      A compatible RhythmParameter
     * @param rpValue The value to convert
     * @return The rpValue converted for this RhythmParameter. Can't be null.
     * @throws IllegalArgumentException If rp is not a compatible with this RhythmParameter.
     */
    <T> E convertValue(RhythmParameter<T> rp, T rpValue);

    /**
     * Get a copy of this RhythmParameter.
     *
     * @param r Can be null. Optionally set the target Rhythm of the returned RhythmParameter -though it might be ignored by the implementing method.
     * @return
     */
    RhythmParameter<E> getCopy(Rhythm r);

    /**
     * Indicate if this RhythmParameter is a primary parameter for its rhythm.
     * <p>
     * This information is made available mainly for the user interface, e.g. to select which RhythmParameters to show first.
     * <p>
     * Default implementation returns true.
     *
     * @return
     */
    default boolean isPrimary()
    {
        return true;
    }


}

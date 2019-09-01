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

import java.util.List;

/**
 * Define a parameter that influences the way a Rhythm generates music.
 *
 * @param <E> The type of value of this RhythmParameter.
 */
public interface RhythmParameter<E>
{

    /**
     * @return A unique identifier. Usually the english name.
     */
    String getId();

    /**
     * @return The localized display name of the rhythmparameter.
     */
    String getDisplayName();

    String getDescription();

    E getMaxValue();

    E getMinValue();

    /**
     * @return Object The default value.
     */
    E getDefaultValue();

    /**
     * @param value
     * @return A percentage between 0 and 1 representing value in the range of the possible values.
     * @see calculateValue()
     */
    double calculatePercentage(E value);

    /**
     * Calculate the RhythmParameter value corresponding to a percentage of the value range. For example if RhythmParameter is an
     * integer between 0 and 4: percentage=0 -> value=0 percentage=0.5 -> value=2 percentage=1 -> value=4.
     *
     * @param percentage A float between 0 and 1.
     * @return A RhythmParameter value.
     * @see calculatePercentage()
     */
    E calculateValue(double percentage);

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
     * @see valueToString()
     */
    E stringToValue(String s);

    /**
     * Get the next value after specified value.
     *
     * @param value
     * @return Object
     */
    E getNextValue(E value);

    /**
     * Get the next value before specified value.
     *
     * @param value
     * @return Object
     */
    E getPreviousValue(E value);

    /**
     * @param value
     * @return True is value is valid.
     */
    boolean isValidValue(E value);

    /**
     * Get the list of possible values for this RhythmParameter. Only the first 10000 values are returned.
     *
     * @return
     */
    List<E> getPossibleValues();

    static public class Utilities
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
    }
}

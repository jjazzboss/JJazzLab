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
import org.openide.util.Lookup;

/**
 * Define a parameter that influences the way a Rhythm generates music.
 * <p>
 * A parameter may have additional capabilities: the relevant interfaces must be stored in the RhythmParameter lookup.<br>
 * Example:
 *
 *
 * @param <E> The type of value of this RhythmParameter.
 */
public interface RhythmParameter<E> extends Lookup.Provider
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
     * The lookup contains the optional additional capabilities of this RhythmParameter.
     * <p>
     * The default implementation returns an empty lookup. Implementations which have additional capabilities (e.g. Enumerable)
     * should override this method to return a non-empty lookup.
     *
     * @return
     * @see org.openide.util.lookup.Lookups
     */
    @Override
    default Lookup getLookup()
    {
        return Lookup.EMPTY;
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

}

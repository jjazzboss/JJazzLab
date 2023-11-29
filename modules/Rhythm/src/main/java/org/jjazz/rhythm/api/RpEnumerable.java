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
 * A RhythmParameter capability : enumerable.
 *
 * @param <E> The type of value of this RhythmParameter.
 */
public interface RpEnumerable<E extends Object>
{

    E getMaxValue();

    E getMinValue();

    /**
     * @param value
     * @return A percentage between 0 and 1 representing value in the range of the possible values.
     */
    double calculatePercentage(E value);

    /**
     * Calculate the RhythmParameter value corresponding to a percentage of the value range.
     * <p>
     * For example if RhythmParameter is an integer between 0 and 4: percentage=0 -&gt; value=0 percentage=0.5 -&gt; value=2
     * percentage=1 -&gt; value=4.
     *
     * @param percentage A float between 0 and 1.
     * @return A RhythmParameter value.
     */
    E calculateValue(double percentage);

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
     * Get the list of possible values for this RhythmParameter.
     * <p>
     * Only the first 10000 values are returned.
     *
     * @return
     */
    List<E> getPossibleValues();

}

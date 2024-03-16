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
package org.jjazz.analytics.spi;

import java.util.Map;
import org.jjazz.analytics.api.Analytics;

/**
 * A processor of feature analytics events.
 * <p>
 * Authorized value classes: String, Long, Float, Boolean, or a Collection of one these classes.
 * <p>
 * The Analytics class will call the relevant methods on each service provider instance found in the global lookup.
 */
public interface AnalyticsProcessor
{

    /**
     * Log a generic event with no properties.
     *
     * @param eventName
     */
    void logEvent(String eventName);

    /**
     * Generic event with properties.
     *
     * @param eventName
     * @param properties Authorized value classes:  String, Integer, Long, Float, Boolean, or a Collection of one these classes.
     */
    void logEvent(String eventName, Map<String, ?> properties);

    /**
     * Update the properties of the current JJazzLab computer.
     * <p>
     *
     * @param properties Authorized value classes: String, Integer, Long, Float, Boolean, or a Collection of one these classes.
     * @see Analytics#getJJazzLabComputerId()
     */
    void setProperties(Map<String, ?> properties);

    /**
     * Update the properties of the current JJazzLab computer only if they are not already set.
     * <p>
     *
     * @param properties Authorized value classes: String, Integer, Long, Float, Boolean, or a Collection of one these classes.
     * @see Analytics#getJJazzLabComputerId()
     */
    void setPropertiesOnce(Map<String, ?> properties);

    /**
     * Increment numeric properties of the current JJazzLab computer by the corresponding Long value.
     *
     * @param properties
     * @see Analytics#getJJazzLabComputerId()
     */
    void incrementProperties(Map<String, Long> properties);

}

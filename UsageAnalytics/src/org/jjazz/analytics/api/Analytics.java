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
package org.jjazz.analytics.api;

import org.json.JSONObject;

/**
 * Usage analytics method.
 * <p>
 */
public class Analytics
{

    /**
     * Generic event with no properties.
     *
     * @param name
     */
    public static void event(String name)
    {

    }

    /**
     * Generic event with properties.
     *
     * @param name
     * @param properties
     */
    public static void event(String name, JSONObject properties)
    {

    }

    /**
     * Update the properties of the current profile.
     * <p>
     * The current profile correponds to the running JJazzLab-X installation.
     *
     * @param properties
     */
    public static void set(org.json.JSONObject properties)
    {

    }
}

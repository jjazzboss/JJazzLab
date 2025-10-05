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
package org.jjazz.utilities.api;

import java.awt.Color;
import java.io.Serializable;

/**
 * String-value based and serializable ObservableProperties.
 */
public class StringProperties extends ObservableProperties<String> implements Serializable
{

    public StringProperties()
    {
    }

    public StringProperties(Object owner)
    {
        super(owner);
    }

    public StringProperties(Object owner, StringProperties sp)
    {
        super(owner, sp);
    }

    public Boolean getBoolean(String prop, Boolean defaultValue)
    {
        String s = get(prop);
        Boolean res = defaultValue;
        try
        {
            res = Boolean.valueOf(s);
        } catch (NumberFormatException ex)
        {
            // Nothing
        }
        return res;
    }

    public void putBoolean(String prop, Boolean value)
    {
        put(prop, String.valueOf(value));
    }

    public int getInt(String prop, Integer defaultValue)
    {
        String s = get(prop);
        Integer res = defaultValue;
        try
        {
            res = Integer.valueOf(s);
        } catch (NumberFormatException ex)
        {
            // Nothing
        }
        return res;
    }

    public void putInt(String prop, Integer value)
    {
        put(prop, String.valueOf(value));
    }

    /**
     * Add an offset to an int property value.
     * <p>
     * If property does not exist yet, the method creates it with initValue then adds offset.
     *
     * @param prop
     * @param offset
     * @param initValue
     * @return The new int property value.
     */
    public int shiftInt(String prop, int offset, int initValue)
    {
        int value = getInt(prop, initValue);
        value += offset;
        putInt(prop, value);
        return value;
    }

    public Color getColor(String prop, Color defaultValue)
    {
        String s = get(prop);
        Color res = defaultValue;
        try
        {
            int rgb = Integer.parseInt(s);
            res = new Color(rgb);
        } catch (NumberFormatException ex)
        {
            // Nothing
        }
        return res;
    }

    public void putColor(String prop, Color value)
    {
        putInt(prop, value != null ? value.getRGB() : null);
    }

}

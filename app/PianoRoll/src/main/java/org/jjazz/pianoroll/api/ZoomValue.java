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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Zoom value for an editor.
 * <p>
 * hValue and vValue must be in the range [0;100], 50 is default.
 */
public record ZoomValue(int hValue, int vValue)
        {

    private static final Logger LOGGER = Logger.getLogger(ZoomValue.class.getSimpleName());

    /**
     * Create a zoom value with 50 / 50.
     */
    public ZoomValue()
    {
        this(50, 50);
    }

    public ZoomValue 
    {
        if (!checkValue(hValue) || !checkValue(vValue))
        {
            throw new IllegalArgumentException("hValue=" + hValue + " vValue=" + vValue);
        }
    }

    /**
     * Get a copy of this instance with hValue changed.
     *
     * @param newHValue
     * @return
     */
    public ZoomValue setH(int newHValue)
    {
        if (!checkValue(newHValue))
        {
            throw new IllegalArgumentException("newHValue=" + newHValue);
        }
        return new ZoomValue(newHValue, vValue);
    }


    /**
     * Get a copy of this instance with vValue changed.
     *
     * @param newVValue
     * @return
     */
    public ZoomValue setV(int newVValue)
    {
        if (!checkValue(newVValue))
        {
            throw new IllegalArgumentException("newVValue=" + newVValue);
        }
        return new ZoomValue(hValue, newVValue);
    }

    /**
     * Get a copy with the specified parameters added to the existing hValue and vValue.
     *
     * @param hDelta
     * @param vDelta
     * @return
     */
    public ZoomValue offset(int hDelta, int vDelta)
    {
        int newh = hValue + hDelta;
        newh = Math.max(0, newh);
        newh = Math.min(100, newh);
        int newv = vValue + vDelta;
        newv = Math.max(0, newv);
        newv = Math.min(100, newv);
        return new ZoomValue(newh, newv);
    }

    /**
     * hValue as a float.
     *
     * @return [0;1]
     */
    public float hValueFloat()
    {
        return hValue() / 100f;
    }
    
        /**
     * hValue as a float.
     *
     * @return [0;1]
     */
    public float vValueFloat()
    {
        return vValue() / 100f;
    }

    /**
     * Save the object as a string.
     *
     * @return
     * @see #loadFromString(java.lang.String)
     */
    public String saveAsString()
    {
        return Integer.toString(hValue) + ";" + Integer.toString(vValue);
    }

    /**
     * Get a ZoomValue instance from the specified string.
     *
     * @param s
     * @return
     * @see #saveAsString()
     */
    static public ZoomValue loadFromString(String s)
    {
        Preconditions.checkNotNull(s);
        ZoomValue res = null;

        String strs[] = s.split("\\s*;\\s*");
        if (strs.length == 2)
        {
            int h = Integer.parseInt(strs[0]);
            int v = Integer.parseInt(strs[1]);
            try
            {
                res = new ZoomValue(h, v);
            } catch (IllegalArgumentException e)
            {
                // Nothing
            }
        }

        if (res == null)
        {
            LOGGER.log(Level.WARNING, "loadFromString() Illegal ZoomValue string={0}. Using default value instead.", s);
            res = new ZoomValue();
        }

        return res;
    }

    private boolean checkValue(int v)
    {
        return (v >= 0 && v <= 100);
    }
}

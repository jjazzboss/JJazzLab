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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.util.logging.Logger;

/**
 * Zoom value for an editor.
 * <p>
 * hFactor and vFactor must be in the range [0;100]. One of them can be the special ZOOM_TO_FIT value.
 */
public record ZoomValue(int hFactor, int vFactor)
        {

    /**
     * Special value which tells the editor to select the appropriate value to fit the available width OR height (not both).
     */
    public static final int ZOOM_TO_FIT = 99999;
    private static final Logger LOGGER = Logger.getLogger(ZoomValue.class.getSimpleName());

    public ZoomValue()
    {
        this(50, 50);
    }

    public ZoomValue 
    {
        if (!checkValue(hFactor) || !checkValue(vFactor) || (hFactor == ZOOM_TO_FIT && vFactor == ZOOM_TO_FIT))
        {
            throw new IllegalArgumentException("hFactor=" + hFactor + " vFactor=" + vFactor);
        }
    }

    /**
     * Get a copy of this instance with hFactor changed.
     *
     * @param newHFactor
     * @return
     */
    public ZoomValue getHCopy(int newHFactor)
    {
        if (!checkValue(newHFactor))
        {
            throw new IllegalArgumentException("newHFactor=" + newHFactor);
        }
        return new ZoomValue(newHFactor, vFactor);
    }

    /**
     * Get a copy of this instance with vFactor changed.
     *
     * @param newVFactor
     * @return
     */
    public ZoomValue getVCopy(int newVFactor)
    {
        if (!checkValue(newVFactor))
        {
            throw new IllegalArgumentException("newVFactor=" + newVFactor);
        }
        return new ZoomValue(hFactor, newVFactor);
    }

    /**
     * Save the object as a string.
     *
     * @return
     * @see #loadFromString(java.lang.String)
     */
    public String saveAsString()
    {
        return Integer.toString(hFactor) + ";" + Integer.toString(vFactor);
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
            LOGGER.warning("loadFromString() Illegal ZoomValue string=" + s + ". Using default value instead.");
            res = new ZoomValue();
        }

        return res;
    }

    private boolean checkValue(int v)
    {
        return (v >= 0 && v <= 100) || v == ZOOM_TO_FIT;
    }
}

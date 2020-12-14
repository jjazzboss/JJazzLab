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
package org.jjazz.ui.mixconsole;

import java.util.logging.Logger;
import org.jjazz.ui.flatcomponents.FlatIntegerKnob;

/**
 * A knob for panoramic
 */
public class PanoramicKnob extends FlatIntegerKnob
{

    private static final Logger LOGGER = Logger.getLogger(PanoramicKnob.class.getSimpleName());

    public PanoramicKnob()
    {
        setLabel("panoramic");
    }

    @Override
    protected String valueToString(int v)
    {
        String text = "C";
        if (v < 64)
        {
            text = "L" + (64 - v);
        } else if (v > 64)
        {
            text = "R" + (v - 64);
        }
        return text;
    }

    /**
     * Return the Midi value : 0-127
     *
     * @param text Accepted strings are "C", "R12" (0-64), "L20" (0-63), "120" (0-127)
     * @return -1 if string is not valid
     */
    @Override
    protected int stringToValue(String text)
    {
        int value = -1;
        String t = text.toUpperCase();
        if (t.equals("C"))
        {
            value = 64;
        } else if (t.charAt(0) == 'L')
        {
            try
            {
                int v = Integer.parseUnsignedInt(t.substring(1));
                v = Math.max(0, v);
                v = Math.min(64, v);
                value = 64 - v;
            } catch (NumberFormatException e)
            {
                // Nothing leave value unchanged
            }
        } else if (t.charAt(0) == 'R')
        {
            try
            {
                int v = Integer.parseUnsignedInt(t.substring(1));
                v = Math.max(0, v);
                v = Math.min(63, v);
                value = 64 + v;
            } catch (NumberFormatException e)
            {
                // Nothing leave value unchanged
            }
        } else
        {
            try
            {
                int v = Integer.parseUnsignedInt(t.substring(1));
                v = Math.max(0, v);
                v = Math.min(127, v);
                value = v;
            } catch (NumberFormatException e)
            {
                // Nothing leave value unchanged
            }
        }
        if (value == -1)
        {
            LOGGER.fine("parseString() text=" + text);   //NOI18N
        }
        return value;
    }
}

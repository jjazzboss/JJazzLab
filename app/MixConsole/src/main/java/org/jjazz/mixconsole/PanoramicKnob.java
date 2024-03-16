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
package org.jjazz.mixconsole;

import java.util.logging.Logger;
import org.jjazz.flatcomponents.api.FlatIntegerKnob;

/**
 * A knob for panoramic
 */
public class PanoramicKnob extends FlatIntegerKnob
{

    private static final Logger LOGGER = Logger.getLogger(PanoramicKnob.class.getSimpleName());

    @Override
    protected String prepareToolTipText()
    {
        String valueAstring = isEnabled() ? valueToPanString(getValue()) : "OFF";
        String text = (getTooltipLabel() == null) ? valueAstring : getTooltipLabel() + "=" + valueAstring;
        return text;
    }

    private String valueToPanString(int v)
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
}

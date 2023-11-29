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
package org.jjazz.flatcomponents.api;

import java.awt.Color;
import java.awt.Font;

/**
 * A factory for predefined FlatIntegerKnobs.
 */
public class FlatKnobFactory
{

    public static final Font FONT = new Font("Arial", Font.PLAIN, 9);
    public static final Color FONT_COLOR = new Color(242, 242, 242);
    public static final Color BACKGROUND_DARK = new Color(46, 46, 46);
    public static final Color BACKGROUND_BRIGHT = new Color(28, 28, 28);

    static FlatIntegerKnob getSmallDarkKnob()
    {
        FlatIntegerKnob knob = new FlatIntegerKnob();
        knob.setKnobUpperColor(new Color(115, 115, 115));
        knob.setKnobLowerColor(new Color(87, 87, 87));
        knob.setKnobRectColor(new Color(253, 253, 253));
        knob.setKnobRadius(11);
        knob.setKnobStartAngle(220);
        knob.setValueLineColor(new Color(80, 241, 255));        // Cyan
        knob.setValueLineGap(3);
        knob.setValueLineThickness(2);
        return knob;
    }

    static FlatIntegerKnob getLargeBrightKnob()
    {
        FlatIntegerKnob knob = new FlatIntegerKnob();
        knob.setKnobUpperColor(new Color(241, 242, 241));
        knob.setKnobLowerColor(new Color(197, 197, 197));
        knob.setKnobRectColor(new Color(5, 5, 5));
        knob.setKnobRadius(21);
        knob.setKnobStartAngle(220);
        knob.setValueLineColor(new Color(248, 132, 41));        // Orange
        knob.setValueLineGap(3);
        knob.setValueLineThickness(5);
        return knob;
    }

}

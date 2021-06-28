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
package org.jjazz.ui.utilities.api;

import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;

public class StringMetrics
{

    Font font;
    FontRenderContext context;

    public StringMetrics(Graphics2D g2)
    {
        font = g2.getFont();
        context = g2.getFontRenderContext();
    }

    /**
     * Return a baseline relative coordinates, include the leading (interline spacing).
     *
     * @param message
     * @return
     */
    public Rectangle2D getLogicalBounds(String message)
    {
        return font.getStringBounds(message, context);
    }

    public double getWidth(String message)
    {
        Rectangle2D bounds = getLogicalBounds(message);
        return bounds.getWidth();
    }

    public double getHeight(String message)
    {
        Rectangle2D bounds = getLogicalBounds(message);
        return bounds.getHeight();
    }

}

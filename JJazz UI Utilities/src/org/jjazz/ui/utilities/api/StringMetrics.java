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

/**
 * Compute string metrics.
 * <p>
 * Try to cache data whenever possible.
 */
public class StringMetrics
{

    Font font;
    FontRenderContext fontRendererContext;
    LineMetrics lineMetrics;
    Rectangle2D bounds, boundsNoLeading;
    String lastText;

    public StringMetrics(Graphics2D g2, Font font)
    {
        fontRendererContext = g2.getFontRenderContext();
        this.font = font;
    }

    /**
     * Return a rectangle in baseline relative coordinates, include the leading (interline spacing).
     *
     * @param text
     * @return
     */
    public Rectangle2D getLogicalBounds(String text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        if (bounds == null || !text.equals(lastText))
        {
            bounds =  font.getStringBounds(text, fontRendererContext);
            boundsNoLeading = null;
        }
        lastText = text;
        return bounds;
    }

    /**
     * Return a rectangle in baseline relative coordinates, excluding the leading (interline spacing).
     *
     * @param text
     * @return
     */
    public Rectangle2D getLogicalBoundsNoLeading(String text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        if (boundsNoLeading == null || !text.equals(lastText))
        {
            bounds = font.getStringBounds(text, fontRendererContext);
            lineMetrics = font.getLineMetrics(text, fontRendererContext);
            boundsNoLeading = new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight() - lineMetrics.getLeading());
        }
        lastText = text;
        return boundsNoLeading;
    }

    public double getWidth(String text)
    {
        return getLogicalBounds(text).getWidth();
    }

    /**
     * get the height of this text, including the leading (interline space).
     *
     * @param text
     * @return
     */
    public double getHeight(String text)
    {
        return getLogicalBounds(text).getHeight();
    }

    /**
     * Get the height of this text, excluding the leading (interline space).
     *
     * @param text
     * @return
     */

    public double getHeightNoLeading(String text)
    {
        return getLogicalBoundsNoLeading(text).getHeight();
    }

}

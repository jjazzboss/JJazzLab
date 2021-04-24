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
package org.jjazz.ui.rpviewer;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.logging.Logger;

/**
 * A LayoutManager to arrange the 2 children in the top left/right corners, with a margin.
 */
public class RpViewerLayoutManager implements LayoutManager
{

    public static final String NORTH_EAST = "NE";
    public static final String NORTH_WEST = "NW";
    private static final int PADDING = 1;
    private Component cNW, cNE;
    private static final Logger LOGGER = Logger.getLogger(RpViewerLayoutManager.class.getSimpleName());

    @Override
    public void layoutContainer(Container container)
    {
        Insets in = container.getInsets();
        int xMin = in.left + PADDING;
        int y = in.top + PADDING;
                
        if (cNW != null)
        {
            cNW.setSize(cNW.getPreferredSize());
            cNW.setLocation(xMin, y);
            xMin += cNW.getWidth();
        }

        if (cNE != null)
        {
            cNE.setSize(cNE.getPreferredSize());
            int x = Math.max(xMin, container.getWidth() - in.right - PADDING - cNE.getWidth());
            cNE.setLocation(x, y);
        }
    }

    @Override
    public void addLayoutComponent(String string, Component comp)
    {
        if (NORTH_EAST.equals(string))
        {
            cNE = comp;
        } else if (NORTH_WEST.equals(string))
        {
            cNW = comp;
        }
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        if (comp == cNE)
        {
            cNE = null;
        } else if (comp == cNW)
        {
            cNW = null;
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        // Need a value, but actually won't be used because RpViewer subclasses override getPreferredSize().
        return minimumLayoutSize(parent);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        int w = 2 * PADDING, h = 2 * PADDING;
        if (cNW != null)
        {
            w += cNW.getPreferredSize().width;
            h += cNW.getPreferredSize().height;
        }
        if (cNE != null)
        {
            w += cNE.getPreferredSize().width;
            h += cNE.getPreferredSize().height;
        }
        return new Dimension(w, h);
    }

}

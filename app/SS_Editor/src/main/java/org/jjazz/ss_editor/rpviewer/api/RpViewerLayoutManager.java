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
package org.jjazz.ss_editor.rpviewer.api;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A LayoutManager to arrange children components in the top left or top right corners, with a margin.
 * <p>
 * If more than 1 component in a given corner, put them in line.
 */
public class RpViewerLayoutManager implements LayoutManager
{

    public static final String NORTH_EAST = "NE";
    public static final String NORTH_WEST = "NW";
    private static final int PADDING = 1;
    private static final int COMPONENTS_H_GAP = 3;
    private final List<Component> componentsNW, componentsNE;
    private static final Logger LOGGER = Logger.getLogger(RpViewerLayoutManager.class.getSimpleName());

    public RpViewerLayoutManager()
    {
        this.componentsNW = new ArrayList<>();
        this.componentsNE = new ArrayList<>();

    }


    @Override
    public void layoutContainer(Container container)
    {
        Insets in = container.getInsets();
        int y = in.top + PADDING;

        int x = in.left + PADDING;
        for (var comp : componentsNW)
        {
            comp.setSize(comp.getPreferredSize());
            comp.setLocation(x, y);
            x += comp.getWidth() + COMPONENTS_H_GAP;
        }

        int xRight = container.getWidth() - in.right - PADDING;
        for (var comp : componentsNE)
        {
            comp.setSize(comp.getPreferredSize());
            int xLeft = xRight - comp.getWidth() - 1;
            comp.setLocation(xLeft, y);
            xRight -= comp.getWidth() + COMPONENTS_H_GAP;
        }
    }

    @Override
    public void addLayoutComponent(String string, Component comp)
    {
        if (NORTH_EAST.equals(string))
        {
            componentsNE.add(comp);
        } else if (NORTH_WEST.equals(string))
        {
            componentsNW.add(comp);
        } else
        {
            throw new IllegalArgumentException("string=" + string + " comp=" + comp);
        }
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        componentsNE.remove(comp);
        componentsNW.remove(comp);
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
        int w = 2 * PADDING;
        int maxCompHeight = 0;

        for (var comp : componentsNE)
        {
            w += comp.getPreferredSize().width + (comp != componentsNE.getLast() ? COMPONENTS_H_GAP : 0);
            maxCompHeight = Math.max(comp.getHeight(), maxCompHeight);
        }

        for (var comp : componentsNW)
        {
            w += comp.getPreferredSize().width + (comp != componentsNW.getLast() ? COMPONENTS_H_GAP : 0);
            maxCompHeight = Math.max(comp.getHeight(), maxCompHeight);
        }

        int h = 2 * PADDING + maxCompHeight;
        return new Dimension(w, h);
    }

}

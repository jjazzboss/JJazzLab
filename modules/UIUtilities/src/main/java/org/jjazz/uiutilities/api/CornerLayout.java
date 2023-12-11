/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.uiutilities.api;

import com.google.common.base.Preconditions;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;


/**
 * Simple layout which puts components at their preferred size in one of the 4 corners.
 * <p>
 * Components must be added to container using Container.add(Component comp, Object constraints), constraints being NORTH_WEST etc.
 */
public class CornerLayout implements LayoutManager2
{

    public static final String NORTH_WEST = "NW";
    public static final String SOUTH_WEST = "SW";
    public static final String NORTH_EAST = "NE";
    public static final String SOUTH_EAST = "SE";
    private final int padding;
    private Component compNW, compSW, compNE, compSE;

    /**
     * A layout using 0 padding.
     */
    public CornerLayout()
    {
        this(0);
    }

    /**
     * A layout with a custom padding.
     *
     * @param padding The space between container's inside border and the child components.
     */
    public CornerLayout(int padding)
    {
        this.padding = padding;
    }
    
    public int getPadding()
    {
        return this.padding;
    }

    @Override
    public void layoutContainer(Container container)
    {
        Insets in = container.getInsets();
        int xLeft = in.left + padding;
        int yTop = in.top + padding;

        if (compNW != null)
        {
            compNW.setSize(compNW.getPreferredSize());
            compNW.setLocation(xLeft, yTop);
        }

        if (compNE != null)
        {
            compNE.setSize(compNE.getPreferredSize());
            int x = container.getWidth() - in.right - padding - compNE.getWidth();
            compNE.setLocation(x, yTop);
        }

        if (compSW != null)
        {
            compSW.setSize(compSW.getPreferredSize());
            int y = container.getHeight() - in.bottom - padding - compSW.getHeight();
            compSW.setLocation(xLeft, y);
        }

        if (compSE != null)
        {
            compSE.setSize(compSE.getPreferredSize());
            int x = container.getWidth() - in.right - padding - compSE.getWidth();
            int y = container.getHeight() - in.bottom - padding - compSE.getHeight();
            compSE.setLocation(x, y);
        }
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints)
    {
        Preconditions.checkArgument(constraints instanceof String, "constraints = %s", constraints);
        String s = (String) constraints;
        switch (s)
        {
            case NORTH_EAST -> compNE = comp;
            case SOUTH_EAST -> compSE = comp;
            case SOUTH_WEST -> compSW = comp;
            case NORTH_WEST -> compNW = comp;
            default -> throw new IllegalArgumentException("Constraints=" + constraints + "is not one of the 4 authorized constants such as CornerLayout.NORTH_WEST.");
        }
    }

    @Override
    public void addLayoutComponent(String s, Component comp)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        if (comp == compNW)
        {
            compNW = null;
        } else if (comp == compSW)
        {
            compSW = null;
        } else if (comp == compSW)
        {
            compSW = null;
        } else if (comp == compSE)
        {
            compSE = null;
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    @Override
    public Dimension maximumLayoutSize(Container target)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public float getLayoutAlignmentX(Container target)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public float getLayoutAlignmentY(Container target)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void invalidateLayout(Container target)
    {
        // Nothing
    }

}

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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * Simple layout which puts components at their preferred size in one of the 4 corners.
 * <p>
 * Components must be added to container using Container.add(Component comp, Object constraints), constraints being NORTH_WEST etc.
 */
public class CornerLayout implements LayoutManager
{

    public static final String NORTH_WEST = "NW";
    public static final String SOUTH_WEST = "SW";
    public static final String NORTH_EAST = "NE";
    public static final String SOUTH_EAST = "SE";
    private final int padding;
    private List<Component> compNW, compSW, compNE, compSE;
    private List<Component> defaultCompList;
    private String defaultCorner;
    private static final Logger LOGGER = Logger.getLogger(CornerLayout.class.getSimpleName());

    /**
     * A layout using 0 padding and NW as default corner.
     */
    public CornerLayout()
    {
        this(0, NORTH_WEST);
    }

    /**
     * Create the layout.
     *
     * @param padding       The space between container's inside border and the child components.
     * @param defaultCorner Default corner to be used when no constraint provided to the addLayoutComponent() method
     * @see #addLayoutComponent(java.lang.String, java.awt.Component)
     */
    public CornerLayout(int padding, String defaultCorner)
    {
        this.padding = padding;
        compNW = new ArrayList<>();
        compSW = new ArrayList<>();
        compNE = new ArrayList<>();
        compSE = new ArrayList<>();
        this.defaultCorner = defaultCorner;
        defaultCompList = switch (defaultCorner)
        {
            case NORTH_EAST ->
                compNE;
            case SOUTH_EAST ->
                compSE;
            case SOUTH_WEST ->
                compSW;
            case NORTH_WEST ->
                compNW;
            default -> throw new IllegalArgumentException(
                        "defaultCorner=" + defaultCorner + "is not one of the 4 authorized values such as CornerLayout.NORTH_WEST.");
        };
    }

    public String getDefaultCorner()
    {
        return defaultCorner;
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

        // If a child component has no corner assigned, assign the default one
        var children = container.getComponents();
        for (var c : children)
        {
            if (!compNW.contains(c) && !compSW.contains(c) && !compNE.contains(c) && !compSE.contains(c))
            {
                defaultCompList.add(c);
            }
        }

        compNW.forEach(c -> 
        {
            c.setSize(c.getPreferredSize());
            c.setLocation(xLeft, yTop);
        });

        compNE.forEach(c -> 
        {
            c.setSize(c.getPreferredSize());
            int x = container.getWidth() - in.right - padding - c.getWidth();
            c.setLocation(x, yTop);
        });

        compSE.forEach(c -> 
        {
            c.setSize(c.getPreferredSize());
            int y = container.getHeight() - in.bottom - padding - c.getHeight();
            c.setLocation(xLeft, y);
        });

        compSW.forEach(c -> 
        {
            c.setSize(c.getPreferredSize());
            int x = container.getWidth() - in.right - padding - c.getWidth();
            int y = container.getHeight() - in.bottom - padding - c.getHeight();
            c.setLocation(x, y);
        });
    }

    @Override
    public void addLayoutComponent(String corner, Component comp)
    {
        if (corner == null)
        {
            corner = defaultCorner;
        }
        switch (corner)
        {
            case NORTH_EAST -> compNE.add(comp);
            case SOUTH_EAST -> compSE.add(comp);
            case SOUTH_WEST -> compSW.add(comp);
            case NORTH_WEST -> compNW.add(comp);
            default -> throw new IllegalArgumentException(
                        "corner=" + corner + "is not one of the 4 authorized values such as CornerLayout.NORTH_WEST.");
        }
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        compNW.remove(comp);
        compNE.remove(comp);
        compSW.remove(comp);
        compSE.remove(comp);
    }

    /**
     * Compute a size so that the biggest component of each corner is visible.
     *
     * @param parent
     * @return
     */
    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        int nwWidth = 0, nwHeight = 0, neWidth = 0, neHeight = 0;
        int swWidth = 0, swHeight = 0, seWidth = 0, seHeight = 0;

        for (var c : compNW)
        {
            var pd = c.getPreferredSize();
            nwWidth = Math.max(pd.width, nwWidth);
            nwHeight = Math.max(pd.height, nwHeight);
        }
        for (var c : compNE)
        {
            var pd = c.getPreferredSize();
            neWidth = Math.max(pd.width, neWidth);
            neHeight = Math.max(pd.height, neHeight);
        }
        for (var c : compSW)
        {
            var pd = c.getPreferredSize();
            swWidth = Math.max(pd.width, swWidth);
            swHeight = Math.max(pd.height, swHeight);
        }
        for (var c : compSE)
        {
            var pd = c.getPreferredSize();
            seWidth = Math.max(pd.width, seWidth);
            seHeight = Math.max(pd.height, seHeight);
        }
        int w = Math.max(nwWidth, swWidth) + Math.max(neWidth, seWidth);
        int h = Math.max(nwHeight, neHeight) + Math.max(swHeight, swHeight);
        return new Dimension(w, h);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        return new Dimension();
    }

}

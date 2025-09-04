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
import java.awt.LayoutManager;

/**
 * Layout objects as a left aligned column.
 * <p>
 * Size of the container is ignored in the layout process.
 */
public class ColumnLeftAlignedLayoutManager implements LayoutManager
{

    public static final int DEFAULT_PADDING = 3;
    private final int padding;

    /**
     * Use DEFAULT_PADDING.
     */
    public ColumnLeftAlignedLayoutManager()
    {
        this(DEFAULT_PADDING);
    }

    /**
     *
     * @param padding Space around each object.
     */
    public ColumnLeftAlignedLayoutManager(int padding)
    {
        Preconditions.checkArgument(padding >= 0, "padding=%s", padding);
        this.padding = padding;
    }

    @Override
    public void layoutContainer(Container parent)
    {
        int y = padding;
        for (var c : parent.getComponents())
        {
            var pd = c.getPreferredSize();
            c.setSize(pd);
            c.setLocation(padding, y);
            y += pd.height + padding;
        }
    }

    @Override
    public void addLayoutComponent(String name, Component comp)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        int maxObjectWidth = 0;
        int h = padding;
        for (var c : parent.getComponents())
        {
            var pd = c.getPreferredSize();
            maxObjectWidth = Math.max(pd.width, maxObjectWidth);
            h += pd.height + padding;
        }
        int w = 2 * padding + maxObjectWidth;
        return new Dimension(w, h);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        return preferredLayoutSize(parent);
    }


}

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
import java.util.Objects;

// =====================================================================================================

// Inner classes
// =====================================================================================================
/**
 * Layout components in a grid with fixed-size components on a fixed nb of columns.
 */
public class FixedCompSizeGridLayout implements LayoutManager
{

    private final Dimension compSize;
    private final int nbCols;

    /**
     *
     * @param nbCols   Number of columns
     * @param compSize Size used to layout all components
     */
    public FixedCompSizeGridLayout(int nbCols, Dimension compSize)
    {
        Objects.requireNonNull(compSize);
        Preconditions.checkArgument(nbCols > 0, "nbCols=%s", nbCols);
        this.nbCols = nbCols;
        this.compSize = compSize;
    }

    @Override
    public void layoutContainer(Container parent)
    {
        int x = 0;
        int y = 0;
        int col = 0;
        for (java.awt.Component c : parent.getComponents())
        {
            c.setSize(compSize.width, compSize.height);
            c.setLocation(x, y);
            col = (col + 1) % nbCols;
            x = col * compSize.width;
            if (col == 0)
            {
                y += compSize.height;
            }
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        int w = nbCols * compSize.width;
        int nbRows = ((parent.getComponentCount() - 1) / nbCols) + 1;
        int h = nbRows * compSize.height;
        return new Dimension(w, h);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        return preferredLayoutSize(parent);
    }

    @Override
    public void addLayoutComponent(String name, Component comp)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        // Nothing
    }
    
}

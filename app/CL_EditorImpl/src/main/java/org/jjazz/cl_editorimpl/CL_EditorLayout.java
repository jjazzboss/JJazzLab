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
package org.jjazz.cl_editorimpl;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A custom LayoutManager for the CL_Editor that arranges components in a fixed number of columns (like GridLayout), and optionally adds
 * extra vertical space above rows that start a new section line.
 * <p>
 * All columns have equal width. All rows have the same base height (determined by the tallest component's preferred height). Rows whose
 * index is listed in the "new line rows" set get an additional {@code newLineExtraHeight} pixels added above them.
 */
public class CL_EditorLayout implements LayoutManager
{

    private int nbColumns;
    private int newLineExtraHeight;
    /**
     * The set of row indices (0-based) that should have extra space added above them.
     */
    private Set<Integer> newLineRows = Collections.emptySet();

    /**
     * Create a layout with the given number of columns and no extra height.
     *
     * @param nbColumns       must be &gt;= 1
     * @param newLineExtraHeight pixels to add above "new line" rows, must be &gt;= 0
     */
    public CL_EditorLayout(int nbColumns, int newLineExtraHeight)
    {
        if (nbColumns < 1)
        {
            throw new IllegalArgumentException("nbColumns=" + nbColumns);
        }
        if (newLineExtraHeight < 0)
        {
            throw new IllegalArgumentException("newLineExtraHeight=" + newLineExtraHeight);
        }
        this.nbColumns = nbColumns;
        this.newLineExtraHeight = newLineExtraHeight;
    }

    public int getNbColumns()
    {
        return nbColumns;
    }

    public void setNbColumns(int nbColumns)
    {
        if (nbColumns < 1)
        {
            throw new IllegalArgumentException("nbColumns=" + nbColumns);
        }
        this.nbColumns = nbColumns;
    }

    public int getNewLineExtraHeight()
    {
        return newLineExtraHeight;
    }

    public void setNewLineExtraHeight(int newLineExtraHeight)
    {
        if (newLineExtraHeight < 0)
        {
            throw new IllegalArgumentException("newLineExtraHeight=" + newLineExtraHeight);
        }
        this.newLineExtraHeight = newLineExtraHeight;
    }

    /**
     * Set the row indices (0-based) that should have extra vertical space added above them.
     *
     * @param rows set of row indices; must not be null
     * @return true if the set of new-line rows changed
     */
    public boolean setNewLineRows(Set<Integer> rows)
    {
        if (rows == null)
        {
            throw new IllegalArgumentException("rows must not be null");
        }
        Set<Integer> newSet = new HashSet<>(rows);
        if (newSet.equals(newLineRows))
        {
            return false;
        }
        newLineRows = newSet;
        return true;
    }

    /**
     * Get the number of rows for the given number of components.
     *
     * @param nbComponents
     * @return
     */
    public int getNbRows(int nbComponents)
    {
        if (nbComponents == 0)
        {
            return 0;
        }
        return (nbComponents + nbColumns - 1) / nbColumns;
    }

    @Override
    public void addLayoutComponent(String name, Component comp)
    {
        // Nothing to do
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        // Nothing to do
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        return computeLayoutSize(parent, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        return computeLayoutSize(parent, false);
    }

    @Override
    public void layoutContainer(Container parent)
    {
        synchronized (parent.getTreeLock())
        {
            Insets insets = parent.getInsets();
            int nbComponents = parent.getComponentCount();
            if (nbComponents == 0)
            {
                return;
            }

            int totalWidth = parent.getWidth() - insets.left - insets.right;
            int colWidth = totalWidth / nbColumns;

            // Compute base row height from components' preferred heights
            int rowHeight = getMaxPreferredHeight(parent, true);

            int nbRows = getNbRows(nbComponents);

            // Layout each component
            int compIndex = 0;
            int y = insets.top;
            for (int row = 0; row < nbRows; row++)
            {
                // Add extra space above this row if it's a "new line" row
                if (newLineRows.contains(row))
                {
                    y += newLineExtraHeight;
                }

                int x = insets.left;
                for (int col = 0; col < nbColumns && compIndex < nbComponents; col++, compIndex++)
                {
                    Component c = parent.getComponent(compIndex);
                    c.setBounds(x, y, colWidth, rowHeight);
                    x += colWidth;
                }
                y += rowHeight;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private methods
    // -----------------------------------------------------------------------

    private Dimension computeLayoutSize(Container parent, boolean preferred)
    {
        synchronized (parent.getTreeLock())
        {
            Insets insets = parent.getInsets();
            int nbComponents = parent.getComponentCount();

            if (nbComponents == 0)
            {
                return new Dimension(insets.left + insets.right, insets.top + insets.bottom);
            }

            int rowHeight = getMaxPreferredHeight(parent, preferred);
            int colWidth = getMaxPreferredWidth(parent, preferred);

            int nbRows = getNbRows(nbComponents);

            // Compute total extra height for new line rows
            int totalExtraHeight = 0;
            for (int row : newLineRows)
            {
                if (row < nbRows)
                {
                    totalExtraHeight += newLineExtraHeight;
                }
            }

            int totalWidth = nbColumns * colWidth + insets.left + insets.right;
            int totalHeight = nbRows * rowHeight + totalExtraHeight + insets.top + insets.bottom;

            return new Dimension(totalWidth, totalHeight);
        }
    }

    private int getMaxPreferredHeight(Container parent, boolean preferred)
    {
        int max = 0;
        for (int i = 0; i < parent.getComponentCount(); i++)
        {
            Component c = parent.getComponent(i);
            int h = preferred ? c.getPreferredSize().height : c.getMinimumSize().height;
            if (h > max)
            {
                max = h;
            }
        }
        return max;
    }

    private int getMaxPreferredWidth(Container parent, boolean preferred)
    {
        int max = 0;
        for (int i = 0; i < parent.getComponentCount(); i++)
        {
            Component c = parent.getComponent(i);
            int w = preferred ? c.getPreferredSize().width : c.getMinimumSize().width;
            if (w > max)
            {
                max = w;
            }
        }
        return max;
    }
}

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
 * A custom LayoutManager for the CL_Editor that arranges BarBox components in a fixed number of columns.
 * <p>
 * All columns have equal width. All rows have the same base height (determined by the tallest component's preferred height).
 * <p>
 * BarBox indices listed in the "new line BarBox indices" set are forced to start on a new row. Additionally,
 * {@code newLineExtraHeight} pixels of vertical space are inserted above each such row (provided it is not the very first row).
 */
public class CL_EditorLayout implements LayoutManager
{

    private int nbColumns;
    private int newLineExtraHeight;
    /**
     * The set of BarBox indices (0-based) that must start on a new row, with extra space above.
     */
    private Set<Integer> newLineBarBoxIndices = Collections.emptySet();

    /**
     * Create a layout with the given number of columns and extra height.
     *
     * @param nbColumns          must be &gt;= 1
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
     * Set the BarBox indices (0-based) that must start on a new row (with extra space above).
     *
     * @param indices set of BarBox indices; must not be null
     * @return true if the set changed
     */
    public boolean setNewLineBarBoxIndices(Set<Integer> indices)
    {
        if (indices == null)
        {
            throw new IllegalArgumentException("indices must not be null");
        }
        Set<Integer> newSet = new HashSet<>(indices);
        if (newSet.equals(newLineBarBoxIndices))
        {
            return false;
        }
        newLineBarBoxIndices = newSet;
        return true;
    }

    /**
     * Get the row index (0-based) of the BarBox at the given index.
     * <p>
     * This accounts for forced new-line breaks from {@code newLineBarBoxIndices}.
     *
     * @param bbIndex the BarBox index
     * @return the 0-based row index
     */
    public int getRowIndex(int bbIndex)
    {
        int col = 0;
        int row = 0;
        for (int i = 0; i < bbIndex; i++)
        {
            if (newLineBarBoxIndices.contains(i) && col != 0)
            {
                row++;
                col = 0;
            }
            col++;
            if (col == nbColumns)
            {
                col = 0;
                row++;
            }
        }
        // Check whether the target BarBox itself forces a new row
        if (newLineBarBoxIndices.contains(bbIndex) && col != 0)
        {
            row++;
        }
        return row;
    }

    /**
     * Get the total number of rows needed to display {@code nbBarBoxes} BarBoxes.
     * <p>
     * This accounts for forced new-line breaks from {@code newLineBarBoxIndices}.
     *
     * @param nbBarBoxes total number of BarBox components
     * @return number of rows (&gt;= 0)
     */
    public int getNbRows(int nbBarBoxes)
    {
        if (nbBarBoxes == 0)
        {
            return 0;
        }
        int col = 0;
        int row = 0;
        for (int i = 0; i < nbBarBoxes; i++)
        {
            if (newLineBarBoxIndices.contains(i) && col != 0)
            {
                row++;
                col = 0;
            }
            col++;
            if (col == nbColumns)
            {
                col = 0;
                row++;
            }
        }
        // If we finished in the middle of a row, count that partial row
        if (col > 0)
        {
            row++;
        }
        return row;
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
            int rowHeight = getMaxPreferredHeight(parent, true);

            int col = 0;
            int y = insets.top;

            for (int i = 0; i < nbComponents; i++)
            {
                // If this BarBox must start on a new line...
                if (newLineBarBoxIndices.contains(i))
                {
                    if (col != 0)
                    {
                        // Wrap to the next row
                        y += rowHeight;
                        col = 0;
                    }
                    // Add extra space above this row (not at the very top of the editor)
                    if (y > insets.top)
                    {
                        y += newLineExtraHeight;
                    }
                }

                int x = insets.left + col * colWidth;
                parent.getComponent(i).setBounds(x, y, colWidth, rowHeight);

                col++;
                if (col == nbColumns)
                {
                    col = 0;
                    y += rowHeight;
                }
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

            // Count extra height: one extra block per new-line BarBox that is not the very first component
            int totalExtraHeight = 0;
            for (int bbIndex : newLineBarBoxIndices)
            {
                if (bbIndex > 0 && bbIndex < nbComponents)
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

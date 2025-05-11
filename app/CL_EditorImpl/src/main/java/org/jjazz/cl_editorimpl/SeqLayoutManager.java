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

import org.jjazz.cl_editor.barrenderer.api.BarRenderer;
import java.awt.*;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;

/**
 * This layoutmanager simply puts the children one after the other at their preferredSize.
 * 
 * If not enough width, the manager makes sure each child is at least partly visible.
 */
public class SeqLayoutManager implements LayoutManager
{
    private final static int ER_GAP = 2;

    @Override
    public void layoutContainer(Container parent)
    {
        if (!(parent instanceof BarRenderer))
        {
            throw new IllegalArgumentException("parent=" + parent);   
        }
        BarRenderer br = (BarRenderer) parent;
        var r = br.getDrawingArea();
        int x = r.x;
        for (ItemRenderer ir : br.getItemRenderers())
        {
            ir.setSize(ir.getPreferredSize());
            float eventHeight = ir.getHeight();
            int y = r.y + (int) ((r.height - eventHeight) / 2);
            ir.setLocation(x, y);           
            x += ir.getWidth() + ER_GAP;
            if (x >= r.x + r.width - 3)
            {
                x -= ir.getWidth() - ER_GAP + 5;
            }
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLayoutComponent(String name, Component comp)
    {
        // Nothing
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        // Nothing
    }

}

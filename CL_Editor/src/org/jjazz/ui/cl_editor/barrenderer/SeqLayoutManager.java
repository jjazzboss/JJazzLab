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
package org.jjazz.ui.cl_editor.barrenderer;

import org.jjazz.ui.cl_editor.barrenderer.api.BarRenderer;
import java.awt.*;
import org.jjazz.ui.itemrenderer.api.ItemRenderer;

/**
 * This layoutmanager simply puts the children one after the other at their preferredSize.
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
        int barHeight = br.getDrawingArea().height;
        int barLeft = br.getDrawingArea().x;
        int barTop = br.getDrawingArea().y;
        int x = barLeft;
        for (ItemRenderer ir : br.getItemRenderers())
        {
            ir.setSize(ir.getPreferredSize());
            float eventHeight = ir.getHeight();
            int y = barTop + (int) ((barHeight - eventHeight) / 2);
            ir.setLocation(x, y);
            x += ir.getWidth() + ER_GAP;
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

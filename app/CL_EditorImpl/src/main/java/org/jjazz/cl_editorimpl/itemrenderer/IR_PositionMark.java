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
package org.jjazz.cl_editorimpl.itemrenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;

public class IR_PositionMark extends ItemRenderer
{

    private static final Dimension DEFAULT_SIZE = new Dimension(11, 6);
    private static final Color DEFAULT_COLOR = Color.RED;
    private int zoomFactor = 50;

    public IR_PositionMark(ChordLeadSheetItem<?> item)
    {
        super(item, IR_Type.PositionMark);
        setForeground(DEFAULT_COLOR);
        setPreferredSize(DEFAULT_SIZE);
    }

    @Override
    protected void modelChanged()
    {
    }

    @Override
    protected void modelMoved()
    {
    }

    /**
     * Zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    @Override
    public void setZoomFactor(int factor)
    {
        zoomFactor = factor;

    }

    @Override
    public int getZoomFactor()
    {
        return zoomFactor;
    }

    /**
     * Render the event.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(getForeground());
        int[] x =
        {
            0, getWidth() / 2, getWidth()
        };
        int[] y =
        {
            0, getHeight(), 0
        };
        g2.fillPolygon(x, y, 3);
    }
}

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
package org.jjazz.ss_editorimpl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import org.jjazz.flatcomponents.api.CrossShape;

/**
 * The component used to show the insertion mark in copy or move mode.
 *
 * @author Jerome
 */
public class InsertionSptMark extends JPanel
{

    private boolean copyMode;
    private static CrossShape crossShape = new CrossShape(3, 2);

    public InsertionSptMark()
    {
        super();
        setOpaque(false);
        copyMode = false;
        setPreferredSize(new Dimension(20, 30));
    }

    public void setCopyMode(boolean copyMode)
    {
        if (copyMode != this.copyMode)
        {
            this.copyMode = copyMode;
            repaint();
        }
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Insets in = getInsets();

        // Draw an inversed triangle
        g.setColor(Color.LIGHT_GRAY);
        final int HGAP = 3;
        final int VGAP = 2;
        int[] x =
        {
            in.left + HGAP, getWidth() / 2, getWidth() - in.right - HGAP
        };
        int[] y =
        {
            in.top + VGAP, in.top + 10, in.top + VGAP
        };
        g.fillPolygon(x, y, 3);

        if (!copyMode)
        {
            return;
        }

        // Add a '+' sign
        int size = crossShape.getBounds().width + 1;
        Graphics2D g2 = (Graphics2D) g.create(Math.max(getWidth() - in.left - in.right - size - 1, in.left), in.top + 2 * VGAP, size, size);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1f));
        g2.setPaint(Color.WHITE);
        g2.fill(crossShape);
        g2.setPaint(Color.GRAY.darker());
        g2.draw(crossShape);
        g2.dispose();
    }

    /**
     * Overridden to enable component to be aligned on their baseline (for example by FlowLayout).
     *
     * @param width
     * @param height
     * @return 0 Means these components are aligned on the top.
     */
    @Override
    public int getBaseline(int width, int height)
    {
        return 0;
    }

    /**
     * Overridden to be consistent with getBaseline override
     *
     * @return
     */
    @Override
    public Component.BaselineResizeBehavior getBaselineResizeBehavior()
    {
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }
}

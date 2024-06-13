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
package org.jjazz.ss_editorimpl.sptviewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;
import org.jjazz.utilities.api.ResUtil;

/**
 * The component used for the "multi-selection" of song parts in the SptViewer.
 * <p>
 * Component is painted only if opaque.
 */
public class MultiSelectBar extends JComponent implements MouseListener
{

    private static final int V_BORDER = 2;
    private static final int H_BORDER = 0;
    private int lineThickness;
    private Color lineColor;
    private final Border borderDefault;
    private final Border borderEntered;
    private boolean on;
    private boolean multiSelectFirst;

    public MultiSelectBar()
    {
        borderDefault = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        borderEntered = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
        setBorder(borderDefault);
        setLineThickness(1);
        setLineColor(Color.GRAY);
        addMouseListener(this);
    }

    @Override
    public Dimension getPreferredSize()
    {
        Insets in = this.getInsets();
        Dimension d = super.getPreferredSize();
        d.height = in.top + in.bottom + getLineThickness() + 2 * V_BORDER;
        return d;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);    // Honor the opaque property
        if (!isOn())
        {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getLineColor());
        Insets in = this.getInsets();
        int x = in.left + H_BORDER;
        int y = in.top + V_BORDER;
        int w = getWidth() - in.left - in.right - 2 * H_BORDER;
        int h = getLineThickness();
        g2.fillRect(x, y, w, h);
        if (multiSelectFirst)
        {
            // Add some triangle effect on the left
            g2.drawLine(x, y - 2, x, y + h - 1 + 2);
            g2.drawLine(x + 1, y - 1, x + 1, y + h - 1 + 1);
        }
    }

    public boolean isOn()
    {
        return on;
    }

    /**
     *
     * @param on If true make the component "active"
     */
    public void setOn(boolean on)
    {
        this.on = on;
        String s = on ? ResUtil.getString(getClass(), "CTL_MultiSelectBarToolTip") : null;
        setToolTipText(s);
        repaint();
    }

    public void setMultiSelectFirst(boolean b)
    {
        this.multiSelectFirst = b;
        repaint();
    }

    public int getLineThickness()
    {
        return lineThickness;
    }

    public final void setLineThickness(int lineThickness)
    {
        this.lineThickness = lineThickness;
    }

    public Color getLineColor()
    {
        return lineColor;
    }

    public final void setLineColor(Color lineColor)
    {
        this.lineColor = lineColor;
    }

    // ================================================================================
    // MouseListener interface
    // ================================================================================
    @Override
    public void mouseClicked(java.awt.event.MouseEvent evt)
    {
        // Nothing
    }

    @Override
    public void mouseExited(java.awt.event.MouseEvent evt)
    {
        if (isEnabled() && isOn())
        {
            setBorder(borderDefault);
        }
    }

    @Override
    public void mouseEntered(java.awt.event.MouseEvent evt)
    {
        if (isEnabled() && isOn())
        {
            setBorder(borderEntered);
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
//        if (isEnabled())
//        {
//            setBorder(borderPressed);
//            fireChanged(e);
//        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
//        if (isEnabled() && getBorder() == borderPressed)
//        {
//            setBorder(borderEntered);
//        }
    }

}

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
package org.jjazz.ui.flatcomponents;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * A rounded JPanel.
 */
public class RoundedPanel extends JPanel
{

    /**
     * Horizontal and vertical arc diameter
     */
    private int arcDiameter;
    private int thickness;
    private boolean showBorder;

    /**
     * Create a RoundedPanel with a 10 point arc diameter and no border drawn.
     */
    public RoundedPanel()
    {
        this(10);
    }

    public RoundedPanel(int arc)
    {
        this(arc, 0);
    }

    public RoundedPanel(int arc, int thickness)
    {
        setOpaque(false);
        setArcDiameter(arc);
        setThickness(thickness);
    }

    /**
     * @return the arcDiameter
     */
    public int getArcDiameter()
    {
        return arcDiameter;
    }

    @Override
    public Insets getInsets()
    {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    /**
     * @param arcDiameter the arcDiameter to set
     */
    public void setArcDiameter(int arcDiameter)
    {
        if (arcDiameter < 0 || arcDiameter > 2000)
        {
            throw new IllegalArgumentException("arcDiameter=" + arcDiameter);   //NOI18N
        }
        this.arcDiameter = arcDiameter;
        repaint();
    }

    /**
     * @return the thickness
     */
    public int getThickness()
    {
        return thickness;
    }

    /**
     * @param thickness the thickness to set
     */
    public void setThickness(int thickness)
    {
        if (thickness < 0 || thickness > 1000)
        {
            throw new IllegalArgumentException("thickness=" + thickness);   //NOI18N
        }
        this.thickness = thickness;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        int width = getWidth();
        int height = getHeight();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, width - 1, height - 1, arcDiameter, arcDiameter);
        if (showBorder && thickness > 0)
        {
            g2.setColor(getForeground());
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(0, 0, width - 1, height - 1, arcDiameter, arcDiameter);
        }
    }

    /**
     * @return the showBorder
     */
    public boolean isShowBorder()
    {
        return showBorder;
    }

    /**
     * @param showBorder the showBorder to set
     */
    public void setShowBorder(boolean showBorder)
    {
        this.showBorder = showBorder;
        repaint();
    }

}

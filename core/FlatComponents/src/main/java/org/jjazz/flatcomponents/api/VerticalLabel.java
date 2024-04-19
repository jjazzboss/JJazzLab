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
package org.jjazz.flatcomponents.api;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;

/**
 * Display a text vertically (text is rotated on the left).
 */
public class VerticalLabel extends JComponent implements PropertyChangeListener
{

    static private final double NINETY_DEGREES = Math.toRadians(90.0);
    static private final int BUFFER_SPACE = 5;
    static private final int POSITION_CENTER = 0;
    static private final int POSITION_BOTTOM_LEFT = 1;
    private int position;
    private String label;
    private int fWidth, fHeight, fCharHeight, fDescent; // Cached for speed

    /**
     * Create a vertical label with "Text" label and center position.
     */
    public VerticalLabel()
    {
        this("Text");
    }

    /**
     * Create a vertical label with center position.
     */
    public VerticalLabel(String label)
    {
        this(label, POSITION_CENTER);
    }

    /**
     *
     * @param label
     * @param position
     */
    public VerticalLabel(String label, int position)
    {
        setFont(new Font("Arial", Font.PLAIN, 10));
        setForeground(Color.BLACK);
        setSize(20, 50);
        this.position = position;
        this.label = label;
        calcDimensions();
        addPropertyChangeListener(this);
    }

    /**
     * sets the label to the given string, and invalidating the layout if the size changes
     *
     * @param label
     */
    public void setLabel(String label)
    {
        this.label = label;
        recalcDimensions();
    }

    public String getLabel()
    {
        return label;
    }

    /**
     * Checks for changes to the font on the fComponent so that it can invalidate the layout if the size changes
     *
     * @param e
     */
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        String prop = e.getPropertyName();
        if ("font".equals(prop))
        {
            recalcDimensions();
        }
    }

    /**
     * Calculates the dimensions. If they've changed, invalidates the component
     */
    void recalcDimensions()
    {
        int wOld = getPreferredSize().width;
        int hOld = getPreferredSize().height;
        calcDimensions();
        if (wOld != getPreferredSize().width || hOld != getPreferredSize().height)
        {
            invalidate();
        }
    }

    void calcDimensions()
    {
        FontMetrics fm = getFontMetrics(getFont());
        fCharHeight = fm.getAscent() + fm.getDescent();
        fDescent = fm.getDescent();
        // if rotated, width is the height of the string
        fWidth = fCharHeight;
        // and height is the width, plus some buffer space 
        fHeight = fm.stringWidth(label) + 2 * BUFFER_SPACE;

    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); // Honor the opaque property

        Graphics2D g2 = (Graphics2D) g.create();   // Duplicate our context because we will translate/rotate it
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(getFont());
        g2.setColor(getForeground());

        Insets in = getInsets();
        int w = getWidth();
        int h = getHeight();
        double x, y;

        if (getPosition() == VerticalLabel.POSITION_BOTTOM_LEFT)
        {
            x = in.left;
            y = h - in.bottom - fHeight - 1;

        } else
        {
            // Center text
            x = (w - in.left - in.right) / 2 - fWidth / 2;
            y = (h - in.bottom - in.top) / 2 - fHeight / 2;
        }

        g2.translate(x + fWidth, y + fHeight);
        g2.rotate(-NINETY_DEGREES);
        g2.drawString(label, BUFFER_SPACE, -fDescent);
        g2.rotate(NINETY_DEGREES);
        g2.translate(-(x + fWidth), -(y + fHeight));
    }

    @Override
    public Dimension getPreferredSize()
    {
        Insets in = getInsets();
        return new Dimension(fWidth + in.left + in.right, fHeight + in.top + in.bottom);
    }

    /**
     * @return the position
     */
    public int getPosition()
    {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(int position)
    {
        this.position = position;
    }

}

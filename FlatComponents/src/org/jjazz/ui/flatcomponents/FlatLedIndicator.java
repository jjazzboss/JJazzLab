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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;
import javax.swing.Timer;
import org.jjazz.ui.utilities.HSLColor;

/**
 * A simple activity indicator.
 * <p>
 * Use showActivity() to indicate activity. The more often it is used, the brightest the color.
 * <p>
 * Foreground color is used to draw the contour of the indicator, colorFill to fill the circle.<p>
 * Luminance of the fill color is modified with EventReceivedLuminanceStep (brighter) when an event is received.<br>
 * Luminance of the fill color is modified with OnePeriodLuminanceStep (whiter) every 50ms.<br>
 */
public class FlatLedIndicator extends JComponent implements ActionListener, MouseListener
{

    /**
     * The diameter of the indicator.
     */
    private int diameter;
    /**
     * The fill color. Luminance will be adjusted depending on the received events.
     */
    private Color colorFill;
    /**
     * 0-100. Used when no events (whiter color).
     */
    private int luminanceNoEvent;
    /**
     * 0-100. Used when many events are received (brighter color).
     */
    private int luminanceMaxEvents;
    /**
     * Decay time in milliseconds
     */
    private final int decayTime;
    private final Timer timer;
    private int luminance;
    private int luminanceStepEventReceived;
    private int luminanceStepOnePeriod;
    private HSLColor hslFillColor;

    public FlatLedIndicator()
    {
        setLuminanceNoEvent(100);
        setLuminanceMaxEvents(50);
        setColorFill(Color.RED);
        setLuminanceStepOnePeriod(5);
        setLuminanceStepEventReceived(-20);
        setDiameter(11);

        luminance = getLuminanceNoEvent();
        decayTime = 75;         // ms
        timer = new Timer(decayTime, this);

        // addMouseListener(this);     // For test only
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); // Honor the opaque property
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Insets in = getInsets();
        double width = getWidth() - in.left - in.right;
        double height = getHeight() - in.top - in.bottom;
        double xCenter = in.left + width / 2;
        double yCenter = in.top + height / 2;
        double radius = diameter / 2d;
        int boundedLuminance = Math.min(luminance, getLuminanceNoEvent());
        boundedLuminance = Math.max(boundedLuminance, getLuminanceMaxEvents());
        g2.setColor(hslFillColor.adjustLuminance(boundedLuminance));
        g2.fill(new Ellipse2D.Double(xCenter - radius, yCenter - radius, diameter, diameter));
        g2.setColor(getForeground());
        g2.setStroke(new BasicStroke(1));
        g2.draw(new Ellipse2D.Double(xCenter - radius, yCenter - radius, diameter, diameter));
    }

    /**
     * Send an "activity event", luminance goes one step down.
     */
    public synchronized void showActivity()
    {
        luminance += getLuminanceStepEventReceived();
        luminance = Math.max(luminance, getLuminanceMaxEvents());
        timer.start();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (luminance < getLuminanceNoEvent())
        {
            luminance += getLuminanceStepOnePeriod();
            luminance = Math.min(getLuminanceNoEvent(), luminance);
        } else
        {
            timer.stop();
        }
        repaint();
    }

    public int getLuminanceMaxEvents()
    {
        return luminanceMaxEvents;
    }

    /**
     * @param luminanceMaxEvents The minimum allowed luminance percentage (the lower the value the darker the color)
     */
    public final void setLuminanceMaxEvents(int luminanceMaxEvents)
    {
        this.luminanceMaxEvents = luminanceMaxEvents;
    }

    public int getLuminanceNoEvent()
    {
        return luminanceNoEvent;
    }

    /**
     * @param luminanceNoEvent The maximum allowed luminance percentage (the higher the value the whiter the color)
     */
    public final void setLuminanceNoEvent(int luminanceNoEvent)
    {
        this.luminanceNoEvent = luminanceNoEvent;
    }

    /**
     * @return the colorFill
     */
    public Color getColorFill()
    {
        return colorFill;
    }

    /**
     * @param colorFill the colorFill to set
     */
    public final void setColorFill(Color colorFill)
    {
        this.colorFill = colorFill;
        hslFillColor = new HSLColor(this.colorFill);
        repaint();
    }

    /**
     * @return the EventReceivedLuminanceStep
     */
    public int getLuminanceStepEventReceived()
    {
        return luminanceStepEventReceived;
    }

    /**
     * @param step the EventReceivedLuminanceStep as a percentage change.
     */
    public final void setLuminanceStepEventReceived(int step)
    {
        this.luminanceStepEventReceived = step;
    }

    /**
     * @return the OnePeriodLuminanceStep
     */
    public int getLuminanceStepOnePeriod()
    {
        return luminanceStepOnePeriod;
    }

    /**
     * @param OnePeriodLuminanceStep the OnePeriodLuminanceStep as a percentage change.
     */
    public final void setLuminanceStepOnePeriod(int OnePeriodLuminanceStep)
    {
        this.luminanceStepOnePeriod = OnePeriodLuminanceStep;
    }

    /**
     * @return the diameter
     */
    public int getDiameter()
    {
        return diameter;
    }

    /**
     * @param diameter the diameter to set
     */
    public final void setDiameter(int diameter)
    {
        this.diameter = diameter;
        repaint();
        revalidate();
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        //
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        showActivity();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        //
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        //
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        //
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(diameter + 4, diameter + 4);
    }

    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

}

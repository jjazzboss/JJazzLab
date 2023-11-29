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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * A simple activity indicator.
 * <p>
 * Call showActivity() to indicate activity on the led: led color goes from colorMin (minimum activity) to colorMax.<p>
 * Color transition is done by first filling the led with colorMin, then overwriting it with colorMax with the transparency
 * (alpha) channel set. When showActivity() is called, colorMax alpha channel is increased by getAlphaStepActivity(). Every
 * DECAY_TIME_MS, colorMax alpha channel is decreased by getAlphaStepDecay().
 * <p>
 */
public class FlatLedIndicator extends JComponent implements MouseListener
{

    public final static int DECAY_TIME_MS = 75;
    private int diameter;
    private Color colorMin, colorMax;
    private int alphaStepDecay, alphaStepActivity;

    private final Timer timer;
    private int alphaColorMax;
    private static final Logger LOGGER = Logger.getLogger(FlatLedIndicator.class.getSimpleName());

    public FlatLedIndicator()
    {
        alphaColorMax = 0;  // Fully transparent
        setColorMax(Color.RED);
        setColorMin(Color.LIGHT_GRAY);
        setAlphaStepActivity(40);
        setAlphaStepDecay(20);
        setDiameter(11);

        timer = new Timer(DECAY_TIME_MS, evt -> timerElapsed());

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


        Shape shape = new Ellipse2D.Double(xCenter - radius, yCenter - radius, diameter, diameter);
        g2.setColor(colorMin);
        g2.fill(shape);
        g2.setColor(new Color(colorMax.getRed(), colorMax.getGreen(), colorMax.getBlue(), alphaColorMax));
        // If alpha==0, colorMax is transparent and we see colorMin
        g2.fill(shape);     
    }

    /**
     * Send an "activity event".
     */
    public synchronized void showActivity()
    {
        alphaColorMax += getAlphaStepActivity();
        alphaColorMax = Math.min(alphaColorMax, 255);
        timer.start();
        repaint();
    }

    /**
     * The color to show maximal activity.
     *
     * @return the colorMax
     */
    public Color getColorMax()
    {
        return colorMax;
    }

    /**
     * @param colorMax the colorMax to set
     */
    public final void setColorMax(Color colorMax)
    {
        this.colorMax = colorMax;
        repaint();
    }

    /**
     * The color to show minimal activity.
     *
     * @return the colorMin
     */
    public Color getColorMin()
    {
        return colorMin;
    }

    /**
     * @param colorMin the colorMin to set
     */
    public final void setColorMin(Color colorMin)
    {
        this.colorMin = colorMin;
        repaint();
    }

    /**
     *
     * @return the alphaStepDecay
     */
    public int getAlphaStepDecay()
    {
        return alphaStepDecay;
    }

    /**
     * Every DECAY_TIME_MS, the alpha channel of colorMax is decreased by alphaStepDecay.
     *
     * @param alphaStepDecay the alphaStepDecay to set
     */
    public final void setAlphaStepDecay(int alphaStepDecay)
    {
        this.alphaStepDecay = alphaStepDecay;
    }

    /**
     * @return the alphaStepActivity
     */
    public int getAlphaStepActivity()
    {
        return alphaStepActivity;
    }

    /**
     * Each time showActivity() is called, the alpha channel of colorMax is increased by alphaStepActivity.
     *
     * @param alphaStepActivity the alphaStepActivity to set
     */
    public final void setAlphaStepActivity(int alphaStepActivity)
    {
        this.alphaStepActivity = alphaStepActivity;
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

    // =================================================================================
    // Private methods
    // =================================================================================
    private void timerElapsed()
    {
        if (alphaColorMax > 0)
        {
            alphaColorMax -= getAlphaStepDecay();
            alphaColorMax = Math.max(alphaColorMax, 0);
        } else
        {
            timer.stop();
        }
        repaint();
    }

}

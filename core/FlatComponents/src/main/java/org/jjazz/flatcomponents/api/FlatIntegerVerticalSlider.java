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
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * A vertical flat slider.
 * <p>
 * Whatever the actual component size, the drawing is done at preferred size.
 */
public class FlatIntegerVerticalSlider extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener
{

    public static final String PROP_VALUE = "PropValue";    

    // UI variables
    private int padding = 5;
    private int buttonHeight = 30;
    private Color valueLineColor = new Color(185, 69, 24);
    private Color buttonColor = new Color(176, 176, 176);
    private Color buttonColorDisabled = new Color(69, 69, 69);
    private Color buttonContourColor = new Color(48, 48, 48);
    private Color grooveUpperColor = new Color(109, 109, 109);
    private Color grooveLowerColor = new Color(64, 64, 64);

    // Model
    private int minValue = 0;
    private int maxValue = 127;
    private int value = 64;
    private String tooltipLabel;

    // State variables    
    private MouseEvent lastMouseEvent;
    private int yDragStart = Integer.MIN_VALUE;
    private int yValueDragStart;

    private static final Logger LOGGER = Logger.getLogger(FlatIntegerVerticalSlider.class.getSimpleName());

    public FlatIntegerVerticalSlider()
    {
        addMouseListener(this);
        addMouseMotionListener(this);


        // Use mouse wheel only if enabled
        FlatComponentsGlobalSettings.getInstance().installChangeValueWithMouseWheelSupport(this, this);

        BorderManager.getInstance().register(this, false, false, true);
    }

    @Override
    public Dimension getPreferredSize()
    {
        int w = (int) Math.ceil(calcButtonWidth() + calcDoubleMarkLength() / 2 + 2 * padding);
        int h = (int) Math.ceil(calcHeight() + 2 * padding);
        return new Dimension(w, h);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); // Honor the opaque property

        final int arc = 6;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        Insets in = getInsets();
        int w = getWidth();
        int h = getHeight();
        var pd = getPreferredSize();
        double x0 = (w - pd.getWidth()) / 2;
        double y0 = (h - pd.getHeight()) / 2;
        double wDoubleMark = calcDoubleMarkLength();
        double xButton = x0 + padding + wDoubleMark / 2;
        double wButton = calcButtonWidth();
        double wGroove = calcGrooveWidth();
        double hGroove = calcHeight();
        double hValue = hGroove - buttonHeight;
        double xGroove = xButton + wButton / 2 - wGroove / 2;
        double yGroove = y0 + padding;
        double yValue0 = yGroove + buttonHeight / 2;
        double valuePercentage = ((double) value - minValue) / (maxValue - minValue);
        double yValue = yValue0 + hValue - hValue * valuePercentage;


        // Draw groove
        RoundRectangle2D.Double groove = new RoundRectangle2D.Double(xGroove, yGroove,
                wGroove, hGroove,
                arc, arc
        );
        GradientPaint paint = new GradientPaint((float) x0, (float) yGroove, grooveUpperColor, (float) x0, (float) (yGroove + hGroove), grooveLowerColor);
        g2.setPaint(paint);
        g2.fill(groove);
        g2.setPaint(null);


        // Draw marks
        if (wDoubleMark > 0)
        {
            g2.setColor(buttonColor);
            g2.setStroke(new BasicStroke(1));
            final int nbMarks = 5;
            for (int i = 0; i < nbMarks; i++)
            {
                double yMark = yValue0 + i * (hGroove - buttonHeight) / (nbMarks - 1);
                double length = (i == 0 || i == 2 || i == 4) ? wDoubleMark : wDoubleMark / 2;
                Line2D.Double line = new Line2D.Double(x0, yMark, x0 + length, yMark);
                g2.draw(line);
            }
        }


        // Draw value line
        if (isEnabled() && valueLineColor != null)
        {
            RoundRectangle2D.Double valueLine = new RoundRectangle2D.Double(xGroove, yValue,
                    wGroove, valuePercentage * hValue + buttonHeight / 2,
                    arc, arc
            );
            g2.setColor(valueLineColor);
            g2.fill(valueLine);
        }


        // Draw the button
        RoundRectangle2D.Double button = new RoundRectangle2D.Double(xButton, yValue - buttonHeight / 2,
                wButton, buttonHeight,
                arc, arc
        );
        g2.setColor(isEnabled() ? buttonColor : buttonColorDisabled);
        g2.fill(button);
        if (buttonContourColor != null)
        {
            g2.setStroke(new BasicStroke(2));
            g2.setColor(buttonContourColor);
            g2.draw(button);
        }
        final int hPadding = 4;
        double x0Line = xButton + hPadding;
        double x1Line = xButton + wButton - hPadding;
        double vGap = buttonHeight / 5;
        Line2D.Double lineMiddle = new Line2D.Double(x0Line, yValue, x1Line, yValue);
        Line2D.Double lineUpper = new Line2D.Double(x0Line, yValue - vGap, x1Line, yValue - vGap);
        Line2D.Double lineLower = new Line2D.Double(x0Line, yValue + vGap, x1Line, yValue + vGap);
        g2.setColor(grooveUpperColor);
        g2.setStroke(new BasicStroke(1));
        g2.draw(lineUpper);
        g2.draw(lineMiddle);
        g2.draw(lineLower);


        g2.dispose();
    }

    /**
     * The last MouseEvent corresponding to the last mouse drag or wheel user action to change the slider value.
     * <p>
     * Can be used by listeners to retrieve the shift/ctrl/alt modifiers after having received a value property change.
     *
     * @return Can be null if last user action was not a mouse drag/wheel (e.g. if he used the inline editor)
     */
    public MouseEvent getLastMouseEvent()
    {
        return lastMouseEvent;
    }

    /**
     * @return the valueLineColor
     */
    public Color getValueLineColor()
    {
        return valueLineColor;
    }

    /**
     * @param valueLineColor the valueLineColor to set
     */
    public void setValueLineColor(Color valueLineColor)
    {
        this.valueLineColor = valueLineColor;
        repaint();
    }

    /**
     * @return the buttonHeight
     */
    public int getButtonHeight()
    {
        return buttonHeight;
    }

    /**
     * The unique data to compute the preferred size.
     *
     * @param buttonHeight
     */
    public void setButtonHeight(int buttonHeight)
    {
        this.buttonHeight = buttonHeight;
        revalidate();
        repaint();
    }

    /**
     * @return the buttonColor
     */
    public Color getButtonColor()
    {
        return buttonColor;
    }

    /**
     * @param buttonColor the buttonColor to set
     */
    public void setButtonColor(Color buttonColor)
    {
        this.buttonColor = buttonColor;
        repaint();
    }

    /**
     * @return the minValue
     */
    public int getMinValue()
    {
        return minValue;
    }

    /**
     * @param minValue the minValue to set
     */
    public void setMinValue(int minValue)
    {
        this.minValue = minValue;
        repaint();
    }

    /**
     * @return the maxValue
     */
    public int getMaxValue()
    {
        return maxValue;
    }

    /**
     * @param maxValue the maxValue to set
     */
    public void setMaxValue(int maxValue)
    {
        this.maxValue = maxValue;
        repaint();
    }

    public void setValue(int v)
    {
        if (v < 0 || v > 127)
        {
            throw new IllegalArgumentException("v=" + v);   
        }
        if (value != v)
        {
            int old = value;
            value = v;
            updateToolTipText();
            repaint();
            firePropertyChange(PROP_VALUE, old, value);
        }
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        updateToolTipText();
    }

    public String getTooltipLabel()
    {
        return tooltipLabel;
    }

    public void setTooltipLabel(String tooltipLabel)
    {
        this.tooltipLabel = tooltipLabel;
        updateToolTipText();
    }

    /**
     * @return the buttonContourColor
     */
    public Color getButtonContourColor()
    {
        return buttonContourColor;
    }

    /**
     * @param buttonContourColor the buttonContourColor to set
     */
    public void setButtonContourColor(Color buttonContourColor)
    {
        this.buttonContourColor = buttonContourColor;
        repaint();
    }

    /**
     * @return the grooveUpperColor
     */
    public Color getGrooveUpperColor()
    {
        return grooveUpperColor;
    }

    /**
     * @param grooveUpperColor the grooveUpperColor to set
     */
    public void setGrooveUpperColor(Color grooveUpperColor)
    {
        this.grooveUpperColor = grooveUpperColor;
        repaint();
    }

    /**
     * @return the grooveLowerColor
     */
    public Color getGrooveLowerColor()
    {
        return grooveLowerColor;
    }

    /**
     * @param grooveLowerColor the grooveLowerColor to set
     */
    public void setGrooveLowerColor(Color grooveLowerColor)
    {
        this.grooveLowerColor = grooveLowerColor;
        repaint();
    }

    /**
     * @return the buttonColorDisabled
     */
    public Color getButtonColorDisabled()
    {
        return buttonColorDisabled;
    }

    /**
     * @param buttonColorDisabled the buttonColorDisabled to set
     */
    public void setButtonColorDisabled(Color buttonColorDisabled)
    {
        this.buttonColorDisabled = buttonColorDisabled;
        if (!isEnabled())
        {
            repaint();
        }
    }

    /**
     * @return the padding
     */
    public int getPadding()
    {
        return padding;
    }

    /**
     * @param padding the padding to set
     */
    public void setPadding(int padding)
    {
        this.padding = padding;
        revalidate();
        repaint();
    }

    protected String prepareToolTipText()
    {
        String valueAstring = isEnabled() ? String.valueOf(getValue()) : "OFF";
        String text = (getTooltipLabel() == null) ? valueAstring : getTooltipLabel() + "=" + valueAstring;
        return text;
    }

    // ==========================================================================
    // MouseListener + MouseMotionListener interface
    // ==========================================================================
    @Override
    public void mouseClicked(MouseEvent e)
    {

    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if (e.isControlDown())
        {
            setValue((minValue + maxValue) / 2);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        // Possibly dragging end
        yDragStart = Integer.MIN_VALUE;
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        int y = e.getY();
        if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e))
        {
            return;
        }

        if (yDragStart == Integer.MIN_VALUE)
        {
            // Start dragging
            yDragStart = e.getY();
            yValueDragStart = value;
        } else
        {
            // Continue dragging            
            final int FULL_DRAG_Y = 200;
            float yDrag = -(e.getY() - yDragStart);
            float f = yDrag / FULL_DRAG_Y;
            int v = yValueDragStart + Math.round((getMaxValue() - getMinValue()) * f);
            v = Math.max(v, minValue);
            v = Math.min(v, maxValue);
            lastMouseEvent = e;
            setValue(v);
            lastMouseEvent = null;
        }
    }

    @Override

    public void mouseMoved(MouseEvent e)
    {
        // Nothing
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if (!isEnabled())
        {
            return;
        }
        lastMouseEvent = e;
        boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        int step = ctrl ? 6 : 2;
        if (e.getWheelRotation() < 0)
        {
            if (value + step <= maxValue)
            {
                setValue(value + step);
            } else
            {
                setValue(maxValue);
            }

        } else if (e.getWheelRotation() > 0)
        {
            if (value - step >= minValue)
            {
                setValue(value - step);
            } else
            {
                setValue(minValue);
            }
        }
        lastMouseEvent = null;
    }

    // ==========================================================================
    // Private fucntions
    // ==========================================================================
    private void updateToolTipText()
    {
        setToolTipText(prepareToolTipText());
    }

    private double calcButtonWidth()
    {
        return buttonHeight / 1.867;
    }

    private double calcHeight()
    {
        return buttonHeight * 4.8;
    }

    private double calcGrooveWidth()
    {
        return buttonHeight / 4;
    }

    private double calcDoubleMarkLength()
    {
        // return buttonHeight / 9;
        return 0;
    }
}

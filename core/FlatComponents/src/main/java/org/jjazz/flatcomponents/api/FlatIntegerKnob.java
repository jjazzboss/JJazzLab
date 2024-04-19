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
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A flat knob.
 * <p>
 * Value can be changed by dragging mouse or mouse wheel. ctrl-click reset the value. Whatever the actual component size, the
 * drawing is done at preferred size.
 */
public class FlatIntegerKnob extends JPanel implements MouseMotionListener, MouseWheelListener, MouseListener
{

    /**
     * Fire a PROP_Value property change event when user changes the value.
     */
    public static final String PROP_VALUE = "PropValue";    

    private double valueLineThickness = 4;
    private double valueLineGap = 2;
    private Color valueLineColor = new Color(0, 255, 255);
    private double knobRadius = 11;
    private double knobStartAngle = 230;
    private Color knobUpperColor = new Color(109, 109, 109);
    private Color knobLowerColor = new Color(64, 64, 64);
    private Color knobRectColor = new Color(230, 230, 230);
    private Color knobRectColorDisabled = new Color(69, 69, 69);
    private int padding = 2;
    private boolean panoramicType = false;
    private int minValue = 0;
    private int maxValue = 127;
    private int value = 64;
    private String tooltipLabel;
    private int startDragY = Integer.MIN_VALUE;
    private int startDragValue;
    private Color saveForeground;
    private Color saveColorLine;
    private boolean useValueTooltip = true;


    private static final Logger LOGGER = Logger.getLogger(FlatIntegerKnob.class.getSimpleName());

    public FlatIntegerKnob()
    {

        BorderManager.getInstance().register(this, false, false, true);


        // Listen to mouse drag events to change value
        addMouseMotionListener(this);
        addMouseListener(this);

        // Use mouse wheel to change value, only if enabled
        FlatComponentsGlobalSettings.getInstance().installChangeValueWithMouseWheelSupport(this, this);

        updateToolTipText();

    }

    @Override
    public Dimension getPreferredSize()
    {
        Insets in = getInsets();
        int w = (int) Math.ceil(getFullKnobWidth()) + 2 * padding + in.left + in.right;
        int h = (int) Math.ceil(getFullKnobHeight()) + 2 * padding + in.top + in.bottom;
        var d = new Dimension(w, h);
        return d;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); // Honor the opaque property

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);


        Insets in = getInsets();
        int w = getWidth();
        int h = getHeight();
        double wFullKnob = getFullKnobWidth();
        double hFullKnob = getFullKnobHeight();


        // Degrees
        double valuePercentage = ((double) value - minValue) / (maxValue - minValue);
        double degFullArc = knobStartAngle + knobStartAngle - 180;
        double degValueArc = valuePercentage * degFullArc;


        // position the knob X/Y centered
        double xFullKnob = (w - wFullKnob) / 2d;
        double yFullKnob = (h - hFullKnob) / 2d;
        double xKnobCenter = xFullKnob + valueLineThickness + valueLineGap + knobRadius;
        double yKnobCenter = yFullKnob + valueLineThickness + valueLineGap + knobRadius;


        // Paint the knob
        Ellipse2D.Double knob = new Ellipse2D.Double(
                xKnobCenter - knobRadius,
                yKnobCenter - knobRadius,
                2 * knobRadius,
                2 * knobRadius
        );
        Paint p = new GradientPaint((float) xKnobCenter, (float) (yKnobCenter - knobRadius), knobUpperColor, (float) xKnobCenter, (float) (yKnobCenter + knobRadius), knobLowerColor);
        g2.setPaint(p);
        g2.fill(knob);
        g2.setPaint(null);


        // Add a rectangle inside the knob
        double theta = Math.toRadians(degValueArc - knobStartAngle);
        double wRect = knobRadius * 0.7;
        double hRect = wRect * 0.3;
        Rectangle2D rect = new Rectangle2D.Double(-wRect / 2., -hRect / 2., wRect, hRect);
        AffineTransform transform = new AffineTransform();
        double xRect = (knobRadius - wRect / 2 - 1) * Math.cos(theta);
        double yRect = (knobRadius - wRect / 2 - 1) * Math.sin(theta);
        transform.translate(xKnobCenter + xRect, yKnobCenter + yRect);
        transform.rotate(theta);
        Shape rotatedRect = transform.createTransformedShape(rect);
        g2.setColor(isEnabled() ? knobRectColor : getKnobRectColorDisabled());
        g2.fill(rotatedRect);


        // Paint the complete arc with knobLowerColor
        double xArcUp = xFullKnob;
        double yArcUp = yFullKnob;
        double wArcUp = wFullKnob;
        double hArcUp = wFullKnob;
        double xArcLow = xFullKnob + valueLineThickness - 0.5;
        double yArcLow = yFullKnob + valueLineThickness - 0.5;
        double wArcLow = wFullKnob - 2 * valueLineThickness + 1;
        double hArcLow = wFullKnob - 2 * valueLineThickness + 1;
        GeneralPath fullArc = new GeneralPath();
        fullArc.append(new Arc2D.Double(xArcUp,
                yArcUp,
                wArcUp,
                hArcUp,
                knobStartAngle,
                -degFullArc,
                Arc2D.OPEN),
                false);
        fullArc.append(new Arc2D.Double(xArcLow,
                yArcLow,
                wArcLow,
                hArcLow,
                180 - knobStartAngle,
                degFullArc,
                Arc2D.OPEN),
                true);
        g2.setColor(knobLowerColor);
        g2.fill(fullArc);


        // Overwrite with the value arc
        if (isEnabled())
        {
            GeneralPath valueArc = new GeneralPath();
            if (!isPanoramicType())
            {
                valueArc.append(new Arc2D.Double(xArcUp,
                        yArcUp,
                        wArcUp,
                        hArcUp,
                        knobStartAngle,
                        -degValueArc,
                        Arc2D.OPEN),
                        false);
                valueArc.append(new Arc2D.Double(xArcLow,
                        yArcLow,
                        wArcLow,
                        hArcLow,
                        knobStartAngle - degValueArc,
                        degValueArc,
                        Arc2D.OPEN),
                        true);
            } else
            {
                double degValueAngle = knobStartAngle - degValueArc;
                double northAngle = 90;
                if (Math.abs(degValueAngle - northAngle) < 5)
                {
                    // Color line is too small, make it appear a little bigger
                    degValueAngle = 94;
                    northAngle = 86;
                }
                valueArc.append(new Arc2D.Double(xArcUp,
                        yArcUp,
                        wArcUp,
                        hArcUp,
                        degValueAngle,
                        -(degValueAngle - northAngle),
                        Arc2D.OPEN),
                        false);
                valueArc.append(new Arc2D.Double(xArcLow,
                        yArcLow,
                        wArcLow,
                        hArcLow,
                        northAngle,
                        degValueAngle - northAngle,
                        Arc2D.OPEN),
                        true);
            }
            g2.setColor(valueLineColor);
            g2.fill(valueArc);
        }


        g2.dispose();
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        updateToolTipText();
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
     * @return the valueLineThickness
     */
    public double getValueLineThickness()
    {
        return valueLineThickness;
    }

    /**
     * @param valueLineThickness the valueLineThickness to set
     */
    public void setValueLineThickness(double valueLineThickness)
    {
        this.valueLineThickness = valueLineThickness;
        revalidate();
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

    public String getTooltipLabel()
    {
        return tooltipLabel;
    }

    /**
     * If not null and isUseValueTooltip==true, use tooltipLabel to automatically form the tooltip.
     *
     * @param tooltipLabel
     * @see #setUseValueTooltip(boolean)
     */
    public void setTooltipLabel(String tooltipLabel)
    {
        this.tooltipLabel = tooltipLabel;
        updateToolTipText();
    }

    /**
     * True if the tooltip is automatically set from the value and tooltipLabel.
     *
     * @return True by default.
     * @see #setUseValueTooltip(boolean)
     */
    public boolean isUseValueTooltip()
    {
        return useValueTooltip;
    }

    /**
     * If true the tooltip is automatically set using the tooltip label (if not null) and the knob value.
     *
     * @param useValueTooltip
     * @see #setTooltipLabel(java.lang.String)
     */
    public void setUseValueTooltip(boolean useValueTooltip)
    {
        this.useValueTooltip = useValueTooltip;
        updateToolTipText();
    }


    public void setValue(int v)
    {
        if (v < getMinValue() || v > getMaxValue())
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

    /**
     * @return the knobRadius
     */
    public double getKnobRadius()
    {
        return knobRadius;
    }

    /**
     * @param knobRadius the knobRadius to set
     */
    public void setKnobRadius(double knobRadius)
    {
        this.knobRadius = knobRadius;
        revalidate();
        repaint();
    }

    /**
     * @return the knobStartAngle
     */
    public double getKnobStartAngle()
    {
        return knobStartAngle;
    }

    /**
     *
     * @param angle Between 180 and 270 degrees.
     */
    public void setKnobStartAngle(double angle)
    {
        this.knobStartAngle = angle;
        if (angle < 180 || angle > 270)
        {
            throw new IllegalArgumentException("angle=" + angle);
        }
        repaint();
    }

    /**
     * @return the knobUpperColor
     */
    public Color getKnobUpperColor()
    {
        return knobUpperColor;
    }

    /**
     * @param knobUpperColor the knobUpperColor to set
     */
    public void setKnobUpperColor(Color knobUpperColor)
    {
        this.knobUpperColor = knobUpperColor;
        repaint();
    }

    /**
     * @return the knobLowerColor
     */
    public Color getKnobLowerColor()
    {
        return knobLowerColor;
    }

    /**
     * @param knobLowerColor the knobLowerColor to set
     */
    public void setKnobLowerColor(Color knobLowerColor)
    {
        this.knobLowerColor = knobLowerColor;
        repaint();
    }

    /**
     * @return the valueLineGap
     */
    public double getValueLineGap()
    {
        return valueLineGap;
    }

    /**
     * @param valueLineGap the valueLineGap to set
     */
    public void setValueLineGap(double valueLineGap)
    {
        this.valueLineGap = valueLineGap;
        revalidate();
        repaint();
    }

    /**
     * @return the knobRectColor
     */
    public Color getKnobRectColor()
    {
        return knobRectColor;
    }

    /**
     * @param knobRectColor the knobRectColor to set
     */
    public void setKnobRectColor(Color knobRectColor)
    {
        this.knobRectColor = knobRectColor;
        repaint();
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
        repaint();
    }

    /**
     * @return the knobRectColorDisabled
     */
    public Color getKnobRectColorDisabled()
    {
        return knobRectColorDisabled;
    }

    /**
     * @param knobRectColorDisabled the knobRectColorDisabled to set
     */
    public void setKnobRectColorDisabled(Color knobRectColorDisabled)
    {
        this.knobRectColorDisabled = knobRectColorDisabled;
        if (!isEnabled())
        {
            repaint();
        }
    }

    /**
     * @return the panoramicType. Default is false.
     */
    public boolean isPanoramicType()
    {
        return panoramicType;
    }

    /**
     * @param panoramicType the panoramicType to set
     */
    public void setPanoramicType(boolean panoramicType)
    {
        this.panoramicType = panoramicType;
        repaint();
    }

    protected String prepareToolTipText()
    {
        String valueAstring = isEnabled() ? String.valueOf(getValue()) : "OFF";
        String text = (getTooltipLabel() == null) ? valueAstring : getTooltipLabel() + "=" + valueAstring;
        return text;
    }

    // ==========================================================================
    // MouseMotionListener interface
    // ==========================================================================
    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e))
        {
            return;
        }
        if (startDragY == Integer.MIN_VALUE)
        {
            // Start dragging
            startDragY = e.getY();
            startDragValue = value;
        } else
        {
            // Continue dragging            
            final int FULL_DRAG_Y = 200;
            float yDrag = -(e.getY() - startDragY);
            float f = yDrag / FULL_DRAG_Y;
            int v = startDragValue + Math.round((getMaxValue() - getMinValue()) * f);
            v = Math.max(v, minValue);
            v = Math.min(v, maxValue);
            setValue(v);
        }

    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
    }

    // ==========================================================================
    // MouseListener + MouseMotinoListener interface
    // ==========================================================================
    @Override
    public void mouseClicked(MouseEvent e)
    {
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
    public void mousePressed(MouseEvent e)
    {
        if (e.isControlDown())
        {
            resetValue();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (startDragY != Integer.MIN_VALUE)
        {
            startDragY = Integer.MIN_VALUE;
        }
    }

    // ==========================================================================
    // MouseWheelListener interface
    // ==========================================================================
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if (!isEnabled())
        {
            return;
        }
        boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        int step = ctrl ? 6 : 2;
        if (e.getWheelRotation() < 0)
        {
            if (getValue() + step <= getMaxValue())
            {
                setValue(getValue() + step);
            } else
            {
                setValue(getMaxValue());
            }

        } else if (e.getWheelRotation() > 0)
        {
            if (getValue() - step >= getMinValue())
            {
                setValue(getValue() - step);
            } else
            {
                setValue(getMinValue());
            }
        }
    }

    // ==========================================================================
    // Private fucntions
    // ==========================================================================
    private double getFullKnobWidth()
    {
        double res = knobRadius * 2 + 2 * valueLineGap + 2 * valueLineThickness;
        return res;
    }

    private double getFullKnobHeight()
    {
        double hUpper = knobRadius + valueLineGap + valueLineThickness;
        double res = hUpper - Math.cos(Math.toRadians(knobStartAngle)) * hUpper;
        return res;
    }

    private void resetValue()
    {
        if (!panoramicType)
        {
            setValue(minValue);
        } else
        {
            // Compute the value associated to the north direction
            setValue((maxValue + minValue) / 2);
        }
    }

    private void updateToolTipText()
    {
        if (useValueTooltip)
        {
            setToolTipText(prepareToolTipText());
        }
    }

}

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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import org.openide.windows.WindowManager;

/**
 * A vertical flat slider.
 */
public class FlatIntegerVerticalSlider extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener, PropertyChangeListener
{

    public static final String PROP_VALUE = "PropValue";
    /**
     * Client Property: top color, used to created the gradient
     */
    public final static String PROP_COLOR_TOP = "PropColorTop";
    /**
     * Client Property: bottom color, used to created the gradient
     */
    public final static String PROP_COLOR_BOTTOM = "PropColorBottom2";
    /**
     * Client Property: line thickness
     */
    public final static String PROP_LINE_THICKNESS = "PropLineThickness";
    /**
     * Client Property: number of graduation marks
     */
    public final static String PROP_NB_GRADUATION_MARKS = "PropNbGraduationMarks";
    /**
     * Client Property: hide value text if not active : 0=false, 1=true
     */
    public final static String PROP_HIDE_VALUE_IF_NOT_ACTIVE = "PropHideValueIfNotActive";
    private static final Font FONT = new Font("Arial", Font.PLAIN, 8);
    public static FlatTextEditDialog TEXT_EDIT_DIALOG;
    private MouseEvent lastMouseEvent;
    private Color colorLine;
    private Color colorKnobFill;
    private int minValue = 0;
    private int maxValue = 127;
    private int value = 64;
    private int yValue;
    private int width;
    private int faderLength;
    private int height;
    private int yMin;
    private int yMax;
    private int faderWidth;
    private int knobDiameter;
    private int graduationLength;
    private double yFactor;
    private int xLine;
    private boolean isDragging = false;
    private int yDragStart;
    private int yValueDragStart;
    private boolean hideValue = true;
    private Color saveColorLine;

    /**
     * Used to hide the value some time after value has been set.
     */
    private Timer timer;
    private ActionListener timerAction = new ActionListener()
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            timer.stop();
            hideValue = true;
            timer = null;
            repaint();
        }
    };
    private static final Logger LOGGER = Logger.getLogger(FlatIntegerVerticalSlider.class.getSimpleName());

    /**
     * Creates new form PanoramicPanel
     */
    public FlatIntegerVerticalSlider()
    {
        colorLine = new Color(103, 139, 176);
        colorKnobFill = new Color(240, 240, 240);
        putClientProperty(PROP_COLOR_BOTTOM, new Color(0, 135, 255));
        putClientProperty(PROP_COLOR_TOP, new Color(255, 0, 255));
        putClientProperty(PROP_NB_GRADUATION_MARKS, 13);
        putClientProperty(PROP_LINE_THICKNESS, 1);
        putClientProperty(PROP_HIDE_VALUE_IF_NOT_ACTIVE, 1);
        setFont(FONT);
        setForeground(new Color(97, 97, 97));
        setFaderWidth(8);
        setKnobDiameter(20);
        setGraduationLength(2);
        formComponentResized(null);
        addComponentListener(new java.awt.event.ComponentAdapter()
        {
            @Override
            public void componentResized(java.awt.event.ComponentEvent evt)
            {
                formComponentResized(evt);
            }
        });
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addPropertyChangeListener(this);
        FlatHoverManager.getInstance().associate(this);
    }

    /**
     * The last MouseEvent corresponding to the last mouse drag or wheel user action to change the slider value. Can be used by
     * listeners to retrieve the shift/ctrl/alt modifiers after having received a value property change.
     *
     * @return Can be null if last user action was not a mouse drag/wheel (e.g. if he used the inline editor)
     */
    public MouseEvent getLastMouseEvent()
    {
        return lastMouseEvent;
    }

    /**
     * @return the colorLine
     */
    public Color getColorLine()
    {
        return colorLine;
    }

    /**
     * @param colorLine the colorLine to set
     */
    public void setColorLine(Color colorLine)
    {
        this.colorLine = colorLine;
    }

    /**
     * @return the knobDiameter
     */
    public int getKnobDiameter()
    {
        return knobDiameter;
    }

    /**
     * @param knobDiameter the knobDiameter to set
     */
    public void setKnobDiameter(int knobDiameter)
    {
        this.knobDiameter = knobDiameter;
        repaint();
    }

    /**
     * @return the colorKnobFill
     */
    public Color getColorKnobFill()
    {
        return colorKnobFill;
    }

    /**
     * @param colorKnobFill the colorKnobFill to set
     */
    public void setColorKnobFill(Color colorKnobFill)
    {
        this.colorKnobFill = colorKnobFill;
    }

    /**
     * @return the faderWidth
     */
    public int getFaderWidth()
    {
        return faderWidth;
    }

    /**
     * @param faderWidth the faderWidth to set
     */
    public void setFaderWidth(int faderWidth)
    {
        this.faderWidth = faderWidth;
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
        formComponentResized(null);
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
        formComponentResized(null);
    }

    /**
     * @return the graduationLength
     */
    public int getGraduationLength()
    {
        return graduationLength;
    }

    /**
     * @param graduationLength the graduationLength to set
     */
    public void setGraduationLength(int graduationLength)
    {
        this.graduationLength = graduationLength;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); // Honor the opaque property

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        yValue = yMax - (int) Math.rint((double) value * yFactor);
        int lineThickness = getInt(PROP_LINE_THICKNESS);

        // The fader       
        g2.setColor(getColorKnobFill());
        g2.fillRoundRect(xLine - faderWidth / 2, yMin - faderWidth / 2 + 1, faderWidth, faderLength + faderWidth, faderWidth, faderWidth);
        g2.setColor(getColorLine());
        g2.setStroke(new BasicStroke(lineThickness));
        g2.drawRoundRect(xLine - faderWidth / 2, yMin - faderWidth / 2 + 1, faderWidth, faderLength + faderWidth, faderWidth, faderWidth);

        // Fill the lower part of the fader with a gradient
        if (isEnabled())
        {
            GradientPaint gp = new GradientPaint(xLine, yMax, getColor(PROP_COLOR_BOTTOM), xLine, yMin, getColor(PROP_COLOR_TOP));
            g2.setPaint(gp);
            g2.fillRoundRect(xLine - faderWidth / 2, yValue - faderWidth / 2 + 1, faderWidth + 1, yMax - yValue + faderWidth + 2, faderWidth, faderWidth);
        }

        // Graduation marks
        int nbMarks = getInt(PROP_NB_GRADUATION_MARKS);
        if (nbMarks > 0)
        {
            g2.setColor(getColorLine());
            g2.setStroke(new BasicStroke(1));

            int x = xLine + faderWidth / 2 + lineThickness + 3;
            for (int i = 0; i <= nbMarks; i++)
            {
                int y = (int) Math.rint(i * faderLength / (double) nbMarks) + yMin;
                g2.drawLine(x, y, x + graduationLength - 1, y);
            }
        }

        // The knob
        if (isEnabled())
        {
            g2.setColor(getColorKnobFill());
            g2.setStroke(new BasicStroke(lineThickness + 1));
            g2.fill(new Ellipse2D.Double(xLine - getKnobDiameter() / 2d, yValue - getKnobDiameter() / 2d, getKnobDiameter(), getKnobDiameter()));
            g2.setColor(getColorLine());
            g2.draw(new Ellipse2D.Double(xLine - getKnobDiameter() / 2d, yValue - getKnobDiameter() / 2d, getKnobDiameter(), getKnobDiameter()));
        }

        // Write the value in knob
        g2.setColor(getForeground());
        g2.setStroke(new BasicStroke(1));
        String text = String.valueOf(value);
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D stringBounds = fm.getStringBounds(text, g2);
        double stringHeight = stringBounds.getHeight();
        double stringWidth = stringBounds.getWidth();
        double xText = xLine - stringWidth / 2 + 1;
        double yText = yValue + stringHeight / 2 - 1;
        if (getInt(PROP_HIDE_VALUE_IF_NOT_ACTIVE) == 0 || !hideValue)
        {
            g2.drawString(text, (float) xText, (float) yText);
        }
//        g2.setColor(Color.RED);
//        g2.draw(new Rectangle2D.Double(xText, yText - stringHeight + 1, stringWidth, stringHeight));
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
            if (timer == null)
            {
                timer = new Timer(1000, timerAction);
                timer.start();
            } else
            {
                timer.restart();
            }
            hideValue = false;
            this.updateToolTipText();
            repaint();
            firePropertyChange(PROP_VALUE, old, value);
        }
    }

    public int getValue()
    {
        return value;
    }

    /**
     * Overridden to update our settings.
     *
     * @param b
     */
    @Override
    public void setBorder(Border b)
    {
        super.setBorder(b);
        formComponentResized(null);
    }

    @Override
    public Dimension getPreferredSize()
    {
        int w = getKnobDiameter() + 10;
        int h = (int) (w * 4.5d);
        return new Dimension(w, h);
    }

    @Override
    public void setEnabled(boolean b)
    {
        LOGGER.fine("setEnabled() b=" + b);
        if (isEnabled() && !b)
        {
            saveColorLine = getColorLine();
            setColorLine(Color.LIGHT_GRAY);
        } else if (!isEnabled() && b)
        {
            setColorLine(saveColorLine);
        }
        super.setEnabled(b);
        updateToolTipText();
    }

    // ==========================================================================
    // MouseListener + MouseMotionListener interface
    // ==========================================================================
    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (isEnabled() && e.getClickCount() == 2)
        {
            openEditor();
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        // Nothing
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        // Nothing
        isDragging = false;
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
        LOGGER.fine("mouseDragged() y=" + y + " yValue=" + yValue + " value=" + value);
        if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e))
        {
            return;
        }
        if (isDragging)
        {   // Continue current drag operation        
            lastMouseEvent = e;
            setValue(getValue(yValueDragStart + (y - yDragStart)));
            lastMouseEvent = null;
        } else if (y > yValue - getKnobDiameter() && y < yValue + getKnobDiameter())
        {
            // We're dragging inside the knob, start the dragging operation
            isDragging = true;
            yDragStart = y;
            yValueDragStart = yValue;
        }
        repaint();
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
        int step = ctrl ? 5 : 1;
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

    // ================================================================================
    // PropertyChangeListener interface
    // ================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        String key = evt.getPropertyName();
        if (key == PROP_LINE_THICKNESS || key == PROP_NB_GRADUATION_MARKS)
        {
            repaint();
        }
    }

    // ==========================================================================
    // Private fucntions
    // ==========================================================================
    private Color getColor(String key)
    {
        return (Color) getClientProperty(key);
    }

    private Integer getInt(String key)
    {
        return (Integer) getClientProperty(key);
    }

    private Border getBorder(String key)
    {
        return (Border) getClientProperty(key);
    }

    /**
     * Return the value between minValue and maxValue based on the yPos in the component coordinates.
     *
     * @param y
     * @return
     */
    private int getValue(int yPos)
    {
        double v = (yMax - yPos) / yFactor;
        int iv = (int) Math.rint(v);
        iv = Math.max(iv, getMinValue());
        iv = Math.min(iv, getMaxValue());
        return iv;
    }

    private void formComponentResized(java.awt.event.ComponentEvent evt)
    {
        Insets in = getInsets();
        width = getWidth() - in.left - in.right;
        height = getHeight() - in.top - in.bottom;
        // Knob diameter must be > fader width. Does not take into account the rounded end
        yMin = in.top + getKnobDiameter() / 2 + 2;
        faderLength = height - getKnobDiameter() - 4;
        yMax = yMin + faderLength - 1;
        xLine = in.left + width / 2;
        yFactor = (yMax - yMin + 1d) / (getMaxValue() - getMinValue() + 1d);
        isDragging = false;
        repaint();
    }

    private void openEditor()
    {
        if (TEXT_EDIT_DIALOG == null)
        {
            TEXT_EDIT_DIALOG = new FlatTextEditDialog(WindowManager.getDefault().getMainWindow(), true);
            TEXT_EDIT_DIALOG.setBackground(getColorKnobFill());
            TEXT_EDIT_DIALOG.setForeground(getForeground());
            TEXT_EDIT_DIALOG.setHorizontalAlignment(JTextField.CENTER);
            TEXT_EDIT_DIALOG.setColumns(3);
        }
        String strOldValue = valueToString(value);
        TEXT_EDIT_DIALOG.setText(strOldValue);
        TEXT_EDIT_DIALOG.pack();
        TEXT_EDIT_DIALOG.setPositionCenter(this);

        TEXT_EDIT_DIALOG.setVisible(true);
        String text = TEXT_EDIT_DIALOG.getText().trim();
        if (TEXT_EDIT_DIALOG.isExitOk() && text.length() > 0 && !text.equals(strOldValue))
        {
            int newValue = stringToValue(text);
            if (newValue != -1)
            {
                setValue(newValue);
            }
        }
    }

    /**
     * Get the String reprensentation of the specified value. By default return String.valueOf(getValue()).
     *
     * @param v
     * @return The text used to represent the value in the knob.
     */
    protected String valueToString(int v)
    {
        return String.valueOf(v);
    }

    private int stringToValue(String text)
    {
        int r = -1;
        try
        {
            r = Integer.parseUnsignedInt(text);
            r = Math.max(0, r);
            r = Math.min(127, r);
        } catch (NumberFormatException e)
        {
            // Nothing leave value unchanged
        }
        if (r == -1)
        {
            LOGGER.fine("parseString() text=" + text);
        }
        return r;
    }

    private void updateToolTipText()
    {
        String valueAstring = isEnabled() ? valueToString(value) : "OFF";
        setToolTipText(valueAstring);
    }

}

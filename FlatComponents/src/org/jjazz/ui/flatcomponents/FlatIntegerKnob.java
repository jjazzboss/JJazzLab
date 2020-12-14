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
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import org.jjazz.uisettings.GeneralUISettings;
import org.openide.windows.WindowManager;

/**
 * A flat knob
 */
public class FlatIntegerKnob extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener, PropertyChangeListener
{

    public static final String PROP_VALUE = "PropValue";   //NOI18N 
    /**
     * Client Property: line thickness
     */
    public final static String PROP_LINE_THICKNESS = "PropLineThickness";
    /**
     * Client Property: graduation length
     */
    public final static String PROP_GRADUATION_LENGTH = "PropGraduationLength";
    /**
     * Client Property: hide value text if not active : 0=false, 1=true
     */
    public final static String PROP_HIDE_VALUE_IF_NOT_ACTIVE = "PropHideValueIfNotActive";
    private static final Font FONT = new Font("Arial", Font.PLAIN, 9);
    public static FlatTextEditDialog TEXT_EDIT_DIALOG;
    private Color colorLine;
    private Color colorKnobFill;
    private int minValue = 0;
    private int maxValue = 127;
    private int value;
    private double xValue;
    private double yValue;
    private int width;
    private int height;
    private int yMax;
    private int knobDiameter;
    private int xKnobCenter;
    private int yKnobCenter;
    private String label;
    private String tooltipLabel;
    private boolean hideValue = true;
    private Color saveForeground;
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
    private static final Logger LOGGER = Logger.getLogger(FlatIntegerKnob.class.getSimpleName());

    public FlatIntegerKnob()
    {
        putClientProperty(PROP_LINE_THICKNESS, 2);
        putClientProperty(PROP_GRADUATION_LENGTH, 2);
        putClientProperty(PROP_HIDE_VALUE_IF_NOT_ACTIVE, 1);
        colorLine = new Color(103, 139, 176);
        colorKnobFill = new Color(240, 240, 240);
        setForeground(new Color(97, 97, 97));
        setFont(FONT);
        setKnobDiameter(30);
        setValue(64);
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
        // Use mouse wheel only if enabled
        GeneralUISettings.getInstance().installChangeValueWithMouseWheelSupport(this, this);
        addPropertyChangeListener(this);
        FlatHoverManager.getInstance().associate(this);
    }

    @Override
    public void setEnabled(boolean b)
    {
        LOGGER.fine("setEnabled() b=" + b);   //NOI18N
        if (isEnabled() && !b)
        {
            saveForeground = getForeground();
            saveColorLine = getColorLine();
            setForeground(Color.LIGHT_GRAY);
            setColorLine(Color.LIGHT_GRAY);
        } else if (!isEnabled() && b)
        {
            setForeground(saveForeground);
            setColorLine(saveColorLine);
        }
        super.setEnabled(b);
        updateToolTipText();
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
     * @return the label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @param label the label to set. Can be null.
     */
    public void setLabel(String label)
    {
        this.label = label;
        repaint();
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

    @Override
    public Dimension getPreferredSize()
    {
        int w = knobDiameter + getInt(PROP_GRADUATION_LENGTH) * 2 + 10;
        int h = knobDiameter + getInt(PROP_GRADUATION_LENGTH) * 2 + 15; // More for the label
        return new Dimension(w, h);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); // Honor the opaque property

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // The knob
        g2.setColor(colorKnobFill);
        g2.fill(new Ellipse2D.Double(xKnobCenter - getKnobDiameter() / 2d, yKnobCenter - getKnobDiameter() / 2d, getKnobDiameter(), getKnobDiameter()));
        g2.setColor(colorLine);
        g2.setStroke(new BasicStroke(getInt(PROP_LINE_THICKNESS)));
        g2.draw(new Ellipse2D.Double(xKnobCenter - getKnobDiameter() / 2d, yKnobCenter - getKnobDiameter() / 2d, getKnobDiameter(), getKnobDiameter()));

        // The point in the knob
        double pointSize = 4;
        double pointDiameter = getKnobDiameter() * 0.6d;
        double valuePercentage = ((double) value - minValue) / (maxValue - minValue);
        double radians = valuePercentage * (3 * Math.PI / 2) - Math.PI / 4;
        xValue = xKnobCenter - pointDiameter / 2d * Math.cos(radians);
        yValue = yKnobCenter - pointDiameter / 2d * Math.sin(radians);
        g2.setColor(colorLine);
        g2.fill(new Ellipse2D.Double(xValue - pointSize / 2d, yValue - pointSize / 2d, pointSize, pointSize));

        // Graduation marks
        g2.setColor(colorLine);
        g2.setStroke(new BasicStroke(1));
        int l = getInt(PROP_GRADUATION_LENGTH);
        double ofs = 4;
        for (int i = 0; i < 7; i++)
        {
            radians = i * Math.PI / 4 - Math.PI / 4;
            double x1 = xKnobCenter - (getKnobDiameter() / 2d + ofs) * Math.cos(radians);
            double y1 = yKnobCenter - (getKnobDiameter() / 2d + ofs) * Math.sin(radians);
            double x2 = xKnobCenter - (getKnobDiameter() / 2d + ofs + l) * Math.cos(radians);
            double y2 = yKnobCenter - (getKnobDiameter() / 2d + ofs + l) * Math.sin(radians);
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }

        // Write the value in knob
        g2.setColor(getForeground());
        String text = valueToString(getValue());
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D stringBounds = fm.getStringBounds(text, g2);
        double stringHeight = stringBounds.getHeight();
        double stringWidth = stringBounds.getWidth();
        double xText = xKnobCenter - stringWidth / 2 + 1;
        double yText = yKnobCenter + stringHeight / 2 - 1;
        if (getInt(PROP_HIDE_VALUE_IF_NOT_ACTIVE) == 0 || !hideValue)
        {
            g2.drawString(text, (float) xText, (float) yText);
        }

        // Write the label
        if (label != null)
        {
            g2.setColor(getForeground());
            fm = g2.getFontMetrics();
            stringBounds = fm.getStringBounds(label, g2);
            stringWidth = stringBounds.getWidth();
            xText = xKnobCenter - stringWidth / 2 + 1;
            yText = yMax - 1;
            g2.drawString(label, (float) xText, (float) yText);
        }
        LOGGER.finer("paintComponent() isEnabled()=" + isEnabled());   //NOI18N
    }

    public void setValue(int v)
    {
        if (v < minValue || v > maxValue)
        {
            throw new IllegalArgumentException("v=" + v);   //NOI18N
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

    // ==========================================================================
    // MouseListener + MouseMotinoListener interface
    // ==========================================================================
    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (SwingUtilities.isLeftMouseButton(e) && isEnabled() && e.getClickCount() == 2)
        {
            // Open editor
            openEditor();
        }
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
        // Nothing
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e))
        {
            return;
        }
        double x = e.getX() - xKnobCenter;
        double y = -(e.getY() - yKnobCenter);
        double radians = 0;
        if (x > 0)
        {
            double tan = y / x;
            radians = Math.atan(tan);

        } else if (x < 0)
        {
            double tan = y / -x;
            radians = Math.PI - Math.atan(tan);
        } else
        {
            // x=0;
            radians = (y >= 0) ? Math.PI / 2 : -Math.PI / 4d;
        }
        radians = Math.min(radians, Math.PI * 5 / 4);
        radians = Math.max(radians, -Math.PI / 4);
        double v = maxValue - (radians + Math.PI / 4) / (3 * Math.PI / 2) * (maxValue - minValue);
        setValue((int) Math.rint(v));
    }

    @Override

    public void mouseMoved(MouseEvent e)
    {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if (!isEnabled())
        {
            return;
        }
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
    }

    // ================================================================================
    // PropertyChangeListener interface
    // ================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        String key = evt.getPropertyName();
        if (key == PROP_LINE_THICKNESS || key == PROP_GRADUATION_LENGTH)
        {
            revalidate();
            repaint();
        }
    }

    // ==========================================================================
    // Private fucntions
    // ==========================================================================
    private Integer getInt(String key)
    {
        return (Integer) getClientProperty(key);
    }

    private void formComponentResized(java.awt.event.ComponentEvent evt)
    {
        if (getInt(PROP_LINE_THICKNESS) == null)
        {
            // We're in the JPanel constructor, no problem, we'll be called again later when object will be constructed.
            return;
        }
        Insets in = getInsets();
        width = getWidth() - in.left - in.right;
        height = getHeight() - in.top - in.bottom;
        yMax = in.top + height - 1;
        xKnobCenter = in.left + width / 2;
        yKnobCenter = in.top + height / 2 - 2;  // -2 : leave more room for the text
        repaint();
    }

    private void openEditor()
    {
        if (TEXT_EDIT_DIALOG == null)
        {
            TEXT_EDIT_DIALOG = new FlatTextEditDialog(WindowManager.getDefault().getMainWindow(), true);
            TEXT_EDIT_DIALOG.setBackground(colorKnobFill);
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
     * Get the value from the specified text.
     *
     * @param text
     * @return -1 if no valid value found in text.
     */
    protected int stringToValue(String text)
    {
        int r = -1;
        try
        {
            r = Integer.parseUnsignedInt(text);
            r = Math.max(minValue, r);
            r = Math.min(maxValue, r);
        } catch (NumberFormatException e)
        {
            // Nothing leave value unchanged
        }
        if (r == -1)
        {
            LOGGER.fine("parseString() text=" + text);   //NOI18N
        }
        return r;
    }

    /**
     * Get the String reprensentation of the specified value.
     * <p>
     * By default return String.valueOf(getValue()).
     *
     * @param v
     * @return The text used to represent the value in the knob.
     */
    protected String valueToString(int v)
    {
        return String.valueOf(v);
    }

    private void updateToolTipText()
    {
        String valueAstring = isEnabled() ? valueToString(value) : "OFF";
        setToolTipText(tooltipLabel == null ? valueAstring : tooltipLabel + "=" + valueAstring);
    }

}

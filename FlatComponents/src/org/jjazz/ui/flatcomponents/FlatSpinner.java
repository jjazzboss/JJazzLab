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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import static org.jjazz.ui.flatcomponents.Bundle.CTL_Tooltip;
import org.jjazz.uisettings.GeneralUISettings;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 * A text and a value. Value can be changed using mousewheel or edited with click (if isEditable is set).
 *
 * @param <E>
 */
@NbBundle.Messages(
        {
            "CTL_Tooltip=Use mouse wheel to change value, ctrl+wheel for fast change, double-click to edit."
        })
public class FlatSpinner<E> extends javax.swing.JPanel implements PropertyChangeListener
{

    /**
     * Client Property: default border
     */
    public final static String PROP_BORDER_DEFAULT = "PropBorderDefault";
    /**
     * Client Property: border used when mouse entered the component
     */
    public final static String PROP_BORDER_ENTERED = "PropBorderEntered";
    /**
     * Client Property: background of the editor
     */
    public final static String PROP_EDITOR_BACKGROUND = "PropEditorBackground";
    public static final String PROP_VALUE = "PropValue";
    private static final int VALUE_X_POS = 50;
    private int valueXpos;
    private int valueIndex = -1;
    private ArrayList<E> possibleValues;
    private static FlatTextEditDialog TEXT_EDIT_DIALOG;
    private int nbMaxChars = 1;
    private boolean isEditable = false;
    private static final Logger LOGGER = Logger.getLogger(FlatSpinner.class.getSimpleName());

    public FlatSpinner()
    {
        putClientProperty(PROP_BORDER_DEFAULT, BorderFactory.createEmptyBorder(1, 1, 1, 1));
        putClientProperty(PROP_BORDER_ENTERED, BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        putClientProperty(PROP_EDITOR_BACKGROUND, Color.WHITE);
        initComponents();
        setBorder(getBorder(PROP_BORDER_DEFAULT));
        setValueXpos(VALUE_X_POS);
        addPropertyChangeListener(this);
        // Use mouse wheel only if enabled
        GeneralUISettings.getInstance().installChangeValueWithMouseWheelSupport(this, evt -> mouseWheelChanged(evt));

    }

    /**
     * Prevent excessive vertical resizing : max height is set to preferred height.
     *
     * @return
     */
    @Override

    public Dimension getMaximumSize()
    {
        Dimension p = getPreferredSize();
        Dimension m = super.getMaximumSize();
        return new Dimension(m.width, p.height);
    }

    public void setLabelText(String text)
    {
        lbl_text.setText(text);
    }

    public String getLabelText()
    {
        return lbl_text.getText();
    }

    /**
     * Overridden to update our components.
     *
     * @param c
     */
    @Override
    public void setForeground(Color c)
    {
        super.setForeground(c);
        if (lbl_text != null)
        {
            lbl_text.setForeground(c);
        }
        if (lbl_value != null)
        {
            lbl_value.setForeground(c);
        }
    }

    /**
     * If true, clicking on the component opens a value editor.
     *
     * @param b
     */
    public void setEditable(boolean b)
    {
        isEditable = b;
    }

    public boolean isEditable()
    {
        return isEditable;
    }

    /**
     *
     * @return Null if possibleValues have not been set.
     */
    public E getValue()
    {
        return possibleValues == null ? null : possibleValues.get(valueIndex);
    }

    public void setValue(E value)
    {
        if (possibleValues == null)
        {
            throw new IllegalStateException("possibleValues=" + possibleValues + " value=" + value);
        }
        int index = possibleValues.indexOf(value);
        if (index == -1)
        {
            throw new IllegalArgumentException("value=" + value + " possibleValues=" + possibleValues);
        }
        if (valueIndex != index)
        {
            E oldValue = valueIndex == -1 ? null : possibleValues.get(valueIndex);
            valueIndex = index;
            E newValue = possibleValues.get(valueIndex);
            lbl_value.setText(newValue.toString());
            firePropertyChange(PROP_VALUE, oldValue, newValue);
        }
    }

    /**
     * The relative x position in pixels where the value should start.
     *
     * @return
     */
    public int getValueXpos()
    {
        return valueXpos;
    }

    /**
     * Set the relative x position in pixels where the value should start. Can not be less than 10 pixels.
     */
    public final void setValueXpos(int valueXpos)
    {
        if (valueXpos < 0 || valueXpos > 1000)
        {
            throw new IllegalArgumentException("valueXpos=" + valueXpos);
        }
        this.valueXpos = Math.max(valueXpos, 10);
        Dimension d = lbl_text.getPreferredSize();
        Dimension d2 = new Dimension(valueXpos - filler1.getWidth(), d.height);
        lbl_text.setPreferredSize(d2);
        lbl_text.setMinimumSize(d2);
        lbl_text.setMaximumSize(d2);
        revalidate();
    }

    /**
     * The possible values shown by the spinner.
     *
     * @param values Can not be empty.
     */
    public void setPossibleValues(List<E> values)
    {
        if (values == null || values.isEmpty())
        {
            throw new IllegalArgumentException("values=" + values);
        }
        if (possibleValues == null)
        {
            possibleValues = new ArrayList<>();
        }
        possibleValues.clear();
        for (E value : values)
        {
            possibleValues.add(value);
            nbMaxChars = Math.max(nbMaxChars, value.toString().length());
        }
        setValue(possibleValues.get(0));
    }

    public List<E> getPossibleValues()
    {
        return possibleValues;
    }

    /**
     * If a tooltip text is set on the component, it is returned if mouse is on the text label, otherwise a generic text "use
     * mouse wheel to change value" is used.
     *
     * @param e
     * @return
     */
    @Override
    public String getToolTipText(MouseEvent e)
    {
        Component c = getComponentAt(e.getPoint());
        if (c == lbl_text)
        {
            return super.getToolTipText(e);
        } else
        {
            return CTL_Tooltip();
        }
    }

    /**
     * Should be overridden by subclass. This class implementation throw an UnsupportedOperationException.
     *
     * @param s
     * @return Null if no valid value could be created from s.
     */
    public E stringToValue(String s)
    {
        throw new UnsupportedOperationException();
    }

    // ================================================================================
    // PropertyChangeListener interface
    // ================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        String key = evt.getPropertyName();
        if (key == PROP_BORDER_DEFAULT || key == PROP_BORDER_ENTERED)
        {
            repaint();
        }
    }

    // ================================================================================
    // Private functions
    // ================================================================================
    private Border getBorder(String key)
    {
        return (Border) getClientProperty(key);
    }

    private Color getColor(String key)
    {
        return (Color) getClientProperty(key);
    }

    private void mouseWheelChanged(MouseWheelEvent evt)
    {
        LOGGER.fine("mouseWheelChanged() " + evt.getWheelRotation());
        if (possibleValues == null)
        {
            return;
        }
        boolean ctrl = (evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
        int step = ctrl ? 5 : 1;
        if (evt.getWheelRotation() < 0)
        {
            int nextIndex = valueIndex + step;
            if (nextIndex >= possibleValues.size())
            {
                nextIndex = 0;
            }
            E value = possibleValues.get(nextIndex);
            this.setValue(value);
        } else if (evt.getWheelRotation() > 0)
        {
            int previousIndex = valueIndex - step;
            if (previousIndex < 0)
            {
                previousIndex = possibleValues.size() - 1;
            }
            E value = possibleValues.get(previousIndex);
            this.setValue(value);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        lbl_text = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(4, 0), new java.awt.Dimension(4, 0), new java.awt.Dimension(10, 0));
        lbl_value = new javax.swing.JLabel();

        addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                formMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt)
            {
                formMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt)
            {
                formMouseExited(evt);
            }
        });
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));

        lbl_text.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        lbl_text.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(lbl_text, "label"); // NOI18N
        add(lbl_text);
        add(filler1);

        lbl_value.setFont(new java.awt.Font("Tahoma", 1, 10)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lbl_value, "1"); // NOI18N
        lbl_value.setMaximumSize(new java.awt.Dimension(300, 13));
        add(lbl_value);
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_formMouseEntered
    {//GEN-HEADEREND:event_formMouseEntered
        LOGGER.fine("formMouseEntered()");
        setBorder(getBorder(PROP_BORDER_ENTERED));
    }//GEN-LAST:event_formMouseEntered

    private void formMouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_formMouseExited
    {//GEN-HEADEREND:event_formMouseExited
        LOGGER.fine("formMouseExited()");
        setBorder(getBorder(PROP_BORDER_DEFAULT));
    }//GEN-LAST:event_formMouseExited

    private void formMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_formMouseClicked
    {//GEN-HEADEREND:event_formMouseClicked
        if (isEditable && evt.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(evt))
        {
            LOGGER.fine("formMouseClicked() simple-click");
            if (TEXT_EDIT_DIALOG == null)
            {
                TEXT_EDIT_DIALOG = new FlatTextEditDialog(WindowManager.getDefault().getMainWindow(), true);
            }
            TEXT_EDIT_DIALOG.setFont(lbl_value.getFont());
            TEXT_EDIT_DIALOG.setForeground(lbl_value.getForeground());
            TEXT_EDIT_DIALOG.setBackground(getColor(PROP_EDITOR_BACKGROUND));
            TEXT_EDIT_DIALOG.setText(lbl_value.getText());
            TEXT_EDIT_DIALOG.setColumns(nbMaxChars - 1);
            Rectangle r = lbl_value.getBounds();
            Point p = r.getLocation();
            SwingUtilities.convertPointToScreen(p, lbl_value.getParent());
            int x = p.x - TEXT_EDIT_DIALOG.getLeftMargin();
            int y = p.y - TEXT_EDIT_DIALOG.getTopMargin();
            TEXT_EDIT_DIALOG.setLocation(Math.max(x, 0), Math.max(y, 0));
            TEXT_EDIT_DIALOG.setVisible(true);
            if (TEXT_EDIT_DIALOG.isExitOk())
            {
                String text = TEXT_EDIT_DIALOG.getText().trim();
                E value = stringToValue(text);
                if (value == null || !possibleValues.contains(value))
                {
                    LOGGER.fine("formMouseClicked() invalid value : " + value);
                    Toolkit.getDefaultToolkit().beep();
                } else
                {
                    setValue(value);
                }
            }
        }
    }//GEN-LAST:event_formMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.JLabel lbl_text;
    private javax.swing.JLabel lbl_value;
    // End of variables declaration//GEN-END:variables
}

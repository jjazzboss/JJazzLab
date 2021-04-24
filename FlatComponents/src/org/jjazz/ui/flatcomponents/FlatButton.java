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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.border.Border;

/**
 * A very simple button using a JLabel.
 */
public class FlatButton extends JLabel implements MouseListener, PropertyChangeListener
{

    private Border borderDefault;
    private Border borderEntered;
    private Border borderPressed;
    private Color saveForeground;
    private String saveTooltip;
    private Action action;
    private ArrayList<ActionListener> listeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(FlatButton.class.getSimpleName());

    public FlatButton()
    {
        borderDefault = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        borderEntered = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
        borderPressed = BorderFactory.createLineBorder(Color.GRAY, 1);
        setBorder(borderDefault);
        addMouseListener(this);
    }

    public FlatButton(Action a)
    {
        this();
        setAction(a);
    }

    /**
     * Add an ActionListener which will be called each time button is pressed.
     * <p>
     *
     * @param l
     */
    public void addActionListener(ActionListener l)
    {
        if (!listeners.contains(l))
        {
            listeners.add(l);
        }
    }

    public void removeActionListener(ActionListener l)
    {
        listeners.remove(l);
    }

    /**
     * Configure the button to use the below properties and listen to their changes.
     * <p>
     * - NAME &gt; setText()<br>
     * - SHORT_DESCRIPTION &gt; setTooltipText()<br>
     * - SMALL_ICON &gt; setIcon()<br>
     * - enabled &gt; setEnabled()
     * <p>
     * If button is pressed it calls action's actionPerformed() (in addition to the ActionListeners).
     * <p>
     * If property "hideActionText" is true, ignore the NAME property.
     *
     * @param a A non-null Action.
     */
    public void setAction(Action a)
    {
        if (a == null)
        {
            throw new NullPointerException("a");   //NOI18N
        }
        if (action != null)
        {
            action.removePropertyChangeListener(this);
        }
        action = a;
        action.addPropertyChangeListener(this);
        setIcon((Icon) action.getValue(Action.SMALL_ICON));
        if ((Boolean) action.getValue("hideActionText") != Boolean.TRUE)
        {
            setText((String) action.getValue(Action.NAME));
        }
        setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
        setEnabled(action.isEnabled());
    }

    /**
     * The Action associated to this button.
     *
     * @return Null if no action associated.
     */
    public Action getAction()
    {
        return action;
    }

    /**
     * @return the borderDefault
     */
    public Border getBorderDefault()
    {
        return borderDefault;
    }

    /**
     * @param borderDefault the borderDefault to set
     */
    public void setBorderDefault(Border borderDefault)
    {
        Border old = this.borderDefault;
        this.borderDefault = borderDefault;
        if (getBorder().equals(old))
        {
            setBorder(this.borderDefault);
        }
    }

    /**
     * @return the borderEntered
     */
    public Border getBorderEntered()
    {
        return borderEntered;
    }

    /**
     * @param borderEntered the borderEntered to set
     */
    public void setBorderEntered(Border borderEntered)
    {
        Border old = this.borderEntered;
        this.borderEntered = borderEntered;
        if (getBorder().equals(old))
        {
            setBorder(this.borderEntered);
        }
    }

    /**
     * @return the borderPressed
     */
    public Border getBorderPressed()
    {
        return borderPressed;
    }

    /**
     * @param borderPressed the borderPressed to set
     */
    public void setBorderPressed(Border borderPressed)
    {
        Border old = this.borderPressed;
        this.borderPressed = borderPressed;
        if (getBorder().equals(old))
        {
            setBorder(this.borderPressed);
        }
    }

    @Override
    public void setEnabled(boolean b)
    {
        LOGGER.fine("setEnabled() b=" + b);   //NOI18N
        if (isEnabled() && !b)
        {
            saveForeground = getForeground();
            setForeground(Color.LIGHT_GRAY);
            saveTooltip = getToolTipText();
            setToolTipText("OFF");
            setBorder(borderDefault);
        } else if (!isEnabled() && b)
        {
            setForeground(saveForeground);
            setToolTipText(saveTooltip);
        }
        super.setEnabled(b);
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == action)
        {
            if (evt.getPropertyName() == Action.SMALL_ICON)
            {
                setIcon((Icon) evt.getNewValue());
            } else if (evt.getPropertyName() == Action.NAME)
            {
                if ((Boolean) action.getValue("hideActionText") != Boolean.TRUE)
                {
                    setText((String) evt.getNewValue());
                }
            } else if (evt.getPropertyName() == Action.SHORT_DESCRIPTION)
            {
                setToolTipText((String) evt.getNewValue());
            } else if (evt.getPropertyName() == "enabled")
            {
                setEnabled((boolean) evt.getNewValue());
            }
        }
    }

    // ================================================================================
    // MouseListener interface
    // ================================================================================
    @Override
    public void mouseClicked(java.awt.event.MouseEvent evt)
    {
        // Need to be on mouseClicked, not mousePressed() otherwise cause problems when action triggers a dialog sensitive to mouseevents
        if (isEnabled())
        {
            setBorder(borderPressed);
            buttonClicked(evt);
        }
    }

    @Override
    public void mouseExited(java.awt.event.MouseEvent evt)
    {
        if (isEnabled())
        {
            setBorder(borderDefault);
        }
    }

    @Override
    public void mouseEntered(java.awt.event.MouseEvent evt)
    {
        if (isEnabled())
        {
            setBorder(borderEntered);
        }
    }

    @Override
    public void mousePressed(MouseEvent evt)
    {
        // Nothing
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (isEnabled() && getBorder() == borderPressed)
        {
            setBorder(borderEntered);
        }
    }

    // ================================================================================
    // Private functions
    // ================================================================================
    /**
     * Notify action listeners and perform the action if present.
     * <p>
     *
     * @param e
     */
    protected void buttonClicked(MouseEvent e)
    {
        ActionEvent ae = new ActionEvent(this, 0, "Click", e.getModifiersEx());
        fireActionPerformed(ae);
        if (action != null)
        {
            action.actionPerformed(ae);
        }
    }

    /**
     * Notify the ChangeListeners.
     *
     * @param e
     */
    protected void fireActionPerformed(ActionEvent e)
    {
        for (ActionListener l : listeners)
        {
            l.actionPerformed(e);
        }
    }
}

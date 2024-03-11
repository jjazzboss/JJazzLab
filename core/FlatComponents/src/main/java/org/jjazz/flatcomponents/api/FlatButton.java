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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.border.Border;

/**
 * A very simple button using a JLabel.
 */
public class FlatButton extends JLabel implements PropertyChangeListener
{

    private Action action;
    private ArrayList<ActionListener> listeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(FlatButton.class.getSimpleName());

    /**
     * Equivalent of FlatButton(true, true, false)
     */
    public FlatButton()
    {
        this(true, true, false);
    }

    /**
     * Equivalent to FlatButton(null, enablePressedBorder, enableEnteredBorder, enableDrag).
     * <p>
     * @param enablePressedBorder
     * @param enableEnteredBorder
     * @param enableDrag
     */
    public FlatButton(boolean enablePressedBorder, boolean enableEnteredBorder, boolean enableDrag)
    {
        this(null, enablePressedBorder, enableEnteredBorder, enableDrag);
    }

    /**
     * Equivalent to FlatButton(a, true, true, false).
     *
     * @param a
     */
    public FlatButton(Action a)
    {
        this(a, true, true, false);
    }

    /**
     * Create a FlatButton with the specified settings.
     *
     * @param a If non null use this action to initialize the button.
     * @param enablePressedBorder True means a specific border is used when pressed.
     * @param enableEnteredBorder True means a specific border is used when entered.
     * @param enableDrag True means mouse drag is possible on this button.
     */
    public FlatButton(Action a, boolean enablePressedBorder, boolean enableEnteredBorder, boolean enableDrag)
    {
        BorderManager.getInstance().register(this, enablePressedBorder, enableEnteredBorder, enableDrag);
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                // Need to be on mouseClicked, not mousePressed() otherwise cause problems when action triggers a dialog sensitive to mouseevents
                if (isEnabled())
                {
                    buttonClicked(evt);
                }
            }
        });
        if (a != null)
        {
            setAction(a);
        }
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
     * - "JJazzDisabledIcon" &gt; setDisabledIcon()<br>
     * - "enabled" &gt; setEnabled()
     * <p>
     * If button is pressed it calls action's actionPerformed() (in addition to the ActionListeners).
     * <p>
     * If property "hideActionText" is true, ignore the NAME property.
     *
     * @param a A non-null Action.
     */
    public final void setAction(Action a)
    {
        if (a == null)
        {
            throw new NullPointerException("a");   
        }
        if (action != null)
        {
            action.removePropertyChangeListener(this);
        }
        action = a;
        action.addPropertyChangeListener(this);
        setIcon((Icon) action.getValue(Action.SMALL_ICON));
        setDisabledIcon((Icon) action.getValue("JJazzDisabledIcon"));
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
     * @return
     */
    public Border getBorderNothing()
    {
        return BorderManager.getInstance().getBorderNothing(this);
    }

    /**
     * @return the borderEntered
     */
    public Border getBorderEntered()
    {
        return BorderManager.getInstance().getBorderEntered(this);
    }

    /**
     * @return the borderPressed
     */
    public Border getBorderPressed()
    {
        return BorderManager.getInstance().getBorderPressed(this);
    }

    /**
     * Overridden to add "OFF" if disabled.
     *
     * @return
     */
    @Override
    public String getToolTipText()
    {
        String tt = super.getToolTipText();
        return isEnabled() ? tt : tt + " [OFF]";
    }


    public void setBorderEntered(Border b)
    {
        BorderManager.getInstance().setBorderEntered(this, b);
    }

    public void setBorderPressed(Border b)
    {
        BorderManager.getInstance().setBorderPressed(this, b);
    }

    public void setBorderNothing(Border b)
    {
        BorderManager.getInstance().setBorderNothing(this, b);
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == action)
        {
            if (evt.getPropertyName().equals(Action.SMALL_ICON))
            {
                setIcon((Icon) evt.getNewValue());
            } else if (evt.getPropertyName().equals(Action.NAME))
            {
                if (Boolean.TRUE.equals((Boolean) action.getValue("hideActionText")))
                {
                    setText((String) evt.getNewValue());
                }
            } else if (evt.getPropertyName().equals(Action.SHORT_DESCRIPTION))
            {
                setToolTipText((String) evt.getNewValue());
            } else if ("enabled".equals(evt.getPropertyName()))
            {
                setEnabled((boolean) evt.getNewValue());
            }
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

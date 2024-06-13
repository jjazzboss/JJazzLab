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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * Manage the border changes when a mouse is over registered (flat) components, and when component is enabled/disabled.
 * <p>
 * This manager allows to disable the border change on mouseEntered for a component if this is actually a mouse drag on another
 * component.
 */
public class BorderManager implements MouseListener, MouseMotionListener, PropertyChangeListener, AncestorListener
{

    public static final Border DEFAULT_BORDER_NOTHING = BorderFactory.createEmptyBorder(1, 1, 1, 1);
    public static final Border DEFAULT_BORDER_ENTERED = BorderFactory.createLineBorder(Color.GRAY, 1);
    public static final Border DEFAULT_BORDER_PRESSED = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);

    private static BorderManager INSTANCE;
    private Border defaultBorderNothing;
    private Border defaultBorderEntered;
    private Border defaultBorderPressed;
    private HashMap<JComponent, CompBorders> mapCompBorders;
    private JComponent draggedComponent = null;
    private static final Logger LOGGER = Logger.getLogger(BorderManager.class.getSimpleName());

    public static BorderManager getInstance()
    {
        synchronized (BorderManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new BorderManager();
            }
        }
        return INSTANCE;
    }

    private BorderManager()
    {
        mapCompBorders = new HashMap<>();
        defaultBorderNothing = DEFAULT_BORDER_NOTHING;
        defaultBorderEntered = DEFAULT_BORDER_ENTERED;
        defaultBorderPressed = DEFAULT_BORDER_PRESSED;
    }

    /**
     * Register the component with no special handling for mouse pressed or mouse enter/exit, and can not be dragged.
     *
     * @param component
     * @see register(JComponent, boolean, boolean, boolean)
     */
    public void register(JComponent component)
    {
        register(component, false, false, false);
    }

    /**
     * Register the component so this object will manage its border changes.
     *
     * @param component
     * @param enablePressedBorder if true border changes while button is pressed
     * @param enableEnteredBorder if true border changes when mouse enters/exits
     * @param enableDrag          if true this component can be dragged
     */
    public void register(JComponent component, boolean enablePressedBorder, boolean enableEnteredBorder, boolean enableDrag)
    {
        if (component == null)
        {
            throw new NullPointerException("component");   
        }
        CompBorders cb = mapCompBorders.get(component);
        if (cb == null)
        {
            component.addMouseListener(this);
            component.addPropertyChangeListener(this);
            component.addAncestorListener(this);    // Detect if component is no more visible
            if (enableDrag)
            {
                component.addMouseMotionListener(this);
            }
        } else if (enableDrag && !cb.enableDrag)
        {
            component.addMouseMotionListener(this);
        } else if (!enableDrag && cb.enableDrag)
        {
            component.removeMouseMotionListener(this);
        }

        cb = new CompBorders(enablePressedBorder, enableEnteredBorder, enableDrag);
        mapCompBorders.put(component, cb);
        component.setBorder(defaultBorderNothing);
    }

    public void unregister(JComponent component)
    {
        if (component == null)
        {
            throw new IllegalArgumentException("component=" + component);
        }
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
        component.removePropertyChangeListener(this);
        component.removeAncestorListener(this);
        mapCompBorders.remove(component);
    }

    /**
     * The border to be used for component c when not in pressed or entered state.
     *
     * @param c
     * @return Can be null
     */
    public Border getBorderNothing(JComponent c)
    {
        CompBorders cb = mapCompBorders.get(c);
        Border res = null;
        if (cb != null)
        {
            res = cb.borderNothing;
        }
        return res;
    }

    /**
     *
     * @param c
     * @param b
     */
    public final void setBorderNothing(JComponent c, Border b)
    {
        CompBorders cb = mapCompBorders.get(c);
        if (cb == null)
        {
            throw new IllegalArgumentException("c=" + c + " b=" + b + " mapCompBorders.keySet()=" + mapCompBorders.keySet());
        }
        if (c.getBorder() == cb.borderNothing)
        {
            c.setBorder(b);
        }
        cb.borderNothing = b;
    }

    /**
     * The border to be used for component c when in the pressed state.
     *
     * @param c
     * @return Can be null
     */
    public Border getBorderPressed(JComponent c)
    {
        CompBorders cb = mapCompBorders.get(c);
        Border res = null;
        if (cb != null)
        {
            res = cb.borderPressed;
        }
        return res;
    }

    /**
     *
     * @param c
     * @param b
     */
    public final void setBorderPressed(JComponent c, Border b)
    {
        CompBorders cb = mapCompBorders.get(c);
        if (cb == null)
        {
            throw new IllegalArgumentException("c=" + c + " b=" + b + " mapCompBorders.keySet()=" + mapCompBorders.keySet());
        }
        if (c.getBorder() == cb.borderPressed)
        {
            c.setBorder(b);
        }
        cb.borderPressed = b;
    }

    /**
     * The border to be used for component c when in the entered state.
     *
     * @param c
     * @return Can be null
     */
    public Border getBorderEntered(JComponent c)
    {
        CompBorders cb = mapCompBorders.get(c);
        Border res = null;
        if (cb != null)
        {
            res = cb.borderEntered;
        }
        return res;
    }

    /**
     *
     * @param c
     * @param b
     */
    public final void setBorderEntered(JComponent c, Border b)
    {
        CompBorders cb = mapCompBorders.get(c);
        if (cb == null)
        {
            throw new IllegalArgumentException("c=" + c + " b=" + b + " mapCompBorders.keySet()=" + mapCompBorders.keySet());
        }
        if (c.getBorder() == cb.borderEntered)
        {
            c.setBorder(b);
        }
        cb.borderEntered = b;
    }

    /**
     * The default border "nothing" to be used when no specific per-component border is set.
     *
     * @return the borderDefault
     */
    public Border getDefaultBorderNothing()
    {
        return defaultBorderNothing;
    }

    public final void setDefaultBorderNothing(Border b)
    {
        mapCompBorders.keySet().stream()
                .filter(c -> mapCompBorders.get(c).borderNothing == defaultBorderNothing)
                .forEach(c -> setBorderNothing(c, b));
        this.defaultBorderNothing = b;
    }

    /**
     * The default border "pressed" to be used when no specific per-component border is set.
     *
     * @return the borderDefault
     */
    public Border getDefaultBorderPressed()
    {
        return defaultBorderPressed;
    }

    public final void setDefaultBorderPressed(Border b)
    {
        mapCompBorders.keySet().stream()
                .filter(c -> mapCompBorders.get(c).borderPressed == defaultBorderPressed)
                .forEach(c -> setBorderPressed(c, b));
        this.defaultBorderPressed = b;
    }

    /**
     * The default border "entered" to be used when no specific per-component border is set.
     *
     * @return the borderDefault
     */
    public Border getDefaultBorderEntered()
    {
        return defaultBorderEntered;
    }

    /**
     * @param b the defaultBorderEntered to set
     */
    public final void setDefaultBorderEntered(Border b)
    {
        mapCompBorders.keySet().stream()
                .filter(c -> mapCompBorders.get(c).borderEntered == defaultBorderEntered)
                .forEach(c -> setBorderPressed(c, b));
        this.defaultBorderEntered = b;
    }

    // ==========================================================================
    // AncestorListener interface
    // ==========================================================================
    @Override
    public void ancestorAdded(AncestorEvent event)
    {
        // Nothing
    }

    @Override
    public void ancestorRemoved(AncestorEvent event)
    {
        // A registered component is no more visible (eg if shown in a JPopupMenu which was hidden), change its border
        JComponent jc = event.getComponent();

        if (!jc.isShowing())
        {
            var bc = mapCompBorders.get(jc);
            jc.setBorder(bc.borderNothing);
        }
    }

    @Override
    public void ancestorMoved(AncestorEvent event)
    {
        // Nothing
    }

    // ==========================================================================
    // MouseListener, MouseMotionListener  interface
    // ==========================================================================
    @Override
    public void mouseEntered(MouseEvent e)
    {
        if (draggedComponent != null)
        {
            return;
        }

        JComponent c = (JComponent) e.getSource();
        CompBorders bc = mapCompBorders.get(c);
        if (c.isEnabled() && bc.enableEntered)
        {
            c.setBorder(bc.borderEntered);
        }

    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        if (draggedComponent != null)
        {
            return;
        }

        JComponent c = (JComponent) e.getSource();
        CompBorders bc = mapCompBorders.get(c);
        if (c.isEnabled() && bc.enableEntered)
        {
            c.setBorder(bc.borderNothing);
        }

    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        // Nothing
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        JComponent c = (JComponent) e.getSource();
        CompBorders bc = mapCompBorders.get(c);
        if (c.isEnabled() && bc.enablePressed)
        {
            c.setBorder(bc.borderPressed);
        }
    }

    /**
     *
     * @param e Source component is always the original component for which mouse was initally pressed (event if release point is
     *          outside this component)
     */
    @Override
    public void mouseReleased(MouseEvent e)
    {
        JComponent c = (JComponent) e.getSource();
        if (draggedComponent != null)
        {
            // End of the drag: draggedComponent = c
            // Find if we landed on one of the managed components
            for (JComponent ci : mapCompBorders.keySet())
            {
                if (ci == c)
                {
                    // source component is managed below
                    continue;
                }
                MouseEvent e2 = SwingUtilities.convertMouseEvent(c, e, ci);
                CompBorders cb = mapCompBorders.get(ci);
                if (ci.contains(e2.getPoint()))
                {
                    if (cb.enableEntered)
                    {
                        ci.setBorder(defaultBorderEntered);
                        break;
                    }
                }
            }

            draggedComponent = null;
        }

        // Update the source component
        CompBorders cb = mapCompBorders.get(c);
        if (c.contains(e.getPoint()) && c.isEnabled() && cb.enableEntered)
        {
            c.setBorder(cb.borderEntered);
        } else
        {
            c.setBorder(cb.borderNothing);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        JComponent c = (JComponent) e.getSource();
        if (c.isEnabled())
        {
            draggedComponent = c;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        // nothing
    }

    // ----------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        // Listen to enable state changes
        if ("enabled".equals(e.getPropertyName()))
        {
            JComponent c = (JComponent) e.getSource();
            c.setBorder(mapCompBorders.get(c).borderNothing);
        }
    }


    // ===============================================================================
    // Inner classes
    // ===============================================================================
    private class CompBorders
    {

        boolean enablePressed, enableEntered, enableDrag;
        Border borderPressed, borderNothing, borderEntered;

        CompBorders(boolean enablePressedBorder, boolean enableEnteredBorder, boolean enableDrag)
        {
            this.enablePressed = enablePressedBorder;
            this.enableEntered = enableEnteredBorder;
            this.enableDrag = enableDrag;
            this.borderNothing = defaultBorderNothing;
            this.borderPressed = defaultBorderPressed;
            this.borderEntered = defaultBorderEntered;
        }
    }

}

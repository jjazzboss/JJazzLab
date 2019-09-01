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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

/**
 * Manage the UI changes when a mouse is over our flat (enabled) components.
 */
public class FlatHoverManager implements MouseListener, MouseMotionListener
{

    private static FlatHoverManager INSTANCE;
    private Border borderDefault;
    private Border borderEntered;
    private ArrayList<JComponent> components;
    private JComponent draggedComponent = null;
    private static final Logger LOGGER = Logger.getLogger(FlatHoverManager.class.getSimpleName());

    public static FlatHoverManager getInstance()
    {
        synchronized (FlatHoverManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new FlatHoverManager();
            }
        }
        return INSTANCE;
    }

    private FlatHoverManager()
    {
        components = new ArrayList<>();
        setBorderDefault(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        setBorderEntered(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
    }

    public void associate(JComponent component)
    {
        if (component == null)
        {
            throw new NullPointerException("component");
        }
        if (!components.contains(component))
        {
            components.add(component);
            component.setBorder(getBorderDefault());
            component.addMouseListener(this);
            component.addMouseMotionListener(this);
        }
    }

    public void unassociate(JComponent component)
    {
        if (component == null || !components.contains(component))
        {
            throw new IllegalArgumentException("component=" + component + " components=" + components);
        }
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
        components.remove(component);
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
    public final void setBorderDefault(Border borderDefault)
    {
        this.borderDefault = borderDefault;
        for (JComponent c : components)
        {
            c.repaint();
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
    public final void setBorderEntered(Border borderEntered)
    {
        this.borderEntered = borderEntered;
        for (JComponent c : components)
        {
            c.repaint();
        }
    }

    // ==========================================================================
    // MouseListener, MouseMotionListener  interface
    // ==========================================================================
    @Override
    public void mouseEntered(MouseEvent e)
    {
        if (draggedComponent == null)
        {
            JComponent c = (JComponent) e.getSource();
            if (c.isEnabled())
            {
                c.setBorder(getBorderEntered());
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        if (draggedComponent == null)
        {
            JComponent c = (JComponent) e.getSource();
            if (c.isEnabled())
            {
                c.setBorder(getBorderDefault());
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        // 
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        //
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (draggedComponent != null)
        {
            // Restore borders of our managed components
            for (JComponent c : components)
            {
                MouseEvent e2 = SwingUtilities.convertMouseEvent(draggedComponent, e, c);
                c.setBorder(c.contains(e2.getPoint()) ? getBorderEntered() : getBorderDefault());
            }
            draggedComponent = null;
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
        // 
    }

}

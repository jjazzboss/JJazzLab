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
package org.jjazz.uiutilities.api;

import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.SwingUtilities;

/**
 * A MouseAdapter that transmits MouseEvents to a parent.
 */
public class RedispatchingMouseAdapter implements MouseListener, MouseWheelListener, MouseMotionListener
{

    private Container toParent;
    private final boolean keepEventSource;

    /**
     * Will dispatch to the direct parent with event source changed to direct parent.
     */
    public RedispatchingMouseAdapter()
    {
        this(null, false);
    }

    /**
     * Dispatch to a specific parent.
     *
     * @param toParent        Must be in the parent hierarchy. If null redirect to direct parent.
     * @param keepEventSource If true, redispatched event source is not set to toParent
     */
    public RedispatchingMouseAdapter(Container toParent, boolean keepEventSource)
    {
        this.toParent = toParent;
        this.keepEventSource = keepEventSource;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        redispatchToParent(e);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        redispatchToParent(e);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        redispatchToParent(e);
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        redispatchToParent(e);
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        redispatchToParent(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        redispatchToParent(e);
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        redispatchToParent(e);
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        redispatchToParent(e);
    }

    protected void redispatchToParent(MouseEvent e)
    {
        Container source = (Container) e.getSource();
        if (toParent == null)
        {
            toParent = source.getParent();
        }

        MouseEvent event = keepEventSource ? UIUtilities.convertMouseEventKeepEventSource(source, e, toParent)
                : SwingUtilities.convertMouseEvent(source, e, toParent);
        toParent.dispatchEvent(event);
    }

}

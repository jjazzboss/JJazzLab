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
package org.jjazz.test.rhythm;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.rpviewer.api.RpViewer;
import org.jjazz.ui.rpviewer.api.RpViewerController;
import org.jjazz.ui.rpviewer.api.RpViewerEditableRenderer;

/**
 *
 * @author Administrateur
 */
public class RP_TestViewerEditableRenderer extends MouseAdapter implements RpViewerEditableRenderer, PropertyChangeListener
{

    @Override
    public void setController(RpViewerController controller)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setRpViewer(RpViewer rpv)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RpViewer getRpViewer()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Dimension getPreferredSize()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void paintComponent(Graphics g)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addChangeListener(ChangeListener l)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeChangeListener(ChangeListener l)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    RpViewerController controller;
//    RpViewer rpViewer;
//    SongPart spt;
//    RP_Test rp;
//    RP_TestValue value;
//    RP_TestValue dragStartValue;
//    List<ChangeListener> listeners = new ArrayList<>();
//    private static final Logger LOGGER = Logger.getLogger(RP_TestViewerEditableRenderer.class.getSimpleName());
//
//    public RP_TestViewerEditableRenderer(RP_Test rp)
//    {
//        this.rp = rp;
//    }
//
//    @Override
//    public void mouseDragged(MouseEvent e)
//    {
//        if (dragStartValue == null)
//        {
//            dragStartValue = computeValue(e.getPoint());
//        }
//        RP_TestValue v = computeValue(e.getPoint());
//        value.setBassDrumOffset(v.getBassDrumOffset());
//        value.setSnareOffset(v.getSnareOffset());
//        fireChanged();
//    }
//
//    @Override
//    public void mouseReleased(MouseEvent e)
//    {
//        if (controller!=null && dragStartValue != null)
//        {
//            controller.rhythmParameterEdit(spt, rp, value);
//            dragStartValue = null;
//        }
//    }
//
//    @Override
//    public void setController(RpViewerController controller)
//    {
//        this.controller = controller;
//    }
//
//    @Override
//    public void setRpViewer(RpViewer rpv)
//    {
//        this.rpViewer = rpv;
//        spt = rpViewer.getSptModel();
//        if (rpViewer.getRpModel() != rp)
//        {
//            throw new IllegalArgumentException("rpv=" + rpv);
//        }
//        spt.addPropertyChangeListener(this);
//        value = spt.getRPValue(rp);
//
//    }
//
//    @Override
//    public RpViewer getRpViewer()
//    {
//        return rpViewer;
//    }
//
//    @Override
//    public Dimension getPreferredSize()
//    {
//        return new Dimension(64, 64);
//    }
//
//    @Override
//    public void paintComponent(Graphics g)
//    {
//        Graphics2D g2 = (Graphics2D) g;
//        int w = rpViewer.getWidth();
//        int h = rpViewer.getHeight();
//        g2.setColor(Color.red);
//
//        if (value == null)
//        {
//            g2.drawRect(5, 5, 10, 10);
//        } else
//        {
//            float vx = value.getBassDrumOffset();
//            float vy = value.getSnareOffset();
//            int x = (int) (vx / 64 * w);
//            int y = (int) (vy / 64 * h);
//            g2.fillOval(x, y, 5, 5);
//        }
//    }
//
//    @Override
//    public void addChangeListener(ChangeListener l)
//    {
//        if (!listeners.contains(l))
//        {
//            listeners.add(l);
//        }
//    }
//
//    @Override
//    public void removeChangeListener(ChangeListener l)
//    {
//        listeners.remove(l);
//    }
//
//    @Override
//    public void propertyChange(PropertyChangeEvent evt)
//    {
//        if (evt.getSource() == spt
//                && evt.getPropertyName().equals(SongPart.PROPERTY_RP_VALUE)
//                && evt.getOldValue().equals(rp))
//        {
//            value = (RP_TestValue) evt.getNewValue();
//            fireChanged();
//        }
//    }
//
//    private void fireChanged()
//    {
//        listeners.forEach(l -> l.stateChanged(new ChangeEvent(this)));
//    }
//
//    private RP_TestValue computeValue(Point p)
//    {
//        int bass = (int) (((float) p.x / rpViewer.getWidth()) * 64);
//        int snare = (int) (((float) p.y / rpViewer.getHeight()) * 64);
//        RP_TestValue res = new RP_TestValue(bass, snare, 0);
//        return res;
//    }

}

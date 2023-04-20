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
package org.jjazz.leadsheet.chordleadsheet.item;

import com.google.common.base.Preconditions;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.util.api.StringProperties;

/**
 * An item for a bar annotation.
 */
public class CLI_BarAnnotationImpl implements CLI_BarAnnotation, WritableItem<String>, Serializable
{

    /**
     * Position of the item.
     */
    private Position position = new Position(0, 0);
    /**
     * The data section.
     */
    private String data;

    private final StringProperties clientProperties;
    /**
     * The container of this item. Need to be transient otherwise this introduces circularities in the objects graph that prevent
     * ChordLeadSheetImpl's proxyserialization to work. This field must be restored by its container at deserialization.
     */
    private transient ChordLeadSheet container = null;

    /**
     * The listeners for changes in this ChordLeadSheetItem.
     */
    private final transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(CLI_BarAnnotationImpl.class.getSimpleName());

    /**
     * @param annotation
     * @param barIndex
     */
    public CLI_BarAnnotationImpl(String annotation, int barIndex)
    {
        Preconditions.checkNotNull(annotation);
        Preconditions.checkArgument(barIndex >= 0, "barIndex=%d", barIndex);

        data = annotation;
        position = new Position(barIndex, 0);
        clientProperties = new StringProperties(this);
    }

    @Override
    synchronized public void setContainer(ChordLeadSheet cls)
    {
        if (cls != container)
        {
            ChordLeadSheet old = container;
            container = cls;
            pcs.firePropertyChange(PROP_CONTAINER, old, container);
        }
    }

    @Override
    public StringProperties getClientProperties()
    {
        return clientProperties;
    }

    @Override
    public synchronized String getData()
    {
        return data;
    }

    @Override
    public synchronized void setData(String annotation)
    {
        Preconditions.checkNotNull(annotation);
        if (!annotation.equals(data))
        {
            String oldData = data;
            data = annotation;
            pcs.firePropertyChange(PROP_ITEM_DATA, oldData, data);
        }
    }

    @Override
    public boolean isBarSingleItem()
    {
        return true;
    }

    @Override
    public synchronized CLI_BarAnnotation getCopy(ChordLeadSheet newCls, Position newPos)
    {
        int barIndex = (newPos != null) ? newPos.getBar() : position.getBar();
        ChordLeadSheet cls = (newCls != null) ? newCls : getContainer();
        CLI_BarAnnotationImpl cli = new CLI_BarAnnotationImpl(data, barIndex);
        cli.setContainer(cls);
        return cli;
    }

    /*
     * equals() and hashCode() are NOT defined because they can be used as Map keys and can change while being
     * in the map (InstanceContent for selected items in the CL_Editor, or ChordLeadSheet.moveSection())
     */
//    @Override
//    public boolean equals(Object o)
//    {
//        if (o instanceof CLI_Section)
//        {
//            CLI_Section cli = (CLI_Section) o;
//            return container == cli.getContainer() && data.equals(cli.getData()) && position.equals(cli.getPosition());
//        }
//        else
//        {
//            return false;
//        }
//    }
//
//    @Override
//    public int hashCode()
//    {
//        int hash = 3;
//        hash = 37 * hash + (this.container != null ? this.container.hashCode() : 0);
//        hash = 37 * hash + (this.position != null ? this.position.hashCode() : 0);
//        hash = 37 * hash + (this.data != null ? this.data.hashCode() : 0);
//        return hash;
//    }
    @Override
    public String toString()
    {
        return "" + getData() + getPosition();
    }

    /**
     * Get the position of this item.
     *
     * @return
     */
    @Override
    public synchronized Position getPosition()
    {
        return new Position(position);
    }

    /**
     * Set the position of this item.
     *
     * @param p
     */
    @Override
    public synchronized void setPosition(Position p)
    {
        if (position == null)
        {
            throw new NullPointerException("p=" + p);
        }
        if (!position.equals(p))
        {
            Position oldPos = position;
            position = new Position(p);
            pcs.firePropertyChange(PROP_ITEM_POSITION, oldPos, position);
        }
    }

    @Override
    public synchronized ChordLeadSheet getContainer()
    {
        return container;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    /**
     * Remove a listener for position or content changes.
     *
     * @param l
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ------------------------------------------------------------------------------
    // Implementation of interface Transferable
    // ------------------------------------------------------------------------------
    private static DataFlavor flavor = CLI_BarAnnotation.DATA_FLAVOR;

    private static DataFlavor[] supportedFlavors =
    {
        flavor,
        DataFlavor.stringFlavor
    };

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return supportedFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor fl)
    {
        return fl.equals(flavor) || fl.equals(DataFlavor.stringFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor fl) throws UnsupportedFlavorException
    {
        if (fl.equals(flavor))
        {
            return this;
        } else if (fl.equals(DataFlavor.stringFlavor))
        {
            return toString();
        } else
        {
            throw new UnsupportedFlavorException(fl);
        }
    }

    /* ---------------------------------------------------------------------
     * Serialization
     * --------------------------------------------------------------------- */
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -9872601287001L;
        private int spVERSION = 1;      // Do not make final!
        private String spAnnotation;    
        private int spBarIndex;
        private StringProperties spClientProperties;

        private SerializationProxy(CLI_BarAnnotationImpl cliBa)
        {
            spAnnotation = cliBa.getData(); 
            spBarIndex = cliBa.getPosition().getBar();
            spClientProperties = cliBa.getClientProperties();
        }

        private Object readResolve() throws ObjectStreamException
        {
            CLI_BarAnnotationImpl cli = new CLI_BarAnnotationImpl(spAnnotation, spBarIndex);
            return cli;
        }
    }
}

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
package org.jjazz.chordleadsheet.item;

import org.jjazz.chordleadsheet.api.item.WritableItem;
import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.PROP_ITEM_DATA;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.PROP_ITEM_POSITION;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.StringProperties;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * An item for a bar annotation.
 */
public class CLI_BarAnnotationImpl implements CLI_BarAnnotation, WritableItem<String>, Serializable
{

    /**
     * Position of the item.
     */
    private Position position = new Position(0);
    /**
     * The data section.
     */
    private String data;

    private final StringProperties clientProperties;
    /**
     * The container of this item.
     * <p>
     * Need to be transient otherwise this introduces circularities in the objects graph that prevent ChordLeadSheetImpl's proxyserialization to work. This
     * field must be restored by its container at deserialization.
     */
    private volatile transient ChordLeadSheet container = null;

    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(CLI_BarAnnotationImpl.class.getSimpleName());

    /**
     * @param annotation
     * @param barIndex
     */
    public CLI_BarAnnotationImpl(String annotation, int barIndex)
    {
        Preconditions.checkNotNull(annotation);
        Preconditions.checkArgument(barIndex >= 0, "barIndex=%s", barIndex);

        data = annotation;
        position = new Position(barIndex);
        clientProperties = new StringProperties(this);
    }

    @Override
    public ReentrantReadWriteLock getLock()
    {
        return container == null ? null : container.getLock();
    }

    @Override
    public final ChordLeadSheet getContainer()
    {
        readLock_lock();
        try
        {
            return container;
        } finally
        {
            readLock_unlock();
        }
    }

    @Override
    final public PropertyChangeEvent setContainer(ChordLeadSheet cls)
    {
        var res = CLI_SectionImpl.getVoidEvent(this);
        if (cls != container)
        {
            ChordLeadSheet old = container;
            container = cls;
            res = new PropertyChangeEvent(this, PROP_CONTAINER, old, container);
        }
        return res;
    }

    /**
     * Provide a consistent way to order CLI_BarAnnotation.
     */
    @Override
    public int compareToSamePosition(ChordLeadSheetItem<?> other)
    {
        Objects.requireNonNull(other);
        readLock_lock();
        try
        {
            Preconditions.checkArgument(other instanceof CLI_BarAnnotation && !equals(other), "this=%s other=%s", other);
            Preconditions.checkArgument(position.equals(other.getPosition()) && getPositionOrder() == other.getPositionOrder(), "this=%s other=%s", other);

            CLI_BarAnnotation otherBarAnnotation = (CLI_BarAnnotation) other;
            var res = data.compareTo(otherBarAnnotation.getData());

            return res;
        } finally
        {
            readLock_unlock();
        }
    }

    @Override
    public StringProperties getClientProperties()
    {
        return clientProperties;
    }

    @Override
    public String getData()
    {
        readLock_lock();
        try
        {
            return data;
        } finally
        {
            readLock_unlock();
        }
    }

    @Override
    public PropertyChangeEvent setData(String ecs)
    {
        Objects.requireNonNull(ecs);
        var res = CLI_SectionImpl.getVoidEvent(this);
        if (!ecs.equals(data))
        {
            String oldData = data;
            data = ecs;
            res = new PropertyChangeEvent(this, PROP_ITEM_DATA, oldData, data);
        }
        return res;
    }

    @Override
    public boolean isBarSingleItem()
    {
        return true;
    }

    @Override
    public CLI_BarAnnotation getCopy(String newData, Position newPos)
    {
        readLock_lock();
        try
        {
            int barIndex = (newPos != null) ? newPos.getBar() : position.getBar();
            CLI_BarAnnotationImpl cli = new CLI_BarAnnotationImpl(newData == null ? data : newData, barIndex);
            cli.getClientProperties().set(clientProperties);
            return cli;
        } finally
        {
            readLock_unlock();
        }
    }

    @Override
    public boolean equals(Object o)
    {
        return ChordLeadSheetItem.equals(this, o);
    }

    @Override
    public int hashCode()
    {
        return ChordLeadSheetItem.hashCode(this);
    }

    @Override
    public String toString()
    {
        readLock_lock();
        try
        {
            return "" + getData() + getPosition();
        } finally
        {
            readLock_unlock();
        }
    }

    @Override
    public final Position getPosition()
    {
        readLock_lock();
        try
        {
            return new Position(position);
        } finally
        {
            readLock_unlock();
        }
    }

    @Override
    public final PropertyChangeEvent setPosition(Position p)
    {
        Objects.requireNonNull(p);
        var res = CLI_SectionImpl.getVoidEvent(this);
        if (!position.equals(p))
        {
            Position oldPos = position;
            position = new Position(p);
            res = new PropertyChangeEvent(this, PROP_ITEM_POSITION, oldPos, position);
        }
        return res;
    }


    @Override
    public void firePropertyChangEvent(PropertyChangeEvent event)
    {
        pcs.firePropertyChange(event);
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

    @Override
    public int getPositionOrder()
    {
        return POSITION_ORDER;
    }

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files
                    xstream.alias("CLI_BarAnnotationImpl", CLI_BarAnnotationImpl.class);
                    xstream.alias("CLI_BarAnnotationImplSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spBarIndex");

                }

                case MIDIMIX_LOAD ->
                {
                    // Nothing
                }
                case MIDIMIX_SAVE ->
                {
                    // Nothing
                }
                default -> throw new AssertionError(instanceId.name());
            }
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

    /**
     * Serialization proxy.
     * <p>
     * spVERSION 2 (JJazzLab 4.1.0) introduces several aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction).
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -9872601287001L;
        private int spVERSION = 2;      // Do not make final!
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

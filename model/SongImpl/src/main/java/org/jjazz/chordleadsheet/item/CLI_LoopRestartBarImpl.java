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

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_LoopRestartBar;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.equalsThreadUnsafe;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.hashCodeThreadUnsafe;
import org.jjazz.chordleadsheet.api.item.WritableItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.StringProperties;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;


/**
 * A special item used to indicate the restart bar when playback is in loop mode.
 * <p>
 */
public class CLI_LoopRestartBarImpl implements CLI_LoopRestartBar, WritableItem<String>, Serializable
{

    private Position position;
    private final String data = "NotUsed";
    private final StringProperties clientProperties;
    private volatile transient ChordLeadSheetImpl container = null;


    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(CLI_LoopRestartBarImpl.class.getSimpleName());

    public CLI_LoopRestartBarImpl(int bar)
    {
        Preconditions.checkArgument(bar >= 0, "bar=%s", bar);
        clientProperties = new StringProperties(this);
        position = new Position(bar);
    }

    @Override
    public final PropertyChangeEvent setPosition(Position p)
    {
        Objects.requireNonNull(p);
        Position oldPos = position;
        position = new Position(p);
        var res = new PropertyChangeEvent(this, PROP_ITEM_POSITION, oldPos, position);
        return res;
    }

    @Override
    public PropertyChangeEvent setData(String data)
    {
        return ChordLeadSheetItem.getVoidEvent(this);
    }

    @Override
    final public void setContainer(ChordLeadSheet cls)
    {
        this.container = (ChordLeadSheetImpl) cls;
    }

    @Override
    public ChordLeadSheet getContainer()
    {
        return container;
    }

    @Override
    public String getData()
    {
        return data;
    }

    @Override
    public Position getPosition()
    {
        return position;
    }

    @Override
    public int getPositionOrder()
    {
        return POSITION_ORDER;
    }

    @Override
    public ChordLeadSheetItem getCopy(String newData, Position newPos)
    {
        return performReadAPImethod(() -> 
        {
            int barIndex = (newPos != null) ? newPos.getBar() : position.getBar();
            CLI_LoopRestartBarImpl cli = new CLI_LoopRestartBarImpl(barIndex);
            cli.getClientProperties().set(clientProperties);
            return cli;
        });
    }

    @Override
    public boolean isBarSingleItem()
    {
        return true;
    }

    @Override
    public StringProperties getClientProperties()
    {
        return clientProperties;
    }

    @Override
    public boolean equals(Object o)
    {
        return performReadAPImethod(() -> equalsThreadUnsafe(this, o));
    }

    @Override
    public int hashCode()
    {
        return performReadAPImethod(() -> hashCodeThreadUnsafe(this));
    }

    @Override
    public int compareTo(ChordLeadSheetItem<?> o)
    {
        return performReadAPImethod(() -> compareToThreadUnsafe(o));
    }

    @Override
    public int compareToSamePosition(ChordLeadSheetItem<?> other)
    {
        Objects.requireNonNull(other);
        return -1;
    }

    @Override
    public void firePropertyChangeEvent(PropertyChangeEvent event)
    {
        pcs.firePropertyChange(event);
    }


    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    /**
     * Execute a read operation using the read lock if a container is set.
     *
     * @param <T>
     * @param operation
     * @return
     */
    public <T> T performReadAPImethod(Supplier<T> operation)
    {
        if (container == null)
        {
            return operation.get();
        } else
        {
            return container.getExecutionManager().executeReadOperation(operation);
        }
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
                    // New CLI_LoopRestartBarImpl from 5.2
                    // aliases to get rid of fully qualified class names in .sng files
                    xstream.alias("CLI_LoopRestartBarImpl", CLI_LoopRestartBarImpl.class);
                    xstream.alias("CLI_LoopRestartBarImplSP", SerializationProxy.class);
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
     * spVERSION 1 (JJazzLab 5.1.2) introduces CLI_LoopRestartBarImpl
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -7260917204L;
        private int spVERSION = 1;      // Do not make final!
        private int spBarIndex;

        private SerializationProxy(CLI_LoopRestartBarImpl cli)
        {
            spBarIndex = cli.getPosition().getBar();
        }

        private Object readResolve() throws ObjectStreamException
        {
            CLI_LoopRestartBarImpl cli = new CLI_LoopRestartBarImpl(spBarIndex);
            return cli;
        }
    }
}

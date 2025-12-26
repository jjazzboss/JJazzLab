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
import com.thoughtworks.xstream.XStream;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.StringProperties;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * CLI_ChordSymbol implementation.
 */
public class CLI_ChordSymbolImpl implements CLI_ChordSymbol, WritableItem<ExtChordSymbol>, Serializable
{

    /**
     * Position of the item.
     */
    private Position position = new Position(0);
    /**
     * The data section.
     */
    private ExtChordSymbol data;
    private StringProperties clientProperties;
    /**
     * The container of this item.
     * <p>
     * Need to be transient otherwise this introduces circularities in the objects graph that prevent ChordLeadSheetImpl's proxy serialization to work. This
     * field must be restored by its container at deserialization.
     */
    private transient ChordLeadSheet container = null;
    /**
     * The listeners for changes in this ChordLeadSheetItem.
     */
    private transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(CLI_ChordSymbolImpl.class.getSimpleName());

    /**
     * Create an object from the specified arguments.
     * <p>
     * No alternate data and no filter set.
     *
     * @param cs
     * @param pos
     */
    public CLI_ChordSymbolImpl(ExtChordSymbol cs, Position pos)
    {
        if (cs == null || pos == null)
        {
            throw new NullPointerException(" cs=" + cs + " pos=" + pos);
        }
        data = cs;
        setPosition(pos);
        clientProperties = new StringProperties(this);
    }

    @Override
    final synchronized public void setContainer(ChordLeadSheet cls)
    {
        if (cls != container)
        {
            ChordLeadSheet old = container;
            container = cls;
            pcs.firePropertyChange(PROP_CONTAINER, old, container);
        }
    }

    /**
     * Set the position of this item.
     *
     * @param p
     */
    @Override
    public synchronized final void setPosition(Position p)
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
    public int getPositionOrder()
    {
        return POSITION_ORDER;
    }

    @Override
    public boolean isBarSingleItem()
    {
        return false;
    }

    @Override
    public StringProperties getClientProperties()
    {
        return clientProperties;
    }

    @Override
    public synchronized ExtChordSymbol getData()
    {
        return data;
    }

    @Override
    public synchronized void setData(ExtChordSymbol ecs)
    {
        Objects.requireNonNull(ecs);
        if (!ecs.equals(data))
        {
            ExtChordSymbol oldData = data;
            data = ecs;
            pcs.firePropertyChange(PROP_ITEM_DATA, oldData, data);
        }
    }

    /**
     * Note that client properties are also copied.
     *
     * @param newPos
     * @return
     */
    @Override
    public synchronized CLI_ChordSymbol getCopy(ExtChordSymbol newData, Position newPos)
    {
        CLI_ChordSymbolImpl cli = new CLI_ChordSymbolImpl(newData == null ? data : newData, (newPos != null) ? newPos : position);
        cli.getClientProperties().set(clientProperties);
        return cli;
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
        return "" + getData() + getPosition();
    }

    /**
     * Get the position of this item.
     *
     * @return
     */
    @Override
    public synchronized final Position getPosition()
    {
        return new Position(position);
    }

    @Override
    public synchronized final ChordLeadSheet getContainer()
    {
        return container;
    }

    @Override
    public final void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    /**
     * Remove a listener for position or content changes.
     *
     * @param l
     */
    @Override
    public final void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ------------------------------------------------------------------------------
    // Implementation of interface Transferable
    // ------------------------------------------------------------------------------
    private static DataFlavor flavor = CLI_ChordSymbol.DATA_FLAVOR;

    private static DataFlavor[] supportedFlavors =
    {
        flavor,
        DataFlavor.stringFlavor,
    };

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return supportedFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor fl)
    {
        if (fl.equals(flavor) || fl.equals(DataFlavor.stringFlavor))
        {
            return true;
        }
        return false;
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
                    xstream.alias("CLI_ChordSymbolImpl", CLI_ChordSymbolImpl.class);
                    xstream.alias("CLI_ChordSymbolSP", CLI_ChordSymbolImpl.SerializationProxy.class);

                }

                case MIDIMIX_LOAD ->
                {
                    // Nothing
                }
                case MIDIMIX_SAVE ->
                {
                    // Nothing
                }
                default ->
                    throw new AssertionError(instanceId.name());
            }
        }
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // ---------------------------------------------------------------------
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
     * spVERSION 2 changes some saved fields, see below.<br>
     * spVERSION 3 (JJazzLab 4.1.0) introduces several aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction).
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 909000172651524L;
        private int spVERSION = 3;          // Do not make final!
        private ExtChordSymbol spChord;
        private Position spPos;
        private StringProperties spClientProperties;  // From spVERSION 2        

        private SerializationProxy(CLI_ChordSymbolImpl cli)
        {
            spChord = cli.getData();
            spPos = cli.getPosition();
            spClientProperties = cli.getClientProperties();     // From spVERSION 2
        }

        private Object readResolve() throws ObjectStreamException
        {
            CLI_ChordSymbolImpl cli = new CLI_ChordSymbolImpl(spChord, spPos);
            if (spVERSION >= 2)
            {
                if (spClientProperties != null)
                {
                    cli.getClientProperties().set(spClientProperties);
                } else
                {
                    LOGGER.log(Level.WARNING, "SerializationProxy.readResolve() Unexpected null value for spClientProperties. spChord={0}",
                            spChord);
                }
            }
            return cli;
        }
    }

}

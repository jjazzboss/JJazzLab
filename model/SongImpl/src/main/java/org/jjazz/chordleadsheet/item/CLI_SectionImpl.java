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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.PROP_CONTAINER;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.equalsThreadUnsafe;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.hashCodeThreadUnsafe;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.StringProperties;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * An item for a section.
 */
public class CLI_SectionImpl implements CLI_Section, WritableItem<Section>, Serializable
{

    /**
     * Position of the item.
     */
    private Position position = new Position(0);
    /**
     * The data section.
     */
    private Section data;

    private StringProperties clientProperties;
    /**
     * The container of this item. Need to be transient otherwise this introduces circularities in the objects graph that prevent ChordLeadSheetImpl's
     * proxyserialization to work. This field must be restored by its container at deserialization.
     */
    private volatile transient ChordLeadSheet container = null;
    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(CLI_SectionImpl.class.getSimpleName());

    /**
     * @param sectionName
     * @param ts
     * @param barIndex
     */
    public CLI_SectionImpl(String sectionName, TimeSignature ts, int barIndex)
    {
        if (sectionName == null || sectionName.trim().isEmpty() || ts == null || barIndex < 0)
        {
            throw new IllegalArgumentException("sectionName=" + sectionName + " ts=" + ts + " barIndex=" + barIndex);
        }
        data = new Section(sectionName, ts);
        position = new Position(barIndex);
        clientProperties = new StringProperties(this);
    }

    @Override
    public final ChordLeadSheet getContainer()
    {
        return performReadAPImethod(() -> container);
    }

    @Override
    final public PropertyChangeEvent setContainer(ChordLeadSheet cls)
    {
        var res = getVoidEvent(this);
        if (cls != container)
        {
            ChordLeadSheet old = container;
            container = cls;
            res = new PropertyChangeEvent(this, PROP_CONTAINER, old, container);
        }
        return res;
    }

    @Override
    public int compareTo(ChordLeadSheetItem<?> o)
    {
        return performReadAPImethod(() -> compareToThreadUnsafe(o));
    }

    /**
     * Provide a consistent way to order CLI_Sections.
     */
    @Override
    public int compareToSamePosition(ChordLeadSheetItem<?> other)
    {
        Objects.requireNonNull(other);
        return performReadAPImethod(() -> 
        {

            Preconditions.checkArgument(other instanceof CLI_Section && !equals(other), "this=%s other=%s", other);
            Preconditions.checkArgument(position.equals(other.getPosition()) && getPositionOrder() == other.getPositionOrder(), "this=%s other=%s", other);

            CLI_Section otherSection = (CLI_Section) other;
            Section s = otherSection.getData();

            var res = data.toString().compareTo(s.toString());
            return res;
        });
    }

    @Override
    public StringProperties getClientProperties()
    {
        return clientProperties;
    }

    @Override
    public Section getData()
    {
        return performReadAPImethod(() -> data);
    }

    @Override
    public PropertyChangeEvent setData(Section section)
    {
        Objects.requireNonNull(section);
        var res = getVoidEvent(this);
        if (!section.equals(data))
        {
            Section oldData = data;
            data = section.clone();
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
    public int getPositionOrder()
    {
        return POSITION_ORDER;
    }

    @Override
    public CLI_Section getCopy(Section newData, Position newPos)
    {
        return performReadAPImethod(() -> 
        {
            int barIndex = (newPos != null) ? newPos.getBar() : position.getBar();
            var newData2 = newData == null ? data : newData;
            CLI_SectionImpl cli = new CLI_SectionImpl(newData2.getName(), newData2.getTimeSignature(), barIndex);
            cli.getClientProperties().set(clientProperties);
            return cli;
        });
    }

    @Override
    public CLI_Section getCopy(Position newPos, ChordLeadSheet cls)
    {
        return performReadAPImethod(() -> 
        {
            int barIndex = (newPos != null) ? newPos.getBar() : position.getBar();
            var name = CLI_Section.createSectionName(getData().getName(), cls);   // Make sure name is unique in cls
            CLI_SectionImpl cli = new CLI_SectionImpl(name, data.getTimeSignature(), barIndex);
            cli.getClientProperties().set(clientProperties);
            return cli;
        });
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
    public String toString()
    {
        return performReadAPImethod(() -> "" + getData() + getPosition());
    }

    @Override
    public final Position getPosition()
    {
        return performReadAPImethod(() -> new Position(position));
    }

    @Override
    public final PropertyChangeEvent setPosition(Position p)
    {
        Objects.requireNonNull(p);
        var res = getVoidEvent(this);
        if (!position.equals(p))
        {
            Position oldPos = position;
            position = new Position(p);
            res = new PropertyChangeEvent(this, PROP_ITEM_POSITION, oldPos, position);
        }
        return res;
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


    /**
     * Return a PropertyChangeEvent which will not be fired.
     *
     * @param item
     * @return
     */
    static protected PropertyChangeEvent getVoidEvent(ChordLeadSheetItem<?> item)
    {
        return new PropertyChangeEvent(item, "a", 1, 1);
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
            return ((ChordLeadSheetImpl) container).getExecutionManager().executeReadOperation(operation);
        }
    }

    // ------------------------------------------------------------------------------
    // Implementation of interface Transferable
    // ------------------------------------------------------------------------------
    private static DataFlavor flavor = CLI_Section.DATA_FLAVOR;

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
                    // From 4.1.0 new aliases for better XML readibility
                    xstream.alias("CLI_SectionImpl", CLI_SectionImpl.class);
                    xstream.alias("CLI_SectionImplSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spName");
                    xstream.useAttributeFor(SerializationProxy.class, "spBarIndex");
                    xstream.useAttributeFor(SerializationProxy.class, "spTs");

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
     * spVERSION 2 changes saved fields, see below.<br>
     * spVERSION 3 (JJazzLab 4.1.0) introduces several aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction). <br>
     * spVERSION 4 (JJazzLab 5) changed CL_Editor quantization system, simpler now, mainly relies on rhythm division
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 5519610279173982L;
        private int spVERSION = 4;      // Do not make final!
        private String spName;
        private TimeSignature spTs;
        private int spBarIndex;
        private StringProperties spClientProperties;  // From spVERSION 2

        private SerializationProxy(CLI_SectionImpl section)
        {
            spName = section.getData().getName();
            spTs = section.getData().getTimeSignature();
            spBarIndex = section.getPosition().getBar();
            spClientProperties = section.getClientProperties();
        }

        private Object readResolve() throws ObjectStreamException
        {
            CLI_SectionImpl cli = new CLI_SectionImpl(spName, spTs, spBarIndex);
            if (spVERSION >= 2)
            {
                if (spClientProperties != null)
                {
                    cli.getClientProperties().set(spClientProperties);
                    if (spVERSION < 4)
                    {
                        // For spVERSION 2 and 3 a quantization setting was always saved with the section
                        // Don't reload it, so we benefit from the "auto-mode" by default (JJazzLab 5: move-quantization is based on rhythm division, unless explicitly set by user)
                        cli.getClientProperties().put("PropSectionQuantization", null);
                    }
                } else
                {
                    LOGGER.log(Level.WARNING, "SerializationProxy.readResolve() Unexpected null value for spClientProperties. spName={0}",
                            spName);
                }
            }
            return cli;
        }
    }
}

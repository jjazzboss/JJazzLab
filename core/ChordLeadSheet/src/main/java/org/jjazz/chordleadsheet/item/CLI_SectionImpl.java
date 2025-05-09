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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
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
    private transient ChordLeadSheet container = null;

    /**
     * The listeners for changes in this ChordLeadSheetItem.
     */
    private final transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
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
    public synchronized Section getData()
    {
        return data.clone();
    }

    @Override
    public synchronized void setData(Section section)
    {
        if (section == null)
        {
            throw new NullPointerException("section=" + section);
        }
        if (!section.equals(data))
        {
            Section oldData = data;
            data = section.clone();
            pcs.firePropertyChange(PROP_ITEM_DATA, oldData, data);
        }
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
    public synchronized CLI_Section getCopy(Section newData, Position newPos)
    {
        int barIndex = (newPos != null) ? newPos.getBar() : position.getBar();
        newData = newData == null ? data : newData;
        CLI_SectionImpl cli = new CLI_SectionImpl(newData.getName(), newData.getTimeSignature(), barIndex);
        cli.getClientProperties().set(clientProperties);
        return cli;
    }

    @Override
    public synchronized CLI_Section getCopy(Position newPos, ChordLeadSheet cls)
    {
        int barIndex = (newPos != null) ? newPos.getBar() : position.getBar();
        var name = CLI_Section.createSectionName(getData().getName(), cls);   // Make sure name is unique in cls
        CLI_SectionImpl cli = new CLI_SectionImpl(name, data.getTimeSignature(), barIndex);
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

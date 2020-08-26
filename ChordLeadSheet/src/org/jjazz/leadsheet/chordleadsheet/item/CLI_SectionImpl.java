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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

/**
 * An item for a section.
 */
public class CLI_SectionImpl implements CLI_Section, WritableItem<Section>, Serializable
{

    /**
     * Position of the item.
     */
    private Position position = new Position(0, 0);
    /**
     * The data section.
     */
    private Section data;
    /**
     * The container of this item. Need to be transient otherwise this introduces circularities in the objects graph that prevent
     * ChordLeadSheetImpl's proxyserialization to work. This field must be restored by its container at deserialization.
     */
    private transient ChordLeadSheet container = null;

    /**
     * The listeners for changes in this ChordLeadSheetItem.
     */
    private transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

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
        position = new Position(barIndex, 0);
    }

    @Override
    final public void setContainer(ChordLeadSheet cls)
    {
        if (cls != container)
        {
            ChordLeadSheet old = container;
            container = cls;
            pcs.firePropertyChange(PROP_CONTAINER, old, container);
        }
    }

    @Override
    public Section getData()
    {
        return data.clone();
    }

    @Override
    public void setData(Section section)
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

    /**
     * Make sure the copy has a different name.
     */
    @Override
    public CLI_Section getCopy(ChordLeadSheet newCls, Position newPos)
    {
        int barIndex = (newPos != null) ? newPos.getBar() : position.getBar();
        ChordLeadSheet cls = (newCls != null) ? newCls : getContainer();
        CLI_SectionImpl cli = new CLI_SectionImpl(Util.createSectionName(data.getName(), cls), data.getTimeSignature(), barIndex);
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
    public final Position getPosition()
    {
        return new Position(position);
    }

    /**
     * Set the position of this item.
     *
     * @param p
     */
    @Override
    public final void setPosition(Position p)
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
    public final ChordLeadSheet getContainer()
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

        private static final long serialVersionUID = 5519610279173982L;
        private final int spVERSION = 1;
        private final String spName;
        private final TimeSignature spTs;
        private final int spBarIndex;

        private SerializationProxy(CLI_SectionImpl section)
        {
            spName = section.getData().getName();
            spTs = section.getData().getTimeSignature();
            spBarIndex = section.getPosition().getBar();
        }

        private Object readResolve() throws ObjectStreamException
        {
            CLI_SectionImpl cli = new CLI_SectionImpl(spName, spTs, spBarIndex);
            return cli;
        }
    }
}

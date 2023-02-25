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
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

public class CLI_ChordSymbolImpl implements CLI_ChordSymbol, WritableItem<ExtChordSymbol>, Serializable
{

    /**
     * Position of the item.
     */
    private Position position = new Position(0, 0);
    /**
     * The data section.
     */
    private ExtChordSymbol data;

    /**
     * The container of this item.
     * <p>
     * Need to be transient otherwise this introduces circularities in the objects graph that prevent ChordLeadSheetImpl's proxy
     * serialization to work. This field must be restored by its container at deserialization.
     */
    private transient ChordLeadSheet container = null;
    /**
     * The listeners for changes in this ChordLeadSheetItem.
     */
    private transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

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
    public boolean isBarSingleItem()
    {
        return false;
    }

    @Override
    public synchronized ExtChordSymbol getData()
    {
        return data;
    }

    @Override
    public synchronized void setData(ExtChordSymbol cs)
    {
        if (cs == null)
        {
            throw new NullPointerException("cs=" + cs);   
        }
        if (!cs.equals(data))
        {
            ExtChordSymbol oldData = data;
            data = cs;
            pcs.firePropertyChange(PROP_ITEM_DATA, oldData, data);
        }
    }

    /**
     * Note that we also copy the alternate data and filter.
     *
     * @param newCls
     * @param newPos
     * @return
     */
    @Override
    public synchronized CLI_ChordSymbol getCopy(ChordLeadSheet newCls, Position newPos)
    {
        CLI_ChordSymbolImpl cli = new CLI_ChordSymbolImpl(data, (newPos != null) ? newPos : position);
        ChordLeadSheet cls = (newCls != null) ? newCls : getContainer();
        cli.setContainer(cls);
        return cli;
    }

    /*
    * equals() and hashCode() are NOT defined because they can be used as Map keys and can change while being in the map (InstanceContent
    * for selected items in the CL_Editor, or ChordLeadSheet.moveSection())
     */
//    @Override
//    public boolean equals(Object o)
//    {
//        if (o instanceof CLI_ChordSymbol)
//        {
//            CLI_ChordSymbol cle = (CLI_ChordSymbol) o;
//            return container == cle.getContainer() && data.equals(cle.getData()) && position.equals(cle.getPosition());
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
//        int hash = 7;
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

    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 909000172651524L;
        private final int spVERSION = 1;
        private final ExtChordSymbol spChord;
        private final Position spPos;

        private SerializationProxy(CLI_ChordSymbolImpl cli)
        {
            spChord = cli.getData();
            spPos = cli.getPosition();
        }

        private Object readResolve() throws ObjectStreamException
        {
            CLI_ChordSymbolImpl cli = new CLI_ChordSymbolImpl(spChord, spPos);
            return cli;
        }
    }

}

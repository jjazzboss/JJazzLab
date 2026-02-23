/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.importers.musicxml;

import com.google.common.base.Preconditions;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.PROP_CONTAINER;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.PROP_ITEM_DATA;
import static org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem.PROP_ITEM_POSITION;
import org.jjazz.chordleadsheet.api.item.WritableItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.StringProperties;


/**
 * @param startOrEnd True if it's a start/forward repeat
 * @param times      Not used for an end repeat
 */
record Repeat(boolean startOrEnd, int times)
        {

}

/**
 * A start or end repeat bar, with the number of loops (default is 2).
 */
public class CLI_Repeat implements ChordLeadSheetItem<Repeat>, WritableItem<Repeat>
{

    public static final int POSITION_ORDER_START = -800;
    public static final int POSITION_ORDER_END = 2100;


    private ChordLeadSheet container;
    private Position position;
    private Repeat data;


    public CLI_Repeat(Position position, Repeat data)
    {
        this.position = position;
        this.data = data;
    }

    @Override
    public ChordLeadSheet getContainer()
    {
        return container;
    }

    @Override
    public int compareToSamePosition(ChordLeadSheetItem<?> other)
    {
        Objects.requireNonNull(other);
        Preconditions.checkArgument(other instanceof CLI_Repeat && !equals(other), "this=%s other=%s", other);
        Preconditions.checkArgument(getPosition().equals(other.getPosition()) && getPositionOrder() == other.getPositionOrder(), "this=%s other=%s", other);

        CLI_Repeat otherRepeat = (CLI_Repeat) other;
        Repeat d1 = getData();
        Repeat d2 = otherRepeat.getData();

        var res = d1.toString().compareTo(d2.toString());
        return res;
    }

    @Override
    public Repeat getData()
    {
        return data;
    }

    @Override
    public Position getPosition()
    {
        return position;
    }

    @Override
    public String toString()
    {
        return "CLI_Repeat[" + position + "] " + data;
    }


    @Override
    public ChordLeadSheetItem<Repeat> getCopy(Repeat newData, Position newPos)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getPositionOrder()
    {
        return data.startOrEnd() ? POSITION_ORDER_START : POSITION_ORDER_END;
    }

    @Override
    public boolean isBarSingleItem()
    {
        return false;
    }

    @Override
    public StringProperties getClientProperties()
    {
        return new StringProperties();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
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

    @Override
    public void firePropertyChangeEvent(PropertyChangeEvent event)
    {
        // Nothing
    }

    @Override
    public PropertyChangeEvent setPosition(Position pos)
    {
        var old = this.position;
        this.position = pos;
        return new PropertyChangeEvent(this, PROP_ITEM_POSITION, old, pos);
    }

    @Override
    public PropertyChangeEvent setData(Repeat data)
    {
        var old = this.data;
        this.data = data;
        return new PropertyChangeEvent(this, PROP_ITEM_DATA, old, data);
    }

    @Override
    public PropertyChangeEvent setContainer(ChordLeadSheet cls)
    {
        var old = this.container;
        this.container = cls;
        return new PropertyChangeEvent(this, PROP_CONTAINER, old, cls);
    }

}

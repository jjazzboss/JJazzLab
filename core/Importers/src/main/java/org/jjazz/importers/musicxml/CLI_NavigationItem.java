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
import java.util.List;
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


record NavItem(NavigationMark mark, String value, List<Integer> timeOnly)
        {

    public NavItem
    {
        Objects.requireNonNull(mark);
        Objects.requireNonNull(value);
        Objects.requireNonNull(timeOnly);
    }
}


public class CLI_NavigationItem implements ChordLeadSheetItem<NavItem>, WritableItem<NavItem>
{

    public static final int POSITION_ORDER_CODA_SEGNO = -900;
    public static final int POSITION_ORDER_FINE = 2200;
    public static final int POSITION_ORDER_DS_DC_TOCODA = 2300;

    private ChordLeadSheet container;
    private Position position;
    private NavItem data;

    public CLI_NavigationItem(Position position, NavItem data)
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
    public NavItem getData()
    {
        return data;
    }

    @Override
    public String toString()
    {
        return "CLI_NavItem[" + position + "] " + data;
    }

    @Override
    public Position getPosition()
    {
        return position;
    }

    @Override
    public ChordLeadSheetItem<NavItem> getCopy(NavItem newData, Position newPos)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
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
    public int compareTo(ChordLeadSheetItem<?> other)
    {
        return compareToDefault(other);
    }
    
    @Override
    public int compareToSamePosition(ChordLeadSheetItem<?> other)
    {
        Objects.requireNonNull(other);
        Preconditions.checkArgument(other instanceof CLI_NavigationItem && !equals(other), "this=%s other=%s", other);
        Preconditions.checkArgument(getPosition().equals(other.getPosition()) && getPositionOrder() == other.getPositionOrder(), "this=%s other=%s", other);

        CLI_NavigationItem otherCliNav = (CLI_NavigationItem) other;
        NavItem d1 = getData();
        NavItem d2 = otherCliNav.getData();

        var res = d1.toString().compareTo(d2.toString());
        return res;
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
    public PropertyChangeEvent setData(NavItem data)
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

    @Override
    public int getPositionOrder()
    {
        var res = switch (data.mark())
        {
            case CODA, SEGNO ->
                POSITION_ORDER_CODA_SEGNO;
            case TOCODA, DALSEGNO, DALSEGNO_ALCODA, DALSEGNO_ALFINE, DACAPO, DACAPO_ALCODA, DACAPO_ALFINE ->
                POSITION_ORDER_DS_DC_TOCODA;
            case FINE ->
                POSITION_ORDER_FINE;
            default -> throw new AssertionError(data.mark().name());
        };

        return res;

    }


}

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
package org.jjazz.leadsheet.chordleadsheet.api.item;

import com.google.common.base.Preconditions;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;

/**
 * Items which belong to a ChordLeadSheet.
 * <p>
 * PropertyChangeEvents are fired when an attribute is modified.
 *
 * @param <T>
 */
public interface ChordLeadSheetItem<T> extends Transferable, Comparable<ChordLeadSheetItem<?>>
{

    /**
     * oldValue=old container, newValue=new container.
     */
    public static String PROP_CONTAINER = "PropContainer";
    /**
     * oldValue=old data, newValue=new data.
     */
    public static String PROP_ITEM_DATA = "ItemData";
    /**
     * oldValue=old position, newValue=new position.
     */
    public static String PROP_ITEM_POSITION = "ItemPosition";

    /**
     * Get the ChordLeadSheet this object belongs to.
     *
     * @return Can be null.
     */
    ChordLeadSheet getContainer();


    /**
     * Get the data part of this item.
     *
     * @return
     */
    T getData();

    /**
     * Get the position of this item.
     *
     * @return
     */
    Position getPosition();

    /**
     * Get a copy of this item at a specified position.
     * <p>
     * @param newCls If null, the copy will have the same container that this object.
     * @param newPos If null, the copy will have the same position that this object.
     * @return
     */
    ChordLeadSheetItem<T> getCopy(ChordLeadSheet newCls, Position newPos);

    /**
     * Return true if there can be only one single item perbar, like a time signature.
     * <p>
     * @return
     */
    boolean isBarSingleItem();


    /**
     * First compare using position, then use isBarSingleItem().
     *
     * @param other
     * @return
     */
    @Override
    default int compareTo(ChordLeadSheetItem<?> other)
    {
        int res;
        if (this == other)
        {
            res = 0;
        } else
        {
            res = getPosition().compareTo(other.getPosition());
            if (res == 0)
            {
                if (isBarSingleItem() && !other.isBarSingleItem())
                {
                    res = -1;
                } else if (!isBarSingleItem() && other.isBarSingleItem())
                {
                    res = 1;
                }
            }
        }

        return res;
    }

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);


    /**
     * Create an item right after the specified position for comparison purposes.
     *
     * @param pos
     * @return
     */
    public static ComparableItem createItemAfter(Position pos)
    {
        Position newPos;
        if (Float.compare(pos.getBeat(), Float.MAX_VALUE) == 0)
        {
            newPos = new Position(pos.getBar() + 1, 0);
        } else
        {
            newPos = new Position(pos.getBar(), pos.getBeat() + Float.intBitsToFloat(0x1));
        }
        return new ComparableItem(newPos);
    }

    /**
     * Create an item right before the specified position for comparison purposes.
     *
     * @param pos
     * @return
     */
    public static ComparableItem createItemBefore(Position pos)
    {
        Preconditions.checkArgument(!(pos.getBar() == 0 && pos.isFirstBarBeat()), "pos=%s", pos);
        Position newPos;
        if (pos.isFirstBarBeat())
        {
            newPos = new Position(pos.getBar() - 1, Float.MAX_VALUE);
        } else
        {
            newPos = new Position(pos.getBar(), pos.getBeat() - Float.intBitsToFloat(0x1));
        }
        return new ComparableItem(newPos);
    }

    // ==================================================================================================
    // Inner classes
    // ==================================================================================================
    /**
     * A dummy ChordLeadSheetItem class used for position comparison when using the NavigableSet-based methods of ChordLeadSheet.
     */
    static class ComparableItem implements ChordLeadSheetItem<String>
    {

        private final boolean barSingleItem;
        private final Position position;
        private final String data;

        public ComparableItem(Position pos)
        {
            this(pos, "comparableItem", false);
        }

        public ComparableItem(Position pos, String data, boolean barSingleItem)
        {
            this.barSingleItem = barSingleItem;
            this.position = pos;
            this.data = data;
        }

        @Override
        public ChordLeadSheet getContainer()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public ChordLeadSheetItem getCopy(ChordLeadSheet newCls, Position newPos)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isBarSingleItem()
        {
            return barSingleItem;
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
    }


}

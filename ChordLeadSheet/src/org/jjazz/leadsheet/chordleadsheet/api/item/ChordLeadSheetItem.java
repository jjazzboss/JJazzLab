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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Logger;
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
    static final Logger LOGGER = Logger.getLogger(ChordLeadSheetItem.class.getSimpleName());

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
     * Get a copy of the position of this item.
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
     * First compare using position, then use isBarSingleItem(), then use System.identifyHashCode().
     * <p>
     * Performs a special handling for ComparableItems.
     *
     * @param other
     * @return 0 only if this == other, so that comparison is consistent with equals().
     */
    @Override
    default int compareTo(ChordLeadSheetItem<?> other)
    {
        if (this == other)
        {
            return 0;
        }

        int res = getPosition().compareTo(other.getPosition());
        if (res == 0)
        {
            if (this instanceof ComparableItem ciThis && other instanceof ComparableItem ciOther)
            {
                if (ciThis.isBeforeItem() == ciOther.isBeforeItem())
                {
                    throw new IllegalStateException("this=" + this + " other=" + other);
                }
                res = ciThis.isBeforeItem() ? -1 : 1;
            } else if (this instanceof ComparableItem ciThis)
            {
                if (ciThis.isBeforeItem())
                {
                    res = ciThis.isInclusive() ? -1 : 1;
                } else
                {
                    res = ciThis.isInclusive() ? 1 : -1;
                }
            } else if (other instanceof ComparableItem ciOther)
            {
                if (ciOther.isBeforeItem())
                {
                    res = ciOther.isInclusive() ? 1 : -1;
                } else
                {
                    res = ciOther.isInclusive() ? -1 : 1;
                }
            } else if (isBarSingleItem() && !other.isBarSingleItem())
            {
                res = -1;
            } else if (!isBarSingleItem() && other.isBarSingleItem())
            {
                res = 1;
            } else
            {
                res = Long.compare(System.identityHashCode(this), System.identityHashCode(other));
            }
            // System.out.println("compareTo() samePos > res=" + res + " this=" + this + " other=" + other);            
        }


        return res;
    }

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);


    /**
     * Create an item right after the specified position for comparison purposes.
     * <p>
     * For the Comparable interface, any item whose position is before (or equal if inclusive is true) to pos will be considered BEFORE the
     * returned item.
     *
     * @param pos
     * @param inclusive
     * @return
     */
    public static DefaultComparableItem createItemTo(Position pos, boolean inclusive)
    {
        return new DefaultComparableItem(pos, false, inclusive);
    }

    /**
     * Create an item at the end of the specified bar for comparison purposes.
     * <p>
     * For the Comparable interface, any normal item in the bar will be considered BEFORE the returned item.
     *
     * @param bar
     * @return
     */
    public static DefaultComparableItem createItemTo(int bar)
    {
        return new DefaultComparableItem(new Position(bar, Float.MAX_VALUE), false, true);
    }

    /**
     * Create an item right before the specified position for comparison purposes.
     * <p>
     * For the Comparable interface, any item whose position is after (or equal if inclusive is true) to pos will be considered AFTER the
     * returned item.
     *
     * @param pos
     * @param inclusive
     * @return
     */
    public static DefaultComparableItem createItemFrom(Position pos, boolean inclusive)
    {
        return new DefaultComparableItem(pos, true, inclusive);
    }

    /**
     * Create an item at the beginning of the specified bar for comparison purposes.
     * <p>
     * For the Comparable interface, any normal item in the bar will be considered AFTER the returned item.
     *
     * @param bar
     * @return
     */
    public static DefaultComparableItem createItemFrom(int bar)
    {
        return new DefaultComparableItem(new Position(bar, 0), true, true);
    }

    // ==================================================================================================
    // Inner classes
    // ==================================================================================================

    /**
     * An interface for items used only for position comparison purposes, when using the NavigableSet/SortedSet-based methods of
     * ChordLeadSheet or ChordSequence.
     */
    public interface ComparableItem
    {
        boolean isBeforeItem();

        public boolean isInclusive();
    }

    public static class DefaultComparableItem implements ComparableItem, ChordLeadSheetItem<Object>
    {

        private final Position position;
        private final boolean beforeItem;
        private final boolean inclusive;


        private DefaultComparableItem(Position pos, boolean beforeItem, boolean inclusive)
        {
            this.beforeItem = beforeItem;
            this.position = pos;
            this.inclusive = inclusive;
        }

        @Override
        public boolean isBeforeItem()
        {
            return beforeItem;
        }

        @Override
        public boolean isInclusive()
        {
            return inclusive;
        }

        @Override
        public ChordLeadSheet getContainer()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public ChordLeadSheetItem<Object> getCopy(ChordLeadSheet newCls, Position newPos)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isBarSingleItem()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Object getData()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Position getPosition()
        {
            return new Position(position);
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
        public String toString()
        {
            return (beforeItem ? "beforeCompItem" : "afterCompItem") + "-" + getPosition() + "-" + (inclusive ? "inclusive" : "exclusive");
        }
    }


}

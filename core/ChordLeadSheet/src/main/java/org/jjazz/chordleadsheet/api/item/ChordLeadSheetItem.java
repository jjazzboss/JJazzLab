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
package org.jjazz.chordleadsheet.api.item;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.StringProperties;

/**
 * Items which belong to a ChordLeadSheet.
 * <p>
 * PropertyChangeEvents are fired when an attribute is modified.
 * <p>
 * This is a mutable class and subclasses should define equals() and hashCode(): if you want to use ChordLeadSheetItems as Map keys you should use an
 * IdentityHashMap, unless you are sure ChordLeadSheetItems won't mutate. Same for a Set, you should use Guava Sets.newIdentityHashSet().
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
     * A unique constant value used to order items which have the same position.
     * <p>
     *
     * @return Must be a unique value for each type of item
     * @see #compareTo(org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem)
     */
    int getPositionOrder();

    /**
     * Get a copy of this item using the specified parameters.
     * <p>
     * Client properties are also copied. Returned copy has its ChordLeadSheet container set to null.
     *
     * @param newData If null, the copy will have the same data than this object.
     * @param newPos  If null, the copy will have the same position than this object.
     * @return
     */
    ChordLeadSheetItem<T> getCopy(T newData, Position newPos);


    /**
     * Return true if there can be only one single item perbar, like a time signature.
     * <p>
     * @return
     */
    boolean isBarSingleItem();

    /**
     * Get the client properties.
     *
     * @return
     */
    StringProperties getClientProperties();


    /**
     * Default implementation compares items only using position then positionOrder if required.
     * <p>
     *
     * @param other
     * @return 0 only if this.equals(other), so that comparison is consistent with equals().
     * @see #getPositionOrder()
     */
    @Override
    default int compareTo(ChordLeadSheetItem<?> other)
    {
        Objects.requireNonNull(other);
        if (this.equals(other))
        {
            return 0;
        }
        int res = getPosition().compareTo(other.getPosition());
        if (res == 0)
        {
            res = Integer.compare(getPositionOrder(), other.getPositionOrder());
            if (res == 0)
            {
                // e.g. for non-isBarSingleItem() item like CLI_ChordSymbol
                res = Long.compare(System.identityHashCode(this), System.identityHashCode(other));
                LOGGER.log(Level.FINE, "compareTo() Using hashcode to compare this={0} and other={1} -> res={2}", new Object[]
                {
                    this, other, res
                });
            }
        }
        assert res != 0;        // For consistency with equals(), important because ChordLeadSheetItems are used in order-based collections
        return res;
    }

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Generic equals method relying only on data and position.
     * <p>
     * @param item1 Can not be null
     * @param o
     * @return
     */
    static public boolean equals(ChordLeadSheetItem<?> item1, Object o)
    {
        Objects.requireNonNull(item1);
        if (o == null || item1.getClass() != o.getClass())
        {
            return false;
        }
        var item2 = (ChordLeadSheetItem<?>) o;
        return item1.getData().equals(item2.getData())
                && item1.getPosition().equals(item2.getPosition());
    }

    /**
     * Generic hashCode method relying on data and position.
     *
     * @param item
     * @return
     */
    static public int hashCode(ChordLeadSheetItem<?> item)
    {
        int hash = 7;
        hash = 37 * hash + item.getPosition().hashCode();
        hash = 37 * hash + item.getData().hashCode();
        return hash;
    }

    /**
     * Create an item right after the specified position for comparison purposes.
     * <p>
     * For the Comparable interface, any item whose position is before (or equal if inclusive is true) to pos will be considered BEFORE the returned item.
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
     * For the Comparable interface, any item whose position is after (or equal if inclusive is true) to pos will be considered AFTER the returned item.
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
        return new DefaultComparableItem(new Position(bar), true, true);
    }

    // ==================================================================================================
    // Inner classes
    // ==================================================================================================

    static class DefaultComparableItem implements ChordLeadSheetItem<Object>
    {

        private final int positionOrder;
        private final Position position;

        /**
         *
         * @param pos
         * @param beforeOrAfterItem If true it's a "before item", otherwise an "after" item
         * @param inclusive         If true other items at same pos should be included
         */
        private DefaultComparableItem(Position pos, boolean beforeOrAfterItem, boolean inclusive)
        {
            this.position = pos;

            if (beforeOrAfterItem)
            {
                positionOrder = inclusive ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else
            {
                positionOrder = inclusive ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }
        }

        @Override
        public int getPositionOrder()
        {
            return positionOrder;
        }

        @Override
        public ChordLeadSheet getContainer()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public ChordLeadSheetItem<Object> getCopy(Object data, Position newPos)
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
            return getClass().getSimpleName() + "[" + getPosition() + ", posOrder=" + positionOrder + "]";
        }

        @Override
        public StringProperties getClientProperties()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

    }


}

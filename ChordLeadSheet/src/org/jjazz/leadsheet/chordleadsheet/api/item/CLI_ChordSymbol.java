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
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;

/**
 * A ChordLeadSheetItem which uses ExtChordSymbol objects as data.
 * <p>
 */
public interface CLI_ChordSymbol extends ChordLeadSheetItem<ExtChordSymbol>
{

    public static final DataFlavor DATA_FLAVOR = new DataFlavor(CLI_ChordSymbol.class, "Chord Symbol");

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
    public static CLI_ChordSymbol createItemTo(Position pos, boolean inclusive)
    {
        return new ComparableCsItem(pos, false, inclusive);
    }

    /**
     * Create an item at the end of the specified bar for comparison purposes.
     * <p>
     * For the Comparable interface, any normal item in the bar will be considered BEFORE the returned item.
     *
     * @param bar
     * @return
     */
    public static CLI_ChordSymbol createItemTo(int bar)
    {
        return new ComparableCsItem(new Position(bar, Float.MAX_VALUE), false, true);
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
    public static CLI_ChordSymbol createItemFrom(Position pos, boolean inclusive)
    {
        return new ComparableCsItem(pos, true, inclusive);
    }

    /**
     * Create an item at the beginning of the specified bar for comparison purposes.
     * <p>
     * For the Comparable interface, any normal item in the bar will be considered AFTER the returned item.
     *
     * @param bar
     * @return
     */
    public static CLI_ChordSymbol createItemFrom(int bar)
    {
        return new ComparableCsItem(new Position(bar, 0), true, true);
    }

    // ==================================================================================================
    // Inner classes
    // ==================================================================================================
    /**
     * A dummy CLI_ChordSymbol class which can be used only for position comparison when using the NavigableSet/SortedSet-based methods of
     * ChordLeadSheet or ChordSequence.
     */
    public static class ComparableCsItem implements CLI_ChordSymbol, ComparableItem
    {

        private final Position position;
        private final boolean beforeItem;
        private final boolean inclusive;


        private ComparableCsItem(Position pos, boolean beforeItem, boolean inclusive)
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
        public CLI_ChordSymbol getCopy(ChordLeadSheet newCls, Position newPos)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isBarSingleItem()
        {
            return false;
        }

        @Override
        public ExtChordSymbol getData()
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

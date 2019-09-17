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

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;

/**
 * Items that belong to a ChordLeadSheet.
 *
 * @param <T>
 */
public interface ChordLeadSheetItem<T> extends Item<T>, Transferable
{

    public static String PROP_CONTAINER = "PropContainer";

    /**
     * @return The ChordLeadSheet this object belongs to.
     */
    public ChordLeadSheet getContainer();

    /**
     * Get a copy of this item at a specified position.
     * <p>
     * @param newCls If null, the copy will have the same container that this object.
     * @param newPos If null, the copy will have the same position that this object.
     * @return
     */
    public ChordLeadSheetItem<T> getCopy(ChordLeadSheet newCls, Position newPos);

    /**
     * Return true if there can be only one single item perbar, like a time signature.
     * <p>
     * @return
     */
    public boolean isBarSingleItem();

    /**
     * Convenience functions to work with items.
     */
    public static class Utilities
    {

        /**
         * Sort a list of ChordLeadSheetItems based on their position.
         *
         * @param items
         * @return A new list correponding to sorted items.
         */
        public static List<ChordLeadSheetItem<?>> sortByPosition(final List<ChordLeadSheetItem<?>> items)
        {
            ArrayList<ChordLeadSheetItem<?>> sortedItems = new ArrayList<>(items);
            Collections.sort(sortedItems, new Comparator<ChordLeadSheetItem<?>>()
            {
                @Override
                public int compare(ChordLeadSheetItem<?> i1, ChordLeadSheetItem<?> i2)
                {
                    return i1.getPosition().compareTo(i2.getPosition());
                }
            });
            return sortedItems;
        }
    }
}

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
package org.jjazz.chordleadsheet.api.event;

import java.util.List;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * Some bars were deleted.
 * <p>
 * getItems() returns the deleted items.
 */
public class DeletedBarsEvent extends ClsChangeEvent
{
    private final int barFrom;
    private final int barTo;
    private final List<ChordLeadSheetItem> shiftedItems;
    private final List<ChordLeadSheetItem> adjustedItems; // Items moved due to TimeSignature adjustment

    public DeletedBarsEvent(ChordLeadSheet src, int barFrom, int barTo,
            List<ChordLeadSheetItem> removedItems,
            List<ChordLeadSheetItem> shiftedItems,
            List<ChordLeadSheetItem> adjustedItems)
    {
        super(src, removedItems);
        this.barFrom = barFrom;
        this.barTo = barTo;
        this.shiftedItems = shiftedItems;
        this.adjustedItems = adjustedItems;
    }

    /**
     * The items after the deletion which were shifted.
     *
     * @return
     */
    public List<ChordLeadSheetItem> getShiftedItems()
    {
        return shiftedItems;
    }

    /**
     * The items moved (within their bar) due to a TimeSignature change.
     *
     * @return
     */
    public List<ChordLeadSheetItem> getAdjustedItems()
    {
        return adjustedItems;
    }

    public int getNbDeletedBars()
    {
        return barTo - barFrom + 1;
    }

    @Override
    public String toString()
    {
        return "DeletedBarsEvent[barFrom=" + barFrom + " barTo=" + barTo + "]";
    }

    public String toDebugString()
    {
        return "DeletedBarsEvent[barFrom=" + barFrom + " barTo=" + barTo + "removedItems=" + getItems() + " shiftedItems=" + shiftedItems + " adjustedItems=" + adjustedItems + "]";
    }


}

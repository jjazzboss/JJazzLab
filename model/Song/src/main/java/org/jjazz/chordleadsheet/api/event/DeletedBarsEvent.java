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

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
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
    private final boolean initSectionRemoved;

    public DeletedBarsEvent(ChordLeadSheet src, int barFrom, int barTo,
            List<ChordLeadSheetItem> removedItems,
            List<ChordLeadSheetItem> shiftedItems)
    {
        super(src, removedItems);
        Objects.requireNonNull(shiftedItems);
        Preconditions.checkArgument(barFrom <= barTo, "barFrom=%s barTo=%s", barFrom, barTo);
        this.barFrom = barFrom;
        this.barTo = barTo;
        this.shiftedItems = List.copyOf(shiftedItems);
        this.initSectionRemoved = removedItems.stream().anyMatch(cli -> cli instanceof CLI_Section && cli.getPosition().getBar() == 0);
    }

    public int getBarFrom()
    {
        return barFrom;
    }

    public int getBarTo()
    {
        return barTo;
    }

    /**
     * Check if the initial Section was removed and replaced by the first section from shiftedItems.
     *
     * @return
     */
    public boolean isInitSectionRemoved()
    {
        return initSectionRemoved;
    }

    /**
     * The items after the deletion which were shifted.
     * <p>
     * Note that some items beat position might have been adjusted because of a time signature change.
     *
     * @return
     */
    public List<ChordLeadSheetItem> getShiftedItems()
    {
        return shiftedItems;
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
        return "DeletedBarsEvent[barFrom=" + barFrom + " barTo=" + barTo + "removedItems=" + getItems() + " shiftedItems=" + shiftedItems + "]";
    }


}

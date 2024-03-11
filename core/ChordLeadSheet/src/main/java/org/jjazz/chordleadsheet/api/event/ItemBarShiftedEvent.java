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
 * Items (possibly including sections) have been shifted left or right by a number of bars.
 */
public class ItemBarShiftedEvent extends ClsChangeEvent
{

    private int barDiff;

    /**
     * Items have been shifted left or right.
     *
     * @param src
     * @param items This can include sections.
     * @param nbBars positive or negative integer
     */
    public ItemBarShiftedEvent(ChordLeadSheet src, List<ChordLeadSheetItem> items, int nbBars)
    {
        super(src, items);
        barDiff = nbBars;
    }

    /**
     * The number of bars item(s) have been moved.
     *
     * @return negative or positive integer, negative means moved to the left.
     */
    public int getBarDiff()
    {
        return barDiff;
    }

    @Override
    public String toString()
    {
        return "ItemBarShiftedEvent[items=" + getItems() + " barDiff=" + barDiff + "]";
    }
}

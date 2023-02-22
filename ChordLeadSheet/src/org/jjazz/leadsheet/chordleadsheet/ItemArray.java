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
package org.jjazz.leadsheet.chordleadsheet;

import java.util.ArrayList;
import java.util.List;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * A special array with additional methods to deal with ChordLeadSheetItems and CLI_Sections.
 * <p>
 * Items are stored ordered by position. We guarantee that CLI_Section is always the first item for a bar.
 *
 */
public class ItemArray extends ArrayList<ChordLeadSheetItem<?>>
{

    /**
     * Insert an item at the appropriate position.
     * <p>
     * If 2 items on same position, normal items are inserted last, BarSingleItems are inserted first.
     *
     * @param item
     * @return The index where item has been inserted.
     */
    public int insertOrdered(ChordLeadSheetItem<?> item)
    {
        int i;
        for (i = 0; i < size(); i++)
        {
            ChordLeadSheetItem<?> cli = get(i);
            int compare = item.getPosition().compareTo(cli.getPosition());
            if (compare < 0 || (compare == 0 && item.isBarSingleItem()))
            {
                add(i, item);
                break;
            }
        }
        if (i == size())
        {
            // insert at the end
            add(item);
        }
        return i;
    }

    /**
     * The index of the first item found at fromBarIndex or after fromBarIndex.
     *
     * @param fromBarIndex
     * @return -1 if no item found from fromBarIndex
     */
    public int getItemIndex(int fromBarIndex)
    {
        int index = 0;
        for (ChordLeadSheetItem<?> item : this)
        {
            if (item.getPosition().getBar() >= fromBarIndex)
            {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Return a list which contains items between the specified indexes. Unlike sublist(), the returned list if independant from
     * this object.
     *
     * @param indexFrom
     * @param indexTo
     * @return
     */
    public List<ChordLeadSheetItem<?>> getSubList(int indexFrom, int indexTo)
    {
        if (indexFrom < 0 || indexFrom > indexTo || indexTo > size() - 1)
        {
            throw new IllegalArgumentException("indexFrom=" + indexFrom + " indexTo=" + indexTo);   
        }
        ArrayList<ChordLeadSheetItem<?>> result = new ArrayList<>();
        for (int i = indexFrom; i <= indexTo; i++)
        {
            result.add(get(i));
        }
        return result;
    }
}

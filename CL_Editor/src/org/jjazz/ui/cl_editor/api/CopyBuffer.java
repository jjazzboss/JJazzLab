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
package org.jjazz.ui.cl_editor.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

/**
 * Singleton class to manage ChordLeadSheetItems and Bars for copy/paste operations. There are 2 modes of copy, ItemMode and
 * BarMode.
 */
public class CopyBuffer
{

    static private CopyBuffer INSTANCE;
    /**
     * The buffer for ChordLeadSheetItem.
     */
    private ArrayList<ChordLeadSheetItem<?>> itemsBuffer = new ArrayList<>();
    /**
     * True if copy was made in Bar mode.
     */
    private boolean isBarCopyMode;
    /**
     * Index of the bars in case of BarCopyMode.
     */
    private int barMinIndex;
    /**
     * Index of the bars in case of BarCopyMode.
     */
    private int barMaxIndex;
    /**
     * Once something has been stored, not empty anymore.
     */
    private boolean isEmpty = true;

    private CopyBuffer()
    {
    }

    public static CopyBuffer getInstance()
    {
        synchronized (CopyBuffer.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new CopyBuffer();
            }
        }
        return INSTANCE;
    }

    /**
     * Put items in the buffer in ItemMode.
     *
     * @param items
     */
    public void itemModeCopy(List<ChordLeadSheetItem<?>> items)
    {
        if (items == null || items.isEmpty())
        {
            throw new IllegalArgumentException("items=" + items);
        }
        isBarCopyMode = false;
        isEmpty = false;
        copyItems(items);
    }

    /**
     * Put a copy of each item in the buffer in BarMode
     *
     * @param items
     * @param fromBarIndex
     * @param toBarIndex
     */
    public void barModeCopy(List<ChordLeadSheetItem<?>> items, int fromBarIndex, int toBarIndex)
    {
        if ((items == null) || (fromBarIndex < 0) || (toBarIndex < 0) || (fromBarIndex > toBarIndex))
        {
            throw new IllegalArgumentException("items=" + items + " fromBarIndex=" + fromBarIndex + " toBarIndex="
                    + toBarIndex);
        }

        isBarCopyMode = true;
        barMinIndex = fromBarIndex;
        barMaxIndex = toBarIndex;
        isEmpty = false;
        copyItems(items);
    }

    /**
     * Store in the buffer a clone copy of each item. Items are ordered by position.
     *
     * @param items List
     */
    private void copyItems(List<ChordLeadSheetItem<?>> items)
    {
        itemsBuffer.clear();
        for (ChordLeadSheetItem<?> item : items)
        {
            ChordLeadSheetItem<?> itemCopy = item.getCopy(null, null);
            // Ordered insert
            int i;
            for (i = 0; i < itemsBuffer.size(); i++)
            {
                ChordLeadSheetItem<?> cli = itemsBuffer.get(i);
                int compare = itemCopy.getPosition().compareTo(cli.getPosition());
                if (compare < 0 || (compare == 0 && itemCopy.isBarSingleItem()))
                {
                    itemsBuffer.add(i, itemCopy);
                    break;
                }
            }
            if (i == itemsBuffer.size())
            {
                // insert at the end
                itemsBuffer.add(itemCopy);
            }
        }
    }

    public void clear()
    {
        barMinIndex = 0;
        barMaxIndex = 0;
        itemsBuffer.clear();
        isEmpty = true;
    }

    /**
     * @return int The size of ChordLeadSheetItems in the buffer.
     */
    public int getSize()
    {
        return itemsBuffer.size();
    }

    public boolean isBarCopyMode()
    {
        return isBarCopyMode;
    }

    public int getBarMinIndex()
    {
        return barMinIndex;
    }

    public int getBarMaxIndex()
    {
        return barMaxIndex;
    }

    public boolean isEmpty()
    {
        return isEmpty;
    }

    /**
     * Return a copy of the items in the buffer adjusted to targetBarIndex and with the specified container. In ItemMode, the
     * items are shitfed so the first item start at targetBarIndex. In BarMode, the items are shifted so that getBarMinIndex()
     * match targetBarIndex.
     *
     * @param targetCls The container of the new items. If null container is not changed.
     * @param targetBarIndex The barIndex where items are copied to. If barIndex&lt;0, positions are not changed. @return
     */
    public List<ChordLeadSheetItem<?>> getItemsCopy(ChordLeadSheet targetCls, int targetBarIndex)
    {
        ArrayList<ChordLeadSheetItem<?>> items = new ArrayList<>();
        if (itemsBuffer.isEmpty())
        {
            return items;
        }
        int minBarIndex = itemsBuffer.get(0).getPosition().getBar();
        int barShift = 0;
        if (targetBarIndex >= 0)
        {
            if (!isBarCopyMode)
            {
                barShift = targetBarIndex - minBarIndex;
            } else
            {
                int itemShift = minBarIndex - getBarMinIndex();
                barShift = targetBarIndex + itemShift - minBarIndex;
            }
        }
        HashSet<String> setSectionNames = new HashSet<>();
        for (ChordLeadSheetItem<?> item : itemsBuffer)
        {
            Position newPos = null;
            if (targetBarIndex >= 0)
            {
                newPos = new Position(item.getPosition().getBar() + barShift, item.getPosition().getBeat());
            }
            ChordLeadSheet newCls = (targetCls != null) ? targetCls : null;
            ChordLeadSheetItem<?> newItem = item.getCopy(newCls, newPos);
            items.add(newItem);
        }
        return items;
    }
}

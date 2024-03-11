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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * The base class which represents a change in the ChordLeadSheet.
 */
public abstract class ClsChangeEvent
{

    /**
     * The source of the event.
     */
    protected ChordLeadSheet source;
    /**
     * The ChordLeadSheet items which have changed.
     */
    protected List<ChordLeadSheetItem> items;


    protected ClsChangeEvent(ChordLeadSheet src)
    {
        if (src == null)
        {
            throw new IllegalArgumentException("src=" + src);
        }
        source = src;
        items = new ArrayList<>();
    }

    /**
     * @param src
     * @param item The ChordLeadSheetItem which has changed.
     */
    protected ClsChangeEvent(ChordLeadSheet src, ChordLeadSheetItem<?> item)
    {
        this(src, Arrays.asList(item));
    }

    /**
     * @param src
     * @param items The list of the ChordLeadSheetItems which have changed.
     */
    protected ClsChangeEvent(ChordLeadSheet src, List<ChordLeadSheetItem> items)
    {
        this(src);
        if (items == null)
        {
            throw new NullPointerException("src=" + src + " items=" + items);
        }
        var sortedItems = new ArrayList<>(items);
        Collections.sort(sortedItems);
        this.items.addAll(sortedItems);
    }

    /**
     * @return The first item associated to the event, or null if no item associated to the event.
     */
    public ChordLeadSheetItem<?> getItem()
    {
        return !items.isEmpty() ? items.get(0) : null;
    }

    /**
     *
     * @return An ordered list of items
     */
    public List<ChordLeadSheetItem> getItems()
    {
        return items;
    }

    public ChordLeadSheet getSource()
    {
        return source;
    }
}

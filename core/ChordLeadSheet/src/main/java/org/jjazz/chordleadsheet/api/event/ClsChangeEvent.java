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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * Each mutating ChordLeadSheet API method fires one ClsChangeEvent.
 */
public abstract class ClsChangeEvent
{

    /**
     * The source of the event.
     */
    protected ChordLeadSheet source;
    /**
     * The ChordLeadSheet items which were impacted
     */
    protected List<ChordLeadSheetItem> items;


    private boolean isUndo, isRedo;


    protected ClsChangeEvent(ChordLeadSheet src)
    {   
        this(src, new ArrayList<>());
    }

    /**
     * @param src
     * @param item The ChordLeadSheetItem which has changed.
     */
    protected ClsChangeEvent(ChordLeadSheet src, ChordLeadSheetItem<?> item)
    {
        this(src, List.of(item));
    }

    /**
     * @param src
     * @param items The list of the impacted ChordLeadSheetItems.
     */
    protected ClsChangeEvent(ChordLeadSheet src, List<ChordLeadSheetItem> items)
    {
        Objects.requireNonNull(src);
        Objects.requireNonNull(items);
        this.source = src;
        this.items = new ArrayList<>(items);
        Collections.sort(this.items);
    }

    public void addItem(ChordLeadSheetItem<?> item)
    {
        if (!items.contains(item))
        {
            items.add(item);
            Collections.sort(items);
        }
    }

    /**
     * Set isUndo to true.
     */
    public void setIsUndo()
    {
        this.isUndo = true;
    }

    /**
     * Set isRedo to true.
     */
    public void setIsRedo()
    {
        this.isRedo = true;
    }


    /**
     * True if this is an undo event, i.e. this event's change was just undone.
     *
     * @return
     * @see #setIsUndo()
     */
    public boolean isUndo()
    {
        return isUndo;
    }

    /**
     * True if this is a redo event, i.e. this event's change was just redone.
     *
     * @return
     * @see #setIsRedo()
     */
    public boolean isRedo()
    {
        return isRedo;
    }

    public boolean isUndoOrRedo()
    {
        return isUndo || isRedo;
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

    /**
     * Get the items which are instance the specified class.
     *
     * @param <T>
     * @param clazz
     * @return
     */
    public <T extends ChordLeadSheetItem<?>> List<T> getItems(Class<T> clazz)
    {
        var res = items.stream()
                .filter(item -> clazz.isAssignableFrom(item.getClass()))
                .map(item -> clazz.cast(item))
                .toList();
        return res;
    }

    public ChordLeadSheet getSource()
    {
        return source;
    }
}

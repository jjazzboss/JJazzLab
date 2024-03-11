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

import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * One item has changed its content.
 * <p>
 */
public class ItemChangedEvent extends ClsChangeEvent
{

    private final Object oldData;
    private final Object newData;

    public ItemChangedEvent(ChordLeadSheet src, ChordLeadSheetItem<?> item, Object oldData, Object newData)
    {
        super(src, item);
        this.oldData = oldData;
        this.newData = newData;
    }

    public Object getOldData()
    {
        return oldData;
    }

    public Object getNewData()
    {
        return newData;
    }

    @Override
    public String toString()
    {
        return "ItemChangedEvent[item=" + getItem() + ", prevData=" + oldData + ", newData=" + newData + "]";
    }
}

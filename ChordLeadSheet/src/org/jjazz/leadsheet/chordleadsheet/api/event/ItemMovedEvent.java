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
package org.jjazz.leadsheet.chordleadsheet.api.event;

import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

/**
 * One item (but not a section) has been moved.
 */
public class ItemMovedEvent extends ClsChangeEvent
{

    Position oldPosition;
    Position newPosition;

    /**
     *
     * @param src
     * @param item Must not be a CLI_Section.
     * @param oldPos
     * @param newPos
     */
    public ItemMovedEvent(ChordLeadSheet src, ChordLeadSheetItem<?> item, Position oldPos, Position newPos)
    {
        super(src, item);
        if (oldPos == null || newPos == null || item instanceof CLI_Section)
        {
            throw new IllegalArgumentException("item=" + item + " oldPos=" + oldPos + " newPos=" + newPos);   
        }
        oldPosition = oldPos;
        newPosition = newPos;
    }

    /**
     * @return The previous position of the item.
     */
    public Position getOldPosition()
    {
        return oldPosition;
    }

    public Position getNewPosition()
    {
        return newPosition;
    }


    @Override
    public String toString()
    {
        return "ItemMovedEvent[item=" + getItem() + ", prevPosition=" + oldPosition + ", newPosition=" + newPosition + "]";
    }
}

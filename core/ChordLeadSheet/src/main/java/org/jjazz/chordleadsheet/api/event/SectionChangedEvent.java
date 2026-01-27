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
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * One section name and/or TimeSignature has changed.
 */
public class SectionChangedEvent extends ClsChangeEvent
{

    private final Section oldData;
    private final Section newData;
    private final List<ChordLeadSheetItem<?>> adjustedItems;

    /**
     *
     * @param src
     * @param item
     * @param oldData
     * @param newData
     * @param adjustedItems   Possible items (like CLI_ChordSymbols) whose beat was moved sooner within their bar because of a new time signature with less beats. Can be
     *                     empty.
     */
    public SectionChangedEvent(ChordLeadSheet src, CLI_Section item, Section oldData, Section newData, List<ChordLeadSheetItem<?>> adjustedItems)
    {
        super(src, item);
        this.oldData = oldData;
        this.newData = newData;
        this.adjustedItems = adjustedItems;
    }

    public List<ChordLeadSheetItem<?>> getAdjustedItems()
    {
        return adjustedItems;
    }

    public Section getOldSection()
    {
        return oldData;
    }

    public Section getNewSection()
    {
        return newData;
    }

    public CLI_Section getCLI_Section()
    {
        return (CLI_Section) getItem();
    }

    @Override
    public String toString()
    {
        return "SectionChangedEvent[section=" + getCLI_Section() + ", oldData=" + oldData + ", newData=" + newData + "]";
    }
}

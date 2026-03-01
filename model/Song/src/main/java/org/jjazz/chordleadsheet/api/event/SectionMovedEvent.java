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
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;

/**
 * A section was moved.
 * <p>
 * getItemChanges() returns a PROP_ITEM_POSITION for the section, and possible additional ones for the adjusted items because of a TimeSignature change.
 */
public class SectionMovedEvent extends ClsChangeEvent
{

    private final int oldBar, newBar;

    public SectionMovedEvent(ChordLeadSheet src, CLI_Section item, int oldBar, int newBar)
    {
        super(src, item);
        Preconditions.checkArgument(oldBar > 0 && newBar > 0 && oldBar != newBar, "item=%s oldBar=%s newBar=%s", item, oldBar, newBar);
        Preconditions.checkArgument(item.getPosition().getBar() == newBar || item.getPosition().getBar() == oldBar,
                "item=%s oldBar=%s newBar=%s", item, oldBar, newBar);
        this.oldBar = oldBar;
        this.newBar = newBar;
    }


    /**
     * @return The barIndex of the section before it was moved
     */
    public int getOldBar()
    {
        return oldBar;
    }

    public int getNewBar()
    {
        return newBar;
    }

    public CLI_Section getCLI_Section()
    {
        return (CLI_Section) getItem();
    }

    @Override
    public String toString()
    {
        return "SectionMovedEvent[section=" + getCLI_Section() + ", oldBar=" + oldBar + ", newBar=" + newBar + "]";
    }
}

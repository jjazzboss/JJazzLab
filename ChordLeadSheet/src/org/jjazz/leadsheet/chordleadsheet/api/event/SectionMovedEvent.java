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

/**
 * One section has been moved from a number of bars.
 */
public class SectionMovedEvent extends ClsChangeEvent
{

    /**
     * The index of the section before it was moved.
     */
    private int prevBar;

    public SectionMovedEvent(ChordLeadSheet src, CLI_Section item, int prevBar)
    {
        super(src, item);
        this.prevBar = prevBar;
    }

    /**
     * @return The barIndex of the section before it was moved
     */
    public int getPrevBar()
    {
        return prevBar;
    }

    public CLI_Section getSection()
    {
        return (CLI_Section) getItem();
    }

    @Override
    public String toString()
    {
        return "SectionMovedEvent[section=" + getSection() + " prevBar=" + prevBar + "]";
    }
}

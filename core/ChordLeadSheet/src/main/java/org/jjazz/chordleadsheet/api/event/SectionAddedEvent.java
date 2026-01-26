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
import java.util.List;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * A CLI_Section was added, possibly replacing an existing one in the same bar.
 */
public class SectionAddedEvent extends ClsChangeEvent
{

    private final CLI_Section replacedCliSection;
    private final List<ChordLeadSheetItem> adjustedItems;

    /**
     *
     * @param src
     * @param cliSection         The added section
     * @param replacedCliSection The replaced section (must be in the same bar than cliSection). Can be null.
     * @param adjustedItems      Possible items (like CLI_ChordSymbols) whose beat was moved sooner in the bar because of a new time signature with less beats.
     *                           Can be empty.
     */
    public SectionAddedEvent(ChordLeadSheet src, CLI_Section cliSection, CLI_Section replacedCliSection, List<ChordLeadSheetItem> adjustedItems)
    {
        super(src, cliSection);
        Preconditions.checkArgument(replacedCliSection == null || replacedCliSection.getPosition().getBar() == cliSection.getPosition().getBar(),
                "cliSection=%s replacedCliSection=%s", cliSection, replacedCliSection);
        this.replacedCliSection = replacedCliSection;
        this.adjustedItems = adjustedItems;
    }

    public List<ChordLeadSheetItem> getAdjustedItems()
    {
        return adjustedItems;
    }

    public CLI_Section getReplacedSection()
    {
        return replacedCliSection;
    }

    public CLI_Section getCLI_Section()
    {
        return (CLI_Section) getItem();
    }

    @Override
    public String toString()
    {
        return "SectionAddedEvent[item=" + getCLI_Section() + ", replacedCliSection=" + replacedCliSection + ", adjustedItems=" + adjustedItems + "]";
    }
}

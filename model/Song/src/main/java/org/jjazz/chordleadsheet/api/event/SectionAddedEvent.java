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
 * A CLI_Section was added, possibly replacing an existing one in the same bar.
 * <p>
 * getItemChanges() returns PROP_ITEM_POSITION change events: adjusted items because of a TimeSignature change.
 */
public class SectionAddedEvent extends ClsChangeEvent
{

    private final CLI_Section sameBarReplacedCliSection;
    private final CLI_Section previousBarSection;

    /**
     *
     * @param src
     * @param cliSection         The added section
     * @param replacedCliSection The replaced section (must be in the same bar than cliSection). Can be null.
     */
    public SectionAddedEvent(ChordLeadSheet src, CLI_Section cliSection, CLI_Section replacedCliSection)
    {
        super(src, cliSection);
        int bar = cliSection.getPosition().getBar();
        Preconditions.checkArgument(replacedCliSection == null || replacedCliSection.getPosition().getBar() == bar,
                "cliSection=%s replacedCliSection=%s", cliSection, replacedCliSection);
        this.sameBarReplacedCliSection = replacedCliSection;
        this.previousBarSection = bar == 0 ? null : src.getSection(bar - 1);
    }

    /**
     * The optional section at same bar which was replaced by getCLI_Section().
     *
     * @return Can be null
     */
    public CLI_Section getSameBarReplacedSection()
    {
        return sameBarReplacedCliSection;
    }

    /**
     * The section of the previous bar.
     *
     * @return Null if getCLI_Section() is at bar 0
     */
    public CLI_Section getPreviousBarSection()
    {
        return previousBarSection;
    }

    public CLI_Section getCLI_Section()
    {
        return (CLI_Section) getItem();
    }

    @Override
    public String toString()
    {
        return "SectionAddedEvent[item=" + getCLI_Section() + ", replacedCliSection=" + sameBarReplacedCliSection + "]";
    }
}

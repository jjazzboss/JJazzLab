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
 * A CLI_Section was removed.
 * <p>
 * getItemChanges() returns PROP_ITEM_POSITION change events: the adjusted items because of a TimeSignature change.
 */
public class SectionRemovedEvent extends ClsChangeEvent
{

    private final CLI_Section previousBarSection;

    /**
     *
     * @param src
     * @param cliSection The removed section
     */
    public SectionRemovedEvent(ChordLeadSheet src, CLI_Section cliSection)
    {
        super(src, cliSection);
        Preconditions.checkArgument(cliSection.getPosition().getBar() > 0, "cliSection=%s", cliSection);
        var pos = cliSection.getPosition();
        this.previousBarSection = src.getSection(pos.getBar() - 1);
    }

    /**
     * The removed section.
     *
     * @return
     */
    public CLI_Section getCLI_Section()
    {
        return (CLI_Section) getItem();
    }

    /**
     * The replacing section from previous bar.
     *
     * @return
     */
    public CLI_Section getPreviousBarSection()
    {
        return previousBarSection;
    }


    @Override
    public String toString()
    {
        return "SectionRemovedEvent[item=" + getCLI_Section() + "]";
    }
}

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
import java.util.Objects;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.item.CLI_Section;

/**
 * One section name and/or TimeSignature has changed.
 * <p>
 * getItemChanges() returns a PROP_ITEM_DATA change event for the section and possibly some PROP_ITEM_POSITION change events: the adjusted items because of a
 * TimeSignature change.
 */
public class SectionChangedEvent extends ClsChangeEvent
{

    private final Section oldData;
    private final Section newData;

    /**
     *
     * @param src
     * @param item
     * @param oldData
     * @param newData
     */
    public SectionChangedEvent(ChordLeadSheet src, CLI_Section item, Section oldData, Section newData)
    {
        super(src, item);
        Objects.requireNonNull(oldData);
        Objects.requireNonNull(newData);
        Preconditions.checkArgument(!oldData.equals(newData), "oldData=%s", oldData);
        this.oldData = oldData;
        this.newData = newData;
    }


    public Section getOldSection()
    {
        return oldData;
    }

    public Section getNewSection()
    {
        return newData;
    }

    public boolean isNameChanged()
    {
        return !oldData.getName().equals(newData.getName());
    }

    public boolean isTimeSignatureChanged()
    {
        return !oldData.getTimeSignature().equals(newData.getTimeSignature());
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

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
package org.jjazz.chordleadsheet.api.item;

import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.harmony.api.Position;

public interface WritableItem<T> extends ChordLeadSheetItem<T>
{

    public void setPosition(Position pos);

    public void setData(T data);

    /**
     * Set an optional container for this item.
     * <p>
     * No consistency check is performed: caller is responsible to make sure this item is consistent with cls (e.g. position is within cls bounds, section does
     * not have a name clash, ...).
     *
     * @param cls Can be null.
     */
    public void setContainer(ChordLeadSheet cls);
}

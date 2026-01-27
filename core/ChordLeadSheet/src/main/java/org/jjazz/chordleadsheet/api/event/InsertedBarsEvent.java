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
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * Some bars were inserted.
 * <p>
 * getItems() returns the shifted items.
 */
public class InsertedBarsEvent extends ClsChangeEvent
{

    private final int barFrom;
    private final int nbBars;

    public InsertedBarsEvent(ChordLeadSheet src, int barFrom, int nbBars, List<ChordLeadSheetItem> shiftedItems)
    {
        super(src, shiftedItems);
        this.barFrom = barFrom;
        this.nbBars = nbBars;
    }

    public int getBarFrom()
    {
        return barFrom;
    }

    public int getBarTo()
    {
        return barFrom + nbBars - 1;
    }

    public int getNbBars()
    {
        return nbBars;
    }

    @Override
    public String toString()
    {
        return "InsertedBarsEvent[barFrom=" + barFrom + " nbBars=" + nbBars + "]";
    }

    public String toDebugString()
    {
        return "InsertedBarsEvent[barFrom=" + barFrom + " nbBars=" + nbBars + "shiftedItems=" + getItems() + "]";
    }

}

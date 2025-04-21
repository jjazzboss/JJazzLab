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

import java.util.Comparator;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Comparator which uses Position then PositionOrder to order ChordLeadSheetItems.
 *
 * Conditions to return the 0 value can be adjusted at construction.
 */
public class CLI_Comparator<T extends ChordLeadSheetItem<?>> implements Comparator<T>
{

    private final boolean useZeroForEquals;
    static final Logger LOGGER = Logger.getLogger(CLI_Comparator.class.getSimpleName());

    /**
     *
     * @param useZeroForEquals It true, returns 0 only when cli1.equals(cli2), otherwise only when cli1 == cli2.
     */
    public CLI_Comparator(boolean useZeroForEquals)
    {
        this.useZeroForEquals = useZeroForEquals;
    }

    @Override
    public int compare(T cli1, T cli2)
    {
        Objects.requireNonNull(cli1);
        Objects.requireNonNull(cli2);
        if ((useZeroForEquals && cli1.equals(cli2)) || cli1 == cli2)
        {
            return 0;
        }
        
        int res = cli1.getPosition().compareTo(cli2.getPosition());
        if (res == 0)
        {
            res = Integer.compare(cli1.getPositionOrder(), cli2.getPositionOrder());
            if (res == 0)
            {
                // e.g. for non-isBarSingleItem() item like CLI_ChordSymbol
                res = Long.compare(System.identityHashCode(cli1), System.identityHashCode(cli2));
                LOGGER.log(Level.FINE, "compareTo() Using hashcode to compare this={0} and other={1} -> res={2}", new Object[]
                {
                    cli1, cli2, res
                });
            }
        }
        return res;
    }

}

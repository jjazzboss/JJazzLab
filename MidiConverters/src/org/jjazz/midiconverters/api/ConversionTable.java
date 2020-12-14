/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midiconverters.api;

/**
 * A bidirectional conversion table between instrument indexes of 2 banks, bankFrom and bankTo.
 */
public class ConversionTable
{

    private String id;
    private int[] map;
    private int[] reverseMap;

    /**
     * Create a ConversionTable.
     * <p>
     * bankFrom size must be &gt;= bankTo size.
     *
     * @param id For debugging message, e.g. "GM2=>GS"
     * @param map map[bankFromIndex] = bankToIndex. bankToIndex must not be &gt; (bankFrom.size()-1);
     */
    public ConversionTable(String id, int[] map)
    {
        if (id == null || id.isEmpty() || map == null || map.length == 0)
        {
            throw new IllegalArgumentException("id=" + id + " map=" + map);   //NOI18N
        }
        this.map = map;
    }

    /**
     * Get the index in bankTo corresponding to the specified index in bankFrom.
     * <p>
     * E.g. for a "GM2=>GS" table, get the GS index corresponding to a GM2 index.
     *
     * @param bankFromIndex
     * @return
     */
    public int convert(int bankFromIndex)
    {
        return map[bankFromIndex];
    }

    /**
     * Get the index in bankFrom corresponding to the specified index in bankTo.
     *
     * E.g. for a "GM2=>GS" table, get the GM2 index corresponding to a GS index.
     * 
     * @param bankToIndex
     * @return
     */
    public int reverseConvert(int bankToIndex)
    {
        if (reverseMap == null)
        {
            // First use, build the reverse table
            reverseMap = new int[map.length];  // might be too large but not a problem
            int lastBankToIdx = -1;
            for (int bankFromIdx = 0; bankFromIdx < map.length; bankFromIdx++)
            {
                int bankToIdx = map[bankFromIdx];
                if (bankToIdx != lastBankToIdx)
                {
                    // Save only the first bankFromIndex if several ones are mapped to the same bankToIndex
                    reverseMap[bankToIdx] = bankFromIdx;
                }
                lastBankToIdx = bankToIdx;
            }
        }
        return reverseMap[bankToIndex];
    }

    @Override
    public String toString()
    {
        return id;
    }
}

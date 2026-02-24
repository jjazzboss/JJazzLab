/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.chordleadsheet.item;

import java.util.Objects;
import java.util.function.Supplier;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * Helper methods to make ChordLeadSheetItems correctly implement equals()/hashCode()/compareTo().
 */
public class CLI_Utils
{
    // Static helper methods

    /**
     * Generic implementation consistent with {@link #equals(org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem, java.lang.Object).
     *
     * @param cli
     * @param other
     * @return @see #compareToSamePosition(org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem)
     */
    static public int compareTo(ChordLeadSheetItem<?> cli, ChordLeadSheetItem<?> other)
    {
        Objects.requireNonNull(cli);
        Objects.requireNonNull(other);

        return performReadAPImethod(cli, () -> 
        {
            if (cli.equals(other))
            {
                return 0;
            }
            int res = cli.getPosition().compareTo(other.getPosition());
            if (res == 0)
            {
                res = Integer.compare(cli.getPositionOrder(), other.getPositionOrder());
                if (res == 0)
                {
                    // same position, same position order => both items should be instance of the same ChordLeadSheetItem subclass
                    res = cli.compareToSamePosition(other);
                }
            }
            assert res != 0;        // For consistency with equals(), VERY important because ChordLeadSheetItems are used in order-based collections such as TreeSet
            return res;
        });
    }

    /**
     * Generic equals method relying only on data and position.
     * <p>
     *
     * @param item Cannot be null
     * @param o
     * @return
     */
    static public boolean equals(ChordLeadSheetItem<?> item, Object o)
    {
        Objects.requireNonNull(item);
        if (o == null || item.getClass() != o.getClass())
        {
            return false;
        }

        var cli = (ChordLeadSheetItem<?>) o;
        return performReadAPImethod(item, () -> item.getData().equals(cli.getData()) && item.getPosition().equals(cli.getPosition()));
    }

    /**
     * Generic hashCode method relying on data and position.
     * <p>
     * @param item
     * @return
     */
    static public int hashCode(ChordLeadSheetItem<?> item)
    {
        Objects.requireNonNull(item);
        return performReadAPImethod(item, () -> 
        {
            int hash = 7;
            hash = 37 * hash + item.getPosition().hashCode();
            hash = 37 * hash + item.getData().hashCode();
            return hash;
        });
    }

    static private <T> T performReadAPImethod(ChordLeadSheetItem<?> item, Supplier<T> operation)
    {
        var res = switch (item)
        {
            case CLI_SectionImpl cli ->
                cli.performReadAPImethod(operation);
            case CLI_BarAnnotationImpl cli ->
                cli.performReadAPImethod(operation);
            case CLI_ChordSymbolImpl cli ->
                cli.performReadAPImethod(operation);
            default -> throw new IllegalArgumentException("item=" + item);
        };

        return res;
    }
}

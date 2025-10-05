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
package org.jjazz.cl_editor.api;

import java.util.Objects;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;

/**
 * A selected ChordLeadSheetItem in the chordleadsheet editor.
 * <p>
 * This is an immutable class.
 */
final public class SelectedCLI implements Comparable<SelectedCLI>
{

    private final ChordLeadSheetItem<?> cli;


    /**
     *
     * @param cli Not null
     */
    public SelectedCLI(ChordLeadSheetItem<?> cli)
    {
        Objects.requireNonNull(cli);
        this.cli = cli;
    }

    public ChordLeadSheetItem getItem()
    {
        return cli;
    }

    /**
     * Relies on cli identity only.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == null || this.getClass() != o.getClass())
        {
            return false;
        }
        SelectedCLI s = (SelectedCLI) o;
        return s.cli == cli;
    }

    /**
     * Relies on cli identity only.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 53 * hash + System.identityHashCode(cli);
        return hash;
    }

    @Override
    public String toString()
    {
        return "sel(" + cli.toString() + ")";
    }

    @Override
    public int compareTo(SelectedCLI other)
    {
        Objects.requireNonNull(other);
        int res = cli.compareTo(other.cli);
        return res;
    }
}

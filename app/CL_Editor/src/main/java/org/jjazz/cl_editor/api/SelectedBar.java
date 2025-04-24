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
import org.jjazz.chordleadsheet.api.ChordLeadSheet;

/**
 * A class to represent a selected bar in the chordleadsheet editor.
 * <p>
 * This is an immutable class.
 */
final public class SelectedBar implements Comparable<SelectedBar>
{

    /**
     * The modelBarIndex to use for a selectedBar after the end of the leadsheet.
     */
    public static final int POST_END_BAR_MODEL_BAR_INDEX = -1;

    private int barBoxIndex = 0;
    private final ChordLeadSheet container;

    public SelectedBar(int barBoxIndex, ChordLeadSheet cls)
    {
        if (barBoxIndex < 0)
        {
            throw new IllegalArgumentException("barBoxIndex=" + barBoxIndex);
        }
        this.barBoxIndex = barBoxIndex;
        container = cls;
    }

    /**
     *
     * @return
     */
    public int getBarBoxIndex()
    {
        return barBoxIndex;
    }

    /**
     * Return getBarBoxIndex() if it is less than container's size, otherwise return POST_END_BAR_MODEL_BAR_INDEX.
     *
     * @return
     */
    public int getModelBarIndex()
    {
        return barBoxIndex < container.getSizeInBars() ? barBoxIndex : POST_END_BAR_MODEL_BAR_INDEX;
    }

    public ChordLeadSheet getContainer()
    {
        return container;
    }

    /**
     * Relies on container and barBoxIndex.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        SelectedBar sb = (SelectedBar) o;
        return sb.barBoxIndex == barBoxIndex && container == sb.container;
    }

    /**
     * Relies on container and barBoxIndex.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 19 * hash + this.barBoxIndex;
        hash = 19 * hash + System.identityHashCode(this.container);
        return hash;
    }

    @Override
    public String toString()
    {
        return "s-bar" + barBoxIndex + "(modelBarIndex=" + getModelBarIndex() + ")";
    }

    /**
     * Comparison based on barBoxIndex, ignore container.
     *
     * @param t
     * @return
     */
    @Override
    public int compareTo(SelectedBar t)
    {
        return Integer.compare(barBoxIndex, t.barBoxIndex);
    }
}

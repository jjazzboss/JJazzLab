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
package org.jjazz.proswing.walkingbass.generator;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * A SimpleChordSequence extension which can define usable bars.
 */
public class SimpleChordSequenceExt extends SimpleChordSequence
{

    private final List<Integer> usableBars = new ArrayList<>();

    /**
     * Build an object from a SimpleChordSequence.
     *
     * @param scs
     * @param addUsableBars If true all bars from scs are considered usable
     */
    public SimpleChordSequenceExt(SimpleChordSequence scs, boolean addUsableBars)
    {
        super(scs.getBarRange(), scs.getTimeSignature());
        addAll(scs);
        if (addUsableBars)
        {
            usableBars.addAll(scs.getBarRange().stream().boxed().toList());
        }
    }

    /**
     * Build an empty object.
     *
     * @param barRange
     * @param timeSignature
     * @param addUsableBars If true all bars from barRange are considered usable
     */
    private SimpleChordSequenceExt(IntRange barRange, TimeSignature timeSignature, boolean addUsableBars)
    {
        super(barRange, timeSignature);
        if (addUsableBars)
        {
            usableBars.addAll(barRange.stream().boxed().toList());
        }
    }


    /**
     * Merge this SimpleChordSequenceExt with scs into a new instance.
     * <p>
     * You might want to use removeRedundantChords() on the result.
     *
     * @param scs           must have the same TimeSignature that this object.
     * @param addUsableBars If true the added bars are considered new usable bars.
     * @return A new SimpleChordSequenceExt instance
     * @see #removeRedundantChords()
     */
    public SimpleChordSequenceExt getMerged(SimpleChordSequence scs, boolean addUsableBars)
    {
        Preconditions.checkArgument(scs.getTimeSignature() == getTimeSignature(), "scs.ts=%s this.ts=%s", scs.getTimeSignature(), getTimeSignature());
        IntRange newRange = scs.getBarRange().getUnion(getBarRange());
        SimpleChordSequenceExt res = new SimpleChordSequenceExt(newRange, getTimeSignature(), false);
        res.addAll(this);
        res.addAll(scs);
        var newUsableBars = new ArrayList<>(usableBars);
        if (addUsableBars)
        {
            newUsableBars.addAll(scs.getBarRange().stream().boxed().toList());
        }
        res.setUsableBars(newUsableBars);
        return res;
    }

    public boolean isUsable(int bar)
    {
        return usableBars.contains(bar);
    }

    public boolean isUsable(IntRange rg)
    {
        for (int bar : rg)
        {
            if (!isUsable(bar))
            {
                return false;
            }
        }
        return true;
    }

    public void setUsableBars(List<Integer> bars)
    {
        usableBars.clear();
        usableBars.addAll(bars);
        Collections.sort(usableBars);
        if (!usableBars.isEmpty()
            && (!getBarRange().contains(usableBars.get(0)) || !getBarRange().contains(usableBars.get(usableBars.size() - 1))))
        {
            throw new IllegalArgumentException("barRange=" + getBarRange() + " usableBars=" + usableBars);
        }
    }

    /**
     * Ascending order.
     *
     * @return
     */
    public List<Integer> getUsableBars()
    {
        return Collections.unmodifiableList(usableBars);
    }

    @Override
    public String toString()
    {
        return super.toString() + "usableBars=" + usableBars;
    }

}

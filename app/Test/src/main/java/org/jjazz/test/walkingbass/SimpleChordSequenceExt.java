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
package org.jjazz.test.walkingbass;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * A SimpleChordSequence extension which can define non-usable bars.
 */
public class SimpleChordSequenceExt extends SimpleChordSequence
{

    private final List<Integer> usableBars = new ArrayList<>();

    public SimpleChordSequenceExt(SimpleChordSequence scs)
    {
        super(scs.getBarRange(), scs.getTimeSignature());
        addAll(scs);
    }

    private SimpleChordSequenceExt(IntRange barRange, TimeSignature timeSignature)
    {
        super(barRange, timeSignature);
    }

    @Override
    public SimpleChordSequenceExt merge(SimpleChordSequence scs)
    {
        Preconditions.checkArgument(scs.getTimeSignature() == getTimeSignature());
        IntRange newRange = scs.getBarRange().getUnion(getBarRange());
        SimpleChordSequenceExt res = new SimpleChordSequenceExt(newRange, getTimeSignature());
        res.addAll(this);
        res.addAll(scs);
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
        return usableBars;
    }

    @Override
    public String toString()
    {
        return "<" + getBarRange() + super.toString() + " usableBars=" + usableBars + ">";
    }

}

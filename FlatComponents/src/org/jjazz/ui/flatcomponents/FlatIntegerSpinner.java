/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.ui.flatcomponents;

import java.util.ArrayList;

/**
 * A flat spinner for integers. E
 */
public class FlatIntegerSpinner extends FlatSpinner<Integer>
{

    public FlatIntegerSpinner()
    {
        setEditable(true);
    }

    public void setPossibleValues(int min, int max)
    {
        if (min == max || max < min)
        {
            throw new IllegalArgumentException("min=" + min + " max=" + max);
        }
        ArrayList<Integer> values = new ArrayList<>();
        for (int i = min; i <= max; i++)
        {
            values.add(i);
        }
        setPossibleValues(values);
    }

    @Override
    public Integer stringToValue(String s)
    {
        Integer r = null;
        try
        {
            r = new Integer(s);
        } catch (NumberFormatException e)
        {
        }
        return r;
    }
}

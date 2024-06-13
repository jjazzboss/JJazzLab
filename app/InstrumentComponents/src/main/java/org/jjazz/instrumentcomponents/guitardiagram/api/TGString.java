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
 *  
 */
 /*
 *  NOTE: code reused and modified from the TuxGuitar software (GNU Lesser GPL license), author: Julián Gabriel Casadesús
 */
package org.jjazz.instrumentcomponents.guitardiagram.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class TGString
{
    
    public static final int[][] DEFAULT_TUNING_VALUES =
    {
        {
            43, 38, 33, 28
        },
        {
            43, 38, 33, 28, 23
        },
        {
            64, 59, 55, 50, 45, 40
        },
        {
            64, 59, 55, 50, 45, 40, 35
        },
    };
    private int number;
    private int value;

    public TGString()
    {
        this.number = 0;
        this.value = 0;
    }

    public int getNumber()
    {
        return this.number;
    }

    public int getValue()
    {
        return this.value;
    }

    public void setNumber(int number)
    {
        this.number = number;
    }

    public void setValue(int value)
    {
        this.value = value;
    }

    public boolean isEqual(TGString string)
    {
        return (this.getNumber() == string.getNumber() && this.getValue() == string.getValue());
    }

    @Override
    public TGString clone()
    {
        TGString tgString = new TGString();
        tgString.copyFrom(this);
        return tgString;
    }

    public void copyFrom(TGString string)
    {
        this.setNumber(string.getNumber());
        this.setValue(string.getValue());
    }


    public static List<TGString> createDefaultInstrumentStrings()
    {
        return createDefaultInstrumentStrings(6);
    }

    public static List<TGString> createDefaultPercussionStrings()
    {
        return createPercussionStrings(6);
    }

    public static List<TGString> createDefaultInstrumentStrings(int stringCount)
    {
        return createStrings(stringCount, DEFAULT_TUNING_VALUES);
    }

    public static List<TGString> createPercussionStrings(int stringCount)
    {
        return createStrings(stringCount, null);
    }

    public static List<TGString> createStrings(int stringCount, int[][] defaultTunings)
    {
        List<TGString> strings = new ArrayList<TGString>();
        if (defaultTunings != null)
        {
            for (int i = 0; i < defaultTunings.length; i++)
            {
                if (stringCount == defaultTunings[i].length)
                {
                    for (int n = 0; n < defaultTunings[i].length; n++)
                    {
                        strings.add(newString((n + 1), defaultTunings[i][n]));
                    }
                    break;
                }
            }
        }
        if (strings.isEmpty())
        {
            for (int i = 1; i <= stringCount; i++)
            {
                strings.add(newString(i, 0));
            }
        }
        return strings;
    }

    public static TGString newString(int number, int value)
    {
        TGString string = new TGString();
        string.setNumber(number);
        string.setValue(value);
        return string;
    }

    public static int[] getTuning(List<TGString> strings)
    {
        int[] tuning = new int[strings.size()];
        Iterator<TGString> it = strings.iterator();
        while (it.hasNext())
        {
            TGString string = it.next();
            tuning[(tuning.length - string.getNumber())] = string.getValue();
        }
        return tuning;
    }

}

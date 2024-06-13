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
package org.jjazz.yamjjazz.rhythm.api;

/**
 * The possible types of a StylePart, eg "Main_A", "Intro_A", etc.
 */
public enum StylePartType
{
    Intro_A, Intro_B, Intro_C, Intro_D, Main_A, Main_B, Main_C, Main_D, Fill_In_AA, Fill_In_BB, Fill_In_CC, Fill_In_DD, Fill_In_BA, Fill_In_AB, Ending_A, Ending_B, Ending_C, Ending_D;

    /**
     * @return Same as name() except '_' is replaced by ' ' : "Intro_A" => "Intro A"
     */
    @Override
    public String toString()
    {
        return this.name().replace('_', ' ');
    }

    /**
     * True if type is a fill or a break, eg a "Fill In XX" type.
     *
     * @return
     */
    public boolean isFillOrBreak()
    {
        return name().contains("_In_");
    }

    public boolean isIntro()
    {
        return name().startsWith("Intro_");
    }

    public boolean isEnding()
    {
        return name().startsWith("Ending_");
    }

    /**
     * True if type is a main, eg "Main A" to "Main D"
     *
     * @return
     */
    public boolean isMain()
    {
        return name().startsWith("Main");
    }

    /**
     * Get the corresponding fill type of a main type.
     * <p>
     * Eg Return "Fill In BB" if this="Main B".
     *
     * @return Null if this type is not a Main type.
     */
    public StylePartType getFill()
    {
        StylePartType t = null;
        switch (this)
        {
            case Main_A:
                t = Fill_In_AA;
                break;
            case Main_B:
                t = Fill_In_BB;
                break;
            case Main_C:
                t = Fill_In_CC;
                break;
            case Main_D:
                t = Fill_In_DD;
                break;
        }
        return t;
    }

    /**
     * @param s Must be a string such as returned by Type.toString()
     * @return e.g. Intro_C for s="Intro C". Null if no match found (string match is case sensitive).
     * @see toString()
     */
    public static StylePartType getType(String s)
    {
        for (StylePartType sp : StylePartType.values())
        {
            if (sp.toString().equals(s))
            {
                return sp;
            }
        }
        return null;
    }

}

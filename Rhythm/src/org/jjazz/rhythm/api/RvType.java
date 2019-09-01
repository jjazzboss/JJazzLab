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
package org.jjazz.rhythm.api;

import java.util.ArrayList;
import java.util.List;
import org.jjazz.midi.GM1Bank;

/**
 * The type of accompaniment voice.
 * <p>
 * It is not the instrument type: an organ can be used to play a bass type, Pad type can be performed by strings or a synth,
 * Keyboard type accompaniment can be performed by a vibraphone, etc...
 */
public enum RvType
{
    Percussion, Drums, Bass, Keyboard, Guitar, Pad, Horn_Section, Other;

    @Override
    public String toString()
    {
        switch (this)
        {
            case Drums:
                return "Drums";
            case Percussion:
                return "Perc";
            case Bass:
                return "Bass";
            case Keyboard:
                return "Keyb";
            case Guitar:
                return "Guit";
            case Pad:
                return "Pad";
            case Horn_Section:
                return "Horn";
            case Other:
                return "Other";
        }
        return null;
    }

    /**
     * Percussion=8, Drums=10, ...., Other=15
     *
     * @return
     */
    public int getPreferredChannel()
    {
        return this.ordinal() + 8;
    }

    public String toLongString()
    {
        return name().replace("_", " ");
    }

    /**
     * Try to return the default GM Family associated to this type.
     *
     * @return Can be null if no possible association (eg if type==Drums, Percussion or Other)
     */
    public GM1Bank.Family getDefaultFamily()
    {
        GM1Bank.Family f;
        switch (this)
        {
            case Keyboard:
                f = GM1Bank.Family.Piano;
                break;
            case Guitar:
                f = GM1Bank.Family.Guitar;
                break;
            case Bass:
                f = GM1Bank.Family.Bass;
                break;
            case Pad:
                f = GM1Bank.Family.Synth_Pad;
                break;
            case Horn_Section:
                f = GM1Bank.Family.Reed;
                break;
            default:
                // Other, Drums, Percussion
                f = null;
        }
        return f;
    }

    /**
     * Try to return a default type associated to a GM Family.
     *
     * @param gmFamily
     * @return Can be null if no possible association.
     */
    static public RvType getType(GM1Bank.Family gmFamily)
    {
        if (gmFamily == null)
        {
            throw new NullPointerException("gmFamily");
        }
        RvType type = null;
        switch (gmFamily)
        {
            case Organ:
            case Chromatic_Percussion:
            case Piano:
                type = RvType.Keyboard;
                break;
            case Guitar:
                type = RvType.Guitar;
                break;
            case Bass:
                type = RvType.Bass;
                break;
            case Ensemble:
            case Synth_Pad:
                type = RvType.Pad;
                break;
            case Brass:
            case Reed:
            case Pipe:
                type = RvType.Horn_Section;
                break;
            default:
                // Synth_Lead, Synth_Effects, Ethnic, Sound_Effects, Percussive, Strings
                type = null;
        }
        return type;
    }

    /**
     * Get the list of values as String.
     *
     * @return
     */
    static public List<String> getValuesAsString()
    {
        ArrayList<String> res = new ArrayList<>();
        for (RvType type : RvType.values())
        {
            res.add(type.name());
        }
        return res;
    }

}

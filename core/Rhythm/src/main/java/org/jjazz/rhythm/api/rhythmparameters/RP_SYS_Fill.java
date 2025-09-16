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
package org.jjazz.rhythm.api.rhythmparameters;

import org.jjazz.rhythm.api.RP_State;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.utilities.api.ResUtil;

/**
 * Standard RhythmParameter: is there a fill at the end of a SongPart.
 * <p>
 */
public class RP_SYS_Fill extends RP_State
{

    public static String ID = "RpFillID";
    public static final String VALUE_ALWAYS = "always";
    public static final String VALUE_RANDOM = "random";
    public static final String VALUE_RANDOM_RARE = "random_rare";
    public static final String VALUE_BREAK = "break";
    public static final String VALUE_FADE_OUT = "fade_out";

    /**
     * Possible values: "" (default), "always", "random_rare", "random", "break"
     *
     * @param isPrimary
     */
    public RP_SYS_Fill(boolean isPrimary)
    {
        super(ID, ResUtil.getString(RP_SYS_Fill.class, "CTL_RpFillName"), ResUtil.getString(RP_SYS_Fill.class, "CTL_RpFillDesc"), isPrimary, "", "",
                VALUE_ALWAYS, VALUE_RANDOM, VALUE_RANDOM_RARE, VALUE_BREAK, VALUE_FADE_OUT);
    }

    public RP_SYS_Fill(boolean isPrimary, String defaultVal, String... possibleValues)
    {
        super(ID, ResUtil.getString(RP_SYS_Fill.class, "CTL_RpFillName"), ResUtil.getString(RP_SYS_Fill.class, "CTL_RpFillDesc"), isPrimary, defaultVal,
                possibleValues);
    }

    static public RP_SYS_Fill getFillRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");
        }
        return (RP_SYS_Fill) rhythm.getRhythmParameters()
                .stream()
                .filter(r -> (r instanceof RP_SYS_Fill))
                .findAny()
                .orElse(null);
    }

    /**
     * Return true if we need to add a Fill In.
     *
     * @param rpFillValue
     * @return
     */
    static public boolean needFill(String rpFillValue)
    {
        boolean r = false;
        rpFillValue = rpFillValue.toLowerCase();
        double x = Math.random();
        if (rpFillValue.contains(RP_SYS_Fill.VALUE_ALWAYS) || rpFillValue.contains(RP_SYS_Fill.VALUE_BREAK))
        {
            r = true;
        } else if (rpFillValue.contains(RP_SYS_Fill.VALUE_RANDOM_RARE) && x <= 0.25)
        {
            r = true;
        } else if (rpFillValue.contains(RP_SYS_Fill.VALUE_RANDOM) && x <= 0.5)
        {
            r = true;
        }
        return r;
    }
}

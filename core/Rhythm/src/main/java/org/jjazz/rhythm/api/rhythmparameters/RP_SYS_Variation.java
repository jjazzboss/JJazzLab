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

import java.util.Objects;
import org.jjazz.rhythm.api.RP_State;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.utilities.api.ResUtil;

/**
 * Standard RhythmParameter : a variation of a rhythm.
 * 
 */
public class RP_SYS_Variation extends RP_State
{

    public static String ID = "rpVariationID";

    public RP_SYS_Variation(boolean isPrimary)
    {
        super(ID, ResUtil.getString(RP_SYS_Variation.class, "CTL_StdVariationName"), ResUtil.getString(RP_SYS_Variation.class, "CTL_StdVariationDesc"), isPrimary, "V1", "V1", "V2", "V3");
    }

    public RP_SYS_Variation(boolean isPrimary, String defaultVal, String... possibleValues)
    {
        super(ID, ResUtil.getString(RP_SYS_Variation.class, "CTL_StdVariationName"), ResUtil.getString(RP_SYS_Variation.class, "CTL_StdVariationDesc"), isPrimary, defaultVal, possibleValues);
    }

    /**
     * Find the first RP_STD_Variation instance in the rhythm parameters of r.
     *
     * @param rhythm
     * @return Can be null if not found
     */
    static public RP_SYS_Variation getVariationRp(Rhythm rhythm)
    {
        Objects.requireNonNull(rhythm);
        return (RP_SYS_Variation) rhythm.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_SYS_Variation))
                .findAny()
                .orElse(null);
    }

}

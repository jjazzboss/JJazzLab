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
package org.jjazz.rhythm.api.rhythmparameters;

import org.jjazz.rhythm.api.RP_State;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.util.api.ResUtil;

/**
 * Standard RhythmParameter
 */
public class RP_STD_Variation extends RP_State
{

    public static String ID = "rpVariationID";

    public RP_STD_Variation()
    {
        super(ID, ResUtil.getString(RP_STD_Variation.class, "CTL_StdVariationName"), ResUtil.getString(RP_STD_Variation.class, "CTL_StdVariationDesc"), "V1", "V1", "V2", "V3");
    }

    public RP_STD_Variation(String defaultVal, String... possibleValues)
    {
        super(ID, ResUtil.getString(RP_STD_Variation.class, "CTL_StdVariationName"), ResUtil.getString(RP_STD_Variation.class, "CTL_StdVariationDesc"), defaultVal, possibleValues);
    }

    /**
     * Find the first RP_STD_Variation instance in the rhythm parameters of r.
     *
     * @param rhythm
     * @return Can be null if not found
     */
    static public RP_STD_Variation getVariationRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");   //NOI18N
        }
        return (RP_STD_Variation) rhythm.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_STD_Variation))
                .findAny()
                .orElse(null);
    }
    
}

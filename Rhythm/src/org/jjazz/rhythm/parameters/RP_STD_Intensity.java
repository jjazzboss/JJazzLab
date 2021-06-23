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
package org.jjazz.rhythm.parameters;

import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.util.api.ResUtil;

/**
 * Standard RhythmParameter: Intensity=[-10;+10]
 */
public final class RP_STD_Intensity extends RP_Integer
{

    public static String ID = "rpIntensityID";

    public RP_STD_Intensity()
    {
        super(ID, ResUtil.getString(RP_STD_Intensity.class, "CTL_RpIntensityName"), ResUtil.getString(RP_STD_Intensity.class, "CTL_RpIntensityDesc"), 0, -10, 10, 1);
    }

    public RP_STD_Intensity(int defaultVal, int minVal, int maxVal, int step)
    {
        super(ID, ResUtil.getString(RP_STD_Intensity.class, "CTL_RpIntensityName"), ResUtil.getString(RP_STD_Intensity.class, "CTL_RpIntensityDesc"), defaultVal, minVal, maxVal, step);
    }

    static public RP_STD_Intensity getIntensityRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");   //NOI18N
        }
        return (RP_STD_Intensity) rhythm.getRhythmParameters()
                .stream()
                .filter(r -> (r instanceof RP_STD_Intensity))
                .findAny()
                .orElse(null);
    }
}

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

import com.google.common.base.Preconditions;
import org.jjazz.rhythm.api.RP_Integer;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.utilities.api.ResUtil;

/**
 * Standard RhythmParameter: Intensity=[-10;+10]
 */
public final class RP_SYS_Intensity extends RP_Integer
{

    public static String ID = "rpIntensityID";

    public RP_SYS_Intensity(boolean isPrimary)
    {
        super(ID, ResUtil.getString(RP_SYS_Intensity.class, "CTL_RpIntensityName"), ResUtil.getString(RP_SYS_Intensity.class, "CTL_RpIntensityDesc"), isPrimary,
                0, -10, 10, 1);
    }

    public RP_SYS_Intensity(boolean isPrimary, int defaultVal, int minVal, int maxVal, int step)
    {
        super(ID, ResUtil.getString(RP_SYS_Intensity.class, "CTL_RpIntensityName"), ResUtil.getString(RP_SYS_Intensity.class, "CTL_RpIntensityDesc"), isPrimary,
                defaultVal, minVal, maxVal, step);
    }

    /**
     * The recommended velocity shift to apply for a given RP_SYS_Intensity value.
     *
     * @param rpValue
     * @return
     */
    static public int getRecommendedVelocityShift(int rpValue)
    {
        Preconditions.checkArgument(rpValue >= -10 && rpValue <= 10, "rpValue=%s", rpValue);
        return 3 * rpValue;
    }

    static public RP_SYS_Intensity getIntensityRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");
        }
        return (RP_SYS_Intensity) rhythm.getRhythmParameters()
                .stream()
                .filter(r -> (r instanceof RP_SYS_Intensity))
                .findAny()
                .orElse(null);
    }
}

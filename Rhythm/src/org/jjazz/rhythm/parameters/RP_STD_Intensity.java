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
import org.openide.util.NbBundle.Messages;
import static org.jjazz.rhythm.parameters.Bundle.*;

/**
 * Standard RhythmParameter: Intensity=[-10;+10]
 */
@Messages(
        {
            "CTL_RpIntensityName=Intensity",
            "CTL_RpIntensityDesc=Rhythm intensity",
        })
public final class RP_STD_Intensity extends RP_Integer
{

    public static String ID = "rpIntensityID";

    public RP_STD_Intensity()
    {
        super(ID, CTL_RpIntensityName(), CTL_RpIntensityDesc(), 0, -10, 10, 1);
    }

    public RP_STD_Intensity(int defaultVal, int minVal, int maxVal, int step)
    {
        super(ID, CTL_RpIntensityName(), CTL_RpIntensityDesc(), defaultVal, minVal, maxVal, step);
    }
    
     static public RP_STD_Intensity getIntensityRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");
        }
        return (RP_STD_Intensity) rhythm.getRhythmParameters()
                .stream()
                .filter(r -> (r instanceof RP_STD_Intensity))
                .findAny()
                .orElse(null);
    }
}

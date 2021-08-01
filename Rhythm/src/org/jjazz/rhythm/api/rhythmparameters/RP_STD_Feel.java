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
import org.openide.util.NbBundle;

/**
 * Standard RhythmParameter: double feel
 */
@NbBundle.Messages(
        {

        })
public final class RP_STD_Feel extends RP_State
{

    public static String ID = "rpFeelID";

    public RP_STD_Feel()
    {
        super(ID, ResUtil.getString(RP_STD_Feel.class, "CTL_StdFeelName"), ResUtil.getString(RP_STD_Feel.class, "CTL_StdFeelDesc"), "", "", "half-time");
    }

    static public RP_STD_Feel getFeelRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");   //NOI18N
        }
        return (RP_STD_Feel) rhythm.getRhythmParameters()
                .stream()
                .filter(r -> (r instanceof RP_STD_Feel))
                .findAny()
                .orElse(null);
    }
}

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
import static org.jjazz.rhythm.parameters.Bundle.CTL_StdFeelDesc;
import static org.jjazz.rhythm.parameters.Bundle.CTL_StdFeelName;
import org.openide.util.NbBundle;

/**
 * Standard RhythmParameter: double feel
 */
@NbBundle.Messages(
        {
            "CTL_StdFeelName=Hafl-time feel",
            "CTL_StdFeelDesc=Give a feeling tempo is half timed",
        })
public final class RP_STD_Feel extends RP_State
{

    public static String ID = "rpFeelID";

    public RP_STD_Feel()
    {
        super(ID, CTL_StdFeelName(), CTL_StdFeelDesc(), "", "", "half-time");
    }

    static public RP_STD_Feel getFeelRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");
        }
        return (RP_STD_Feel) rhythm.getRhythmParameters()
                .stream()
                .filter(r -> (r instanceof RP_STD_Feel))
                .findAny()
                .orElse(null);
    }
}

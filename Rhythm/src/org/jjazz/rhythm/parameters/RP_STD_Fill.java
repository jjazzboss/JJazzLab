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

import org.openide.util.NbBundle;
import static org.jjazz.rhythm.parameters.Bundle.*;

/**
 * Standard RhythmParameter: is there a fill at the end of a SongPart.
 * <p>
 */
@NbBundle.Messages(
        {
            "CTL_RpFillName=Fill",
            "CTL_RpFillDesc=Add a rhythm fill at last bar",
        })
public class RP_STD_Fill extends RP_State
{

    public static String ID = "RpFillID";

    /**
     * Possible values: "" (default), "always", "random_rare", "random", "break"
     */
    public RP_STD_Fill()
    {
        super(ID, CTL_RpFillName(), CTL_RpFillDesc(), "", "", "always", "random", "random_rare", "break");
    }

    public RP_STD_Fill(String defaultVal, String... possibleValues)
    {
        super(ID, CTL_RpFillName(), CTL_RpFillDesc(), defaultVal, possibleValues);
    }
}

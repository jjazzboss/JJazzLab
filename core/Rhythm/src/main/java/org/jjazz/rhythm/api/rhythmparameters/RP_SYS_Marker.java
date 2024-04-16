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
 * Standard RhythmParameter: a marker for song parts, used to conditionnaly enable the optional alternate chord symbols.
 * <p>
 * Possible values are Theme, Theme2..Theme4, Solo, Solo2...Solo4.<br>
 * This RP is not primary and can't be customized: it's a shared instance.
 */
public class RP_SYS_Marker extends RP_State
{

    private static RP_SYS_Marker INSTANCE;
    public static final String ID = "rpSysMarkerID";

    public static final String THEME = "theme";
    public static final String SOLO = "solo";

    public static RP_SYS_Marker getInstance()
    {
        synchronized (RP_SYS_Marker.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RP_SYS_Marker();
            }
        }
        return INSTANCE;
    }

    private RP_SYS_Marker()
    {
        super(ID, ResUtil.getString(RP_SYS_Marker.class, "CTL_SysMarker"), ResUtil.getString(RP_SYS_Marker.class, "CTL_SysMarkerDesc"), false, THEME, THEME, SOLO, THEME + "2", THEME + "3", THEME + "4", SOLO + "2", SOLO + "3", SOLO + "4");
    }

    /**
     * Overridden for backwards compatibility, accept that s can have the wrong upper/lower case.
     *
     * @param s
     * @return
     */
    @Override
    public String loadFromString(String s)
    {
        for (String value : getPossibleValues())
        {
            if (value.equalsIgnoreCase(s))
            {
                return value;
            }
        }
        return null;
    }

    /**
     * Get the RP_SYS_Marker instance if the specified rhythm uses it.
     *
     * @param r
     * @return Can be null if not found
     */
    static public RP_SYS_Marker getMarkerRp(Rhythm r)
    {
        if (r == null)
        {
            throw new NullPointerException("r");
        }
        return r.getRhythmParameters().contains(INSTANCE) ? INSTANCE : null;
    }


}

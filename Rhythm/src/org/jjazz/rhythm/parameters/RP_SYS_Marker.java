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
import org.openide.util.NbBundle;
import static org.jjazz.rhythm.parameters.Bundle.*;

/**
 * Standard RhythmParameter: a marker for song parts, used to conditionnaly enable the optional alternate chord symbols.
 * <p>
 * Possible values are Theme, Theme2..Theme4, Solo, Solo2...Solo4.<br>
 * This RP can't be customized: it's a shared instance.
 */
@NbBundle.Messages(
        {
            "CTL_SysMarker=Marker",
            "CTL_SysMarkerDesc=Marker for a song part",
        })
public class RP_SYS_Marker extends RP_State
{

    private static RP_SYS_Marker INSTANCE;
    public static final String ID = "rpSysMarkerID";

// To be used later if RP_State redesign to use enum
//
//    public enum Values
//    {
//        THEME, THEME2, THEME3, THEME4, SOLO, SOLO2, SOLO3, SOLO4;
//
//        @Override
//        public String toString()
//        {
//            String s = null;
//            String themeStr = "Theme";
//            String soloStr = "Solo";
//            switch (this)
//            {
//                case THEME:
//                    s = themeStr;
//                    break;
//                case THEME2:
//                    s = themeStr + "2";
//                    break;
//                case THEME3:
//                    s = themeStr + "3";
//                    break;
//                case THEME4:
//                    s = themeStr + "4";
//                    break;
//                case SOLO:
//                    s = soloStr;
//                    break;
//                case SOLO2:
//                    s = soloStr + "2";
//                    break;
//                case SOLO3:
//                    s = soloStr + "3";
//                    break;
//                case SOLO4:
//                    s = soloStr + "4";
//                    break;
//                default:
//                    throw new IllegalStateException("this=" + this);
//            }
//            return s;
//        }
//    }
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
        super(ID, CTL_SysMarker(), CTL_SysMarkerDesc(), THEME, THEME, SOLO, THEME + "2", THEME + "3", THEME + "4", SOLO + "2", SOLO + "3", SOLO + "4");
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

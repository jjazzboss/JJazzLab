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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.openide.util.NbBundle;
import static org.jjazz.rhythm.parameters.Bundle.*;

/**
 * System RhythmParameter: mute one or more instruments.
 * <p>
 */
@NbBundle.Messages(
        {
            "CTL_RpMuteName=Mute",
            "CTL_RpMuteDesc=Instrument mute",
        })
public class RP_SYS_Mute extends RP_StringSet
{

    public static String ID = "RpSysMuteID";

    /**
     * Create a RP_SYS_Mute using the specified parameters.
     *
     * @param defaultVal
     * @param possibleValues
     */
    private RP_SYS_Mute(Set<String> defaultVal, String... possibleValues)
    {
        super(ID, CTL_RpMuteName(), CTL_RpMuteDesc(), defaultVal, possibleValues);
    }

    /**
     * A factory method to build a RhythmParameter for a specified rhythm.
     * <p>
     * Use all the rhythmVoice's names to create the possible values of RP_SYS_Mute. Default value is an empty set.
     *
     * @param r
     * @return
     */
    static public RP_SYS_Mute createMuteRp(Rhythm r)
    {
        ArrayList<String> muteValues = new ArrayList<>();
        for (RhythmVoice rv : r.getRhythmVoices())
        {
            muteValues.add(rv.getName());
        }
        RP_SYS_Mute rp = new RP_SYS_Mute(new HashSet<String>(), muteValues.toArray(new String[0]));
        return rp;
    }

    /**
     * Retrieve the muted RhythmVoices from the specified rp value.
     * <p>
     * Works for a RP_SYS_Mute created using createMuteRp(r).
     *
     * @param r
     * @param value
     * @return
     */
    static public List<RhythmVoice> getMutedRhythmVoices(Rhythm r, Set<String> value)
    {
        ArrayList<RhythmVoice> res = new ArrayList<>();
        if (!value.isEmpty())
        {
            HashMap<String, RhythmVoice> mapNameRv = new HashMap<>();
            for (RhythmVoice rv : r.getRhythmVoices())
            {
                mapNameRv.put(rv.getName(), rv);
            }
            for (String s : value)
            {
                RhythmVoice rv = mapNameRv.get(s);
                assert rv != null : "r=" + r + " s=" + s;
                res.add(rv);
            }
        }
        return res;
    }

    /**
     * Find the first RP_SYS_Mute instance in the rhythm parameters of r.
     *
     * @param r
     * @return Can be null if not found
     */
    static public RP_SYS_Mute getMuteRp(Rhythm r)
    {
        if (r == null)
        {
            throw new NullPointerException("r");
        }
        RP_SYS_Mute rpMute = null;
        List<RhythmParameter<?>> rps = r.getRhythmParameters();
        int index = org.jjazz.util.Utilities.indexOfInstance(rps, RP_SYS_Mute.class);
        if (index >= 0)
        {
            rpMute = (RP_SYS_Mute) rps.get(index);
        }
        return rpMute;
    }
}

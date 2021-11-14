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

import org.jjazz.rhythm.api.RP_StringSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.util.api.ResUtil;

/**
 * System RhythmParameter: mute one or more instruments.
 * <p>
 */
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
        super(ID, ResUtil.getString(RP_SYS_Mute.class, "CTL_RpMuteName"), ResUtil.getString(RP_SYS_Mute.class, "CTL_RpMuteDesc"), defaultVal, possibleValues);
    }

    /**
     * A factory method to build a RhythmParameter for a specified rhythm.
     * <p>
     * Use the rhythmVoice's names to create the possible values of RP_SYS_Mute. If a RhythmVoice is a RhythmVoiceDelegate, use
     * the name of its source RhythmVoice instead.
     *
     * @param r
     * @return
     */
    static public RP_SYS_Mute createMuteRp(Rhythm r)
    {
        RP_SYS_Mute rp = new RP_SYS_Mute(new HashSet<String>(), getMuteValuesMap(r).keySet().toArray(new String[0]));
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
        var res = new ArrayList<RhythmVoice>();
        if (!value.isEmpty())
        {
            var map = getMuteValuesMap(r);
            for (String s : value)
            {
                RhythmVoice rv = map.get(s);
                assert rv != null : "r=" + r + " s=" + s + " value=" + value;   //NOI18N
                res.add(rv);
            }
        }
        return res;
    }

    /**
     * Find the first RP_SYS_Mute instance in the rhythm parameters of r.
     *
     * @param rhythm
     * @return Can be null if not found
     */
    static public RP_SYS_Mute getMuteRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");   //NOI18N
        }
        return (RP_SYS_Mute) rhythm.getRhythmParameters()
                .stream()
                .filter(r -> (r instanceof RP_SYS_Mute))
                .findAny()
                .orElse(null);
    }

    // ========================================================================================
    // Private methods
    // ========================================================================================
    /**
     * Get a map muteValue->RhythmVoice for the specified rhythm.
     * <p>
     *
     * @param r
     * @return
     */
    static private HashMap<String, RhythmVoice> getMuteValuesMap(Rhythm r)
    {
        var res = new HashMap<String, RhythmVoice>();
        r.getRhythmVoices().forEach(rv -> res.put(rv.getName(), rv));
        return res;
    }

}

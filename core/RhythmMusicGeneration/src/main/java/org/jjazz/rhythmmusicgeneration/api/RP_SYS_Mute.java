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
package org.jjazz.rhythmmusicgeneration.api;

import org.jjazz.rhythm.api.RP_StringSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.utilities.api.ResUtil;

/**
 * System RhythmParameter: mute one or more instruments.
 * <p>
 */
public class RP_SYS_Mute extends RP_StringSet
{

    public static String ID = "RpSysMuteID";
    /**
     * Special UserRhythmVoice instance to indicate that the "1st" user track of the MidiMix should be muted.
     *
     * @see #getMutedRhythmVoices(org.jjazz.rhythm.api.Rhythm, org.jjazz.midimix.api.MidiMix, java.util.Set)
     */
    public static UserRhythmVoice RV_MUTE_USER_TRACK_1 = new UserRhythmVoice("UserTrack1");
    /**
     * Special UserRhythmVoice instance to indicate that the "2nd" user track of the MidiMix should be muted.
     *
     * @see #getMutedRhythmVoices(org.jjazz.rhythm.api.Rhythm, org.jjazz.midimix.api.MidiMix, java.util.Set)
     */
    public static UserRhythmVoice RV_MUTE_USER_TRACK_2 = new UserRhythmVoice("UserTrack2");
    /**
     * Special UserRhythmVoice instance to indicate that the "3rd" user track of the MidiMix should be muted.
     *
     * @see #getMutedRhythmVoices(org.jjazz.rhythm.api.Rhythm, org.jjazz.midimix.api.MidiMix, java.util.Set
     */
    public static UserRhythmVoice RV_MUTE_USER_TRACK_3 = new UserRhythmVoice("UserTrack3");

    /**
     * Create a RP_SYS_Mute using the specified parameters.
     *
     * @param isPrimary
     * @param defaultVal
     * @param possibleValues
     */
    private RP_SYS_Mute(boolean isPrimary, Set<String> defaultVal, String... possibleValues)
    {
        super(ID, ResUtil.getString(RP_SYS_Mute.class, "CTL_RpMuteName"), ResUtil.getString(RP_SYS_Mute.class, "CTL_RpMuteDesc"), isPrimary, defaultVal,
                possibleValues);
    }

    /**
     * A factory method to build a RhythmParameter for a specified rhythm.
     * <p>
     * Use the rhythmVoice's names to create the possible values of RP_SYS_Mute. If a RhythmVoice is a RhythmVoiceDelegate, use the name of its source
     * RhythmVoice instead. Add also the names of the 3 special UserRhythmVoices RV_MUTE_USER_TRACK_1/2/3.
     *
     * @param r
     * @param isPrimary
     * @return
     */
    static public RP_SYS_Mute createMuteRp(Rhythm r, boolean isPrimary)
    {
        var values = getMuteValuesMap(r).keySet();
        RP_SYS_Mute rp = new RP_SYS_Mute(isPrimary, new HashSet<>(), values.toArray(String[]::new));
        return rp;
    }

    /**
     * Retrieve the muted RhythmVoices from the specified rp value.
     * <p>
     * Works for a RP_SYS_Mute created using createMuteRp(r).
     *
     * @param r
     * @param midiMix Needed in order to retrieve the corresponding UserRhythmVoice if value contains some of the RV_MUTE_USER_TRACK_1/2/3 names.
     * @param value
     * @return
     */
    static public List<RhythmVoice> getMutedRhythmVoices(Rhythm r, MidiMix midiMix, Set<String> value)
    {
        var res = new ArrayList<RhythmVoice>();
        if (!value.isEmpty())
        {
            var map = getMuteValuesMap(r);
            for (String s : value)
            {
                RhythmVoice rv = map.get(s);
                rv = processUserRhythmVoice(midiMix, rv);
                if (rv != null)
                {
                    res.add(rv);
                }
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
            throw new NullPointerException("r");
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
     * Also add the special RV_MUTE_USER_TRACK_1/2/3.
     *
     * @param r
     * @return
     */
    static private Map<String, RhythmVoice> getMuteValuesMap(Rhythm r)
    {
        var res = new HashMap<String, RhythmVoice>();
        r.getRhythmVoices().forEach(rv -> res.put(rv.getName(), rv));
        res.put(RV_MUTE_USER_TRACK_1.getName(), RV_MUTE_USER_TRACK_1);
        res.put(RV_MUTE_USER_TRACK_2.getName(), RV_MUTE_USER_TRACK_2);
        res.put(RV_MUTE_USER_TRACK_3.getName(), RV_MUTE_USER_TRACK_3);
        return res;
    }

    /**
     * Handle the case where rv is one of the 3 special UserRhythmVoices RV_MUTE_USER_TRACK_1/2/3.
     * <p>
     * For example if rv == RV_MUTE_USER_TRACK_1, return the 1st user rhythm voice from the MidiMix.
     *
     * @param mm
     * @param rv
     * @return Can be null.
     */
    static private RhythmVoice processUserRhythmVoice(MidiMix mm, RhythmVoice rv)
    {

        RhythmVoice res = switch (rv)
        {
            case UserRhythmVoice urv when urv == RV_MUTE_USER_TRACK_1 ->
                getUserRhythmVoice(mm, 1);
            case UserRhythmVoice urv when urv == RV_MUTE_USER_TRACK_2 ->
                getUserRhythmVoice(mm, 2);
            case UserRhythmVoice urv when urv == RV_MUTE_USER_TRACK_3 ->
                getUserRhythmVoice(mm, 3);
            default ->
                rv;
        };
        return res;
    }

    static private RhythmVoice getUserRhythmVoice(MidiMix mm, int urvIndex)
    {
        List<UserRhythmVoice> urvs = mm.getUserRhythmVoices();
        return urvIndex <= urvs.size() ? urvs.get(urvIndex - 1) : null;
    }
    
    // ========================================================================================
    // Inner classes
    // ========================================================================================
}

/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.yamjjazz.rhythm;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;

/**
 * Service to propose an appropriate rhythm to YamJJazz importers.
 */
public class ImporterRhythmFinder
{

    private static final Map<String, String> MAP_TEXT_RHYTHMID = new HashMap<>();
    protected static final Logger LOGGER = Logger.getLogger(ImporterRhythmFinder.class.getName());

    /**
     * Try to find a rhythm from the specified parameters.
     * <p>
     *
     * @param styleText eg "rock", "bossa"
     * @param tempo
     * @param ts
     * @return Can be null
     */
    static public Rhythm findRhythm(String styleText, int tempo, TimeSignature ts)
    {
        Objects.requireNonNull(styleText);
        Objects.requireNonNull(ts);
        Rhythm res = null;


        // Simplify for 3/4: ignore styleText
        if (ts.equals(TimeSignature.THREE_FOUR))
        {
            if (tempo > 170)
            {
                styleText = "waltz swing fast";
            } else if (tempo > 130)
            {
                styleText = "waltz swing";
            } else
            {
                styleText = "waltz swing slow";
            }
        }


        String strRhythm = getValueFromMap(styleText);
        if (strRhythm != null)
        {
            String rId = strRhythm + "-ID";
            try
            {
                res = RhythmDatabase.getDefault().getRhythmInstance(rId);
            } catch (UnavailableRhythmException ex)
            {
                LOGGER.log(Level.WARNING, "findRhythm() can''t get rhythm instance for rId={0}  (styleText={1}) ", new Object[]
                {
                    rId, styleText
                });
            }
        }

        return res;
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================

    /**
     * Get the key which has the longest match for styleText, then return the corresponding value.
     *
     * @param styleText
     * @return Can be null
     */
    static private String getValueFromMap(String styleText)
    {
        if (styleText == null)
        {
            return null;
        }
        styleText = styleText.toLowerCase();

        String res = null;
        int maxKeySize = 0;
        for (String key : getMap().keySet())
        {
            if (styleText.startsWith(key) && key.length() > maxKeySize)
            {
                res = getMap().get(key);
                maxKeySize = key.length();
            }
        }

        return res;
    }

    static private Map<String, String> getMap()
    {
        if (MAP_TEXT_RHYTHMID.isEmpty())
        {
            MAP_TEXT_RHYTHMID.put("afr", "AfroCuban.S730.prs");
            MAP_TEXT_RHYTHMID.put("bossa", "BossaNova2.S469.prs");
            MAP_TEXT_RHYTHMID.put("song-for-my-father", "PopBossa1.S629.prs");
            MAP_TEXT_RHYTHMID.put("cha", "ChaCha.S628.prs");
            MAP_TEXT_RHYTHMID.put("mambo", "Mambo5.S722.prs");
            MAP_TEXT_RHYTHMID.put("rhumba", "PopRumba.S625.bcs");
            MAP_TEXT_RHYTHMID.put("rumba", "PopRumba.S625.bcs");
            MAP_TEXT_RHYTHMID.put("salsa", "BigBandSalsa.STY");
            MAP_TEXT_RHYTHMID.put("samba", "SambaCity213.s460.yjz");
            MAP_TEXT_RHYTHMID.put("latin", "SambaCity213.s460.yjz");
            MAP_TEXT_RHYTHMID.put("calyp", "Calypso.S354.prs");

            MAP_TEXT_RHYTHMID.put("rock", "StandardRock.STY");
            MAP_TEXT_RHYTHMID.put("rock-heavy-even", "StandardRock.STY");
            MAP_TEXT_RHYTHMID.put("rock pop", "AcousticRock.S080.prs");
            MAP_TEXT_RHYTHMID.put("rock-slow", "90'sOrgRockBld.T162.STY");
            MAP_TEXT_RHYTHMID.put("slow rock", "90'sOrgRockBld.T162.STY");
            MAP_TEXT_RHYTHMID.put("rock-triplet", "RockShuffle.S547.bcs");

            MAP_TEXT_RHYTHMID.put("even 8", "Cool8Beat.S737.sst");
            MAP_TEXT_RHYTHMID.put("even 16", "16beat.S556.yjz");

            MAP_TEXT_RHYTHMID.put("folk", "Folkball.S702.sty");
            MAP_TEXT_RHYTHMID.put("blues", "BluesRock.S524.sst");   // son bizarre Em        
            MAP_TEXT_RHYTHMID.put("shuffle", "PopShuffle1.S552.prs");
            MAP_TEXT_RHYTHMID.put("soul", "Soul.S199.prs");
            MAP_TEXT_RHYTHMID.put("funk", "Urban Funk.S066.STY");
            MAP_TEXT_RHYTHMID.put("ballad", "16BeatBallad2.S014.prs");

            MAP_TEXT_RHYTHMID.put("swing", "MediumJazz.S737.sst");
            MAP_TEXT_RHYTHMID.put("medium swing", "MediumJazz.S737.sst");
            MAP_TEXT_RHYTHMID.put("medium slow", "CoolJazzBallad.S738.prs");
            MAP_TEXT_RHYTHMID.put("slow swing", "CoolJazzBallad.S738.prs");
            MAP_TEXT_RHYTHMID.put("medium up", "FastJazz.S741.sst");
            MAP_TEXT_RHYTHMID.put("up tempo", "FastJazz.S741.sst");
            MAP_TEXT_RHYTHMID.put("waltz", "JazzWaltzMed.S351.sst");
            MAP_TEXT_RHYTHMID.put("waltz swing", "JazzWaltzMed.S351.sst");
            MAP_TEXT_RHYTHMID.put("footprints", "JazzWaltzMed.S351.sst");
            MAP_TEXT_RHYTHMID.put("waltz swing slow", "JazzWaltzSlow.S423.prs");
            MAP_TEXT_RHYTHMID.put("waltz swing fast", "JazzWaltzFast.S499.sty");


            // Log errors (if list of available styles has changed)
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            for (String key : MAP_TEXT_RHYTHMID.keySet().toArray(new String[0]))
            {
                String id = MAP_TEXT_RHYTHMID.get(key) + "-ID";
                RhythmInfo ri = rdb.getRhythm(id);
                if (ri == null)
                {
                    LOGGER.log(Level.WARNING, "getMap() initialization : no instance found for rhythmId: {0}", id);
                    MAP_TEXT_RHYTHMID.remove(key);
                }
            }
        }

        return MAP_TEXT_RHYTHMID;

    }
}

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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
     * Find a rhythm from the specified parameters.
     * <p>
     * If no match, return a default rhythm for the tempo and time signature.
     *
     * @param styleText eg "rock", "bossa". If null returns the default rhythm for the time signature.
     * @param tempo
     * @param ts
     * @return Can't be null
     */
    static public Rhythm findRhythm(String styleText, int tempo, TimeSignature ts)
    {
        Objects.requireNonNull(ts);


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


        // Get the mapped rhythmId
        String s = getValueFromMap(styleText);
        if (s == null)
        {
            // Default rhythms
            if (ts.equals(TimeSignature.FOUR_FOUR))
            {
                s = tempo > 130 ? getValueFromMap("bossa") : getValueFromMap("even 8");
                assert s != null;
            } else
            {
                s = "";     // nothing will be found in the database
            }
        }


        String rId = s + "-ID";
        Rhythm r = null;
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        try
        {
            r = (YamJJazzRhythm) rdb.getRhythmInstance(rId);
        } catch (UnavailableRhythmException ex)
        {
            var ri = rdb.getDefaultRhythm(ts);
            LOGGER.log(Level.WARNING, "findRhythm() can''t get rhythm instance for styleText={0} rId={1}. Using {2} instead.", new Object[]
            {
                styleText, rId, ri
            });
            try
            {
                r = rdb.getRhythmInstance(ri);
            } catch (UnavailableRhythmException ex1)
            {
                r = rdb.getDefaultStubRhythmInstance(ts);
                LOGGER.log(Level.WARNING, "findRhythm() Can''t get rhythm instance for {0}. Using rhythm stub {1} instead.", new Object[]
                {
                    ri, r
                });
            }
        }

        assert r != null;
        return r;
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================

    /**
     * Special get: match if the map key matches the first chars of style.
     *
     * @param style
     * @return
     */
    static private String getValueFromMap(String style)
    {
        if (style == null)
        {
            return null;
        }
        String value = null;
        for (String key : getMap().keySet())
        {
            if (style.toLowerCase().startsWith(key.toLowerCase()))
            {
                value = getMap().get(key);
                break;
            }
        }
        return value;
    }

    static Map<String, String> getMap()
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

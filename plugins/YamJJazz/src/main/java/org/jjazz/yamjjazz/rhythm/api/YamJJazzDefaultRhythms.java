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
package org.jjazz.yamjjazz.rhythm.api;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Genre;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import static org.jjazz.yamjjazz.rhythm.api.YamJJazzDefaultRhythms.LOGGER;
import org.openide.windows.OnShowing;

/**
 * YamJJazz default rhythm infos.
 * <p>
 * Provide the Genre and tags of the default YamJJazz rhythms, so they become searchable by the RhythmDatabase via a simple string or RhythmFeatures. This is
 * used for instance when we need to find a substitute rhythm for a song's rhythm which is not available on the system (see SongPartImpl serialization proxy).
 */
public class YamJJazzDefaultRhythms
{

    private static YamJJazzDefaultRhythms INSTANCE;
    private HashMap<String, Genre> DEF_GENRE;
    private Multimap<String, String> DEF_TAGS;
    protected static final Logger LOGGER = Logger.getLogger(YamJJazzDefaultRhythms.class.getName());

    static public YamJJazzDefaultRhythms getInstance()
    {
        synchronized (YamJJazzDefaultRhythms.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new YamJJazzDefaultRhythms();
            }
        }
        return INSTANCE;
    }

    private YamJJazzDefaultRhythms()
    {
        initData();
    }

    /**
     * The names of the default rhythms.
     *
     * @return
     */
    public Collection<String> getDefaultRhythmNames()
    {
        return DEF_GENRE.keySet();
    }

    /**
     * Return the Genre for styleFilename, if it's a default rhythm.
     * <p>
     *
     * @param styleFilename
     * @return Can be null
     */
    public Genre getGenre(String styleFilename)
    {
        Objects.requireNonNull(styleFilename);
        return DEF_GENRE.get(styleFilename);
    }

    /**
     * Return the tags for styleFilename, if it's a default rhythm.
     * <p>
     *
     * @param styleFilename
     * @return Can be an empty list
     */
    public Collection<String> getDefaultTags(String styleFilename)
    {
        Objects.requireNonNull(styleFilename);
        return DEF_TAGS.get(styleFilename); // Multimap returns an empty collection if no match
    }

    private void initData()
    {
        if (DEF_GENRE != null)
        {
            return;
        }
        DEF_GENRE = new HashMap<>();
        DEF_TAGS = ArrayListMultimap.create();

        // Use the tags so that a rhythm can be an approximation for an absent rhythm (eg "tango" => "chacha", not perfect, but better than RockShuffle!)

        add("AfroCuban.S730.prs", Genre.CUBAN, "afr", "cuba");
        add("BossaNova2.S469.prs", Genre.BOSSA, "bossa", "song-for-my-father");
        add("ChaCha.S628.prs", Genre.CHACHACHA, "chach", "cha-ch", "tango");
        add("Mambo5.S722.prs", Genre.MAMBO, "mambo", "mereng", "world", "latin");
        add("PopRumba.S625.bcs", Genre.RUMBA, "rhumba", "rumba");
        add("BigBandSalsa.STY", Genre.SALSA, "salsa");
        add("SambaCity213.s460.yjz", Genre.SAMBA, "samba", "brazi", "brasi", "forro", "chorro");
        add("Calypso.S354.prs", Genre.CALYPSO, "calyp");
        add("HappyReggae.S655.prs", Genre.REGGAE, "reggae");

        add("DiscoHouse.S145.prs", Genre.DANCE, "party", "dance", "club", "disco", "house", "garage");
        add("Folkball.S702.sty", Genre.FOLK, "folk", "country");
        add("BluesRock.S524.sst", Genre.BLUES, "blues");
        add("Soul.S199.prs", Genre.SOUL, "soul");
        add("SoulR&B.S130.prs", Genre.RnB, "r&b", "rnb");
        add("Urban Funk.S066.STY", Genre.FUNK, "funk");
        add("ClassicHipHop.S145.prs", Genre.HIP_HOP, "hip");

        add("StandardRock.STY", Genre.ROCK, "rock", "heavy", "metal");
        add("90'sOrgRockBld.T162.STY", Genre.ROCK, "slow rock", "rock slow", "slow-rock", "rock-slow");
        add("RockShuffle.S547.bcs", Genre.ROCK, "rock-triplet", "rock shuffle");

        add("Cool8Beat.S737.sst", Genre.POP, "even 8", "8beat", "8-beat", "rock pop", "pop rock", "pop-rock");
        add("16beat.S556.yjz", Genre.POP, "even 16", "16beat", "16-beat", "pop bal", "pop bld");
        add("PopShuffle1.S552.prs", Genre.POP, "shuffle");
        add("6-8ModernBallad.S560.prs", Genre.POP, "12/8", "12-8", "6/8", "6-8");

        // Don't need anymore the three 4/4 swing styles now that we have jjSwing
//        add("CoolJazzBallad.S738.prs", Genre.JAZZ, "medium slow", "medium jazz", "slow swing", "slow jazz", "jazz slow", "ballad", "bld");
//        add("MediumJazz.S737.sst", Genre.JAZZ, "swing", "jazz", "medium swing", "jazz medium", "swing medium");
//        add("FastJazz.S741.sst", Genre.JAZZ, "medium up", "up swing", "jazz up", "up tempo");
        add("JazzWaltzSlow.S423.prs", Genre.JAZZ, "waltz swing slow", "jazz waltz slow", "waltz slow", "slow waltz");
        add("JazzWaltzMed.S351.sst", Genre.JAZZ, "waltz", "waltz swing", "jazz waltz", "footprints");
        add("JazzWaltzFast.S499.sty", Genre.JAZZ, "waltz swing fast", "jazz waltz fast", "waltz fast", "slow fast");
        add("FastFolkWaltz7.S093.STY", Genre.JAZZ, "waltz straight");

    }


    private void add(String text, Genre g, String... tags)
    {
        // Consistency checks
        if (DEF_GENRE.containsKey(text))
        {
            LOGGER.log(Level.INFO, "text={0} is already used in DEF_GENRE. g={1}", new Object[]
            {
                text, g
            });
        }
        for (var tag : tags)
        {
            if (DEF_TAGS.values().contains(tag))
            {
                LOGGER.log(Level.INFO, "tag={0} is already used in DEF_TAGS. text={1}", new Object[]
                {
                    tag, text
                });
            }
        }

        DEF_GENRE.put(text, g);
        DEF_TAGS.putAll(text, List.of(tags));
    }


    /**
     * Debug task to check that default rhythms are in the default JJazzLab distribution.
     * <p>
     * Because RhythmDatabase initialization will end up using YamJJazzDefaultRhythm instance, we need to do the check after complete initialization is done,
     * hence @OnShowing.
     */
    // @OnShowing    // enable only for testing
    public static class CheckDefaultRhythms implements Runnable
    {

        @Override
        public void run()
        {
            var rdb = RhythmDatabase.getDefault();
            var names = YamJJazzDefaultRhythms.getInstance().getDefaultRhythmNames();
            for (var name : names)
            {
                var rId = name + "-ID";
                var ri = rdb.getRhythm(rId);
                if (ri == null)
                {
                    LOGGER.log(Level.WARNING, "CheckDefaultRhythms rhythm rId={0} not found", rId);
                } else
                {
                    LOGGER.log(Level.INFO, "CheckDefaultRhythms rId={0} found => rf={1} tags={2}", new Object[]
                    {
                        rId, ri.rhythmFeatures(), List.of(ri.tags())
                    });
                }
            }
        }
    }

}

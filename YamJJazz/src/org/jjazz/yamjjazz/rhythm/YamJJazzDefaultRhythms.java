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
package org.jjazz.yamjjazz.rhythm;

import java.util.HashMap;
import org.jjazz.rhythm.api.Beat;
import org.jjazz.rhythm.api.Feel;
import org.jjazz.rhythm.api.Intensity;
import org.jjazz.rhythm.api.Genre;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.TempoRange;

/**
 * YamJJazz default rhythm infos.
 * <p>
 * Store the RhythmFeatures data associated to some file names.
 */
public class YamJJazzDefaultRhythms
{

    private static YamJJazzDefaultRhythms INSTANCE;
    private static HashMap<String, RhythmFeatures> DEF_RHYTHMS; // Key is lowercase name

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
     * Return the RhythmFeatures if name corresponds to a default rhythm.
     * <p>
     *
     * @param name
     * @return Null if name is not a default rhythm's name
     */
    public RhythmFeatures getDefaultFeatures(String name)
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("name=" + name);   //NOI18N
        }
        return DEF_RHYTHMS.get(name.toLowerCase());
    }

    private void initData()
    {
        if (DEF_RHYTHMS != null)
        {
            return;
        }
        DEF_RHYTHMS = new HashMap<>();
        add("poprock.sty", Genre.ROCK, Feel.BINARY, Beat.EIGHT, TempoRange.MEDIUM, Intensity.MEDIUM);
        add("guitarpop.s557.sty", Genre.POP, Feel.BINARY, Beat.SIXTEEN, TempoRange.MEDIUM_SLOW, Intensity.MEDIUM);
        add("sambacity213.s460.yjz", Genre.LATIN, Feel.BINARY, Beat.SIXTEEN, TempoRange.MEDIUM, Intensity.MEDIUM);
        add("slowbossa.s642.prs", Genre.LATIN, Feel.BINARY, Beat.EIGHT, TempoRange.MEDIUM_SLOW, Intensity.LIGHT);
        add("16beat.s556.yjz", Genre.POP, Feel.BINARY, Beat.SIXTEEN, TempoRange.MEDIUM_SLOW, Intensity.MEDIUM);
        add("jazzrock_cz2k.s653.yjz", Genre.JAZZ, Feel.BINARY, Beat.EIGHT, TempoRange.MEDIUM, Intensity.MEDIUM);
        add("soulbeat.sty", Genre.RB, Feel.BINARY, Beat.EIGHT, TempoRange.MEDIUM, Intensity.MEDIUM);
        add("urban funk.s066.sty", Genre.FUNK, Feel.BINARY, Beat.SIXTEEN, TempoRange.MEDIUM_SLOW, Intensity.LIGHT);
        add("mediumjazz.s737.sst", Genre.JAZZ, Feel.TERNARY, Beat.EIGHT, TempoRange.MEDIUM, Intensity.MEDIUM);
        add("fastjazz.s741.sst", Genre.JAZZ, Feel.TERNARY, Beat.EIGHT, TempoRange.FAST, Intensity.MEDIUM);
        add("jazzwaltzmed.s351.sst", Genre.JAZZ, Feel.TERNARY, Beat.EIGHT, TempoRange.FAST, Intensity.MEDIUM);
        add("jazzwaltzslow.s423.prs", Genre.JAZZ, Feel.TERNARY, Beat.EIGHT, TempoRange.MEDIUM, Intensity.MEDIUM);
        add("country8beat.s650.prs", Genre.COUNTRY, Feel.BINARY, Beat.EIGHT, TempoRange.MEDIUM_FAST, Intensity.LIGHT);        
        add("happyreggae.s655.prs", Genre.REGGAE, Feel.BINARY, Beat.EIGHT, TempoRange.MEDIUM_SLOW, Intensity.MEDIUM);        
    }

    private void add(String n, Genre g, Feel f, Beat b, TempoRange tr, Intensity i)
    {
        DEF_RHYTHMS.put(n.toLowerCase(), new RhythmFeatures(f, b, g, tr, i));
    }

}

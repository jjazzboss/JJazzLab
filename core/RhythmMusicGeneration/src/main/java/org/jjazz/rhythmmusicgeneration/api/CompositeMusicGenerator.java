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
package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.songcontext.api.SongContext;

/**
 * A MusicGenerator which combines several MusicGenerators.
 */
public class CompositeMusicGenerator implements MusicGenerator
{

    private final Multimap<MusicGenerator, RhythmVoice> mapGenRvs;
    private final Rhythm rhythm;
    private static final Logger LOGGER = Logger.getLogger(CompositeMusicGenerator.class.getSimpleName());

    /**
     *
     * @param r
     * @param mapGenRvs MusicGenerators and their associated RhythmVoices. Must be consistent with r.
     */
    public CompositeMusicGenerator(Rhythm r, Multimap<MusicGenerator, RhythmVoice> mapGenRvs)
    {
        Objects.requireNonNull(r);
        Objects.requireNonNull(mapGenRvs);
        Preconditions.checkArgument(mapGenRvs.values().stream().allMatch(rv -> r.getRhythmVoices().contains(rv)), "mapGenRvs=%s", mapGenRvs);
        Preconditions.checkArgument(mapGenRvs.values().stream().allMatch(new HashSet<>()::add), "mapGenRvs=%s", mapGenRvs);    // Detect a rv duplicate

        this.rhythm = r;
        this.mapGenRvs = ArrayListMultimap.create(mapGenRvs);
    }


    @Override
    public Map<RhythmVoice, Phrase> generateMusic(SongContext context, RhythmVoice... rvs) throws MusicGenerationException
    {
        var rhythmRvs = rhythm.getRhythmVoices();
        var rvsList = List.of(rvs);
        Preconditions.checkArgument(rvsList.stream().allMatch(rv -> rhythmRvs.contains(rv)), "rvs=", rvsList);
        var rhythmVoices = rvsList.isEmpty() ? rhythmRvs : rvsList;


        Map<RhythmVoice, Phrase> res = new HashMap<>();


        for (var mg : mapGenRvs.keySet())
        {
            var genRvs = new ArrayList<>(rhythmVoices);
            genRvs.retainAll(mapGenRvs.get(mg));
            if (!genRvs.isEmpty())
            {
                LOGGER.log(Level.FINE, "generateMusic() generating music for mg={0} and rvs={1}", new Object[]
                {
                    mg, genRvs
                });
                var map = mg.generateMusic(context, genRvs.toArray(RhythmVoice[]::new));
                res.putAll(map);
            }
        }

        return res;
    }

}

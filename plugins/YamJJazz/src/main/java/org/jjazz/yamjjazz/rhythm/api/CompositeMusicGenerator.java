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
package org.jjazz.yamjjazz.rhythm.api;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.Exceptions;

/**
 * A MusicGenerator which delegates to other target MusicGenerators.
 * <p>
 * Delegation mechanism is configured via the RvToMgTargetMapper class.
 * <p>
 * This lets a base YamJJazzRhythm instance use more than one MusicGenerator, for example :<br>
 * - use an internal MusicGenerator for the bass RhythmVoice and another internal one for the rest of the RhythmVoices<br>
 * - use 2 internal MusicGenerators for the bass RhythmVoice, depending on the SongPart's RP_SYS_Variation value<br>
 * - use the percussion MusicGenerator from another YamJJazzRhythm instance for our drums<br>
 */
public class CompositeMusicGenerator implements MusicGenerator
{


    /**
     * Identify a target MusicGenerator and its associated target RhythmVoice.
     *
     * @param mg
     * @param rv Must be a RhythmVoice of a YamJJazzRhythm. Can be RhythmVoice of the baseRhythm.
     */
    public record MgTarget(MusicGenerator mg, RhythmVoice rv)
            {

        public MgTarget 
        {
            Objects.requireNonNull(mg);
            Preconditions.checkArgument(rv != null && rv.getContainer() instanceof YamJJazzRhythm, "rv=%s", rv);
        }

        public YamJJazzRhythm getRhythm()
        {
            return (YamJJazzRhythm) rv.getContainer();
        }

        @Override
        public String toString()
        {
            return "MgTarget(" + mg.getClass().getSimpleName() + "," + rv + ")";
        }
    }

    /**
     * Map a base RhythmVoice to a MgTarget for a given SongPart context.
     */
    public interface RvToMgTargetMapper
    {

        /**
         * Get the MgTarget to be used to generate music for baseRv in the context of spt.
         *
         * @param baseRv A RhythmVoice of the baseRhythm
         * @param spt    A SongPart which uses the baseRhythm
         * @return A non-null value
         */
        MgTarget get(RhythmVoice baseRv, SongPart spt);
    }
    private final RvToMgTargetMapper rvMapper;
    private final YamJJazzRhythm baseRhythm;
    private static final Logger LOGGER = Logger.getLogger(CompositeMusicGenerator.class.getSimpleName());


    /**
     * Create a CompositeMusicGenerator for a base YamJJazzRhythm.
     *
     * @param baseRhythm
     * @param rvMapper   get() must return a non-null value for each RhythmVoice with a null SongPart
     */
    public CompositeMusicGenerator(YamJJazzRhythm baseRhythm, RvToMgTargetMapper rvMapper)
    {
        Objects.requireNonNull(baseRhythm);
        Objects.requireNonNull(rvMapper);
        Preconditions.checkArgument(
                baseRhythm.getRhythmVoices().stream().allMatch(rv -> rvMapper.get(rv, null) != null),
                "mgTargetProvider returns a null value for at least one baseRhythm RhythmVoice. baseRhythm=%s", baseRhythm);
        this.baseRhythm = baseRhythm;
        this.rvMapper = rvMapper;
    }

    public YamJJazzRhythm getBaseRhythm()
    {
        return baseRhythm;
    }


    @Override
    public Map<RhythmVoice, Phrase> generateMusic(SongContext sgContext, RhythmVoice... rvs) throws MusicGenerationException
    {
        var rvsList = List.of(rvs);
        var rhythmRvs = baseRhythm.getRhythmVoices();
        Preconditions.checkArgument(rvsList.stream().allMatch(rv -> rhythmRvs.contains(rv)), "rvs=", rvsList);
        var rhythmVoices = rvsList.isEmpty() ? rhythmRvs : rvsList;


        Map<RhythmVoice, Phrase> res = new HashMap<>();


        // Identify the unique MusicGenerators used
        Set<MusicGenerator> uniqueMgs = new HashSet<>();
        for (var spt : sgContext.getSongParts())
        {
            rhythmVoices.stream()
                    .map(rv -> rvMapper.get(rv, spt))
                    .forEach(mgt -> uniqueMgs.add(mgt.mg));
        }


        for (var mg : uniqueMgs)
        {
            List<MgTarget> prevSptMgTargets = null;
            List<SongPart> prevSptList = new ArrayList<>();

            for (var spt : sgContext.getSongParts())
            {
                // Get a MgTargets vector, with null values for MgTargets not using mg
                var sptMgTargets = rhythmVoices.stream()
                        .map(rv -> rvMapper.get(rv, spt))
                        .map(mgt -> mgt.mg == mg ? mgt : null)
                        .toList();

                if (prevSptMgTargets == null)
                {
                    prevSptMgTargets = sptMgTargets;
                } else if (!sptMgTargets.equals(prevSptMgTargets))
                {
                    // At least one target MgTarget has changed because of the SongPart context, start a music generation on the previous subContext
                    var subContext = createSubContext(sgContext, prevSptList);

                    Map<RhythmVoice, MgTarget> mapBaseRvMgTarget = new HashMap<>();
                    for (int i = 0; i < rhythmVoices.size(); i++)
                    {
                        var mgt = prevSptMgTargets.get(i);
                        if (mgt != null)
                        {
                            mapBaseRvMgTarget.put(rhythmVoices.get(i), mgt);
                        }
                    }

                    if (!mapBaseRvMgTarget.isEmpty())
                    {
                        var mapRvPhrases = callGenerator(mg, mapBaseRvMgTarget, subContext);     // throws MusicGenerationException
                        mergePhrases(sgContext.getMidiMix(), res, mapRvPhrases);
                    }

                    prevSptList.clear();
                    prevSptMgTargets = sptMgTargets;
                }

                prevSptList.add(spt);
            }


            // Music generation for the last subContext
            var subContext = createSubContext(sgContext, prevSptList);
            Map<RhythmVoice, MgTarget> mapBaseRvMgTarget = new HashMap<>();
            for (int i = 0; i < rhythmVoices.size(); i++)
            {
                var mgt = prevSptMgTargets.get(i);
                if (mgt != null)
                {
                    mapBaseRvMgTarget.put(rhythmVoices.get(i), mgt);
                }
            }

            var mapRvPhrases = callGenerator(mg, mapBaseRvMgTarget, subContext);     // throws MusicGenerationException
            mergePhrases(sgContext.getMidiMix(), res, mapRvPhrases);
        }


        return res;
    }

    // =================================================================================================================================
    // Private methods
    // =================================================================================================================================

    /**
     * Generate phrases for a "uniform" subContext (which uses a single MusicGenerator with the same source/target RhythmVoices configuration).
     * <p>
     *
     * @param mg
     * @param mapBaseRvMgTarget Associates a MgTarget to a base RhythmVoice. All MgTargets must use mg.
     * @param subContext
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private Map<RhythmVoice, Phrase> callGenerator(MusicGenerator mg, Map<RhythmVoice, MgTarget> mapBaseRvMgTarget, SongContext subContext) throws MusicGenerationException
    {
        Preconditions.checkArgument(!mapBaseRvMgTarget.isEmpty(), "mg=%s subContext=%s", mg.getClass().getSimpleName(), subContext);

        Map<RhythmVoice, Phrase> res = new HashMap<>();

        var targetRvsSorted = mapBaseRvMgTarget.values().stream()
                .filter(mgt -> mgt.mg == mg) // consistency check                
                .sorted((mgt1, mgt2) -> Integer.compare(mgt1.rv.getPreferredChannel(), mgt2.rv.getPreferredChannel()))
                .map(mgt -> mgt.rv)
                .toArray(RhythmVoice[]::new);
        if (targetRvsSorted.length != mapBaseRvMgTarget.size())
        {
            throw new IllegalArgumentException("targetRvsSorted=" + Arrays.asList(targetRvsSorted) + " mg=" + mg + " mapBaseRvMgTarget=" + mapBaseRvMgTarget);
        }


        YamJJazzRhythm targetRhythm = (YamJJazzRhythm) targetRvsSorted[0].getContainer();
        SongContext targetContext = subContext;      // by default     
        if (targetRhythm != baseRhythm)
        {
            // We need a new targetContext with a song using targetRhythm instead of baseRhythm
            targetContext = createTargetContext(subContext, targetRhythm);
        }


        // Call MusicGenerator
        LOGGER.log(Level.FINE, "callGenerator() generating music mg={0} subContext={1}  mapBaseRvMgTarget=\n{2} ", new Object[]
        {
            mg, subContext, mapBaseRvMgTarget
//            mg.getClass().getSimpleName(),
//            subContext.getSongParts().stream().map(spt -> spt.toShortString()).toList(),
//            Utilities.toMultilineString(mapBaseRvMgTarget, "   ")
        });
        var mapRvPhrases = mg.generateMusic(targetContext, targetRvsSorted);


        // We need to map back the Phrases to their source RhythmVoices
        for (var baseRv : mapBaseRvMgTarget.keySet())
        {
            var targetRv = mapBaseRvMgTarget.get(baseRv).rv;
            if (baseRv != targetRv)
            {
                Phrase p = mapRvPhrases.remove(targetRv);
                assert p != null : "targetRv=" + targetRv + " baseRv=" + baseRv;
                mapRvPhrases.put(baseRv, p);
            }
        }


        res.putAll(mapRvPhrases);
        assert res.keySet().size() == mapBaseRvMgTarget.keySet().size() : "res=" + res + " mapBaseRvMgTarget=" + mapBaseRvMgTarget;

        return res;
    }

    /**
     * Merge phrases from src into dest.
     *
     * @param mm
     * @param mapDest
     * @param mapSrc
     */
    private void mergePhrases(MidiMix mm, Map<RhythmVoice, Phrase> mapDest, Map<RhythmVoice, Phrase> mapSrc)
    {
        for (var rv : mapSrc.keySet())
        {
            var pSrc = mapSrc.get(rv);
            mapDest.putIfAbsent(rv, new Phrase(mm.getChannel(rv)));
            var pDest = mapDest.get(rv);
            pDest.add(pSrc);
        }
    }

    /**
     * A subpart of context.
     *
     * @param context
     * @param spts
     * @return
     */
    private SongContext createSubContext(SongContext context, List<SongPart> spts)
    {
        return new SongContext(context, new IntRange(spts.getFirst().getStartBarIndex(), spts.getLast().getBarRange().to));
    }

    /**
     * Create a new context with the baseRhythm replaced by targetRhythm.
     *
     * @param context
     * @param targetRhythm
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private SongContext createTargetContext(SongContext context, YamJJazzRhythm targetRhythm) throws MusicGenerationException
    {
        SongContext res = context.deepClone(false, true);       // setMidiMixSong=true because MidiMix must update itself when we will later replace the rhythm


        // Precheck Midi channels number limit before replacing rhythm
        int unusedChannels = res.getMidiMix().getUnusedChannels().size();
        if (targetRhythm.getRhythmVoices().size() - baseRhythm.getRhythmVoices().size() > unusedChannels)
        {
            throw new MusicGenerationException("CompositeMusicGenerator can not temporarily replace " + baseRhythm.getName()
                    + " by " + targetRhythm.getName() + ": not enough Midi channels");
        }

        // Replace rhythm
        SongStructure sgs = res.getSong().getSongStructure();
        var targetOldSpts = context.getSongParts().stream()
                .map(spt -> sgs.getSongPart(spt.getStartBarIndex()))
                .toList();
        assert targetOldSpts.stream().allMatch(spt -> spt.getRhythm() == baseRhythm) : "targetOldSpts=" + targetOldSpts;

        var targetNewSpts = targetOldSpts.stream()
                .map(spt -> spt.clone(targetRhythm, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection()))
                .toList();
        try
        {
            sgs.replaceSongParts(targetOldSpts, targetNewSpts);
        } catch (UnsupportedEditException ex)
        {
            // Should not happen since we checked before
            Exceptions.printStackTrace(ex);
        }

        return res;
    }

}

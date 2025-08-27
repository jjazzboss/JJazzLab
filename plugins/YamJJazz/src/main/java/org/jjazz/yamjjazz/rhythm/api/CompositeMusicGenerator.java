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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.Exceptions;

/**
 * A rhythm's MusicGenerator which can delegate to other MusicGenerators for specific RhythmVoices, possibly depending on the SongPart context.
 * <p>
 */
public class CompositeMusicGenerator implements MusicGenerator
{


    /**
     * Identify a target MusicGenerator and its associated target RhythmVoice.
     *
     * @param mg
     * @param rv Must be a RhythmVoice of a YamJJazzRhythm
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
    }

    public interface MgTargetProvider
    {

        /**
         * Provide a MgTarget to be used to generate music for baseRv in the context of spt.
         *
         * @param baseRv A RhythmVoice from the baseRhythm
         * @param spt    A SongPart which uses the baseRhythm
         * @return A non-null value
         */
        MgTarget getRvTarget(RhythmVoice baseRv, SongPart spt);
    }
    private final MgTargetProvider mgTargetProvider;
    private final Rhythm baseRhythm;
    private static final Logger LOGGER = Logger.getLogger(CompositeMusicGenerator.class.getSimpleName());


    /**
     * Create a CompositeMusicGenerator for a base YamJJazzRhythm.
     *
     * @param baseRhythm
     * @param mgTargetProvider get() must return a non-null value for each RhythmVoice with a null SongPart
     */
    public CompositeMusicGenerator(YamJJazzRhythm baseRhythm, MgTargetProvider mgTargetProvider)
    {
        Objects.requireNonNull(baseRhythm);
        Objects.requireNonNull(mgTargetProvider);
        Preconditions.checkArgument(
                baseRhythm.getRhythmVoices().stream().allMatch(rv -> mgTargetProvider.getRvTarget(rv, null) != null),
                "mgTargetProvider returns a null value for at least one baseRhythm RhythmVoice. baseRhythm=%s", baseRhythm);
        this.baseRhythm = baseRhythm;
        this.mgTargetProvider = mgTargetProvider;
    }


    @Override
    public Map<RhythmVoice, Phrase> generateMusic(SongContext sgContext, RhythmVoice... rvs) throws MusicGenerationException
    {
        var rvsList = List.of(rvs);
        var rhythmRvs = baseRhythm.getRhythmVoices();
        Preconditions.checkArgument(rvsList.stream().allMatch(rv -> rhythmRvs.contains(rv)), "rvs=", rvsList);
        var rhythmVoices = rvsList.isEmpty() ? rhythmRvs : rvsList;


        Map<RhythmVoice, Phrase> res = new HashMap<>();


        var contextSpts = sgContext.getSongParts();
        List<MgTarget> prevSptMgTargets = null;
        List<SongPart> prevSptList = new ArrayList<>();

        for (var spt : contextSpts)
        {
            var sptMgTargets = rhythmVoices.stream()
                    .map(rv -> mgTargetProvider.getRvTarget(rv, spt))
                    .toList();


            if (prevSptMgTargets == null)
            {
                prevSptMgTargets = sptMgTargets;
            } else if (!sptMgTargets.equals(prevSptMgTargets))
            {
                // At least one target MgTarget has changed because of the SongPart context
                // Start a music generation on the last subContext
                var subContext = createSubContext(sgContext, prevSptList);
                var mapRvPhrases = callTargetGenerators(subContext, prevSptMgTargets);     // throws MusicGenerationException
                mergePhrases(sgContext.getMidiMix(), res, mapRvPhrases);

                prevSptList.clear();
                prevSptMgTargets = sptMgTargets;
            }

            prevSptList.add(spt);
        }


        // Start a music generation on the last subContext
        var subContext = createSubContext(sgContext, prevSptList);
        var mapRvPhrases = callTargetGenerators(subContext, prevSptMgTargets);    // MusicGenerationException
        mergePhrases(sgContext.getMidiMix(), res, mapRvPhrases);


        return res;
    }

    // =================================================================================================================================
    // Private methods
    // =================================================================================================================================

    /**
     * Generate phrases using the MusicGenerators of the mgTargets for the specified subContext.
     *
     * @param subContext
     * @param mgTargets  One mgTarget per base RhythmVoice
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     */
    private Map<RhythmVoice, Phrase> callTargetGenerators(SongContext subContext, List<MgTarget> mgTargets) throws MusicGenerationException
    {
        var allBaseRvs = baseRhythm.getRhythmVoices();
        Preconditions.checkArgument(mgTargets.size() == allBaseRvs.size(), "mgTargets=%s allBaseRvs=%s", mgTargets, allBaseRvs);


        Map<RhythmVoice, Phrase> res = new HashMap<>();

        Set<MusicGenerator> processedMgs = new HashSet<>();
        for (var mgTarget : mgTargets)
        {
            MusicGenerator mg = mgTarget.mg;
            if (processedMgs.contains(mg))
            {
                continue;
            }
            processedMgs.add(mg);

            // Get all the base RhythmVoices for which mg must be used, save the associated target RhythmVoice
            BiMap<RhythmVoice, RhythmVoice> bimapBaseTargetRv = HashBiMap.create();
            for (int i = 0; i < allBaseRvs.size(); i++)
            {
                if (mgTargets.get(i).mg == mg)
                {
                    bimapBaseTargetRv.put(allBaseRvs.get(i), mgTargets.get(i).rv);
                }
            }


            var targetRhythm = mgTarget.getRhythm();
            SongContext targetContext = subContext;      // by default     
            if (targetRhythm != baseRhythm)
            {
                // We need a new targetContext with a song using targetRhythm instead of baseRhythm
                targetContext = createTargetContext(subContext, targetRhythm);
            }


            // Call MusicGenerator
            LOGGER.log(Level.SEVERE, "callTargetGenerators() generating music mg={0} bimapBaseTargetRv={1} spts={2}", new Object[]
            {
                mg.getClass().getSimpleName(), bimapBaseTargetRv, targetContext.getSongParts()
            });
            var mapRvPhrases = mg.generateMusic(targetContext, getSortedRvArray(bimapBaseTargetRv.values()));


            // We need to map back the Phrases to their source RhythmVoices
            for (var targetRv : mapRvPhrases.keySet())
            {
                var baseRv = bimapBaseTargetRv.inverse().get(targetRv);
                if (baseRv != targetRv)
                {
                    Phrase p = mapRvPhrases.remove(targetRv);
                    assert p != null : "targetRv=" + targetRv + " baseRv=" + baseRv;
                    mapRvPhrases.put(baseRv, p);
                }
            }


            res.putAll(mapRvPhrases);

        }

        assert res.keySet().size() == allBaseRvs.size() : "res=" + res + " rvs=" + allBaseRvs;

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

    private RhythmVoice[] getSortedRvArray(Collection<RhythmVoice> rvs)
    {
        RhythmVoice[] res = rvs.toArray(RhythmVoice[]::new);
        Arrays.sort(res, (rv1, rv2) -> Integer.compare(rv1.getPreferredChannel(), rv2.getPreferredChannel()));
        return res;
    }
}

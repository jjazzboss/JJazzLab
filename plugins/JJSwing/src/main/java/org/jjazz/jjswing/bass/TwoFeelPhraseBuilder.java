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
package org.jjazz.jjswing.bass;

import org.jjazz.jjswing.bass.db.WbpSource;
import org.jjazz.jjswing.bass.db.WbpSourceDatabase;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.jjswing.api.BassStyle;
import static org.jjazz.jjswing.bass.BassGenerator.DURATION_BEAT_MARGIN;
import org.jjazz.jjswing.bass.db.Velocities;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;

/**
 * A factory for BassStyle.TWO_FEEL.
 */
public class TwoFeelPhraseBuilder implements PhraseBuilder
{

    private static int sessionCount = 0;
    private static final BassStyle STYLE = BassStyle.TWO_FEEL;
    private static final Logger LOGGER = Logger.getLogger(TwoFeelPhraseBuilder.class.getSimpleName());

    @Override
    public Phrase build(List<SimpleChordSequence> scsList, int tempo)
    {
        LOGGER.log(PhraseBuilderLogLevel, "build() -- tempo={0} scsList={1}", new Object[]
        {
            tempo,
            scsList
        // Utilities.toMultilineString(scsList, " ")
        });


        WbpTiling tiling = new WbpTiling(scsList);
        WbpsaStore store = new WbpsaStore(tiling, tempo);
        store.populate(tiling.getNonTiledBars(), List.of(STYLE));        // pre/post target matching scores of WbpSourceAdaptations will be all zero since tiling is empty


        Predicate<WbpSourceAdaptation> premiumWbpsaTester = wbpsa -> Score.PREMIUM_ONLY_TESTER.test(wbpsa.getCompatibilityScore());
        Predicate<WbpSourceAdaptation> stdWbpsaTester = wbpsa -> Score.DEFAULT_TESTER.test(wbpsa.getCompatibilityScore());


        // PREMIUM PHASE
        LOGGER.log(PhraseBuilderLogLevel, "\n");
        LOGGER.log(PhraseBuilderLogLevel, "build() ================  tiling PREMIUM LongestFirstNoRepeat");
        var tilerLongestPremium = new TilerLongestFirstNoRepeat(premiumWbpsaTester);
        tilerLongestPremium.tile(tiling, store);
        if (LOGGER.isLoggable(PhraseBuilderLogLevel))
        {
            LOGGER.log(PhraseBuilderLogLevel, tiling.toMultiLineString());      // takes time to build
        }
        int nbTiledBars = tiling.getTiledBars().size();


        var untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(PhraseBuilderLogLevel, "\n");
            LOGGER.log(PhraseBuilderLogLevel, "build() ================  tiling PREMIUM MaxDistance");
            var tilerMaxDistancePremium = new TilerMaxDistance(premiumWbpsaTester);
            tilerMaxDistancePremium.tile(tiling, store);
            nbTiledBars = logTilingIfChanged(tiling, nbTiledBars);

        }


        // STANDARD PHASE
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(PhraseBuilderLogLevel, "\n");
            LOGGER.log(PhraseBuilderLogLevel, "build() ================  tiling STANDARD LongestFirstNoRepeat");
            var tilerLongestStandard = new TilerLongestFirstNoRepeat(stdWbpsaTester);
            tilerLongestStandard.tile(tiling, store);
            nbTiledBars = logTilingIfChanged(tiling, nbTiledBars);


            untiled = !tiling.isFullyTiled();
            if (untiled)
            {
                LOGGER.log(PhraseBuilderLogLevel, "\n");
                LOGGER.log(PhraseBuilderLogLevel, "build() ================  tiling STANDARD MaxDistance");
                var tilerMaxDistanceStandard = new TilerMaxDistance(stdWbpsaTester);
                tilerMaxDistanceStandard.tile(tiling, store);
                nbTiledBars = logTilingIfChanged(tiling, nbTiledBars);
            }
        }


        // If still untiled, try using previously computed CUSTOM source phrases
        Predicate<WbpSourceAdaptation> stdCustomTester = wbpsa -> wbpsa.getWbpSource().getBassStyle() == STYLE.getCustomStyle()
                && Score.DEFAULT_TESTER.test(wbpsa.getCompatibilityScore());
        var tilerMaxDistanceCustomStandard = new TilerMaxDistance(stdCustomTester);
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            store.populate(tiling.getNonTiledBars(), List.of(STYLE.getCustomStyle()));

            LOGGER.log(PhraseBuilderLogLevel, "\n");
            LOGGER.log(PhraseBuilderLogLevel, "build() ================  tiling EXISTING CUSTOM MaxDistance");
            tilerMaxDistanceCustomStandard.tile(tiling, store);
            nbTiledBars = logTilingIfChanged(tiling, nbTiledBars);
        }


        // If still untiled, create new CUSTOM source phrases then retile -that should be enough
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(PhraseBuilderLogLevel, "\n");
            LOGGER.log(PhraseBuilderLogLevel, "build() ================  tiling CREATED CUSTOM MaxDistance");

            // Create custom WbpSources 
            var customWbpSources = tiling.buildMissingWbpSources((chordSeq, targetNote) -> create2feelCustomWbpSources(chordSeq, targetNote),
                    WbpSourceDatabase.SIZE_MIN);

            // Add them to the database
            var wbpDb = WbpSourceDatabase.getInstance();
            for (var wbps : customWbpSources)
            {
                if (!wbpDb.addWbpSource(wbps))
                {
                    // This is normal if non-tiled bars contained 2 (or more) similar-but-different bars like |D . .  Ab7#5| and |Eb . . A7#5| which resulted
                    // in tiling.buildMissingWbpSources() above providing WbpSources considered equal by the WbpSourceDatabase   
                    LOGGER.log(Level.INFO, "build() add failed for {0} ", new Object[]
                    {
                        wbps
                    });
                }
            }

            // Update the store to use these new WbpSources
            if (!customWbpSources.isEmpty())
            {
                store.populate(tiling.getNonTiledBars(), List.of(STYLE.getCustomStyle()));
            }

            // Redo a tiling
            tilerMaxDistanceCustomStandard.tile(tiling, store);
            nbTiledBars = logTilingIfChanged(tiling, nbTiledBars);
        }


        // Control
        if (!tiling.isFullyTiled())
        {
            LOGGER.log(Level.WARNING, "build() could not fully tile, untiled bars={0}", tiling.getNonTiledBars());
        }


//        LOGGER.log(PhraseBuilderLogLevel, "\n");
//        LOGGER.log(PhraseBuilderLogLevel, "\n");
//        LOGGER.log(PhraseBuilderLogLevel, "build() ######################      Tiling STATS       ######################");
//        LOGGER.log(PhraseBuilderLogLevel, tiling.toStatsString());
//        LOGGER.log(PhraseBuilderLogLevel, "\n");
        var p = tiling.buildPhrase(new DefaultPhraseAdapter());

        return p;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================

    /**
     * Try to create one or more new WbpSources for scs.
     *
     * @param scs         Must start at bar 0.
     * @param targetPitch -1 if unknown
     * @return
     */
    private List<WbpSource> create2feelCustomWbpSources(SimpleChordSequence scs, int targetPitch)
    {
        Preconditions.checkArgument(scs.getBarRange().from == 0, "subSeq=%s", scs);

        LOGGER.log(Level.ALL, "createCustomWbpSources() - scs={0}", scs);

        if (scs.size() == 1)
        {
            LOGGER.log(Level.WARNING, "createCustomWbpSources() chord sequence with only 1 chord should have been processed earlier. scs={0}", scs);
        }

        var ts = scs.getTimeSignature();
        boolean is2chordsPerBar = scs.isMatchingInBarBeatPositions(false,
                new FloatRange(0, 0.001f),
                new FloatRange(ts.getHalfBarBeat(true) - 0.05f, ts.getHalfBarBeat(true) + 0.05f));
        var idPrefix = is2chordsPerBar ? "c2feel-2chords" : "c2feel-default";
        var phrases = is2chordsPerBar ? create2ChordsPerBarPhrases(scs, targetPitch) : createDefaultPhrases(scs, targetPitch);
        List<WbpSource> res = new ArrayList<>();

        for (var sp : phrases)
        {
            String id = getUniqueCustomId(idPrefix);
            WbpSource wbpSource = new WbpSource(id, 0, STYLE.getCustomStyle(), scs, sp, 0, null);
            WbpSources.humanizeCustomWbpSource(wbpSource);
            res.add(wbpSource);
        }

        return res;
    }


    private String getUniqueCustomId(String prefix)
    {
        var res = prefix + "_" + sessionCount;
        sessionCount++;
        return res;
    }

    /**
     * Create one or more 2-feel bass phrases for a 2-chord per bar chord sequence.
     *
     * @param scs
     * @param targetPitch -1 if unknown
     * @return
     */
    private List<SizedPhrase> create2ChordsPerBarPhrases(SimpleChordSequence scs, int targetPitch)
    {
        Preconditions.checkArgument(scs.size() == 2 && scs.getBarRange().from == 0, "subSeq=%s", scs);


        var ecsBeat0 = scs.first().getData();
        var ecsBeat2 = scs.last().getData();
        assert ecsBeat0 != ecsBeat2;
        int bassPitchBeat0 = InstrumentFamily.Bass.toAbsolutePitch(ecsBeat0.getBassNote().getRelativePitch());
        int bassPitchBeat2 = InstrumentFamily.Bass.toAbsolutePitch(ecsBeat2.getBassNote().getRelativePitch());


        var res = new ArrayList<SizedPhrase>();


        // A basic phrase with only the chord root/bass notes
        SizedPhrase sp = new SizedPhrase(0, scs.getBeatRange(), scs.getTimeSignature(), false);
        NoteEvent ne = new NoteEvent(bassPitchBeat0, 1.85f, 80, 0);
        sp.add(ne);
        ne = new NoteEvent(bassPitchBeat2, 1.85f, 80, 2);
        sp.add(ne);
        res.add(sp);


        // A second phrase with the first note repeated twice
        sp = new SizedPhrase(0, scs.getBeatRange(), scs.getTimeSignature(), false);
        ne = new NoteEvent(bassPitchBeat0, 0.3f, 80, 0);        // shorter
        sp.add(ne);
        ne = new NoteEvent(bassPitchBeat0, 0.85f, 80, 1);
        sp.add(ne);
        ne = new NoteEvent(bassPitchBeat2, 1.85f, 80, 2);
        sp.add(ne);
        res.add(sp);


        return res;
    }


    /**
     * Create 2-feel phrases for all non standard cases of chord positions (minimum 2 chords per bar).
     *
     * @param scs
     * @param targetPitch -1 if unknown
     * @return
     */
    private List<SizedPhrase> createDefaultPhrases(SimpleChordSequence scs, int targetPitch)
    {
        Preconditions.checkArgument(scs.size() >= 2 && scs.getBarRange().from == 0, "subSeq=%s", scs);

        SizedPhrase sp = new SizedPhrase(0, scs.getBeatRange(), scs.getTimeSignature(), false);

        for (var cliCs : scs)
        {
            int relPitch = cliCs.getData().getBassNote().getRelativePitch();
            int bassPitch = InstrumentFamily.Bass.toAbsolutePitch(relPitch);
            FloatRange brCliCsAdjusted = scs.getBeatRange(cliCs).getTransformed(0, -DURATION_BEAT_MARGIN);
            int velocity = Velocities.getRandomBassVelocity();

            if (brCliCsAdjusted.size() >= 2.8f)
            {
                // Play 2 notes
                float addNote1BeatPos = (float) Math.floor(brCliCsAdjusted.to);
                float addNote1Duration = brCliCsAdjusted.to - addNote1BeatPos;

                float duration = addNote1BeatPos - brCliCsAdjusted.from - DURATION_BEAT_MARGIN;

                NoteEvent ne = new NoteEvent(bassPitch, duration, velocity, brCliCsAdjusted.from);
                sp.add(ne);
                ne = new NoteEvent(bassPitch, addNote1Duration, velocity - 4, addNote1BeatPos);
                sp.add(ne);
            } else
            {
                // Play a single note: bass note
                NoteEvent ne = new NoteEvent(bassPitch, brCliCsAdjusted.size(), velocity, brCliCsAdjusted.from);
                sp.add(ne);
            }
        }

        return List.of(sp);
    }


    private int logTilingIfChanged(WbpTiling tiling, int oldNbTiledBars)
    {
        int newNbTiledBars = tiling.getTiledBars().size();
        if (LOGGER.isLoggable(PhraseBuilderLogLevel))
        {
            String txt = oldNbTiledBars != newNbTiledBars ? tiling.toMultiLineString() : "tiling unchanged";            // takes time to build
            LOGGER.log(PhraseBuilderLogLevel, txt);
        }
        return newNbTiledBars;
    }

}

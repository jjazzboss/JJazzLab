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
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.ChordType.DegreeIndex;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.jjswing.api.BassStyle;
import static org.jjazz.jjswing.bass.BassGenerator.DURATION_BEAT_MARGIN;
import org.jjazz.jjswing.bass.db.Velocities;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import static org.jjazz.jjswing.bass.PhraseBuilder.PhraseBuilderLogLevel;

/**
 * A bass phrase builder for BassStyle.WALKING.
 */
public class WalkingPhraseBuilder implements PhraseBuilder
{

    private static int sessionCount = 0;
    private static final BassStyle STYLE = BassStyle.WALKING;
    private static final Logger LOGGER = Logger.getLogger(WalkingPhraseBuilder.class.getSimpleName());

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
        store.populate(tiling.getNonTiledBars(), List.of(STYLE));          // pre/post target matching scores of WbpSourceAdaptations will be all zero since tiling is empty


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

            // Create custom WbpSources and add them to the database
            var customWbpSources = tiling.buildMissingWbpSources((chordSeq, targetNote) -> createWalkingCustomWbpSources(chordSeq, targetNote),
                    WbpSourceDatabase.SIZE_MIN);

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
        var phrase = tiling.buildPhrase(new DefaultPhraseAdapter());


        return phrase;
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
    private List<WbpSource> createWalkingCustomWbpSources(SimpleChordSequence scs, int targetPitch)
    {
        Preconditions.checkArgument(scs.getBarRange().from == 0, "subSeq=%s", scs);

        LOGGER.log(Level.FINE, "createCustomWbpSources() - scs={0}", scs);

        if (scs.size() == 1)
        {
            LOGGER.log(Level.WARNING, "createCustomWbpSources() chord sequence with only 1 chord should have been processed earlier. scs={0}", scs);
        }

        var ts = scs.getTimeSignature();
        boolean is2chordsPerBar = scs.isMatchingInBarBeatPositions(false,
                new FloatRange(0, 0.001f),
                new FloatRange(ts.getHalfBarBeat(true) - 0.05f, ts.getHalfBarBeat(true) + 0.05f));
        var idPrefix = is2chordsPerBar ? "cWalking-2chords" : "cWalking-default";
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
     * Create one or more walking bass phrases for a 2-chord per bar chord sequence.
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
        int bassPitchBeat1 = bassPitchBeat0;
        int bassPitchBeat2 = InstrumentFamily.Bass.toAbsolutePitch(ecsBeat2.getBassNote().getRelativePitch());
        int bassPitchBeat3 = bassPitchBeat2;


        if (!ecsBeat0.isSlashChord())
        {
            int pitch = getClosestPitch(ecsBeat0, bassPitchBeat2, DegreeIndex.THIRD_OR_FOURTH, DegreeIndex.FIFTH, DegreeIndex.SIXTH_OR_SEVENTH);
            if (pitch != -1)
            {
                bassPitchBeat1 = pitch;
            }
        }
        if (!ecsBeat2.isSlashChord())
        {
            int tPitch = targetPitch != -1 ? targetPitch : InstrumentFamily.Bass.toAbsolutePitch(11);        // B
            int pitch = getClosestPitch(ecsBeat0, tPitch, DegreeIndex.THIRD_OR_FOURTH, DegreeIndex.FIFTH, DegreeIndex.SIXTH_OR_SEVENTH);
            if (pitch != -1)
            {
                bassPitchBeat3 = pitch;
            }
        }


        var res = new ArrayList<SizedPhrase>();


        // A basic phrase with only the chord root/bass notes
        SizedPhrase sp = new SizedPhrase(0, scs.getBeatRange(), scs.getTimeSignature(), false);
        NoteEvent ne = new NoteEvent(bassPitchBeat0, 0.9f, 80, 0);
        sp.add(ne);
        ne = new NoteEvent(bassPitchBeat0, 0.9f, 80, 1);
        sp.add(ne);
        ne = new NoteEvent(bassPitchBeat2, 0.9f, 80, 2);
        sp.add(ne);
        ne = new NoteEvent(bassPitchBeat2, 0.9f, 80, 3);
        sp.add(ne);
        res.add(sp);


        if (bassPitchBeat0 != bassPitchBeat1 || bassPitchBeat2 != bassPitchBeat3)
        {
            // A second phrase using the closest notes to target note
            sp = new SizedPhrase(0, scs.getBeatRange(), scs.getTimeSignature(), false);
            ne = new NoteEvent(bassPitchBeat0, 0.9f, 80, 0);
            sp.add(ne);
            ne = new NoteEvent(bassPitchBeat1, 0.9f, 80, 1);
            sp.add(ne);
            ne = new NoteEvent(bassPitchBeat2, 0.9f, 80, 2);
            sp.add(ne);
            ne = new NoteEvent(bassPitchBeat3, 0.9f, 80, 3);
            sp.add(ne);
            res.add(sp);
        }


        return res;
    }


    /**
     * Create phrases for all non standard cases of chord positions (minimum 2 chords per bar).
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
            var ecs = cliCs.getData();
            int relPitch = cliCs.getData().getBassNote().getRelativePitch();
            int bassPitch = InstrumentFamily.Bass.toAbsolutePitch(relPitch);
            FloatRange brCliCsAdjusted = scs.getBeatRange(cliCs).getTransformed(0, -DURATION_BEAT_MARGIN);
            int velocity = Velocities.getRandomBassVelocity();

            if (brCliCsAdjusted.size() >= 2.8f)
            {
                // Play 3 notes: bass note, 3rd, 5th (or always bass note if slash chord)

                int addNote2RelPitch = ecs.isSlashChord() ? relPitch : ecs.getRelativePitch(DegreeIndex.FIFTH);
                int addNote2Pitch = InstrumentFamily.Bass.toAbsolutePitch(addNote2RelPitch);
                float addNote2BeatPos = (float) Math.floor(brCliCsAdjusted.to);
                float addNote2Duration = brCliCsAdjusted.to - addNote2BeatPos;


                int addNote1RelPitch = ecs.isSlashChord() ? relPitch : ecs.getRelativePitch(DegreeIndex.THIRD_OR_FOURTH);
                int addNote1Pitch = InstrumentFamily.Bass.toAbsolutePitch(addNote1RelPitch != -1 ? addNote1RelPitch : addNote2Pitch);
                float addNote1BeatPos = (float) Math.floor(brCliCsAdjusted.to - 1);
                float addNote1Duration = addNote2BeatPos - addNote1BeatPos - DURATION_BEAT_MARGIN;


                float duration = addNote1BeatPos - brCliCsAdjusted.from - DURATION_BEAT_MARGIN;


                NoteEvent ne = new NoteEvent(bassPitch, duration, velocity, brCliCsAdjusted.from);
                sp.add(ne);
                ne = new NoteEvent(addNote1Pitch, addNote1Duration, velocity - 4, addNote1BeatPos);
                sp.add(ne);
                ne = new NoteEvent(addNote2Pitch, addNote2Duration, velocity + 3, addNote2BeatPos);
                sp.add(ne);


            } else if (brCliCsAdjusted.size() >= 1.8f)
            {
                // Play 2 notes: bass note and 5th  (or always bass note if slash chord)
                int addNote1RelPitch = ecs.isSlashChord() ? relPitch : ecs.getRelativePitch(DegreeIndex.FIFTH);
                int addNote1Pitch = InstrumentFamily.Bass.toAbsolutePitch(addNote1RelPitch);
                float addNote1BeatPos = (float) Math.floor(brCliCsAdjusted.to);
                float addNote1Duration = brCliCsAdjusted.to - addNote1BeatPos;


                float duration = addNote1BeatPos - brCliCsAdjusted.from - DURATION_BEAT_MARGIN;


                NoteEvent ne = new NoteEvent(bassPitch, duration, velocity, brCliCsAdjusted.from);
                sp.add(ne);
                ne = new NoteEvent(addNote1Pitch, addNote1Duration, velocity - 4, addNote1BeatPos);
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

    /**
     * Return, amongst the specified degreeIndexes of ecs, the one corresponding to the closest to targetPitch -but different from.
     *
     * @param ecs
     * @param targetPitch
     * @param degreeIndexes 1 or more DegreeIndexes
     * @return Can be -1 if no valid solution could be found.
     */
    private int getClosestPitch(ExtChordSymbol ecs, int targetPitch, DegreeIndex... degreeIndexes)
    {
        Objects.requireNonNull(ecs);
        Preconditions.checkArgument(degreeIndexes.length > 0);
        Preconditions.checkArgument(MidiConst.check(targetPitch), "targetPitch=%s", targetPitch);


        var validPitches = new ArrayList<Integer>();
        for (var di : degreeIndexes)
        {
            var relPitch = ecs.getRelativePitch(di);
            if (relPitch != -1)
            {
                int absPitch = InstrumentFamily.Bass.toAbsolutePitch(relPitch);
                if (targetPitch != absPitch)
                {
                    validPitches.add(absPitch);
                }
            }
        }


        int res = -1;
        for (var pitch : validPitches)
        {
            if (res == -1 || Math.abs(targetPitch - pitch) < Math.abs(targetPitch - res))
            {
                res = pitch;
            }
        }

        return res;

    }

    private int logTilingIfChanged(WbpTiling tiling, int oldNbTiledBars)
    {
        int newNbTiledBars = tiling.getTiledBars().size();
        if (LOGGER.isLoggable(PhraseBuilderLogLevel))
        {
            String txt = oldNbTiledBars != newNbTiledBars ? tiling.toMultiLineString() : "tiling unchanged";        // takes time to build
            LOGGER.log(PhraseBuilderLogLevel, txt);
        }
        return newNbTiledBars;
    }
}

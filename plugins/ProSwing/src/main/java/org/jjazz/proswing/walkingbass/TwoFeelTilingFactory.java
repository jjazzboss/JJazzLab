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
package org.jjazz.proswing.walkingbass;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.proswing.BassStyle;
import org.jjazz.rhythmmusicgeneration.api.DummyGenerator;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;

/**
 * A factory for BassStyle.TWO_FEEL.
 */
public class TwoFeelTilingFactory implements TilingFactory
{

    private static int sessionCount = 0;
    private static final BassStyle STYLE = BassStyle.TWO_FEEL;
    private static final Logger LOGGER = Logger.getLogger(TwoFeelTilingFactory.class.getSimpleName());

    @Override
    public WbpTiling build(SimpleChordSequenceExt scs, int tempo)
    {
        LOGGER.log(Level.SEVERE, "build() -- tempo={1} scs={2}", new Object[]
        {
            tempo, scs
        });


        var settings = WalkingBassMusicGeneratorSettings.getInstance();
        WbpTiling tiling = new WbpTiling(scs);
        var phraseAdapter = new TransposerPhraseAdapter();


        // PREMIUM PHASE
        WbpsaScorer scorerPremium = new WbpsaScorerDefault(phraseAdapter, tempo, Score.PREMIUM_ONLY_TESTER, STYLE);

        LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling PREMIUM LongestFirstNoRepeat");
        var tilerLongestPremium = new TilerLongestFirstNoRepeat(scorerPremium, settings.getWbpsaStoreWidth());
        tilerLongestPremium.tile(tiling);
        LOGGER.log(Level.SEVERE, tiling.toMultiLineString());


        var untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling PREMIUM MaxDistance");
            var tilerMaxDistancePremium = new TilerMaxDistance(scorerPremium, settings.getWbpsaStoreWidth());
            tilerMaxDistancePremium.tile(tiling);
            LOGGER.log(Level.SEVERE, tiling.toMultiLineString());
        }


        // STANDARD PHASE
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling STANDARD LongestFirstNoRepeat");
            WbpsaScorer scorerStandard = new WbpsaScorerDefault(phraseAdapter, tempo, null, STYLE);

            var tilerLongestStandard = new TilerLongestFirstNoRepeat(scorerStandard, settings.getWbpsaStoreWidth());
            tilerLongestStandard.tile(tiling);
            LOGGER.log(Level.SEVERE, tiling.toMultiLineString());

            untiled = !tiling.isFullyTiled();
            if (untiled)
            {
                LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling STANDARD MaxDistance");
                var tilerMaxDistanceStandard = new TilerMaxDistance(scorerStandard, settings.getWbpsaStoreWidth());
                tilerMaxDistanceStandard.tile(tiling);
                LOGGER.log(Level.SEVERE, tiling.toMultiLineString());
            }
        }


        // If still untiled, try using previously computed CUSTOM source phrases
        WbpsaScorer scorerCustom = new WbpsaScorerDefault(phraseAdapter, tempo, null, STYLE.getCustomStyle());
        var tilerMaxDistanceCustomStandard = new TilerMaxDistance(scorerCustom, settings.getWbpsaStoreWidth());
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling EXISTING CUSTOM MaxDistance");
            tilerMaxDistanceCustomStandard.tile(tiling);
            LOGGER.log(Level.SEVERE, tiling.toMultiLineString());
        }


        // If still untiled, create new CUSTOM source phrases then retile -that should be enough
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling CREATED CUSTOM MaxDistance");

            // Create custom WbpSources and add them to the database
            var customWbpSources = tiling.buildMissingWbpSources((chordSeq, targetNote) -> create2feelCustomWbpSources(chordSeq, targetNote),
                    WbpSourceDatabase.SIZE_MIN);

            var wbpDb = WbpSourceDatabase.getInstance();
            for (var wbps : customWbpSources)
            {
                if (!wbpDb.addWbpSource(wbps))
                {
                    LOGGER.log(Level.WARNING, "build() add failed for {1} ", new Object[]
                    {
                        wbps
                    });
                }
            }

            // Redo a tiling
            tilerMaxDistanceCustomStandard.tile(tiling);
            LOGGER.log(Level.SEVERE, tiling.toMultiLineString());
        }


        return tiling;
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
        boolean is2chordsPerBar = scs.isMatchingInBarBeatPositions(false, 0, ts.getHalfBarBeat(true));
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
        SizedPhrase sp = new SizedPhrase(0, scs.getBeatRange(0), scs.getTimeSignature(), false);
        NoteEvent ne = new NoteEvent(bassPitchBeat0, 1.85f, 80, 0);
        sp.add(ne);
        ne = new NoteEvent(bassPitchBeat2, 1.85f, 80, 2);
        sp.add(ne);
        res.add(sp);


        // A second phrase with the first note repeated twice
        sp = new SizedPhrase(0, scs.getBeatRange(0), scs.getTimeSignature(), false);
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

        var velocityRange = WbpSourceDatabase.getInstance().getMostProbableVelocityRange();
        SizedPhrase sp = new SizedPhrase(0, scs.getBeatRange(0), scs.getTimeSignature(), false);

        for (var cliCs : scs)
        {
            var ecs = cliCs.getData();
            int relPitch = cliCs.getData().getBassNote().getRelativePitch();
            int bassPitch = InstrumentFamily.Bass.toAbsolutePitch(relPitch);
            float duration = scs.getChordDuration(cliCs) - 0.1f;
            float beatPos = scs.toPositionInBeats(cliCs.getPosition(), 0);
            float beatPosEnd = beatPos + duration;
            int velocity = velocityRange.from + (int) Math.round(Math.random() * (velocityRange.size() - 1));
            velocity = MidiConst.clamp(velocity);

            if (duration >= 2.8f)
            {
                // Play 2 notes
                float addNote1BeatPos = (float) Math.floor(beatPosEnd);
                float addNote1Duration = beatPosEnd - addNote1BeatPos;

                duration = addNote1BeatPos - beatPos - 0.1f;

                NoteEvent ne = new NoteEvent(bassPitch, duration, velocity, beatPos);
                sp.add(ne);
                ne = new NoteEvent(bassPitch, addNote1Duration, velocity - 4, addNote1BeatPos);
                sp.add(ne);
            } else
            {
                // Play a single note: bass note
                NoteEvent ne = new NoteEvent(bassPitch, duration, velocity, beatPos);
                sp.add(ne);
            }
        }

        return List.of(sp);
    }

}

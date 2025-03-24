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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.proswing.BassStyle;
import org.jjazz.rhythmmusicgeneration.api.DummyGenerator;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * A factory for BassStyle.WALKING.
 */
public class WalkingTilingFactory implements TilingFactory
{

    private static final BassStyle STYLE = BassStyle.WALKING;
    private static final Logger LOGGER = Logger.getLogger(WalkingTilingFactory.class.getSimpleName());

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
        WbpsaScorer scorerCustom = new WbpsaScorerDefault(phraseAdapter, tempo, null, BassStyle.WALKING_CUSTOM);
        var tilerMaxDistanceCustomStandard = new TilerMaxDistance(scorerCustom, settings.getWbpsaStoreWidth());
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling EXISTING CUSTOM MaxDistance");
            tilerMaxDistanceCustomStandard.tile(tiling);
            LOGGER.log(Level.SEVERE, tiling.toMultiLineString());
        }


        // If still untiled, add new CUSTOM source phrases then retile -that should be enough
        untiled = !tiling.isFullyTiled();
        if (untiled)
        {
            LOGGER.log(Level.SEVERE, "\ngetBassPhrase() ================  tiling CREATED CUSTOM MaxDistance");

            // Create custom WbpSourcea and add them to the database
            var customWbpSources = tiling.buildWbpSources(chordSeq -> buildCustomWbpSources(chordSeq));

            var wbpDb = WbpSourceDatabase.getInstance();
            for (var wbps : customWbpSources)
            {
                if (!wbpDb.addWbpSource(wbps))
                {
                    LOGGER.log(Level.WARNING, "handleNonTiledBars() add failed for {1} ", new Object[]
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

    private List<WbpSource> buildCustomWbpSources(SimpleChordSequence scs)
    {
        Preconditions.checkArgument(scs.getBarRange().from == 0 && scs.size() > 1, "subSeq=%s", scs);

        LOGGER.log(Level.SEVERE, "createCustomWbpSources() - scs={0}", scs);

        List<WbpSource> res = new ArrayList<>();
        if (scs.isTwoChordsPerBar(0.25f, false))
        {
            LOGGER.log(Level.SEVERE, "createCustomWbpSources() => 2-chord-per-bar phrase");
            SizedPhrase sp = create2ChordsPerBarPhrase(scs);
            String id = "Gen2Chords-" + (SESSION_COUNT++);
            WbpSource wbpSource = new WbpSource(id, 0, STYLE.getCustomStyle(), scs, sp, 0, null);
            WbpSource wbpGrooveRef = findGrooveReference(sp);
            if (wbpGrooveRef != null)
            {
                Phrases.applyGroove(wbpGrooveRef.getSizedPhrase(), sp, 0.15f);
            } else
            {
                LOGGER.log(Level.WARNING, "createCustomWbpSources() no groove reference found for phrase {0}", sp);
            }

            res.add(wbpSource);

        } else
        {
            LOGGER.log(Level.SEVERE, "createCustomWbpSources() => random phrase");
            var p = DummyGenerator.getBasicBassPhrase(0, scs, new IntRange(50, 65), 0);
            SizedPhrase sp = new SizedPhrase(0, scs.getBeatRange(0), scs.getTimeSignature(), false);
            sp.add(p);
            String id = "GenDefault-" + (SESSION_COUNT++);
            WbpSource wbpSource = new WbpSource(id, 0, STYLE, scs, sp, 0, null);
            res.add(wbpSource);
        }

        return res;
    }


    /**
     * Create a bass phrase for a 2-chord per bar chord sequence.
     *
     * @param subSeq
     * @return
     */
    private SizedPhrase create2ChordsPerBarPhrase(SimpleChordSequence subSeq)
    {
        SizedPhrase sp = new SizedPhrase(0, subSeq.getBeatRange(0), subSeq.getTimeSignature(), false);
        for (var cliCs : subSeq)
        {
            var ecs = cliCs.getData();
            int cBase = 3 * 12;
            int bassPitch0 = cBase + ecs.getRootNote().getRelativePitch();
            int bassPitch1 = cBase + ecs.getRelativePitch(ChordType.DegreeIndex.THIRD_OR_FOURTH);
            float pos0 = subSeq.toPositionInBeats(cliCs.getPosition(), 0);
            float pos1 = pos0 + 1f;
            NoteEvent ne0 = new NoteEvent(bassPitch0, 1f, 80, pos0);
            NoteEvent ne1 = new NoteEvent(bassPitch1, 1f, 80, pos1);
            sp.add(ne0);
            sp.add(ne1);
        }
        return sp;
    }

//    private boolean isGeneratedWbpSource(WbpSource wbpSource)
//    {
//        return wbpSource.getId().startsWith("Gen");
//    }

    /**
     * Find a random WbpSource which can serve as groove reference for sp.
     *
     * @param sp
     * @return
     */
    private WbpSource findGrooveReference(SizedPhrase sp)
    {
        WbpSource res = null;

        var wbpSources = WbpSourceDatabase.getInstance().getWbpSources(sp.getSizeInBars()).stream()
                .filter(w -> !isGeneratedWbpSource(w) && Phrases.isSamePositions(w.getSizedPhrase(), sp, 0.15f))
                .toList();

        if (wbpSources.size() == 1)
        {
            res = wbpSources.get(0);
        } else if (wbpSources.size() > 1)
        {
            int index = (int) (Math.random() * wbpSources.size());
            res = wbpSources.get(index);
        }

        return res;
    }

}
}

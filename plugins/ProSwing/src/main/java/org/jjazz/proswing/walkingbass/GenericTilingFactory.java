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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.proswing.BassStyle;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * A factory based on 4 phases: Premium, Standard, Custom, Create Custom.
 * <p>
 * 1/ Premium: tile with only the "best" WbpSources.<br>
 * 2/ Standard: if required, continue tiling with compatible WbpSources.<br>
 * 3/ Custom: if required, continue tiling with existing custom WbpSources.<br>
 * 4/ Create Custom: if required, create new custom WbpSources to continue tiling.<br>
 */
public class GenericTilingFactory implements TilingFactory
{

    /**
     * A provider of custom WbpSources.
     */
    public interface CustomWbpSourcesProvider
    {

        /**
         * Create new WbpSources for the specified chord sequence.
         *
         * @param scs
         * @return
         */
        List<WbpSource> build(SimpleChordSequence scs);
    }

    private final BassStyle style;
    private final CustomWbpSourcesProvider cwsProvider;
    private static final Logger LOGGER = Logger.getLogger(GenericTilingFactory.class.getSimpleName());

    public GenericTilingFactory(CustomWbpSourcesProvider cwsProvider, BassStyle... styles )
    {
        Objects.requireNonNull(style);
        Objects.requireNonNull(cwsProvider);
        this.style = style;
        this.cwsProvider = cwsProvider;
    }

    @Override
    public WbpTiling build(SimpleChordSequenceExt scs, int tempo)
    {
        LOGGER.log(Level.SEVERE, "build() -- style={0} tempo={1} scs={2}", new Object[]
        {
            style, tempo, scs
        });


        var settings = WalkingBassMusicGeneratorSettings.getInstance();
        WbpTiling tiling = new WbpTiling(scs);
        var phraseAdapter = new TransposerPhraseAdapter();


        // PREMIUM PHASE
        WbpsaScorer scorerPremium = new WbpsaScorerDefault(phraseAdapter, tempo, Score.PREMIUM_ONLY_TESTER, style);

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
            WbpsaScorer scorerStandard = new WbpsaScorerDefault(phraseAdapter, tempo, null, style);

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
        WbpsaScorer scorerCustom = new WbpsaScorerDefault(phraseAdapter, tempo, null, BassStyle.CUSTOM);
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
            var customWbpSources = buildCustomWbpSources(tiling);

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

    /**
     * For each untiled zone create a custom WbpSource.
     * <p>
     * Untiled zones with only 1 chord are NOT handled by this method (it means that the default WbpSourceDatabase should be updated).
     *
     * @param tiling
     * @return True if there still are some untiled bars.
     */
    private List<WbpSource> buildCustomWbpSources(WbpTiling tiling)
    {
        List<WbpSource> res = new ArrayList<>();

        for (int size = WbpSourceDatabase.SIZE_MAX; size >= WbpSourceDatabase.SIZE_MIN; size--)
        {
            var startBars = tiling.getUntiledZonesStartBarIndexes(size);
            for (int startBar : startBars)
            {
                var br = new IntRange(startBar, startBar + size - 1);
                var subSeq = tiling.getSimpleChordSequenceExt().subSequence(br, true).getShifted(-startBar);
                if (subSeq.size() == 1)
                {
                    // Not normal, some additional default WbpSources should be added in the database
                    LOGGER.log(Level.SEVERE, "buildCustomWbpSources() 1-chord subSeq not previously tiled: {0}", subSeq);
                    continue;
                }

                var wbpSources = cwsProvider.build(subSeq);
                res.addAll(wbpSources);
            }
        }

        return res;
    }

}

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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.jjswing.api.BassStyle;
import org.jjazz.jjswing.tempoadapter.SwingProfile;
import org.jjazz.jjswing.tempoadapter.SwingBassTempoAdapter;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * Double-time walking bass.
 * <p>
 * Based on WalkingDoubleNotePhraseBuilder with pre and post-processing.
 */
public class WalkingDoubleTimePhraseBuilder implements PhraseBuilder
{

    private static final BassStyle STYLE = BassStyle.WALKING_DOUBLE_TIME;
    private static final Logger LOGGER = Logger.getLogger(WalkingDoubleTimePhraseBuilder.class.getSimpleName());

    @Override
    public Phrase build(List<SimpleChordSequence> scsList, int tempo)
    {
        LOGGER.log(PhraseBuilderLogLevel, "build() -- tempo={0} scsList={1}", new Object[]
        {
            tempo, scsList
        });

        // Expand the SimpleChordSequences (2x longer)
        var scsListExpanded = getExpandedSimpleChordSequences(scsList);


        // Delegates to the normal walking bass phrase builder
        PhraseBuilder builder = BassStyle.WALKING_DOUBLE_NOTE.getBassPhraseBuilder();
        Phrase p = builder.build(scsListExpanded, tempo);


        // Adapt to a double tempo
        if (!scsListExpanded.isEmpty())
        {
            var beatRange = new FloatRange(scsListExpanded.getFirst().getBeatRange().from, scsListExpanded.getLast().getBeatRange().to);
            float intensity = BassGeneratorSettings.getInstance().getSwingProfileIntensity();
            SwingProfile profile = SwingProfile.create(intensity);
            SwingBassTempoAdapter bassAdapter = new SwingBassTempoAdapter(profile, TimeSignature.FOUR_FOUR);
            bassAdapter.adaptToTempo(p, beatRange, ne -> true, 2 * tempo);
        }

        // Shrink back the phrase
        p.processNotes(ne -> true, ne -> 
        {
            var newDur = ne.getDurationInBeats() / 2;
            var newPos = ne.getPositionInBeats() / 2;
            return ne.setAll(ne.getPitch(), newDur, ne.getVelocity(), newPos, null, false);
        });


        return p;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================

    private List<SimpleChordSequence> getExpandedSimpleChordSequences(List<SimpleChordSequence> scsList)
    {
        List<SimpleChordSequence> res = new ArrayList<>();
        for (var scs : scsList)
        {
            var br = scs.getBarRange();
            var brNew = new IntRange(br.from * 2, br.to * 2 + 1);
            var scsNew = new SimpleChordSequence(brNew, scs.getStartBeatPosition() * 2, scs.getTimeSignature());
            for (var cliCs : scs)
            {
                float nbBeats = scs.getTimeSignature().getNbNaturalBeats();
                int newBar = cliCs.getPosition().getBar() * 2;
                float newBeat = cliCs.getPosition().getBeat() * 2;
                if (newBeat >= nbBeats)
                {
                    newBar++;
                    newBeat -= nbBeats;
                }
                var posNew = new Position(newBar, newBeat);
                var cliCsNew = (CLI_ChordSymbol) cliCs.getCopy(null, posNew);
                scsNew.add(cliCsNew);
            }
            res.add(scsNew);
        }
        return res;
    }

}

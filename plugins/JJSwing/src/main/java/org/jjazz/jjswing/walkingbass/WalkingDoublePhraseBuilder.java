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
package org.jjazz.jjswing.walkingbass;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.jjswing.api.BassStyle;
import static org.jjazz.jjswing.walkingbass.JJSwingBassMusicGenerator.NON_QUANTIZED_WINDOW;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;

/**
 * A phrase builder for BassStyle.WALKING_DOUBLE.
 * <p>
 * Delegates to a WalkingBassPhraseBuilder then post-process the results.
 */
public class WalkingDoublePhraseBuilder implements BassPhraseBuilder
{

    private static final BassStyle STYLE = BassStyle.WALKING_DOUBLE;
    private static final Logger LOGGER = Logger.getLogger(WalkingDoublePhraseBuilder.class.getSimpleName());

    @Override
    public Phrase build(List<SimpleChordSequence> scsList, int tempo)
    {
        LOGGER.log(BassPhraseBuilderLogLevel, "build() -- tempo={0} scsList={1}", new Object[]
        {
            tempo, scsList
        });


        // Delegates to the normal walking bass phrase builder
        BassPhraseBuilder walkingBuilder = BassStyle.WALKING.getBassPhraseBuilder();
        Phrase p = walkingBuilder.build(scsList, tempo);


        // Turn it into double notes phrase using notes from beat 0 and beat 2
        for (var scs : scsList)
        {
            var barRange = scs.getBarRange();
            for (int bar : barRange)
            {
                float barStartBeatPos = scs.getStartBeatPosition() + ((bar - barRange.from) * scs.getTimeSignature().getNbNaturalBeats());

                FloatRange fr0 = new FloatRange(Math.max(0, barStartBeatPos - NON_QUANTIZED_WINDOW), barStartBeatPos + 2 - NON_QUANTIZED_WINDOW);
                var p0 = p.subSet(fr0, true);
                if (p0.isEmpty())
                {
                    LOGGER.log(Level.WARNING, "build() No note found in first half of bar={0}", bar);
                    continue;
                }
                var ne0 = p0.first();
                float beatPos0 = ne0.getPositionInBeats() < barStartBeatPos + NON_QUANTIZED_WINDOW ? ne0.getPositionInBeats() : barStartBeatPos;
                var cliCs = scs.getChordSymbol(scs.toPosition(barStartBeatPos));
                if (cliCs.getData().getChord().indexOfRelativePitch(ne0.getPitch()) == -1)
                {
                    // It's an unusual note, leave the normal walking bass for a change
                    continue;
                }
                ne0 = ne0.setAll(-1, 0.9f, -1, beatPos0, false);
                var ne1 = ne0.setPosition(barStartBeatPos + 1, false);


                FloatRange fr2 = new FloatRange(barStartBeatPos + 2 - NON_QUANTIZED_WINDOW, barStartBeatPos + 4 - NON_QUANTIZED_WINDOW);
                var p2 = p.subSet(fr2, true);
                if (p2.isEmpty())
                {
                    LOGGER.log(Level.WARNING, "build() No note found in second half of bar={0}", bar);
                    continue;
                }
                var ne2 = p2.first();
                cliCs = scs.getChordSymbol(scs.toPosition(ne2.getPositionInBeats()));
                if (ne0.getPitch() == ne2.getPitch() || cliCs.getData().getChord().indexOfRelativePitch(ne2.getPitch()) == -1)
                {
                    // Leave the normal walking bass for a change
                    continue;
                }
                float beatPos2 = ne2.getPositionInBeats() < barStartBeatPos + 2 + NON_QUANTIZED_WINDOW ? ne2.getPositionInBeats() : barStartBeatPos + 2;
                ne2 = ne2.setAll(-1, 0.9f, -1, beatPos2, false);
                var ne3 = ne2.setPosition(barStartBeatPos + 3, false);


                p.removeAll(new ArrayList<>(p0));
                p.removeAll(new ArrayList<>(p2));
                p.addAll(List.of(ne0, ne1, ne2, ne3));

            }
        }


        return p;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================

}

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
import static org.jjazz.jjswing.walkingbass.BassGenerator.NON_QUANTIZED_WINDOW;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

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
                var barScs = scs.subSequence(new IntRange(bar, bar), true);
                if (barScs.size() > 2 || (barScs.size() == 2 && barScs.getLast().getPosition().getBeat() != 2f))
                {
                    // Non standard case, leave normal walking bass
                    continue;
                }
                var cliCs0 = barScs.getFirst();
                var cliCs2 = barScs.size() == 1 ? null : barScs.getLast();

                FloatRange fr0 = new FloatRange(Math.max(0, barStartBeatPos - NON_QUANTIZED_WINDOW), barStartBeatPos + 2 - NON_QUANTIZED_WINDOW);
                var p0 = p.subSet(fr0, true);
                if (p0.isEmpty())
                {
                    LOGGER.log(Level.WARNING, "build() No note found in first half of bar={0}", bar);
                    continue;
                }
                var ne0 = p0.first();
                float beatPos0 = ne0.getPositionInBeats() < barStartBeatPos + NON_QUANTIZED_WINDOW ? ne0.getPositionInBeats() : barStartBeatPos;
                if (cliCs0.getData().getChord().indexOfRelativePitch(ne0.getPitch()) == -1)
                {
                    // It's an unusual note, leave the normal walking bass for a change
                    continue;
                }
                ne0 = ne0.setAll(-1, 0.9f, -1, beatPos0, false);
                var ne1 = ne0.setPosition(barStartBeatPos + 1, false);


                FloatRange fr2 = new FloatRange(barStartBeatPos + 2f - NON_QUANTIZED_WINDOW, barStartBeatPos + 4 - NON_QUANTIZED_WINDOW);
                var p2 = p.subSet(fr2, true);
                if (p2.isEmpty())
                {
                    LOGGER.log(Level.WARNING, "build() No note found in second half of bar={0}", bar);
                    continue;
                }
                var ne2 = p2.first();
                var ne2pitch = (cliCs2 == null) ? ne2.getPitch()
                        : BassGenerator.getClosestAndAcceptableBassPitch(ne2, cliCs2.getData().getBassNote().getRelativePitch());
                if (ne0.getPitch() == ne2pitch || (cliCs2 == null && cliCs0.getData().getChord().indexOfRelativePitch(ne2pitch) == -1))
                {
                    // Leave the normal walking bass for a change
                    continue;
                }

                float beatPos2 = ne2.getPositionInBeats() < barStartBeatPos + 2f + NON_QUANTIZED_WINDOW ? ne2.getPositionInBeats() : barStartBeatPos + 2f;
                ne2 = ne2.setAll(ne2pitch, 0.9f, -1, beatPos2, false);
                var ne3 = ne2.setPosition(barStartBeatPos + 3f, false);


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

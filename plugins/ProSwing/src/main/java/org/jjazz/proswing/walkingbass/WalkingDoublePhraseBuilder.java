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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.proswing.api.BassStyle;
import static org.jjazz.proswing.walkingbass.JJSwingBassMusicGenerator.NON_QUANTIZED_WINDOW;
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
        LOGGER.log(Level.FINE, "build() -- tempo={0} scsList={1}", new Object[]
        {
            tempo, scsList
        });


        // Delegates to the normal walking bass phrase builder
        BassPhraseBuilder walkingBuilder = BassStyle.WALKING.getBassPhraseBuilder();
        Phrase p = walkingBuilder.build(scsList, tempo);


        var usableBars = scsList.stream()
                .map(scs -> scs.getBarRange())
                .flatMap(br -> br.stream().boxed())
                .toList();


        // Turn it into double notes phrase using notes from beat 0 and beat 2
        float nbBeatsPerBar = 4;
        for (int bar : usableBars)
        {
            float barStartBeatPos = bar * nbBeatsPerBar;


            FloatRange fr0 = new FloatRange(Math.max(0, barStartBeatPos - NON_QUANTIZED_WINDOW), barStartBeatPos + 2 - NON_QUANTIZED_WINDOW);
            var p0 = p.subSet(fr0, true);
            if (p0.isEmpty())
            {
                LOGGER.log(Level.WARNING, "build() No note found in first half of bar={0}", bar);
                continue;
            }
            var ne0 = p0.first();
            float beatPos0 = ne0.getPositionInBeats() < barStartBeatPos + NON_QUANTIZED_WINDOW ? ne0.getPositionInBeats() : barStartBeatPos;
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
            if (ne0.getPitch() == ne2.getPitch())
            {
                // Leave the normal walking bass for a change, better than 4 times the same note!
                continue;
            }
            float beatPos2 = ne2.getPositionInBeats() < barStartBeatPos + 2 + NON_QUANTIZED_WINDOW ? ne2.getPositionInBeats() : barStartBeatPos + 2;
            ne2 = ne2.setAll(-1, 0.9f, -1, beatPos2, false);
            var ne3 = ne2.setPosition(barStartBeatPos + 3, false);


            p.removeAll(new ArrayList<>(p0));
            p.removeAll(new ArrayList<>(p2));
            p.addAll(List.of(ne0, ne1, ne2, ne3));

        }


        return p;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================

}

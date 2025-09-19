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

import java.util.List;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.jjswing.api.BassStyle;
import org.jjazz.rhythmmusicgeneration.api.DummyGenerator;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.IntRange;

/**
 * A phrase builder for BassStyle.ENDING
 * <p>
 */
public class EndingPhraseBuilder implements PhraseBuilder
{

    private static final BassStyle STYLE = BassStyle.ENDING;
    private static final Logger LOGGER = Logger.getLogger(EndingPhraseBuilder.class.getSimpleName());

    @Override
    public Phrase build(List<SimpleChordSequence> scsList, int tempo)
    {
        LOGGER.log(PhraseBuilderLogLevel, "build() -- tempo={0} scsList={1}", new Object[]
        {
            tempo, scsList
        });


        Phrase res = new Phrase(0);

        for (var scs : scsList)
        {
            var p = DummyGenerator.getBasicBassPhrase(scs, new IntRange(60, 63), 0);
            res.add(p);
        }


        return res;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================


}

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

import org.jjazz.phrase.api.Phrase;
import static org.jjazz.jjswing.bass.BassGenerator.NON_QUANTIZED_WINDOW;

/**
 * This implementation just transposes the WbpSource phrase so that the 2 first chord roots match.
 * <p>
 * Manage possible WbpSource first note beat shift.
 */
public class DefaultPhraseAdapter implements PhraseAdapter
{

    @Override
    public Phrase getPhrase(WbpSourceAdaptation wbpsa)
    {
        Phrase res;


        var wbpSource = wbpsa.getWbpSource();
        var scs = wbpsa.getSimpleChordSequence();
        float startPos = scs.getStartBeatPosition();


        // Adapt to target chord symbol
        var sp = wbpSource.getTransposedPhrase(scs.getFirst().getData().getRootNote());      // Starts at beat 0        
        sp.shiftAllEvents(startPos, false);


        if (startPos >= NON_QUANTIZED_WINDOW && wbpSource.getFirstNoteBeatShift() < 0)
        {
            // Restore first note position whose start was anticipated (because non-quantized playing)
            var firstNe = sp.first();
            var firstBeatPos = firstNe.getPositionInBeats();
            assert firstBeatPos == startPos : "firstNe=" + firstNe + " startPos=" + startPos + " sp=" + sp;
            var fixedFirstNe = firstNe.setPosition(firstBeatPos + wbpSource.getFirstNoteBeatShift(), true);
            res = new Phrase(0);        // We need a new phrase because sp is SizedPhrase with fixed beat range
            res.add(fixedFirstNe);
            sp.stream()
                    .filter(ne -> ne != firstNe)
                    .forEach(ne -> res.add(ne));

        } else
        {
            res = sp;
        }

        return res;
    }

    @Override
    public int getTargetPitch(WbpSourceAdaptation wbpsa)
    {
        int res = -1;
        var wbpSource = wbpsa.getWbpSource();
        var tn = wbpSource.getTargetNote();
        if (tn != null)
        {
            var firstRootNote = wbpsa.getSimpleChordSequence().first().getData().getRootNote();
            var t = wbpSource.getRequiredTransposition(firstRootNote);
            res = tn.getTransposed(t).getPitch();
        }
        return res;
    }

}

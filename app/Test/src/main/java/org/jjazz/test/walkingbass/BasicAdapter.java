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
package org.jjazz.test.walkingbass;

import org.jjazz.harmony.api.Note;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.test.walkingbass.generator.WbpSourceAdaptation;

/**
 * Transpose the WbpSource phrase so that 1st chord roots match.
 */
public class BasicAdapter implements WbpSourceAdapter
{

    @Override
    public Phrase getPhrase(WbpSourceAdaptation wbpsa, boolean offsetStart)
    {
        var wbpSource = wbpsa.getWbpSource();
        var scs = wbpsa.getSimpleChordSequence();

        var firstRootNote = scs.first().getData().getRootNote();
        var p = wbpSource.getTransposedPhrase(firstRootNote);
        if (offsetStart)
        {
            p.shiftAllEvents(scs.getBarRange().from * scs.getTimeSignature().getNbNaturalBeats());
        }
        return p;
    }

    @Override
    public Note getTargetNote(WbpSourceAdaptation wbpsa)
    {
        var wbpSource = wbpsa.getWbpSource();
        var firstRootNote = wbpsa.getSimpleChordSequence().first().getData().getRootNote();
        var t = wbpSource.getRequiredTransposition(firstRootNote);
        var res = wbpSource.getTargetNote().getTransposed(t);
        return res;
    }

}

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

import org.jjazz.proswing.walkingbass.WbpSource;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.utilities.api.FloatRange;

/**
 * Algorithms to process WbpSources.
 */
public class WbpSources
{

    private static final ListMultimap<Integer, WbpSource> mmapSizeWbpSources = MultimapBuilder.hashKeys().arrayListValues().build();
    private static final Logger LOGGER = Logger.getLogger(WbpSources.class.getSimpleName());


  

    /**
     * Check if sp has one note per beat.
     *
     * @param sp
     * @param nearBeatWindow Tolerate slight difference in beat position
     * @return
     */
    static public boolean isOneNotePerBeat(SizedPhrase sp, float nearBeatWindow)
    {
        boolean b = true;
        if (sp.size() == sp.getTimeSignature().getNbNaturalBeats())
        {
            int beat = 0;
            for (var ne : sp)
            {
                FloatRange fr = new FloatRange(Math.max(0, beat - nearBeatWindow), beat + nearBeatWindow);
                if (!fr.contains(ne.getPositionInBeats(), false))
                {
                    b = false;
                    break;
                }
                beat++;
            }
        } else
        {
            b = false;
        }
        return b;
    }

    /**
     *
     * @param wbpSources
     * @return A list of list of WbpSources which share the same chord progression
     */
    static public List<List<WbpSource>> groupWbpSourcesPerChordProgression(List<WbpSource> wbpSources)
    {
        return null;
    }
    
    // ================================================================================================================
    // Private methods
    // ================================================================================================================    
}

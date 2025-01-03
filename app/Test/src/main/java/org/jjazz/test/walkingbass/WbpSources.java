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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.quantizer.api.Quantization;

/**
 * Algorithms to process WbpSources.
 */
public class WbpSources
{

    private static final Logger LOGGER = Logger.getLogger(WbpSources.class.getSimpleName());

    /**
     * Find the WbpSources which use identical phrases transposition-wise.
     * <p>
     * Ignore notes duration.
     *
     * @param wbpSources The WbpSources to inspect.
     * @param qPos       Position quantization for the comparison
     * @return A list of list of duplicates
     */
    static public List<List<WbpSource>> findDuplicates(List<WbpSource> wbpSources, Quantization qPos)
    {
        Objects.requireNonNull(wbpSources);
        Objects.requireNonNull(qPos);

        List<List<WbpSource>> res = new ArrayList<>();

        if (wbpSources.size() > 1)
        {
            // Get all note comparison key strings from phrases transposed to match the first phrase note
            int pitch0 = wbpSources.get(0).getSizedPhrase().first().getPitch();
            var phraseKeys = wbpSources.stream()
                    .map(wbps -> wbps.getSizedPhrase())
                    .map(p -> p.getProcessedPhrasePitch(pitch -> pitch - (p.first().getPitch() - pitch0)))
                    .map(p -> Phrases.getNoteComparisonString(p, qPos, null))
                    .toList();


            ListMultimap<String, WbpSource> mmap = MultimapBuilder.hashKeys().arrayListValues().build();
            for (int i = 0; i < wbpSources.size(); i++)
            {
                var wbps = wbpSources.get(i);
                var str = phraseKeys.get(i);
                mmap.put(str, wbps);
            }

            for (var str : mmap.keySet())
            {
                List<WbpSource> wbpsList = mmap.get(str);
                if (wbpsList.size() > 1)
                {
                    res.add(wbpsList);
                }
            }
        }

        return res;
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
}

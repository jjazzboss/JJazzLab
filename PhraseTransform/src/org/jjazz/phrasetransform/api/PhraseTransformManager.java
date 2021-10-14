/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.phrasetransform.api;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.jjazz.midi.api.Instrument;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.PhraseTransformManagerImpl;
import org.openide.util.Lookup;

/**
 * The PhraseTransformManager is the central place to get information about installed PhraseTransforms.
 * <p>
 * Implementation should populate the database upon creation.
 * <p>
 * @TODO Use tags instead of (or in addition to) category.
 */
public interface PhraseTransformManager
{

    /**
     * Use the first implementation present in the global lookup.
     * <p>
     * If nothing found, use the default one.
     *
     * @return
     */
    public static PhraseTransformManager getDefault()
    {
        PhraseTransformManager result = Lookup.getDefault().lookup(PhraseTransformManager.class);
        if (result == null)
        {
            return PhraseTransformManagerImpl.getInstance();
        }
        return result;
    }

    /**
     * Force a rescan of all PhraseTransformProviders to get a refreshed list of PhraseTransforms.
     */
    public void refresh();

    /**
     * Get all the available PhraseTransforms.
     *
     * @return
     */
    public List<PhraseTransform> getPhraseTransforms();


    /**
     * Get a specified PhraseTransform instance.
     *
     * @param uniqueId
     * @return Can be null
     */
    public default PhraseTransform getPhraseTransform(String uniqueId)
    {
        var res = getPhraseTransforms().stream()
                .filter(pt -> pt.getUniqueId().equals(uniqueId))
                .findAny()
                .orElse(null);
        return res;
    }

    /**
     * Get the PhraseTransform which match the specified category.
     *
     * @param category
     * @return
     */
    default public List<PhraseTransform> getPhraseTransforms(PhraseTransformCategory category)
    {
        return getPhraseTransforms().stream()
                .filter(pt -> pt.getCategory().equals(category))
                .collect(Collectors.toList());
    }

    /**
     * Get the available PhraseTransforms sorted by "fit score" for the specified parameters.
     *
     * @param inPhrase
     * @param ins
     * @param exclude0score if true PhraseTransforms with a fit score==0 are ignored.
     * @return First PhraseTransform is the most adapted to the specified parameters (highest fit score, last PhraseTransform is
     * the less adapted.
     * @see PhraseTransform#getFitScore(org.jjazz.phrase.api.SizedPhrase, org.jjazz.midi.api.Instrument)
     */
    default public List<PhraseTransform> getRecommendedPhraseTransforms(SizedPhrase inPhrase, Instrument ins, boolean exclude0score)
    {
        var pts = getPhraseTransforms();

        
        // Compute score for all transforms
        HashMap<PhraseTransform, Integer> mapPtScore = new HashMap<>();
        for (var it = pts.iterator(); it.hasNext();)
        {
            var pt = it.next();
            int score = pt.getFitScore(inPhrase, ins);
            if (score == 0 && exclude0score)
            {
                it.remove();
            } else
            {
                mapPtScore.put(pt, score);
            }
        }

        
        // Sort
        pts.sort((pt1, pt2) -> Integer.compare(mapPtScore.get(pt2), mapPtScore.get(pt1)));
        return pts;
    }


}

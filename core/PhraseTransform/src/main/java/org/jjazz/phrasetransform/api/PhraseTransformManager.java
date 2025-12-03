/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.phrasetransform.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.PhraseTransformManagerImpl;
import org.jjazz.songcontext.api.SongPartContext;
import org.openide.util.Lookup;

/**
 * The PhraseTransformManager is the central place to get information about installed PhraseTransforms.
 * <p>
 * Implementation should populate the database upon creation.
 * <p>
 * @see org.jjazz.phrasetransform.spi.PhraseTransformProvider
 */
public interface PhraseTransformManager
{

    /**
     * Use the first implementation present in the global lookup.
     * <p>
     * If nothing found, use the default implementation which relies on all PhraseTransformProvider implementations found in the global lookup.
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
     * Get new instances of all the available PhraseTransforms.
     *
     * @return An unmodifiable list.
     */
    public List<PhraseTransform> getPhraseTransforms();


    /**
     * Get a new instance of a specific PhraseTransform.
     *
     * @param uniqueId
     * @return Can be null
     * @see PhraseTransform.Info#getUniqueId()
     */
    public default PhraseTransform getPhraseTransform(String uniqueId)
    {
        var res = getPhraseTransforms().stream()
                .filter(pt -> pt.getInfo().getUniqueId().equals(uniqueId))
                .findAny()
                .orElse(null);
        return res != null ? res.getCopy() : res;
    }

    /**
     * Get the PhraseTransforms which match the specified category.
     *
     * @param category
     * @return An unmodifiable list
     */
    default public List<PhraseTransform> getPhraseTransforms(PhraseTransformCategory category)
    {
        return getPhraseTransforms().stream()
                .filter(pt -> pt.getInfo().getCategory().equals(category))
                .toList();
    }

    /**
     * Get the available PhraseTransforms sorted by "fit score" for the specified parameters.
     *
     * @param inPhrase
     * @param context
     * @param exclude0score if true PhraseTransforms with a fit score==0 are ignored.
     * @return First PhraseTransform is the most adapted to the specified parameters (highest fit score, last PhraseTransform is the less adapted.
     * @see PhraseTransform#getFitScore(org.jjazz.phrase.api.SizedPhrase, org.jjazz.songcontext.api.SongPartContext) 
     */
    default public List<PhraseTransform> getRecommendedPhraseTransforms(SizedPhrase inPhrase, SongPartContext context, boolean exclude0score)
    {
        var pts = new ArrayList<>(getPhraseTransforms());


        // Compute score for all transforms
        HashMap<PhraseTransform, Integer> mapPtScore = new HashMap<>();
        for (var it = pts.iterator(); it.hasNext();)
        {
            var pt = it.next();
            int score = pt.getFitScore(inPhrase, context);
            if (pt.getInfo().getUniqueId().contains(PhraseTransform.HIDDEN_ID_TOKEN) || (score == 0 && exclude0score))
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

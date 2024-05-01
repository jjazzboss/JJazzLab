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
package org.jjazz.yamjjazz.rhythm;

import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.phrase.api.SourcePhraseSet;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import org.jjazz.songstructure.api.SongStructure;

/**
 * SourcePhrase Random Picker.
 * <p>
 * Randomly select the appropriate SourcePhraseSet when there are alternatives depending on context and parameters.
 * <p>
 */
public class SpsRandomPicker
{

    private static final HashMap<String, SpsRandomPicker> INSTANCES = new HashMap<>();
    private final StylePart stylePart;
    private final int complexity;
    private final SongStructure sgs;
    private final YamJJazzRhythm rhythm;
    private static final Logger LOGGER = Logger.getLogger(SpsRandomPicker.class.getSimpleName());

    /**
     * Get the SpsRandomPicker instance for the specified parameters.
     *
     * @param sgs
     * @param r
     * @param sp
     * @param complexity
     * @return
     */
    static public SpsRandomPicker getInstance(SongStructure sgs, YamJJazzRhythm r, StylePart sp, int complexity)
    {
        if (r == null || sp == null || complexity < 1 || sgs == null)
        {
            throw new IllegalArgumentException("r=" + r + " sp=" + sp + " complexity=" + complexity + " sgs=" + sgs);   //NOI18N
        }
        synchronized (SpsRandomPicker.class)
        {
            String key = r.getUniqueId() + "-" + sp.getType().toString() + "-" + complexity;
            SpsRandomPicker INSTANCE = INSTANCES.get(key);
            if (INSTANCE == null)
            {
                INSTANCE = new SpsRandomPicker(sgs, r, sp, complexity);
                INSTANCES.put(key, INSTANCE);
            }
            return INSTANCE;
        }
    }

    private SpsRandomPicker(SongStructure sgs, YamJJazzRhythm yr, StylePart sp, int complexity)
    {
        stylePart = sp;
        this.complexity = complexity;
        this.sgs = sgs;
        this.rhythm = yr;
    }

    /**
     * Randomly pick the next SourcePhraseSet to be used depending on context.
     * <p>
     *
     * @param sgsStartBarIndex The bar index in the song structure where the phrase will start.
     * @return
     */
    public SourcePhraseSet pick(int sgsStartBarIndex)
    {
        SourcePhraseSet sps = null;
        List<SourcePhraseSet> spsList = stylePart.getSourcePhraseSets(complexity);
        int nbSets = spsList.size();
        if (nbSets == 0)
        {
            throw new IllegalStateException("No SourcePhraseSet found ! stylePart=" + stylePart + " complexity=" + complexity);   //NOI18N
        }

        // If it's a not a fill, use decreasing thresholds
        // Decreasing thresholds =   {50, 80} for nbSets=3
        // Constant thresholds =   {33, 66} for nbSets=3
        int[] thresholds = stylePart.getType().isFillOrBreak() ? computeConstantThresholds(nbSets) :computeDecreasingThresholds(nbSets);       
        double x = Math.round(Math.random() * 100);          // 0-100
        int index = 0;
        while (index < nbSets - 1 && x > thresholds[index])
        {
            index++;
        }
        sps = spsList.get(index);
        return sps;
    }

    /**
     * Reset the random process.
     * <p>
     * Useful for eg random processes which vary with time.
     */
    public void reset()
    {

    }

    // =================================================================================================
    // Private methods
    // =================================================================================================    
    /**
     * Define a constant threshold list depending on the number of SourcePhraseSets.
     *
     * @param size
     * @return For ex. if size=3 return [33,66]
     */
    private int[] computeConstantThresholds(int size)
    {
        assert size >= 1;   //NOI18N
        int[] res = new int[size - 1];
        int step = (int) Math.floor(100f / size);
        for (int i = 1; i < size; i++)
        {
            res[i - 1] = i * step;
        }
        return res;
    }

    /**
     * Define a decreasing threshold list depending on the number of SourcePhraseSets.
     * <p>
     * SourcePhraseSet N has more chances to be picked than SourcePhraseSet N+1. Example:<br>
     * (size=1) empty <br>
     * (size=2) 1st=60%<br>
     * (size=3) 1st=50%, 2nd=80% <br>
     * (size=4) etc.<br>
     *
     * @param size
     * @return An array with size-1 elements. Values represent a percentage (for ex. 50 means 50%)
     */
    private int[] computeDecreasingThresholds(int size)
    {
        assert size >= 1;   //NOI18N
        int[] res;
        switch (size)
        {
            case 1:
                res = new int[0];
                break;
            case 2:
                res = new int[]
                {
                    60
                };      // 60-40
                break;
            case 3:
                res = new int[]
                {
                    50, 80           // 50-30-20
                };
                break;
            case 4:
                res = new int[]
                {
                    40, 65, 85           // 40-25-20-15
                };
                break;
            case 5:
                res = new int[]
                {
                    35, 57, 74, 88           // 35-22-17-14-12
                };
                break;
            default:
                // 30-20-15 then (35/(size-3))
                // Example for size=6: 30-20-15-12-12-11
                res = new int[size - 1];
                res[0] = 30;
                res[1] = 50;
                res[2] = 65;
                int step = (int) Math.ceil(35f / (size - 3));
                for (int i = 0; i < size - 4; i++)
                {
                    res[3 + i] = 65 + (i + 1) * step;
                }
        }

        return res;
    }

}

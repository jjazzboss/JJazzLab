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
package org.jjazz.jjswing.bass.db;

import java.util.IntSummaryStatistics;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.Utilities;

/**
 * Data and methods about bass source phrase notes velocities.
 */
public class Velocities
{

    /**
     * Statistics performed on WbpSources notes (with length >0.6f). June 2025
     */
    public final static IntRange VELOCITY_BASS_STATS_MIN_MAX = new IntRange(35, 92);
    /**
     * Statistics performed on WbpSources notes (with length >0.6f). June 2025
     */
    public final static int VELOCITY_BASS_STATS_MEDIAN = 62;
    /**
     * Statistics performed on WbpSources notes (with length >0.6f). June 2025
     */
    public final static float VELOCITY_BASS_STATS_STDDEV = 7.63f;

    /**
     * The main normal velocity range for bass notes.
     */
    public final static IntRange VELOCITY_BASS_MAIN_RANGE = new IntRange(55, 70);
    public final static int VELOCITY_BASS_EXTENDED_OFFSET = 6;
    /**
     * The extended velocity range for bass notes.
     *
     * @see #VELOCITY_BASS_MAIN_RANGE
     * @see #VELOCITY_BASS_EXTENDED_OFFSET
     */
    public final static IntRange VELOCITY_BASS_EXTENDED_RANGE = VELOCITY_BASS_MAIN_RANGE.getTransformed(-VELOCITY_BASS_EXTENDED_OFFSET,
            VELOCITY_BASS_EXTENDED_OFFSET);
    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = Logger.getLogger(Velocities.class.getSimpleName());

    /**
     * Get a random velocity using a gaussian distribution in the range returned by getMostProbableVelocityRange().
     *
     * @return
     * @see #VELOCITY_BASS_MAIN_RANGE
     */
    static public int getRandomBassVelocity()
    {
        float r = Utilities.getNextGaussianRandomValue(RANDOM); // [-1;1]
        var rg = VELOCITY_BASS_MAIN_RANGE;
        float f = rg.getCenter() + r * rg.size() / 2;
        int vel = rg.clamp(Math.round(f));
        return vel;
    }

    /**
     * Normalize the velocities of sp notes.
     *
     * @param p A phrase of bass notes
     * @param durMin Process notes whose duration is greater or equal
     * @return True if sp was changed
     */
    static public boolean normalizeBassVelocities(Phrase p, float durMin)
    {
        boolean b = false;

        // Get stats first        
        IntSummaryStatistics stats = new IntSummaryStatistics();
        p.stream()
                .filter(ne -> ne.getDurationInBeats() >= durMin)
                .forEach(ne -> stats.accept(ne.getVelocity()));
        IntRange velRange = new IntRange(stats.getMin(), stats.getMax());

        if (!VELOCITY_BASS_MAIN_RANGE.contains(velRange))
        {
            int offset = (int) Math.round((stats.getAverage() - VELOCITY_BASS_STATS_MEDIAN) / 2);
            b = offset != 0;
            p.processNotes(ne -> ne.getDurationInBeats() >= durMin, ne -> ne.setVelocity(VELOCITY_BASS_EXTENDED_RANGE.clamp(ne.getVelocity() - offset), true));
            LOGGER.log(Level.FINE, "Adjusted phrase velocity. offset={0}", offset);
        }

        return b;
    }
}

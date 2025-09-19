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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;

/**
 * The profile of the chord symbol root notes of a 1-4 bars SimpleChordSequence.
 * <p>
 * Used to compare similar SimpleChordSequences (ignoring chord types).
 */
public record RootProfile(int nbBars, List<Float> relativeChordPositionsInBeats, List<Integer> ascendingIntervals)
        {

    /**
     *
     * @param nbBars                        [1;4]
     * @param relativeChordPositionsInBeats Can not be empty
     * @param ascendingIntervals            Size must be relativeChordPositionsInBeats.size() - 1
     */
    public RootProfile(int nbBars, List<Float> relativeChordPositionsInBeats, List<Integer> ascendingIntervals)
    {
        Objects.requireNonNull(relativeChordPositionsInBeats);
        Objects.requireNonNull(ascendingIntervals);
        Preconditions.checkArgument(nbBars >= 1 && nbBars <= 4, "nbBars=%s", nbBars);
        Preconditions.checkArgument(!relativeChordPositionsInBeats.isEmpty(), "relativeChordPositionsInBeats=%s", relativeChordPositionsInBeats);
        Preconditions.checkArgument(ascendingIntervals.size() == relativeChordPositionsInBeats.size() - 1, "ascendingIntervals=%s", ascendingIntervals);

        this.nbBars = nbBars;
        this.relativeChordPositionsInBeats = Collections.unmodifiableList(relativeChordPositionsInBeats);
        this.ascendingIntervals = Collections.unmodifiableList(ascendingIntervals);
    }

    /**
     * Create a RootProfile from a SimpleChordSequence.
     *
     * @param scs Can not be empty. 1 to 4 bars max.
     * @return
     */
    public static RootProfile of(SimpleChordSequence scs)
    {
        Objects.requireNonNull(scs);
        Preconditions.checkArgument(!scs.isEmpty());

        int barFrom = scs.getBarRange().from;
        var positions = new ArrayList<Float>(scs.size());
        var ascIntervals = new ArrayList<Integer>(scs.size() - 1);

        CLI_ChordSymbol last = null;
        for (var cliCs : scs)
        {
            var pos = cliCs.getPosition();
            var relBeatPos = (pos.getBar() - barFrom) * scs.getTimeSignature().getNbNaturalBeats() + pos.getBeat();
            positions.add(relBeatPos);

            if (last != null)
            {
                int ascInterval = cliCs.getData().getRootNote().getRelativeAscInterval(last.getData().getRootNote());
                ascIntervals.add(ascInterval);
            }

            last = cliCs;
        }

        return new RootProfile(scs.getBarRange().size(), positions, ascIntervals);
    }

  
}

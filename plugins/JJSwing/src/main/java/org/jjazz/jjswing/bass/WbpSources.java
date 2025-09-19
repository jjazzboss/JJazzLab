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

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.jjswing.api.BassStyle;
import org.jjazz.jjswing.bass.db.WbpSource;
import org.jjazz.jjswing.bass.db.WbpSourceDatabase;

/**
 * Algorithms to process WbpSources.
 */
public class WbpSources
{

    private static final float HUMANIZE_NEAR_WINDOW = 0.15f;
    private static final Logger LOGGER = Logger.getLogger(WbpSources.class.getSimpleName());


    /**
     * Try to add position/velocity humanization to a (computer generated) custom WbpSource phrase.
     *
     * @param wbpSource
     * @return If wbpSource phrase was modified by this call
     */
    static public boolean humanizeCustomWbpSource(WbpSource wbpSource)
    {
        Objects.requireNonNull(wbpSource);
        Preconditions.checkArgument(wbpSource.getBassStyle().isCustom(), "wbpSource=%s", wbpSource);

        var sp = wbpSource.getSizedPhrase();
        WbpSource wbpGrooveRef = WbpSources.findHumanizeReference(wbpSource.getBassStyle().getNonCustomStyle(), sp, true);
        if (wbpGrooveRef != null)
        {
            Phrases.applyGroove(wbpGrooveRef.getSizedPhrase(), sp, HUMANIZE_NEAR_WINDOW);
        } else
        {
            LOGGER.log(Level.FINE, "humanizeCustomWbpSource() no humanize reference WbpSource found for phrase {0}", sp);
        }

        return wbpGrooveRef != null;
    }

    /**
     * Find a WbpSource in the database which has the same number of notes than sp, approximatively the same note positions and possibly the same note
     * durations.
     *
     * @param style
     * @param sp
     * @param sameNoteDurations
     * @return Can be null
     */
    static public WbpSource findHumanizeReference(BassStyle style, SizedPhrase sp, boolean sameNoteDurations)
    {
        WbpSource res = null;

        var wbpSources = WbpSourceDatabase.getInstance().getWbpSources(sp.getSizeInBars(), style).stream()
                .filter(w -> Phrases.isSameNotePositions(w.getSizedPhrase(), sp, sameNoteDurations, HUMANIZE_NEAR_WINDOW))
                .toList();

        if (wbpSources.size() == 1)
        {
            res = wbpSources.get(0);
        } else if (wbpSources.size() > 1)
        {
            int index = (int) (Math.random() * wbpSources.size());
            res = wbpSources.get(index);
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

    // ================================================================================================================
    // Private methods
    // ================================================================================================================    
}

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
package org.jjazz.chordleadsheet.api;

import org.jjazz.harmony.api.TimeSignature;
import org.openide.util.Lookup;
import org.jjazz.chordleadsheet.ChordLeadSheetFactoryImpl;

public interface ChordLeadSheetFactory
{

    public static ChordLeadSheetFactory getDefault()
    {
        ChordLeadSheetFactory result = Lookup.getDefault().lookup(ChordLeadSheetFactory.class);
        if (result == null)
        {
            result = ChordLeadSheetFactoryImpl.getInstance();
        }
        return result;
    }

    /**
     * Create an empty leadsheet with just the initial section.
     *
     * @param sectionName  The name of the initial section.
     * @param ts           The time signature of the initial section
     * @param size         The size in bars (must be &gt; 0)
     * @param initialChord e.g. "Cm7". A string describing an initial chord to be put at the start of the lead sheet. If null no chord is inserted.
     * @return
     */
    ChordLeadSheet createEmptyLeadSheet(String sectionName, TimeSignature ts, int size, String initialChord);

    /**
     * Create a 12 bars (or more) leadsheet which contains sample sections and chords.
     *
     * @param sectionName The name of the initial section.
     * @param size        The size in bars must be &gt;= 12.
     * @return
     */
    ChordLeadSheet createSampleLeadSheet12bars(String sectionName, int size);

    /**
     * Create a leadsheet with a randomly generated content (sections and chords).
     *
     * @param sectionName the value of sectionName
     * @param ts          The time signature of the initial section
     * @param size        The size in bars must be &gt; 0.
     * @return
     */
    ChordLeadSheet createRamdomLeadSheet(String sectionName, TimeSignature ts, int size);

}

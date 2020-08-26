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
package org.jjazz.leadsheet.chordleadsheet.api;

import org.jjazz.harmony.TimeSignature;
import org.openide.util.Lookup;
import org.jjazz.leadsheet.chordleadsheet.ChordLeadSheetFactoryImpl;

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
     * Create an empty leadsheet except initial section and a C chord on bar 0.
     *
     * @param sectionName The name of the initial section.
     * @param ts The time signature of the initial section
     * @param size The size in bars (must be &gt; 0)
     * @return
     */
    ChordLeadSheet createEmptyLeadSheet(String sectionName, TimeSignature ts, int size);

    /**
     * Create a 12 bars (or more) leadsheet which contains sample sections and chords.
     *
     * @param sectionName The name of the initial section.
     * @param size The size in bars must be &gt;= 12.
     * @return
     */
    ChordLeadSheet createSampleLeadSheet12bars(String sectionName, int size);

    /**
     * Create a leadsheet with a randomly generated content (sections and chords).
     *
     * @param sectionName the value of sectionName
     * @param ts The time signature of the initial section
     * @param size The size in bars must be &gt; 0.
     * @return
     */
    ChordLeadSheet createRamdomLeadSheet(String sectionName, TimeSignature ts, int size);

    /**
     * Get a deep copy of specified chordleadsheet.
     *
     * @param cls
     * @return
     */
    ChordLeadSheet getCopy(ChordLeadSheet cls);


    /**
     * Create a cls copy but with no more than 2 chord symbols per bar.
     * <p>
     * If more than 1 chord symbol in a bar, keep the first chord symbol and the last one. If interval between them is less than
     * half-bar, reposition them on first beat and half-bar.
     *
     * @param cls
     * @return
     */
    ChordLeadSheet getSimplified(ChordLeadSheet cls);


}

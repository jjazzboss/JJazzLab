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
package org.jjazz.songstructure.api;

import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.openide.util.Lookup;

/**
 *
 */
public abstract class SongStructureFactory
{

    public static SongStructureFactory getDefault()
    {
        SongStructureFactory result = Lookup.getDefault().lookup(SongStructureFactory.class);
        return result;
    }

    abstract public SongStructure createEmptySgs();

    /**
     * A simple SGS with just 1 SongPart of 4 bars and the RhythmDatabase default rhythm.
     *
     * @return
     */
    abstract public SongStructure createSimpleSgs();

    /**
     * Create a SongStructure with cls as parentChordLeadSheet.
     * <p>
     * One SongPart is created for each cls section. <br>
     *
     * @param cls 
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    abstract public SongStructure createSgs(ChordLeadSheet cls) throws UnsupportedEditException;
}

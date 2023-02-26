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

import org.jjazz.harmony.api.Note;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;

/**
 * General methods on ChordLeadSheets.
 */
public class Utilities
{

    /**
     * Transpose the chord symbols of a ChordLeadSheet.
     *
     * @param cls
     * @param transposition
     */
    static public void transpose(ChordLeadSheet cls, int transposition)
    {
        if (transposition == 0)
        {
            return;
        }

        for (CLI_ChordSymbol oldCli : cls.getItems(CLI_ChordSymbol.class))
        {
            ExtChordSymbol newEcs = oldCli.getData().getTransposedChordSymbol(transposition, Note.Alteration.FLAT);
            CLI_ChordSymbol newCli = CLI_Factory.getDefault().createChordSymbol(cls, newEcs, oldCli.getPosition());
            cls.removeItem(oldCli);
            cls.addItem(newCli);
        }
    }
}

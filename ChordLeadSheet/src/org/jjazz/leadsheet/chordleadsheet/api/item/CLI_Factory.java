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
package org.jjazz.leadsheet.chordleadsheet.api.item;

import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.item.CLI_FactoryImpl;
import org.openide.util.Lookup;

public abstract class CLI_Factory
{

    public static CLI_Factory getDefault()
    {
        CLI_Factory result = Lookup.getDefault().lookup(CLI_Factory.class);
        if (result == null)
        {
            return CLI_FactoryImpl.getInstance();
        }
        return result;
    }

    abstract public CLI_Section createSection(ChordLeadSheet cls, String sectionName, TimeSignature ts, int barIndex);

    abstract public CLI_ChordSymbol createChordSymbol(ChordLeadSheet cls, ExtChordSymbol cs, Position pos);

    abstract public CLI_Section getSampleSection();

    abstract public CLI_ChordSymbol getSampleChordSymbol();
}

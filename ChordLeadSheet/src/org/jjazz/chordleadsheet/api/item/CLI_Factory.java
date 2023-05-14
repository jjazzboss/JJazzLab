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
package org.jjazz.chordleadsheet.api.item;

import java.text.ParseException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.item.CLI_FactoryImpl;
import org.openide.util.Lookup;

/**
 * A factory for ChordLeadSheetItems.
 * <p>
 */
public interface CLI_Factory
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

    /**
     * Create a Section.
     *
     * @param cls         Can be null.
     * @param sectionName
     * @param ts
     * @param barIndex
     * @return
     */
    CLI_Section createSection(ChordLeadSheet cls, String sectionName, TimeSignature ts, int barIndex);

    /**
     * Create a CLI_ChordSymbol.
     *
     * @param cls Can be null.
     * @param cs
     * @param pos
     * @return
     */
    CLI_ChordSymbol createChordSymbol(ChordLeadSheet cls, ExtChordSymbol cs, Position pos);

    /**
     * Create a CLI_ChordSymbol from a string specification.
     * <p>
     * Use default values for non specified attributes.
     *
     * @param chordSymbol E.g. "C" or "Bb7#5"
     * @return
     * @throws java.text.ParseException If chord symbol specification is invalid
     */
    CLI_ChordSymbol createChordSymbol(String chordSymbol) throws ParseException;
    
    CLI_BarAnnotation createBarAnnotation(ChordLeadSheet cls, String annotation, int bar);

    CLI_Section getSampleSection();

    CLI_ChordSymbol getSampleChordSymbol();
}

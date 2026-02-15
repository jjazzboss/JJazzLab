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
package org.jjazz.chordleadsheet.api.item;

import java.text.ParseException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.item.CLI_FactoryImpl;
import org.jjazz.harmony.api.Position;
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
     * @param sectionName
     * @param ts
     * @param barIndex
     * @param cls         Can be null. If not null, adapt sectionName if required to avoid name clash with another section in cls.
     * @return
     */
    CLI_Section createSection(String sectionName, TimeSignature ts, int barIndex, ChordLeadSheet cls);

    /**
     * Create a CLI_ChordSymbol.
     *
     * @param ecs
     * @param pos
     * @return
     */
    CLI_ChordSymbol createChordSymbol(ExtChordSymbol ecs, Position pos);

    default CLI_ChordSymbol createChordSymbol(ExtChordSymbol ecs, int bar, float beat)
    {
        return createChordSymbol(ecs, new Position(bar, beat));
    }

    /**
     * Create a CLI_ChordSymbol from a string specification.
     * <p>
     * Use default values for non specified attributes.
     *
     * @param chordSymbol E.g. "C" or "Bb7#5"
     * @param pos
     * @return
     * @throws java.text.ParseException If chord symbol specification is invalid
     */
    CLI_ChordSymbol createChordSymbol(String chordSymbol, Position pos) throws ParseException;

    default CLI_ChordSymbol createChordSymbol(String chordSymbol, int bar, float beat) throws ParseException
    {
        return createChordSymbol(chordSymbol, new Position(bar, beat));
    }

    /**
     * Create a CLI_BarAnnotation at specified bar.
     *
     * @param annotation
     * @param bar
     * @return
     */
    CLI_BarAnnotation createBarAnnotation(String annotation, int bar);

    CLI_Section getSampleSection();

    CLI_ChordSymbol getSampleChordSymbol();
}

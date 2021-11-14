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
package org.jjazz.leadsheet.chordleadsheet.item;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

public class CLI_FactoryImpl extends CLI_Factory
{

    private static CLI_FactoryImpl INSTANCE;
    private static CLI_Section SECTION_SAMPLE;
    private static CLI_ChordSymbol CHORD_SYMBOL_SAMPLE;

    static public CLI_FactoryImpl getInstance()
    {
        synchronized (CLI_FactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new CLI_FactoryImpl();
            }
        }
        return INSTANCE;
    }

    private CLI_FactoryImpl()
    {
        SECTION_SAMPLE = new CLI_SectionImpl("SAMPLE-SECTION", TimeSignature.FOUR_FOUR, 0);
        try
        {
            CHORD_SYMBOL_SAMPLE = new CLI_ChordSymbolImpl(new ExtChordSymbol("C#M7#11"), new Position(0, 0));
        } catch (ParseException ex)
        {
            Logger.getLogger(CLI_FactoryImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public CLI_Section createSection(ChordLeadSheet cls, String sectionName, TimeSignature ts, int barIndex)
    {
        CLI_SectionImpl cli = new CLI_SectionImpl(sectionName, ts, barIndex);
        cli.setContainer(cls);
        return cli;
    }

    @Override
    public CLI_ChordSymbol createChordSymbol(ChordLeadSheet cls, ExtChordSymbol ecs, Position pos)
    {
        CLI_ChordSymbolImpl cli = new CLI_ChordSymbolImpl(ecs, pos);
        cli.setContainer(cls);
        return cli;
    }

    @Override
    public CLI_Section getSampleSection()
    {
        return SECTION_SAMPLE;
    }

    @Override
    public CLI_ChordSymbol getSampleChordSymbol()
    {
        return CHORD_SYMBOL_SAMPLE;
    }
}

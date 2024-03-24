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
package org.jjazz.chordleadsheet;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;

public class ChordLeadSheetFactoryImpl implements ChordLeadSheetFactory
{

    static private ChordLeadSheetFactoryImpl INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(ChordLeadSheetFactoryImpl.class.getSimpleName());

    static public ChordLeadSheetFactoryImpl getInstance()
    {
        synchronized (ChordLeadSheetFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ChordLeadSheetFactoryImpl();
            }
        }
        return INSTANCE;
    }

    private ChordLeadSheetFactoryImpl()
    {
    }

    @Override
    public ChordLeadSheet createEmptyLeadSheet(String sectionName, TimeSignature ts, int size, boolean addInitialChordSymbol)
    {
        ChordLeadSheet cls = new ChordLeadSheetImpl(sectionName, ts, size);
        CLI_Factory clif = CLI_Factory.getDefault();
        if (addInitialChordSymbol)
        {
            cls.addItem(clif.createChordSymbol(new ExtChordSymbol(), new Position(0, 0)));
        }
        return cls;
    }

    @Override
    public ChordLeadSheet createSampleLeadSheet12bars(String sectionName, int size)
    {
        if (size < 12)
        {
            throw new IllegalArgumentException("size=" + size);   
        }
        ChordLeadSheet cls = new ChordLeadSheetImpl(sectionName, TimeSignature.FOUR_FOUR, size);
        CLI_Factory clif = CLI_Factory.getDefault();
        try
        {
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Dm7"), new Position(0, 0)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("C-7"), new Position(0, 2f)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("F#7"),
                    new Position(1, 0)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Bbmaj7#5"),
                    new Position(1, 2)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("A"),
                    new Position(1, 3f)));
            cls.addSection(clif.createSection("Chorus", TimeSignature.THREE_FOUR, 2, null));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("D7b9b5"),
                    new Position(2, 1f)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("FM7#11"),
                    new Position(4, 3f)));
            cls.addSection(clif.createSection("Bridge", TimeSignature.FOUR_FOUR, 5, null));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Eb7b9#5"),
                    new Position(5, 0.75f)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Ab7#11"),
                    new Position(6, 0)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("A#"),
                    new Position(6, 4f)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("F7alt"),
                    new Position(6, 1.5f)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("G7#9#5"),
                    new Position(7, 2f)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("G#7dim"),
                    new Position(8, 0f)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Dbmaj7"),
                    new Position(8, 2)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Gbmaj7"),
                    new Position(9, 0)));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("G#maj7"),
                    new Position(11, 3f)));
        } catch (ParseException | UnsupportedEditException ex)
        {
            String msg = "Error creating sample leadsheet.\n" + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, "createSampleLeadSheet12bars() {0}", msg);   
            throw new IllegalStateException(msg);
        }
        return cls;
    }

    @Override
    public ChordLeadSheet createRamdomLeadSheet(String sectionName, TimeSignature ts, int size)
    {
        int sectionId = 0;
        if (size < 1)
        {
            throw new IllegalArgumentException("size=" + size);   
        }
        CLI_Factory clif = CLI_Factory.getDefault();
        ChordLeadSheet cls = new ChordLeadSheetImpl(sectionName, ts, size);
        for (int i = 0; i < size; i++)
        {
            ExtChordSymbol cs = ExtChordSymbol.createRandomChordSymbol();
            CLI_ChordSymbol cli = clif.createChordSymbol(cs, new Position(i, 0));
            cls.addItem(cli);
            if (Math.random() > .8f)
            {
                cs = ExtChordSymbol.createRandomChordSymbol();
                cli = clif.createChordSymbol(cs, new Position(i, 2));
                cls.addItem(cli);
            }
            if (i > 0 && Math.random() > .9f)
            {
                TimeSignature ts2 = TimeSignature.FOUR_FOUR;
                if (Math.random() > 0.8f)
                {
                    ts2 = TimeSignature.THREE_FOUR;
                }
                CLI_Section section = clif.createSection(sectionName + sectionId++, ts2, i, null);
                try
                {
                    cls.addSection(section);
                } catch (UnsupportedEditException ex)
                {
                    // Do nothing, section is not added but it's not a problem as it's a random thing
                }
            }
        }
        return cls;
    }

    @Override
    public ChordLeadSheet getCopy(ChordLeadSheet cls)
    {
        CLI_Section initSection = cls.getSection(0);
        ChordLeadSheet clsCopy = new ChordLeadSheetImpl(initSection.getData().getName(), initSection.getData().getTimeSignature(), cls.getSizeInBars());
        for (var item : cls.getItems())
        {
            if (item == initSection)
            {
                continue;
            }
            var itemCopy = item.getCopy(null);
            if (itemCopy instanceof CLI_Section cliSectionCopy)
            {
                try
                {
                    clsCopy.addSection(cliSectionCopy);
                } catch (UnsupportedEditException ex)
                {
                    // We should not be there normally
                    throw new IllegalStateException("Unexpected 'UnsupportedEditException'.", ex);   
                }
            } else
            {
                clsCopy.addItem(itemCopy);
            }
        }
        return clsCopy;
    }




    // ============================================================================================
    // Private methods
    // ============================================================================================    
}

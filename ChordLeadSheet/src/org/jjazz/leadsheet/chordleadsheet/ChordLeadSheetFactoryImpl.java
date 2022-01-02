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
package org.jjazz.leadsheet.chordleadsheet;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import static org.jjazz.leadsheet.chordleadsheet.Bundle.ERR_CreateSampleLeadSheet12;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

@NbBundle.Messages(
        {
            "ERR_CreateSampleLeadSheet12=Problem creating chord leadsheet"

        })
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
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol(), new Position(0, 0)));
        }
        return cls;
    }

    @Override
    public ChordLeadSheet createSampleLeadSheet12bars(String sectionName, int size)
    {
        if (size < 12)
        {
            throw new IllegalArgumentException("size=" + size);   //NOI18N
        }
        ChordLeadSheet cls = new ChordLeadSheetImpl(sectionName, TimeSignature.FOUR_FOUR, size);
        CLI_Factory clif = CLI_Factory.getDefault();
        try
        {
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("Dm7"), new Position(0, 0)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("C-7"), new Position(0, 2f)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("F#7"),
                    new Position(1, 0)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("Bbmaj7#5"),
                    new Position(1, 2)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("A"),
                    new Position(1, 3f)));
            cls.addSection(clif.createSection(cls, "Chorus", TimeSignature.THREE_FOUR, 2));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("D7b9b5"),
                    new Position(2, 1f)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("FM7#11"),
                    new Position(4, 3f)));
            cls.addSection(clif.createSection(cls, "Bridge", TimeSignature.FOUR_FOUR, 5));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("Eb7b9#5"),
                    new Position(5, 0.75f)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("Ab7#11"),
                    new Position(6, 0)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("A#"),
                    new Position(6, 4f)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("F7alt"),
                    new Position(6, 1.5f)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("G7#9#5"),
                    new Position(7, 2f)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("G#7dim"),
                    new Position(8, 0f)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("Dbmaj7"),
                    new Position(8, 2)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("Gbmaj7"),
                    new Position(9, 0)));
            cls.addItem(clif.createChordSymbol(cls, new ExtChordSymbol("G#maj7"),
                    new Position(11, 3f)));
        } catch (ParseException | UnsupportedEditException ex)
        {
            String msg = ERR_CreateSampleLeadSheet12() + ".\n" + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, "createSampleLeadSheet12bars() {0}", msg);   //NOI18N
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
        return cls;
    }

    @Override
    public ChordLeadSheet createRamdomLeadSheet(String sectionName, TimeSignature ts, int size)
    {
        int sectionId = 0;
        if (size < 1)
        {
            throw new IllegalArgumentException("size=" + size);   //NOI18N
        }
        CLI_Factory clif = CLI_Factory.getDefault();
        ChordLeadSheet cls = new ChordLeadSheetImpl(sectionName, ts, size);
        for (int i = 0; i < size; i++)
        {
            ExtChordSymbol cs = ExtChordSymbol.createRandomChordSymbol();
            CLI_ChordSymbol cli = clif.createChordSymbol(cls, cs, new Position(i, 0));
            cls.addItem(cli);
            if (Math.random() > .8f)
            {
                cs = ExtChordSymbol.createRandomChordSymbol();
                cli = clif.createChordSymbol(cls, cs, new Position(i, 2));
                cls.addItem(cli);
            }
            if (i > 0 && Math.random() > .9f)
            {
                TimeSignature ts2 = TimeSignature.FOUR_FOUR;
                if (Math.random() > 0.8f)
                {
                    ts2 = TimeSignature.THREE_FOUR;
                }
                CLI_Section section = clif.createSection(cls, sectionName + sectionId++, ts2, i);
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
        for (ChordLeadSheetItem<?> item : cls.getItems())
        {
            if (item == initSection)
            {
                continue;
            }
            ChordLeadSheetItem<?> itemCopy = item.getCopy(clsCopy, null);
            if (itemCopy instanceof CLI_Section)
            {
                try
                {
                    clsCopy.addSection((CLI_Section) itemCopy);
                } catch (UnsupportedEditException ex)
                {
                    // We should not be there normally
                    throw new IllegalStateException("Unexpected 'UnsupportedEditException'.", ex);   //NOI18N
                }
            } else
            {
                clsCopy.addItem(itemCopy);
            }
        }
        return clsCopy;
    }


    @Override
    public ChordLeadSheet getSimplified(ChordLeadSheet cls)
    {
        ChordLeadSheet simplifiedCls = getCopy(cls);

        for (int barIndex = 0; barIndex < simplifiedCls.getSizeInBars(); barIndex++)
        {
            float halfBarBeat = simplifiedCls.getSection(barIndex).getData().getTimeSignature().getHalfBarBeat(false);
            var items = simplifiedCls.getItems(barIndex, barIndex, CLI_ChordSymbol.class);
            if (items.size() <= 1)
            {
                // Nothing
            } else
            {
                // Move first and last items
                var item0 = items.get(0);
                var item0beat = item0.getPosition().getBeat();
                var item1 = items.get(items.size() - 1);
                var item1beat = item1.getPosition().getBeat();
                if (item1beat - item0beat < halfBarBeat)
                {
                    simplifiedCls.moveItem(item0, new Position(barIndex, 0));
                    simplifiedCls.moveItem(item1, new Position(barIndex, halfBarBeat));
                }


                // Remove others
                for (int i = 1; i < items.size() - 1; i++)
                {
                    simplifiedCls.removeItem(items.get(i));
                }
            }
        }

        return simplifiedCls;
    }

    // ============================================================================================
    // Private methods
    // ============================================================================================    
}

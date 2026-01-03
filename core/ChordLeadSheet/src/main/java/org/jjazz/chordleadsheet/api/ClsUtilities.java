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

import java.util.Objects;
import org.jjazz.harmony.api.Note;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;

/**
 * General methods on ChordLeadSheets.
 */
public class ClsUtilities
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
            ExtChordSymbol newEcs = oldCli.getData().getTransposedChordSymbol(transposition, Note.Accidental.FLAT);
            CLI_ChordSymbol newCli = CLI_Factory.getDefault().createChordSymbol(newEcs, oldCli.getPosition());
            cls.removeItem(oldCli);
            cls.addItem(newCli);
        }
    }

    /**
     * Create a cls copy but with no more than 2 chord symbols per bar.
     * <p>
     * If more than 1 chord symbol in a bar, keep the first chord symbol and the last one. If interval between them is less than half-bar, reposition them on
     * first beat and half-bar.
     *
     * @param cls
     * @return
     */
    static public ChordLeadSheet getSimplified(ChordLeadSheet cls)
    {
        ChordLeadSheet simplifiedCls =cls.getDeepCopy();

        for (int barIndex = 0; barIndex < simplifiedCls.getSizeInBars(); barIndex++)
        {
            float halfBarBeat = simplifiedCls.getSection(barIndex).getData().getTimeSignature().getHalfBarBeat(false);
            var items = simplifiedCls.getItems(barIndex, barIndex, CLI_ChordSymbol.class, cli -> true);
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
                    simplifiedCls.moveItem(item0, new Position(barIndex));
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

    /**
     * Remove successive identical "standard" chord symbols per section.
     *
     * @param cls
     * @return True if cls was modified
     * @see ExtChordSymbol#isStandard()
     */
    static public boolean removeRedundantStandardChords(ChordLeadSheet cls)
    { 
        boolean changed = false;
        var csList = cls.getItems(CLI_ChordSymbol.class);
        ExtChordSymbol lastEcs = null;
        CLI_Section lastSection = null;
        for (var cliCs : csList)
        {
            var ecs = cliCs.getData();
            var section = cls.getSection(cliCs.getPosition().getBar());
            if (ecs.isStandard() && section == lastSection && Objects.equals(lastEcs, ecs))
            {
                cls.removeItem(cliCs);
                changed = true;
            } else
            {
                lastEcs = ecs;
                lastSection = section;
            }
        }
        return changed;
    }
}

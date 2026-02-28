/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.chordleadsheet;

import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.spi.item.CLI_Factory;
import org.jjazz.harmony.api.TimeSignature;

/**
 * Perform cyclic changes to a ChordLeadSheet.
 */
public class ClsCyclicMutator
{

    private int counter;
    private final ChordLeadSheet cls;
    private final CLI_ChordSymbol cliChordSymbol1, CliChordSymbol1Save, newCliChordSymbol;
    private final CLI_BarAnnotation CliBarAnnotation;
    private final CLI_Section cliSection0, cliSection2, cliSection2Save;
    private final int sizeSave;

    public ClsCyclicMutator(ChordLeadSheet cls)
    {
        this.cls = cls;
        assert cls.getSizeInBars() >= 8;
        cliChordSymbol1 = cls.getItems(CLI_ChordSymbol.class).get(1);
        CliChordSymbol1Save = (CLI_ChordSymbol) cliChordSymbol1.getCopy(null, null);
        newCliChordSymbol = (CLI_ChordSymbol) cliChordSymbol1.getCopy(null, cliChordSymbol1.getPosition().getMoved(1, 0.666f));
        cliSection0 = cls.getSection(0);
        cliSection2 = cls.getItems(CLI_Section.class).get(2);
        cliSection2Save = (CLI_Section) cliSection2.getCopy((Section) null, null);
        sizeSave = cls.getSizeInBars();
        CliBarAnnotation = CLI_Factory.getDefault().createBarAnnotation("hello", 4);
    }

    public void mutate() throws UnsupportedEditException
    {
        switch (counter % 8)
        {
            case 0 ->
            {
                // Add a chord symbol and an annotation
                cls.addItem(newCliChordSymbol);
                cls.addItem(CliBarAnnotation);
            }

            case 1 ->
            {
                // Remove a chord symbol and an annotation
                cls.removeItem(newCliChordSymbol);
                cls.removeItem(CliBarAnnotation);
            }

            case 2 ->
            {
                // Move a chord symbol
                var pos = cliChordSymbol1.getPosition();
                var newPos = pos.equals(CliChordSymbol1Save.getPosition()) ? pos.getMoved(2, 1) : CliChordSymbol1Save.getPosition();
                cls.moveItem(cliChordSymbol1, newPos);
            }

            case 3 ->
            {
                // Move a section
                int bar = cliSection2.getPosition().getBar();
                var newBar = bar == cliSection2Save.getPosition().getBar() ? bar + 2 : cliSection2Save.getPosition().getBar();
                cls.moveSection(cliSection2, newBar);
            }

            case 4 ->
            {
                // Change timesignature
                var ts = cliSection0.getData().getTimeSignature();
                var newTs = ts == TimeSignature.FOUR_FOUR ? TimeSignature.TWO_FOUR : TimeSignature.FOUR_FOUR;
                cls.setSectionTimeSignature(cliSection0, newTs);
            }

            case 5 ->
            {
                // Change size
                var size = cls.getSizeInBars();
                var newSize = size == sizeSave ? sizeSave + 3 : sizeSave;
                cls.setSizeInBars(newSize);
            }

            case 6 ->
            {
                // Insert bars
                cls.insertBars(0, 6);
            }
            case 7 ->
            {
                // Delete bars
                cls.deleteBars(0, 5);
            }
        }
        counter++;
    }
}

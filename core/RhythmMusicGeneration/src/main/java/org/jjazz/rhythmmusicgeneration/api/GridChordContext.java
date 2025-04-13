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
package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.phrase.api.Grid;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A helper class to calculate a number of data related to one chord symbol of a chord sequence with an associated grid.
 * <p>
 * Data covers the zone from the chord symbol before (or the start of chord sequence if no previous chord symbol) to the chord symbol after
 * (or end of chord sequence if no next chord symbol). When present the bounding chord symbols before/after are not part of the covered
 * zone.
 */
public class GridChordContext
{

    // Context
    public SimpleChordSequence chordSequence;
    public Grid grid;
    public CLI_ChordSymbol chord;
    public TimeSignature timeSignature;

    /**
     * The cell of the chord symbol.
     */
    public int chordCell;
    /**
     * The absolute position of the chord symbol.
     */
    public float chordPosInBeats;
    /**
     * The relative position in beat in the cell, can be negative !
     */
    public float relPosInCell;
    /**
     * The relative cell within the beat.
     */
    public int relCellInBeat;
    /**
     * from=chordCell+1, to=cell before next beat, can be empty.
     */
    public IntRange toNextBeatCellRange;
    /**
     * from=beat start, to=chordCell-1, can be empty.
     */
    public IntRange fromBeatStartCellRange;
    /**
     * from=chordCell+1, to=cell before next chord symbol or end of grid, can be empty.
     */
    public IntRange afterCellRange;
    /**
     * from=cell after previous chord symbol or start of grid, to=chordCell-1, can be empty.
     */
    public IntRange beforeCellRange;
    /**
     * from=1st cell of this zone, to=last cell of this zone
     */
    public IntRange cellRange;
    /**
     * from=chord position, to=end of the zone (last cell position minus the pre-cell window)
     */
    public FloatRange afterBeatRange;

    public GridChordContext(CLI_ChordSymbol cliCs, SimpleChordSequence cSeq, Grid grid)
    {
        Objects.requireNonNull(cliCs);
        Objects.requireNonNull(cSeq);
        Objects.requireNonNull(grid);
        Preconditions.checkArgument(cSeq.contains(cliCs), "cliCs=%s cSeq=%s", cliCs, cSeq);
        
        this.chord = cliCs;
        this.chordSequence = cSeq;
        this.grid = grid;
        this.timeSignature = cSeq.getTimeSignature();
        calculate();
    }


    private void calculate()
    {
        chordPosInBeats = chordSequence.toPositionInBeats(chord.getPosition());
        chordCell = grid.getCell(chordPosInBeats, true);
        int cellFrom, cellTo;
        float posInBeats;


        // After cell range
        var nextChord = chordSequence.higher(chord);
        if (nextChord != null)
        {
            posInBeats = chordSequence.toPositionInBeats(nextChord.getPosition());
            cellTo = grid.getCell(posInBeats, true) - 1;
        } else
        {
            cellTo = grid.getCellRange().to;
        }
        afterCellRange = (chordCell + 1 <= cellTo) ? new IntRange(chordCell + 1, cellTo) : IntRange.EMPTY_RANGE;


        // Before range
        var previousChord = chordSequence.lower(chord);
        if (previousChord != null)
        {
            posInBeats = chordSequence.toPositionInBeats(previousChord.getPosition());
            cellFrom = grid.getCell(posInBeats, true) + 1;

        } else
        {
            cellFrom = grid.getCellRange().from;
        }
        beforeCellRange = (cellFrom <= chordCell - 1) ? new IntRange(cellFrom, chordCell - 1) : IntRange.EMPTY_RANGE;


        // The relative position in beat in the cell.
        relPosInCell = chordPosInBeats - grid.getStartPos(chordCell);


        // The relative cell within the beat.
        relCellInBeat = chordCell % grid.getNbCellsPerBeat();


        // Range to next beat
        if (relCellInBeat == grid.getNbCellsPerBeat() - 1)
        {
            toNextBeatCellRange = IntRange.EMPTY_RANGE;
        } else
        {
            toNextBeatCellRange = new IntRange(chordCell + 1, chordCell + grid.getNbCellsPerBeat() - relCellInBeat - 1);
            toNextBeatCellRange = afterCellRange.getIntersection(toNextBeatCellRange);
        }

        // Range from start of beat
        if (relCellInBeat == 0)
        {
            fromBeatStartCellRange = IntRange.EMPTY_RANGE;
        } else
        {
            fromBeatStartCellRange = new IntRange(chordCell - relCellInBeat, chordCell - 1);
            fromBeatStartCellRange = beforeCellRange.getIntersection(fromBeatStartCellRange);
        }

        // Beat range from the chord symbol location to the end of the zone
        float cellDuration = 1f / grid.getNbCellsPerBeat();
        if (afterCellRange.isEmpty())
        {
            afterBeatRange = new FloatRange(chordPosInBeats, grid.getStartPos(chordCell) + cellDuration - grid.getPreCellBeatWindow());
        } else
        {
            afterBeatRange = new FloatRange(chordPosInBeats,
                    grid.getStartPos(afterCellRange.to) + cellDuration - grid.getPreCellBeatWindow());
        }

        // The global range
        int from = beforeCellRange.isEmpty() ? chordCell : beforeCellRange.from;
        int to = afterCellRange.isEmpty() ? chordCell : afterCellRange.to;
        cellRange = new IntRange(from, to);
    }


    @Override
    public String toString()
    {
        return "GZH[chord=" + chord + " posInBeats=" + chordPosInBeats + " chordCell=" + chordCell + " beforeCellRange=" + beforeCellRange + " afterCellRange=" + afterCellRange + "]";
    }
}

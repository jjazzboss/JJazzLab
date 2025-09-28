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
package org.jjazz.phrase.api;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.Exceptions;

/**
 * A convenience class to manipulate notes from a Phrase.
 * <p>
 * The class assigns notes in fixed-sized "cells" (eg 4 cells per beats=1/16) which can then be directly accessed or modified using the cell index.
 * <p>
 * To accomodate real time playing, notes starting just before a cell are included in that cell, see PRE_CELL_BEAT_WINDOW_DEFAULT.
 * <p>
 * The refresh() method must be called whenever the phrase is modified outside this Grid object.
 */
public class Grid implements Cloneable
{

    /**
     * Notes whose relative position is &gt; -PRE_CELL_BEAT_WINDOW_DEFAULT will be included in the current cell.
     * <p>
     * 0.15 seems to be a good value most of the time (from stats computed on walking bass Midi files recorded in real-time), except maybe when playing at a
     * fast tempo (&gt;220?)
     */
    public static float PRE_CELL_BEAT_WINDOW_DEFAULT = 0.15f;
    private final float preCellBeatWindow;
    private final Phrase phrase;
    private final FloatRange originalBeatRange;
    private FloatRange adjustedBeatRange;
    private IntRange cellRange;
    private final int cellsPerBeat;
    private final float cellDuration;
    private Predicate<NoteEvent> predicate;
    private HashMap<Integer, List<NoteEvent>> mapCellNotes = new HashMap<>();
    protected static final Logger LOGGER = Logger.getLogger(Grid.class.getSimpleName());


    /**
     * Obtain a grid for the specified Phrase p.
     * <p>
     * The first cell starts at range.from (range bounds must be integer values). The filter parameter can be used to accept only specific Phrase notes.<p>
     * If the caller modifies p outside of this grid it must then call Grid.refresh() to keep it up to date.
     *
     * @param p                 Time signature must not change in the phrase.
     * @param beatRange         Grid will contain notes from this beat range, excluding upper bound. Bounds must be integer values.
     * @param nbCellsPerBeat    A value in the range [1;6]
     * @param filter            If null this grid will contain all Phrase notes
     * @param preCellBeatWindow A value in the range [0;1/nbCellsPerBeat[. Used to accomodate for non-quantized notes: notes whose relative position is &gt;
     *                          -preCellBeatWindow will be included in the current cell.
     */
    public Grid(Phrase p, FloatRange beatRange, int nbCellsPerBeat, Predicate<NoteEvent> filter, float preCellBeatWindow)
    {
        Objects.requireNonNull(p);
        Objects.requireNonNull(beatRange);
        Preconditions.checkArgument(beatRange.from % 1 == 0 && beatRange.to % 1 == 0, "beatRange=%s", beatRange);
        Preconditions.checkArgument(nbCellsPerBeat > 0 && nbCellsPerBeat <= 6, "nbCellsPerBeat=%s", nbCellsPerBeat);
        Preconditions.checkArgument(preCellBeatWindow >= 0 && preCellBeatWindow < (1f / nbCellsPerBeat), "nbCellsPerBeat=%s, preCellBeatWindow=%s",
                (Integer) nbCellsPerBeat, (Float) preCellBeatWindow);

        this.phrase = p;
        this.cellsPerBeat = nbCellsPerBeat;
        this.cellDuration = 1f / this.cellsPerBeat;
        this.preCellBeatWindow = preCellBeatWindow;
        this.originalBeatRange = beatRange;
        if (this.originalBeatRange.size() < cellDuration)
        {
            throw new IllegalArgumentException("originalBeatRange=" + originalBeatRange + " cellDuration=" + cellDuration);
        }
        this.adjustedBeatRange = this.originalBeatRange.getTransformed(originalBeatRange.from == 0 ? 0 : -preCellBeatWindow,
                -preCellBeatWindow);
        this.cellRange = new IntRange(0, (int) (this.originalBeatRange.size() * this.cellsPerBeat) - 1);
        this.predicate = (filter != null) ? filter : ne -> true;

        refresh();
    }

    @Override
    public Grid clone()
    {
        Grid newGrid = null;
        try
        {
            newGrid = (Grid) super.clone();
        } catch (CloneNotSupportedException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        // Everything OK with a shallow clone copy, except the mapCellNotes 
        @SuppressWarnings("unchecked")
        HashMap<Integer, List<NoteEvent>> tmp = (HashMap<Integer, List<NoteEvent>>) mapCellNotes.clone();
        newGrid.mapCellNotes = tmp;
        return newGrid;
    }

    public Predicate<NoteEvent> getPredicate()
    {
        return predicate;
    }

    /**
     * Search the cell which contains the maximum of notes and return this cell index.
     *
     * @return -1 if no note in the grid.
     */
    public int getCellWithMaxNotes()
    {
        int cellMax = -1;
        int maxSize = 0;
        for (int index : mapCellNotes.keySet())
        {
            List<NoteEvent> nes = mapCellNotes.get(index);
            if (nes != null && nes.size() > maxSize)
            {
                maxSize = nes.size();
                cellMax = index;
            }
        }
        return cellMax;
    }

    /**
     * The beat range used to create this grid.
     * <p>
     * Does NOT take into account the pre-cell beat window.
     *
     * @return
     * @see getAdjustedBeatRange()
     */
    public FloatRange getOriginalBeatRange()
    {
        return originalBeatRange;
    }

    /**
     * The beat range of this grid adjusted to the pre-cell beat window.
     *
     * @return
     * @see getPreCellBeatWindow()
     */
    public FloatRange getAdjustedBeatRange()
    {
        return adjustedBeatRange;
    }

    /**
     * The cell index range of this object.
     *
     * @return
     */
    public IntRange getCellRange()
    {
        return cellRange;
    }

    /**
     * The FloatRange corresponding to the specified cell.
     * <p>
     * The pre-cell beat window is excluded from the returned range.
     *
     * @param cell
     * @return
     */
    public FloatRange getCellBeatRange(int cell)
    {
        if (!cellRange.contains(cell))
        {
            throw new IllegalArgumentException("cell=" + cell + " cellRange=" + cellRange);
        }
        float start = getStartPos(cell);
        return new FloatRange(start, start + cellDuration - getPreCellBeatWindow());
    }

    /**
     * Return the cell range based on the specified beat range.
     *
     * @param fr
     * @param strict If true and range is outside the bounds of this grid throw an IllegalArgumentException. Otherwise use the first/last cell of the grid.
     * @return
     */
    public IntRange getCellRange(FloatRange fr, boolean strict)
    {
        if (fr == null)
        {
            throw new IllegalArgumentException("fr=" + fr);
        }
        return new IntRange(getCell(fr.from, strict), getCell(fr.to, strict));
    }

    /**
     * Convenience method which just calls getAdjustedBeatRange(posInBeats, true).
     *
     * @param posInBeats
     * @return
     */
    public boolean contains(float posInBeats)
    {
        return adjustedBeatRange.contains(posInBeats, true);
    }

    /**
     * Return the cell index for specified position.
     * <p>
     * Take into account the getPreCellBeatWindow() value.
     *
     * @param posInBeats
     * @param strict     If true and posInBeats is not in this grid bounds, throw an IllegalArgumentException. Otherwise use the first or last cell.
     * @return
     */
    public int getCell(float posInBeats, boolean strict)
    {
        if (posInBeats < adjustedBeatRange.from)
        {
            if (strict)
            {
                throw new IllegalArgumentException("posInBeats=" + posInBeats + " adjustedBeatRange=" + adjustedBeatRange);
            } else
            {
                return cellRange.from;
            }
        } else if (posInBeats >= adjustedBeatRange.to)
        {
            if (strict)
            {
                throw new IllegalArgumentException("posInBeats=" + posInBeats + " adjustedBeatRange=" + adjustedBeatRange);
            } else
            {
                return cellRange.to;
            }
        }
        int cell = (int) Math.floor((posInBeats - originalBeatRange.from) / cellDuration);
        float nextCellStartPos = originalBeatRange.from + (cell + 1) * cellDuration;
        if (nextCellStartPos - posInBeats <= getPreCellBeatWindow())
        {
            cell++;
        }
        return cell;
    }

    /**
     * Get the indexes of the non empty cells.
     *
     * @return List is ordered. Can be empty.
     */
    public List<Integer> getNonEmptyCells()
    {
        ArrayList<Integer> res = new ArrayList<>(mapCellNotes.keySet());
        Collections.sort(res);
        return res;
    }

    /**
     * True if no note in the specified cell.
     *
     * @param cell
     * @return
     */
    public boolean isEmpty(int cell)
    {
        List<NoteEvent> nes = mapCellNotes.get(cell);
        return nes == null ? true : nes.isEmpty();
    }

    /**
     * Change the duration of all notes in specified cell range so that they end at cell cellIndexOff.
     * <p>
     * <p>
     * Notes can be shortened or made longer.
     *
     * @param range     Can be the empty range.
     * @param cellOff   The cell index where notes should go off.
     * @param shorterOk If false do not make notes shorter. Can't be false if longerOk is false.
     * @param longerOk  If false do not make notes longer. Can't be false if shorterOk is false.
     */
    public void changeDuration(IntRange range, int cellOff, boolean shorterOk, boolean longerOk)
    {
        if (range.isEmpty())
        {
            return;
        }
        if (!cellRange.contains(range) || cellOff < range.to || !cellRange.contains(cellOff) || !shorterOk && !longerOk)
        {
            throw new IllegalArgumentException(
                    "range=" + range + " cellIndexOff=" + cellOff + " shorterOk=" + shorterOk + " longerOk=" + longerOk + " cellRange=" + cellRange);
        }


        var nes = getCellNotes(range);
        HashSet<Integer> usedPitches = new HashSet<>();


        for (NoteEvent ne : nes)
        {

            IntRange rg = getCellRange(ne.getBeatRange(), false);

            if (usedPitches.contains(ne.getPitch()))
            {
                phrase.remove(ne);

            } else if ((longerOk && rg.to < cellOff) || (shorterOk && rg.to > cellOff))
            {
                float newDur = ne.getDurationInBeats() + (cellOff - rg.to) * cellDuration;
                newDur = Math.max(cellDuration, newDur);
                NoteEvent newNe = ne.setDuration(newDur, true);
                phrase.replace(ne, newNe);
                usedPitches.add(newNe.getPitch());

            }
        }
        if (!usedPitches.isEmpty())
        {
            refresh();
        }
    }

    /**
     * Modify velocity of notes in the specified cell range.
     * <p>
     * Velocity is always maintained in the 0-127 range.
     *
     * @param range Can be the empty range.
     * @param f     A function to modify velocity to another velocity.
     */
    public void changeVelocity(IntRange range, Function<Integer, Integer> f)
    {
        if (range.isEmpty())
        {
            return;
        }
        if (!cellRange.contains(range) || f == null)
        {
            throw new IllegalArgumentException("range=" + range + " f=" + f);
        }
        List<NoteEvent> nes = getCellNotes(range);
        Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
        for (NoteEvent ne : nes)
        {
            int newVelocity = MidiConst.clamp(f.apply(ne.getVelocity()));
            NoteEvent newNe = ne.setVelocity(newVelocity, true);
            mapOldNew.put(ne, newNe);
        }
        phrase.replaceAll(mapOldNew, false);
        if (!nes.isEmpty())
        {
            refresh();
        }
    }

    /**
     * Get the note of the specified cell.
     *
     * @param cell
     * @return
     */
    public List<NoteEvent> getCellNotes(int cell)
    {
        return getCellNotes(new IntRange(cell, cell));
    }

    /**
     * Get the notes in the specified cell range.
     *
     * @param range Can be the empty range.
     * @return List has the same note order than the Phrase.
     */
    public List<NoteEvent> getCellNotes(IntRange range)
    {
        if (range.isEmpty())
        {
            return Collections.emptyList();
        }
        if (!cellRange.contains(range))
        {
            throw new IllegalArgumentException("range=" + range + " cellRange=" + cellRange);
        }

        List<NoteEvent> res = new ArrayList<>();
        for (int i = range.from; i <= range.to; i++)
        {
            List<NoteEvent> nes = mapCellNotes.get(i);
            if (nes != null)
            {
                res.addAll(nes);
            }
        }
        return res;
    }

    /**
     * Get the first note of the cell
     *
     * @param cell
     * @return Null if no note
     */
    public NoteEvent getFirstNote(int cell)
    {
        if (!cellRange.contains(cell))
        {
            throw new IllegalArgumentException("cell=" + cell);
        }
        NoteEvent res = null;

        List<NoteEvent> nes = mapCellNotes.get(cell);
        if (nes != null)
        {
            res = nes.get(0);
        }
        return res;
    }

    /**
     * Get the cell index of the first note in the specified cell range.
     *
     * @param range Can be the empty range.
     * @return -1 if no note found
     */
    public int getFirstNoteCell(IntRange range)
    {
        if (range.isEmpty())
        {
            return -1;
        }
        if (!cellRange.contains(range))
        {
            throw new IllegalArgumentException("range=" + range);
        }
        int res = -1;
        for (int i = range.from; i <= range.to; i++)
        {
            List<NoteEvent> nes = mapCellNotes.get(i);
            if (nes != null)
            {
                res = i;
                break;
            }
        }
        return res;
    }

    /**
     * Get the last note of the cell
     *
     * @param cell
     * @return Null if no note
     */
    public NoteEvent getLastNote(int cell)
    {
        if (!cellRange.contains(cell))
        {
            throw new IllegalArgumentException("cell=" + cell);
        }
        NoteEvent res = null;

        List<NoteEvent> nes = mapCellNotes.get(cell);
        if (nes != null)
        {
            res = nes.get(nes.size() - 1);
        }
        return res;
    }

    /**
     * Get the cell of the last note in the specified cell range.
     *
     * @param range Can be the empty range.
     * @return -1 if no note found
     */
    public int getLastNoteCell(IntRange range)
    {
        if (range.isEmpty())
        {
            return -1;
        }
        if (!cellRange.contains(range))
        {
            throw new IllegalArgumentException("range=" + range);
        }
        int res = -1;
        for (int i = range.to; i >= range.from; i--)
        {
            List<NoteEvent> nes = mapCellNotes.get(i);
            if (nes != null)
            {
                res = i;
                break;
            }
        }
        return res;
    }

    /**
     * Convenient method that just return removeNotes(new IntRange(cell, cell)).
     *
     * @param cell
     * @return
     */
    public List<NoteEvent> removeNotes(int cell)
    {
        return removeNotes(new IntRange(cell, cell));
    }

    /**
     * Remove all notes in the specified cells range.
     *
     * @param range Can be the empty range.
     * @return The removed notes. Can be empty.
     */
    public List<NoteEvent> removeNotes(IntRange range)
    {
        if (range.isEmpty())
        {
            return Collections.emptyList();
        }
        if (!cellRange.contains(range))
        {
            throw new IllegalArgumentException("range=" + range);
        }
        List<NoteEvent> nes = getCellNotes(range);
        phrase.removeAll(nes);
        refresh();
        return nes;
    }

    /**
     * Add a new NoteEvent from the parameters.
     * <p>
     * Convenience method that add the note to the grid's phrase and calls refresh().
     *
     * @param cell
     * @param n            Pitch, duration and velocity are reused to create the NoteEvent.
     * @param relPosInCell The relative position in beats of the note in the cell. Value must be in the interval [-getPreCellBeatWindow():cellDuration[
     * @return The added note.
     */
    public NoteEvent addNote(int cell, Note n, float relPosInCell)
    {
        if (!cellRange.contains(cell) || n == null || relPosInCell < -getPreCellBeatWindow() || relPosInCell >= cellDuration)
        {
            throw new IllegalArgumentException("cellIndex=" + cell + " relPosInCell=" + relPosInCell);
        }
        float posInBeats = getStartPos(cell) + relPosInCell;
        NoteEvent ne = new NoteEvent(n, posInBeats);
        phrase.add(ne);
        refresh();
        return ne;
    }

    /**
     * Replace a note by another one at same position.
     * <p>
     * The 2 notes must have the same position.
     *
     * @param oldNote
     * @param newNote
     */
    public void replaceNote(NoteEvent oldNote, NoteEvent newNote)
    {
        if (oldNote.getPositionInBeats() != newNote.getPositionInBeats())
        {
            throw new IllegalArgumentException("oldNote=" + oldNote + " newNote=" + newNote);
        }
        phrase.replace(oldNote, newNote);
        refresh();
    }


    /**
     * Move all notes from one cell to another.
     *
     * @param cellFrom            The index of the cell containing the notes to be moved.
     * @param cellTo              The index of the destination cell
     * @param keepNoteOffPosition If true AND notes are moved earlier (cellIndexFrom &gt; cellIndexDest), extend the duration of the moved notes so they keep
     *                            the same NOTE_OFF position.
     * @return The number of moved notes
     */
    public int moveNotes(int cellFrom, int cellTo, boolean keepNoteOffPosition)
    {
        if (!cellRange.contains(cellFrom) || !cellRange.contains(cellTo))
        {
            throw new IllegalArgumentException("cellFrom=" + cellFrom + " cellTo=" + cellTo);
        }
        if (cellFrom == cellTo)
        {
            return 0;
        }
        List<NoteEvent> nes = getCellNotes(cellFrom);
        if (!nes.isEmpty())
        {
            for (NoteEvent ne : nes)
            {
                float cellFromPos = getStartPos(cellFrom);
                float cellDestPos = getStartPos(cellTo);
                float inCellpos = ne.getPositionInBeats() - cellFromPos;       // Can be negative for notes right before the cell !
                float newPosInBeats = Math.max(0, cellDestPos + inCellpos);
                float durationInBeats = ne.getDurationInBeats();
                if (keepNoteOffPosition && cellFrom > cellTo)
                {
                    // Extend the duration
                    durationInBeats = ne.getPositionInBeats() + ne.getDurationInBeats() - newPosInBeats;
                }
                NoteEvent movedNe = ne.setAll(-1, durationInBeats, -1, newPosInBeats, null, true);
                phrase.remove(ne);
                phrase.add(movedNe);
            }
            refresh();
        }
        return nes.size();
    }

    /**
     * Move the first note of cellIndexFrom to another cell.
     *
     * @param cellFrom            The index of the cell containing the note to be moved.
     * @param cellTo              The index of the destination cell
     * @param keepNoteOffPosition If true AND note is moved earlier (cellIndexFrom &gt; cellIndexDest), extend the duration of the moved note so it keeps the
     *                            same NOTE_OFF position.
     * @return True if a note was moved.
     */
    public boolean moveFirstNote(int cellFrom, int cellTo, boolean keepNoteOffPosition)
    {
        if (!cellRange.contains(cellFrom) || !cellRange.contains(cellTo))
        {
            throw new IllegalArgumentException("cellFrom=" + cellFrom + " cellTo=" + cellTo);
        }
        NoteEvent ne = getFirstNote(cellFrom);
        if (ne != null)
        {
            float cellFromPos = getStartPos(cellFrom);
            float cellDestPos = getStartPos(cellTo);
            float relPos = ne.getPositionInBeats() - cellFromPos;
            float newPosInBeats = Math.max(0, cellDestPos + relPos);
            float durationInBeats = ne.getDurationInBeats();
            if (keepNoteOffPosition && cellFrom > cellTo)
            {
                // Extend the duration
                durationInBeats = ne.getPositionInBeats() + ne.getDurationInBeats() - newPosInBeats;
            }
            NoteEvent movedNe = ne.setAll(-1, durationInBeats, -1, newPosInBeats, null, true);
            phrase.remove(ne);
            phrase.add(movedNe);
            refresh();
        }
        return ne != null;
    }

    /**
     * Force all sounding notes to stop (eg shorten their duration) at the start of the specified cell.
     *
     * @param cell
     * @return The number of notes which have been shortened.
     * @see getPreCellBeatWindow()
     */
    public int stopNotesBefore(int cell)
    {
        if (!cellRange.contains(cell))
        {
            throw new IllegalArgumentException("cell=" + cell);
        }
        float pos = getStartPos(cell) - preCellBeatWindow;
        List<NoteEvent> nes = Phrases.getCrossingNotes(phrase, pos, true);
        Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
        for (NoteEvent ne : nes)
        {
            float newDuration = pos - ne.getPositionInBeats();
            NoteEvent newNe = ne.setDuration(newDuration, true);
            mapOldNew.put(ne, newNe);
        }
        phrase.replaceAll(mapOldNew, false);
        refresh();
        return nes.size();
    }

    /**
     * The time window before the start of a cell we consider to be part of the cell.
     * <p>
     * This is used to accomodate real time playing notes which may start just before a cell.
     *
     * @return A duration in beats
     */
    public float getPreCellBeatWindow()
    {
        return preCellBeatWindow;
    }

    /**
     * @return the phrase
     */
    public Phrase getPhrase()
    {
        return phrase;
    }

    /**
     *
     * @return A value &gt; 0
     */
    public int getNbCellsPerBeat()
    {
        return cellsPerBeat;
    }

    /**
     * The position in beats of the start of the specified cell.
     * <p>
     * Note that some notes may belong to a cell even if their position is lower than the value returned by getStartPos(), see getPreCellBeatWindow().
     * <p>
     *
     * @param cell
     * @return
     */
    public float getStartPos(int cell)
    {
        if (!cellRange.contains(cell))
        {
            throw new IllegalArgumentException("cell=" + cell + " cellRange=" + cellRange);
        }
        float pos = originalBeatRange.from + cell * cellDuration;
        return pos;
    }

    /**
     * Update the internal data structure: should be called whenever Phrase is modified externally.
     * <p>
     * Manage the fact that a note can be included in a cell if its start position is just before the cell.
     *
     * @see getPreCellBeatWindow()
     */
    public final void refresh()
    {
        mapCellNotes.clear();
        for (NoteEvent ne : phrase)
        {
            float posInBeats = ne.getPositionInBeats();
            if (adjustedBeatRange.contains(posInBeats, true))
            {
                if (!predicate.test(ne))
                {
                    continue;
                }
                float relPosInBeats = posInBeats - originalBeatRange.from;
                int cellIndex;
                if (relPosInBeats < 0)
                {
                    // Special case: the note is just before startPos (but still in the preCellBeatWindow)
                    cellIndex = 0;
                } else
                {
                    // Normal case
                    cellIndex = (int) Math.floor(relPosInBeats / cellDuration);
                    if (((cellIndex + 1) * cellDuration - relPosInBeats) <= preCellBeatWindow)
                    {
                        // We're in the preCellBeatWindow of next cell
                        cellIndex++;
                    }
                }
                List<NoteEvent> nes = mapCellNotes.get(cellIndex);
                if (nes == null)
                {
                    nes = new ArrayList<>();
                    mapCellNotes.put(cellIndex, nes);
                }
                nes.add(ne);
            } else if (posInBeats >= adjustedBeatRange.to)
            {
                // Stopped if we past the last note
                break;
            }
        }
    }

    /**
     *
     * @param cellFrom If out of bound use 0
     * @param cellTo   If out of bound use lastCell
     * @return
     */
    public String toString(int cellFrom, int cellTo)
    {
        if (cellFrom > cellTo)
        {
            throw new IllegalArgumentException("cellFrom=" + cellFrom + " cellTo=" + cellTo);
        }
        StringBuilder sb = new StringBuilder();
        cellFrom = Math.max(0, cellFrom);
        cellTo = Math.min(cellRange.to, cellTo);
        if (cellFrom % cellsPerBeat == 0)
        {
            sb.append("|");
        }
        for (int i = cellFrom; i <= cellTo; i++)
        {
            List<NoteEvent> nes = mapCellNotes.get(i);
            sb.append(nes == null ? "." : nes.size());
            if ((i + 1) % cellsPerBeat == 0)
            {
                sb.append("|");      // Every beat
            }
        }
        sb.append(" ").append(getCellNotes(new IntRange(cellFrom, cellTo)).toString());
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return toString(0, cellRange.to);
    }


    /**
     * Get the recommended nb of cells for the specified parameters.
     *
     * @param ts
     * @param isTernary
     * @return 3 or 4
     */
    static public int getRecommendedNbCellsPerBeat(TimeSignature ts, boolean isTernary)
    {
        int res = (ts.getLower() == 8 || isTernary) ? 3 : 4;
        return res;
    }


    // =================================================================================
    // Private methods
    // ================================================================================= 
}

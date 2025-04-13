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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.Grid;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * Process the anticipated chords.
 */
public class AnticipatedChordProcessor
{

    private final SimpleChordSequence simpleChordSequence;
    private final FloatRange beatRange;
    private int nbCellsPerBeat;
    private int lastCellIndex;
    private float cellDuration;
    private final float preCellBeatWindow;
    private final TimeSignature timeSignature;
    private final List<CLI_ChordSymbol> anticipatableChords;

    protected static final Logger LOGGER = Logger.getLogger(AnticipatedChordProcessor.class.getSimpleName());


    /**
     * Construct the processor of a chord sequence.
     *
     * @param cSeq              Can't be empty. Start beat position must be an arithmetic integer.
     * @param nbCellsPerBeat    4 or 3. 3 should be used for ternary feel rhythm or 3/8 or 6/8 or 12/8 time signatures.
     * @param preCellBeatWindow A value in the range [0;1/nbCellsPerBeat[. Used to accomodate for non-quantized notes: notes whose relative position is &gt;
     *                          -preCellBeatWindow will be included in the current cell.
     */
    public AnticipatedChordProcessor(SimpleChordSequence cSeq, int nbCellsPerBeat, float preCellBeatWindow)
    {
        Objects.requireNonNull(cSeq);
        Preconditions.checkArgument(!cSeq.isEmpty());
        Preconditions.checkArgument(nbCellsPerBeat >= 3 && nbCellsPerBeat <= 4, "nbCellsPerBeat=%s", nbCellsPerBeat);
        Preconditions.checkArgument(cSeq.getBeatRange().from >= 0 && cSeq.getBeatRange().from == Math.floor(cSeq.getBeatRange().from),
                "cSeq.getBeatRange()=%s", cSeq.getBeatRange());


        this.simpleChordSequence = cSeq;
        this.beatRange = cSeq.getBeatRange();
        this.timeSignature = simpleChordSequence.getTimeSignature();
        this.preCellBeatWindow = preCellBeatWindow;


        float nbNaturalBeats = this.timeSignature.getNbNaturalBeats();
        if (nbNaturalBeats != Math.floor(nbNaturalBeats))
        {
            // The algorithms used here, including grid, needs to be adapted for fractional nb of natural beats like 3.5 for 7/8
            throw new IllegalArgumentException("AccentProcessor() Time signature not yet supported: " + timeSignature);   //NOI18N
        }

        this.nbCellsPerBeat = nbCellsPerBeat;
        lastCellIndex = ((int) nbNaturalBeats * nbCellsPerBeat * simpleChordSequence.getBarRange().size()) - 1;
        cellDuration = 1f / nbCellsPerBeat;


        anticipatableChords = identifyAnticipatableChords(simpleChordSequence);

        LOGGER.log(Level.FINE, "AnticipatedChordProcessor -- beatRange={0} nbCellsPerBeat={1} ={2}", new Object[]
        {
            beatRange, nbCellsPerBeat, anticipatableChords
        });
    }

    public List<CLI_ChordSymbol> getAnticipatableChords()
    {
        return Collections.unmodifiableList(anticipatableChords);
    }

    /**
     * Process the anticipatable chords for a monophonic phrase p (eg a bass phrase).
     * <p>
     * If a chord is anticipatable, try to anticipate the non-ghost notes of next beat (or earlier).
     *
     * @param p
     */
    public void anticipateChords_Mono(Phrase p)
    {
        Objects.requireNonNull(p);


        int nonGhostVelLimit = getGhostNoteVelocityLimitBass(p);
        Grid gridHighPass = new Grid(p, beatRange, nbCellsPerBeat, ne -> ne.getVelocity() >= nonGhostVelLimit, preCellBeatWindow);
        Grid grid = new Grid(p, beatRange, nbCellsPerBeat, null, preCellBeatWindow);


        for (CLI_ChordSymbol cliCs : anticipatableChords)
        {
            int chordCell = grid.getCell(simpleChordSequence.getPositionInBeats(cliCs), true);
            if (chordCell == lastCellIndex)
            {
                continue;
            }

            int inBeatChordCell = chordCell % nbCellsPerBeat;
            int nextBeatChordCell = Math.min(lastCellIndex, chordCell + (nbCellsPerBeat - inBeatChordCell));
            IntRange anticipatedRange = new IntRange(chordCell + 1, nextBeatChordCell);
            int anticipatedCell = gridHighPass.getLastNoteCell(anticipatedRange);

            LOGGER.log(Level.FINE, "anticipateChords_Mono() cliCs={0} chordCell={1} anticipatedCell={2}", new Object[]
            {
                cliCs, chordCell, anticipatedCell
            });

            if (anticipatedCell != -1)
            {
                //LOGGER.log(Level.FINE, "anticipateChords_Mono()   BEFORE grid={0}", grid.toString(chordCell - 3, nextBeatChordCell + 1));
                grid.removeNotes(new IntRange(chordCell, anticipatedCell - 1));
                grid.moveNotes(anticipatedCell, chordCell, true);
                grid.stopNotesBefore(chordCell);
                //LOGGER.log(Level.FINE, "anticipateChords_Mono()   AFTER  grid={0}", grid.toString(chordCell - 3, nextBeatChordCell + 1));
            }
        }
    }


    /**
     * Process the anticipatable chords for a polyphonic phrase p (eg a piano phrase).
     * <p>
     * If a chord is anticipatable, try to anticipate the non ghost-notes of next beat or before.
     *
     * @param p
     */
    public void anticipateChords_Poly(Phrase p)
    {
        Objects.requireNonNull(p);
        Grid grid = new Grid(p, beatRange, nbCellsPerBeat, null, preCellBeatWindow);

        for (CLI_ChordSymbol cliCs : anticipatableChords)
        {
            int chordCell = grid.getCell(simpleChordSequence.getPositionInBeats(cliCs), true);
            int inBeatChordCell = chordCell % nbCellsPerBeat;
            int nextBeatChordCell = Math.min(lastCellIndex, chordCell + (nbCellsPerBeat - inBeatChordCell));
            LOGGER.log(Level.FINE, "anticipateChords_Poly() cliCs={0} chordCell={1} nextBeatChordCell={2}", new Object[]
            {
                cliCs, chordCell, nextBeatChordCell
            });

            //LOGGER.log(Level.FINE, "anticipateChords_Poly()   BEFORE grid={0}", grid.toString(chordCell - 3, nextBeatChordCell + 1));

            // Build a grid per pitch
            List<NoteEvent> nes = grid.getCellNotes(new IntRange(chordCell, nextBeatChordCell));
            HashMap<Integer, Grid> mapPitchGrid = new HashMap<>();
            for (NoteEvent ne : nes)
            {
                int pitch = ne.getPitch();
                if (mapPitchGrid.get(pitch) == null)
                {
                    Grid pitchGrid = new Grid(p, beatRange, nbCellsPerBeat, n -> n.getPitch() == pitch, preCellBeatWindow);
                    mapPitchGrid.put(pitch, pitchGrid);
                }
            }

            // Anticipate last note for each pitch
            for (Integer pitch : mapPitchGrid.keySet())
            {
                Grid pitchGrid = mapPitchGrid.get(pitch);
                int anticipatedCell = (chordCell == lastCellIndex) ? -1 : pitchGrid.getLastNoteCell(new IntRange(chordCell + 1,
                        nextBeatChordCell));
                if (anticipatedCell != -1)
                {
                    pitchGrid.removeNotes(new IntRange(chordCell, anticipatedCell - 1));
                    pitchGrid.moveNotes(anticipatedCell, chordCell, true);
                    pitchGrid.stopNotesBefore(chordCell);
                }
            }
            //LOGGER.log(Level.FINE, "anticipateChords_Poly()   AFTER  grid={0}", grid.toString(chordCell - 3, nextBeatChordCell + 1));
        }
    }

    /**
     * Process the anticipatable chords for Drums phrase p.
     * <p>
     * If a chord is anticipatable, anticipate some non-ghost notes (bass drum, snare, etc.) of next beat (or earlier).
     *
     * @param p
     * @param kit
     */
    public void anticipateChords_Drums(Phrase p, DrumKit kit)
    {
        if (p == null)
        {
            throw new IllegalArgumentException("p=" + p);   //NOI18N
        }
        int nonGhostVelLimit = getGhostNoteVelocityLimitDrums(p);
        List<Integer> accentPitches = kit.getKeyMap().getKeys(DrumKit.Subset.ACCENT);
        Grid gridHighPass = new Grid(p, beatRange, nbCellsPerBeat,
                ne -> ne.getVelocity() >= nonGhostVelLimit && accentPitches.contains(ne.getPitch()),
                preCellBeatWindow);
        Grid grid = new Grid(p, beatRange, nbCellsPerBeat, ne -> accentPitches.contains(ne.getPitch()), preCellBeatWindow);

        for (CLI_ChordSymbol cliCs : anticipatableChords)
        {
            int chordCell = grid.getCell(simpleChordSequence.getPositionInBeats(cliCs), true);
            int inBeatChordCell = chordCell % nbCellsPerBeat;
            int nextBeatChordCell = Math.min(lastCellIndex, chordCell + (nbCellsPerBeat - inBeatChordCell));
            int anticipatedCell = (chordCell == lastCellIndex) ? -1 : gridHighPass.getLastNoteCell(new IntRange(chordCell + 1,
                    nextBeatChordCell));
            LOGGER.log(Level.FINE, "anticipateChords_Drums() cliCs={0} chordCell={1} anticipatedCell={2}", new Object[]
            {
                cliCs, chordCell, anticipatedCell
            });
            if (anticipatedCell != -1)
            {
                //LOGGER.log(Level.FINE, "anticipateChords_Drums()   BEFORE grid={0}", grid.toString(chordCell - 3, nextBeatChordCell + 1));
                grid.removeNotes(new IntRange(chordCell, anticipatedCell - 1));
                grid.moveNotes(anticipatedCell, chordCell, false);
                //LOGGER.log(Level.FINE, "anticipateChords_Drums()   AFTER  grid={0}", grid.toString(chordCell - 3, nextBeatChordCell + 1));
            }
        }

    }

    // ==============================================================================================================
    // Private methods
    // ==============================================================================================================
    /**
     * Identify the anticipatable chords in scs.
     * <p>
     * Conditions to be an anticipatable chord:<br>
     * - latest offbeat chord of a given beat<br>
     * - not followed by a different chord on the next beat (or by the same chord but with a special interpretation)<br>
     * - not in the latest beat of scs<br>
     *
     * @param scs
     * @return
     */
    private List<CLI_ChordSymbol> identifyAnticipatableChords(SimpleChordSequence scs)
    {
        List<CLI_ChordSymbol> res = new ArrayList<>();
        int lastBar = scs.getBarRange().to;

        for (var cliCs : scs)
        {
            Position pos = cliCs.getPosition();
            int bar = pos.getBar();
            float beat = pos.getBeat();
            ChordRenderingInfo cri = cliCs.getData().getRenderingInfo();


            // Check if we can anticipate on next chord
            if (cri.hasOneFeature(ChordRenderingInfo.Feature.NO_ANTICIPATION) || !pos.isOffBeat())
            {
                continue;
            }


            int anticipatedBar = bar;
            int anticipatedBeat = (int) Math.ceil(beat);
            if (pos.isLastBarBeat(timeSignature))
            {
                // If we fall over next bar
                anticipatedBar++;
                anticipatedBeat = 0;
            }

            CLI_ChordSymbol cliCsNext = scs.higher(cliCs);
            if (cliCsNext != null)
            {
                ChordRenderingInfo criNext = cliCsNext.getData().getRenderingInfo();
                int barNext = cliCsNext.getPosition().getBar();
                float beatNext = cliCsNext.getPosition().getBeat();

                if (barNext == bar && Math.floor(beat) == Math.floor(beatNext))
                {
                    // This is not the last off-beat chord of the same beat
                    continue;
                }

                if (barNext > anticipatedBar
                        || beatNext >= (anticipatedBeat + cellDuration)
                        || (cliCsNext.getData().equals(cliCs.getData()) && criNext.getAccentFeature() == null && !criNext.hasOneFeature(
                        ChordRenderingInfo.Feature.HOLD, ChordRenderingInfo.Feature.SHOT)))
                {
                    // There is no problematic chord symbol on start of next beat, cliCs is anticipatable
                    res.add(cliCs);
                }

            } else if (anticipatedBar <= lastBar)
            {
                // Last offbeat chord but not last beat of the chord sequence
                res.add(cliCs);
            }
        }

        return res;
    }

    private int getGhostNoteVelocityLimitBass(Phrase p)
    {
        return 30;
    }

    private int getGhostNoteVelocityLimitDrums(Phrase p)
    {
        return 30;
    }
  
}

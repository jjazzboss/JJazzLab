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
package org.jjazz.rhythmmusicgeneration.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.Grid;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;

/**
 * Process the anticipated chords.
 */
public class AnticipatedChordProcessor
{

    private SimpleChordSequence simpleChordSequence;
    private FloatRange cSeqBeatRange;
    private int nbCellsPerBeat;
    private int lastCellIndex;
    private float cellDuration;
    private TimeSignature timeSignature;
    private final List<CLI_ChordSymbol> anticipatableChords = new ArrayList<>();

    protected static final Logger LOGGER = Logger.getLogger(AnticipatedChordProcessor.class.getSimpleName());

    /**
     * Construct an object to anticipated phrases corresponding to the specified parameters.
     *
     * @param cSeq                Can't be empty
     * @param cSeqStartPosInBeats The start position in beats of cSeq. Must be an integer.
     * @param nbCellsPerBeat      4 or 3. 3 should be used for ternary feel rhythm or 3/8 or 6/8 or 12/8 time signatures.
     */
    public AnticipatedChordProcessor(SimpleChordSequence cSeq, float cSeqStartPosInBeats, int nbCellsPerBeat)
    {
        if (cSeq == null || cSeq.isEmpty() || cSeqStartPosInBeats < 0
                || cSeqStartPosInBeats != Math.floor(cSeqStartPosInBeats)
                || nbCellsPerBeat < 3 || nbCellsPerBeat > 4)
        {
            throw new IllegalArgumentException( //NOI18N
                    "cSeq=" + cSeq + " cSeqStartPosInBeats=" + cSeqStartPosInBeats + " nbCellsPerBeat=" + nbCellsPerBeat);
        }

        this.simpleChordSequence = cSeq;
        this.timeSignature = simpleChordSequence.getTimeSignature();


        float nbNaturalBeats = this.timeSignature.getNbNaturalBeats();
        if (nbNaturalBeats != Math.floor(nbNaturalBeats))
        {
            // The algorithms used here, including grid, needs to be adapted for fractional nb of natural beats like 3.5 for 7/8
            throw new IllegalArgumentException("AccentProcessor() Time signature not yet supported: " + timeSignature);   //NOI18N
        }

        this.nbCellsPerBeat = nbCellsPerBeat;
        lastCellIndex = ((int) nbNaturalBeats * nbCellsPerBeat * simpleChordSequence.getBarRange().size()) - 1;
        cellDuration = 1f / nbCellsPerBeat;
        cSeqBeatRange = new FloatRange(cSeqStartPosInBeats, cSeqStartPosInBeats + cSeq.getBarRange().size() * nbNaturalBeats);

        identifyAnticipatableChords();

        LOGGER.log(Level.FINE, "AnticipatedChordProcessor -- cSeqStartPosInBeats={0} nbCellsPerBeat={1} ={2}", new Object[]
        {
            cSeqStartPosInBeats, nbCellsPerBeat, anticipatableChords
        });
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
        if (p == null)
        {
            throw new IllegalArgumentException("p=" + p);   //NOI18N
        }

        int nonGhostVelLimit = getGhostNoteVelocityLimitBass(p);
        Grid gridHighPass = new Grid(p, cSeqBeatRange, nbCellsPerBeat, ne -> ne.getVelocity() >= nonGhostVelLimit);
        Grid grid = new Grid(p, cSeqBeatRange, nbCellsPerBeat, null);


        for (CLI_ChordSymbol cliCs : anticipatableChords)
        {
            int chordCell = grid.getCell(getChordPositionInBeats(cliCs), true);
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
        if (p == null)
        {
            throw new IllegalArgumentException("p=" + p);   //NOI18N
        }
        Grid grid = new Grid(p, cSeqBeatRange, nbCellsPerBeat, null);

        for (CLI_ChordSymbol cliCs : anticipatableChords)
        {
            int chordCell = grid.getCell(getChordPositionInBeats(cliCs), true);
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
                    Grid pitchGrid = new Grid(p, cSeqBeatRange, nbCellsPerBeat, n -> n.getPitch() == pitch);
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
        Grid gridHighPass = new Grid(p, cSeqBeatRange, nbCellsPerBeat, ne -> ne.getVelocity() >= nonGhostVelLimit && accentPitches.contains(
                ne.getPitch()));
        Grid grid = new Grid(p, cSeqBeatRange, nbCellsPerBeat, ne -> accentPitches.contains(ne.getPitch()));

        for (CLI_ChordSymbol cliCs : anticipatableChords)
        {
            int chordCell = grid.getCell(getChordPositionInBeats(cliCs), true);
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
     * Identify the anticipatable chords in anticipatableChords.
     */
    private void identifyAnticipatableChords()
    {
        int lastBar = simpleChordSequence.getBarRange().to;

        for (var cliCs : simpleChordSequence)
        {
            Position pos = cliCs.getPosition();
            ChordRenderingInfo cri = cliCs.getData().getRenderingInfo();


            // Check if we can anticipate on next chord
            if (cri.hasOneFeature(ChordRenderingInfo.Feature.NO_ANTICIPATION) || !pos.isOffBeat())
            {
                // Chord is OnBeat
                continue;
            }


            int anticipatedBar = pos.getBar();
            int anticipatedBeat = (int) Math.ceil(pos.getBeat());
            if (pos.isLastBarBeat(timeSignature))
            {
                // If we fall over next bar
                anticipatedBar++;
                anticipatedBeat = 0;
            }

            CLI_ChordSymbol cliCsNext = simpleChordSequence.higher(cliCs);
            if (cliCsNext != null)
            {
                // Not the last chord 
                ChordRenderingInfo criNext = cliCsNext.getData().getRenderingInfo();
                int barNext = cliCsNext.getPosition().getBar();
                float beatNext = cliCsNext.getPosition().getBeat();


                if (barNext > anticipatedBar
                        || beatNext >= (anticipatedBeat + cellDuration)
                        || (cliCsNext.getData().equals(cliCs.getData()) && criNext.getAccentFeature() == null && !criNext.hasOneFeature(
                        ChordRenderingInfo.Feature.HOLD, ChordRenderingInfo.Feature.SHOT)))
                {
                    // There is no problematic chord symbol on start of next beat, cliCs is anticipatable
                    anticipatableChords.add(cliCs);
                }

            } else
            {
                // Last chord, don't need to check next chord
                if (anticipatedBar <= lastBar)
                {
                    anticipatableChords.add(cliCs);
                }
            }
        }
    }

    private int getGhostNoteVelocityLimitBass(Phrase p)
    {
        return 30;
    }

    private int getGhostNoteVelocityLimitDrums(Phrase p)
    {
        return 30;
    }

    private float getChordPositionInBeats(CLI_ChordSymbol cliCs)
    {
        return simpleChordSequence.toPositionInBeats(cliCs.getPosition(), cSeqBeatRange.from);
    }

}

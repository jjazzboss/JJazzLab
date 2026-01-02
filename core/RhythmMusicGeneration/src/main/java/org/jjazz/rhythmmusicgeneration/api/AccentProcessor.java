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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.MidiConst;
import static org.jjazz.rhythmmusicgeneration.api.AccentProcessor.LOGGER;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.phrase.api.Grid;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * Phrase manipulation methods dealing with accents.
 *
 * @see ChordRenderingInfo
 */
public class AccentProcessor
{


    /**
     * For a given phrase decide how chord symbols with HOLD/SHOT/EXTENDED_HOLD_SHOT rendering options should be processed.
     */
    public enum HoldShotMode
    {
        /**
         * Process hold/shot normally.
         */
        NORMAL,
        /**
         * Process hold/shot only if it's and extended hold shot.
         */
        EXTENDED,
        /**
         * Do not process hold/shot.
         */
        IGNORE
    }
    private final SimpleChordSequence simpleChordSequence;
    private final FloatRange cSeqBeatRange;
    private int nbCellsPerBeat;
    private int lastCellIndex;
    private float cellDuration;
    private final float preCellBeatWindow;
    private final TimeSignature timeSignature;
    private int tempo;

    protected static final Logger LOGGER = Logger.getLogger(AccentProcessor.class.getSimpleName());

    /**
     * Construct an object to manipulate phrases corresponding to the specified parameters.
     *
     * @param cSeq              Can't be empty. The start position in beats must be an arithmetic integer.
     * @param nbCellsPerBeat    4 or 3. 3 should be used for ternary feel rhythm or 3/8 or 6/8 or 12/8 time signatures.
     * @param tempo             Required to best adjust e.g. "shot" notes duration
     * @param preCellBeatWindow A value in the range [0;1/nbCellsPerBeat[. Used to accomodate for non-quantized notes: notes whose relative position is &gt;
     *                          -preCellBeatWindow will be included in the current cell.
     */
    public AccentProcessor(SimpleChordSequence cSeq, int nbCellsPerBeat, int tempo, float preCellBeatWindow)
    {
        Objects.requireNonNull(cSeq);
        Preconditions.checkArgument(!cSeq.isEmpty());
        Preconditions.checkArgument(cSeq.getStartBeatPosition() >= 0 && cSeq.getStartBeatPosition() == Math.floor(cSeq.getStartBeatPosition()),
                "cSeq.getStartBeatPosition()=%s",
                cSeq.getStartBeatPosition());

        Preconditions.checkArgument(TempoRange.checkTempo(tempo), "tempo=%s", tempo);
        Preconditions.checkArgument(nbCellsPerBeat >= 3 && nbCellsPerBeat <= 4, "nbCellsPerBeat=%s", nbCellsPerBeat);


        this.simpleChordSequence = cSeq;
        this.cSeqBeatRange = cSeq.getBeatRange();
        timeSignature = simpleChordSequence.getTimeSignature();
        this.preCellBeatWindow = preCellBeatWindow;

        float nbNaturalBeats = timeSignature.getNbNaturalBeats();
        if (nbNaturalBeats != Math.floor(nbNaturalBeats))
        {
            // The algorithms used here, including grid, needs to be adapted for fractional nb of natural beats like 3.5 for 7/8
            throw new IllegalArgumentException("AccentProcessor() Time signature not yet supported: " + timeSignature);   //NOI18N
        }


        this.nbCellsPerBeat = nbCellsPerBeat;
        lastCellIndex = ((int) nbNaturalBeats * nbCellsPerBeat * simpleChordSequence.getBarRange().size()) - 1;
        cellDuration = 1f / nbCellsPerBeat;

        this.tempo = tempo;

        LOGGER.log(Level.FINE, "\nAccentProcessor() -- cSeqBeatRange={0} timeSignature={1} nbCellsPerBeat={2}", new Object[]
        {
            cSeqBeatRange, timeSignature, nbCellsPerBeat
        });
    }


    /**
     * Process Hold/Shots for a drums phrase (not percussion).
     * <p>
     *
     * @param p
     * @param kit
     * @param hsMode
     */
    public void processHoldShotDrums(Phrase p, DrumKit kit, HoldShotMode hsMode)
    {
        Objects.requireNonNull(p);
        Objects.requireNonNull(kit);
        Objects.requireNonNull(hsMode);

        LOGGER.log(Level.FINE, "processHoldShotDrums() --  hsMode={0}", hsMode);


        if (p.isEmpty())
        {
            LOGGER.fine("processHoldShotDrums()  p is empty!");
            return;
        }


        // Prepare data
        GridDrumsHelper gdh = new GridDrumsHelper(p, kit);


        for (CLI_ChordSymbol cliCs : simpleChordSequence)
        {

            GridChordContext gct = new GridChordContext(cliCs, simpleChordSequence, gdh.gridAccents);
            ChordRenderingInfo cri = cliCs.getData().getRenderingInfo();


            if (!isProcessHoldShot(cri, hsMode))
            {
                // Nothing to do
                continue;
            }


//        LOGGER.log(Level.FINE, "processHoldShotDrums() gct={0}, features={1}", new Object[]
//        {
//            gct, cri.getFeatures()
//        });
//           LOGGER.log(Level.FINE, "processHoldShotDrums()   BEFORE grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 6));
            if (gct.afterCellRange.isEmpty())
            {
                // Nothing to do
                return;
            }

            // Clean a number of cells
            IntRange cleanRange = new IntRange(gct.chordCell + 1, gct.chordCell + getHoldShotDrumsPostSilenceDuration(gct, cri));
            cleanRange = gct.afterCellRange.getIntersection(cleanRange);
            gct.grid.removeNotes(cleanRange);
            gdh.gridOpenHiHats.removeNotes(cleanRange);
            gdh.gridCrashes.removeNotes(cleanRange);

            // LOGGER.log(Level.FINE, "processHoldShotDrums()   AFTER grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 6));
        }
    }

    /**
     * Process a drums phrase (not percussion).
     * <p>
     *
     * @param p
     * @param kit Required to get the accent and crash cymbals pitches
     */
    public void processAccentDrums(Phrase p, DrumKit kit)
    {
        Objects.requireNonNull(p);
        Objects.requireNonNull(kit);


        LOGGER.log(Level.FINE, "processAccentDrums() --  kit={0}", kit);


        if (p.isEmpty())
        {
            LOGGER.fine("processAccentDrums()  p is empty!");
            return;
        }


        // Prepare data
        GridDrumsHelper gdh = new GridDrumsHelper(p, kit);


        for (CLI_ChordSymbol cliCs : simpleChordSequence)
        {

            GridChordContext gct = new GridChordContext(cliCs, simpleChordSequence, gdh.gridAccents);
            ChordRenderingInfo cri = cliCs.getData().getRenderingInfo();

            if (cri.getAccentFeature() == null)
            {
                // No accent, nothing todo
                continue;
            }

//        LOGGER.log(Level.FINE, "processAccentDrums() gct={0}, features={1}", new Object[]
//        {
//            gct, cri.getFeatures()
//        });
//
//        LOGGER.log(Level.FINE, "processAccentDrums()   BEFORE grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 6));

            // Prepare note
            int notePitch;
            int noteVel;
            float noteDur = 0.1f;       // Don't care for percussion sounds                
            NoteEvent accentNote;       // The accent note


            if (gct.grid.isEmpty(gct.chordCell))
            {
                // Need to add a new accent from scratch
                notePitch = computeDrumsAccentPitch(gdh.refGridAccents, gct.chordCell);
                noteVel = computeNewDrumAccentNoteVelocity(notePitch, gdh.refGridAccents, gct.chordCell, cri);
                accentNote = new NoteEvent(notePitch, noteDur, noteVel, gct.chordPosInBeats);
                gct.grid.addNote(gct.chordCell, accentNote, gct.relPosInCell);

            } else
            {
                // Try to reuse existing note parameter                                
                NoteEvent oldNe = gct.grid.getLastNote(gct.chordCell);
                notePitch = oldNe.getPitch();
                // Ensure minimum velocity of the accent is respected
                noteVel = computeExistingDrumAccentNoteVelocity(gct.chord, oldNe);
                accentNote = new NoteEvent(notePitch, noteDur, noteVel, oldNe.getPositionInBeats());
                gct.grid.replaceNote(oldNe, accentNote);
            }


            // Handle crash cymbal
            if (cri.hasOneFeature(Feature.NO_CRASH))
            {
                // Remove possible existing crash if it was already present in the source phrase
                gdh.gridCrashes.removeNotes(gct.chordCell);

            } else if (gdh.needCrash(gct.chordCell, cri))
            {
                // Add crash cymbal
                gdh.addCrashCymbal(gct.chordCell, accentNote.getPositionInBeats(), cri);
            }


            // Clean cell just before
            if (!gct.beforeCellRange.isEmpty())
            {
                gct.grid.removeNotes(gct.chordCell - 1);
                gdh.gridOpenHiHats.removeNotes(gct.chordCell - 1);
            }

            // Clean cells until next start of next beat
            if (!gct.toNextBeatCellRange.isEmpty())
            {
                gct.grid.removeNotes(gct.toNextBeatCellRange);
            }

//        LOGGER.log(Level.FINE, "processAccentDrums()   AFTER grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 6));

        }
    }

    /**
     * Process the accents of a bass phrase.
     * <p>
     *
     * @param p
     */
    public void processAccentBass(Phrase p)
    {
        Objects.requireNonNull(p);

        LOGGER.log(Level.FINE, "processAccentBass() ");


        if (p.isEmpty())
        {
            LOGGER.fine("processAccentBass()   p is empty!");
            return;
        }


        // Prepare the grids
        Grid grid = new Grid(p, cSeqBeatRange, nbCellsPerBeat, null, preCellBeatWindow);
        Grid refGrid = new Grid(p.clone(), cSeqBeatRange, nbCellsPerBeat, null, preCellBeatWindow);    // Reference grid not modified, used for velocity/pitch calculation                


        for (CLI_ChordSymbol cliCs : simpleChordSequence)
        {

            GridChordContext gct = new GridChordContext(cliCs, simpleChordSequence, grid);
            ChordRenderingInfo cri = cliCs.getData().getRenderingInfo();


            if (cri.getAccentFeature() == null)
            {
                // No accent, nothing todo
                continue;
            }

            // LOGGER.log(Level.FINE, "processAccentBass()   BEFORE grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 6));

            int notePitch;
            int noteVel;
            float noteDur;
            int bassRelPitch = gct.chord.getData().getBassNote().getRelativePitch();

            if (gct.grid.isEmpty(gct.chordCell))
            {
                // Need to add a new note from sratch
                notePitch = computeNewNoteAbsolutePitch(gct.grid, gct.chordCell, bassRelPitch);
                noteVel = computeNewNoteVelocity(gct.grid, gct.chordCell, cri);
                noteDur = getAccentNoteMinDuration(gct);
                // LOGGER.log(Level.FINE, "      no note, create one");
            } else
            {
                // Try to reuse existing note parameters, ensure minimum vel. and duration is used
                NoteEvent ne = gct.grid.getLastNote(gct.chordCell);
                notePitch = (ne.getRelativePitch() == bassRelPitch) ? ne.getPitch() : ne.getClosestPitch(bassRelPitch);
                noteVel = Math.max(ne.getVelocity(), computeNewNoteVelocity(refGrid, gct.chordCell, cri));
                noteDur = Math.max(ne.getDurationInBeats(), getAccentNoteMinDuration(gct));
                // LOGGER.log(Level.FINE, "      reuse existing note, oldDur=" + ne.getDurationInBeats() + " newDur=" + getAccentNoteMinDuration(gct));
            }


            // Make room and add the new note
            NoteEvent ne = new NoteEvent(notePitch, noteDur, noteVel, gct.chordPosInBeats);
            IntRange cleanRange = getSpecialCellRange(gct, ne.getBeatRange());
            gct.grid.removeNotes(cleanRange);
            gct.grid.addNote(gct.chordCell, ne, gct.relPosInCell);


            // LOGGER.log(Level.FINE, "processAccentBass()   AFTER ne=" + ne + " cleanRange=" + cleanRange + " grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 6));
        }
    }

    /**
     * Process the Hold/Shot of a monophonic phrase (bass or others).
     * <p>
     *
     * @param p
     * @param hsMode
     */
    public void processHoldShotMono(Phrase p, HoldShotMode hsMode)
    {
        Objects.requireNonNull(p);
        Objects.requireNonNull(hsMode);

        LOGGER.log(Level.FINE, "processHoldShotMono() -- hsMode={0}", hsMode);


        if (p.isEmpty())
        {
            LOGGER.fine("processHoldShotMono()   p is empty!");
            return;
        }


        // Prepare the grid
        Grid grid = new Grid(p, cSeqBeatRange, nbCellsPerBeat, null, preCellBeatWindow);


        for (CLI_ChordSymbol cliCs : simpleChordSequence)
        {

            GridChordContext gct = new GridChordContext(cliCs, simpleChordSequence, grid);
            ChordRenderingInfo cri = cliCs.getData().getRenderingInfo();


            if (!isProcessHoldShot(cri, hsMode))
            {
                // Nothing todo
                continue;
            }


//        LOGGER.log(Level.FINE, "processHoldShotMono() gct={0}, features={1}", new Object[]
//        {
//            gct, cri.getFeatures()
//        });
//        LOGGER.log(Level.FINE, "processHoldShotMono()   BEFORE grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 12));
            if (gct.grid.isEmpty(gct.chordCell))
            {
                // No existing notes, do nothing

            } else
            {
                // Existing notes, change their duration
                if (cri.hasOneFeature(Feature.HOLD))
                {
                    int holdCell = gct.cellRange.to;
                    gct.grid.changeDuration(new IntRange(gct.chordCell, gct.chordCell), holdCell, false, true);

                } else
                {
                    // SHOT
                    int holdCell = gct.grid.getCell(gct.chordPosInBeats + getShotNoteDuration(gct), false);
                    holdCell = Math.min(gct.cellRange.to, holdCell);
                    gct.grid.changeDuration(new IntRange(gct.chordCell, gct.chordCell), holdCell, true, false);

                }
            }


            // Remove next notes until next chord
            gct.grid.removeNotes(gct.afterCellRange);


            // If SHOT, make sure there is no long notes starting just before to avoid rhythmic confusion
            if (cri.hasOneFeature(Feature.SHOT) && !gct.beforeCellRange.isEmpty())
            {
                for (NoteEvent ne : gct.grid.getCellNotes(gct.chordCell - 1))
                {
                    float dur = ne.getDurationInBeats();
                    if (dur >= 2 * cellDuration)
                    {
                        LOGGER.fine("processHoldShot()   No accent note at chordCell but removed previous cell's parasite long notes");
                        gct.grid.removeNotes(gct.chordCell - 1);
                        break;
                    }
                }
            }

            //LOGGER.log(Level.FINE, "processHoldShotMono()   AFTER grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 12));

        }
    }

    /**
     * Process the Hold/Shot of a chord-based phrase (polyphonic)
     * <p>
     *
     * @param p
     * @param hsMode
     */
    public void processHoldShotChord(Phrase p, HoldShotMode hsMode)
    {
        Objects.requireNonNull(p);
        Objects.requireNonNull(p);
//        if (!p.isEmpty() && p.getNotesBeatRange().from < cSeqBeatRange.from)
//        {
//            throw new IllegalArgumentException("hsMode=" + hsMode + " p=" + p);   //NOI18N
//        }


        LOGGER.log(Level.FINE, "processHoldShotChord() -- ");


        if (p.isEmpty())
        {
            LOGGER.fine("processHoldShotChord()   p is empty!");
            return;
        }

        // Prepare the grid
        Grid grid = new Grid(p, cSeqBeatRange, nbCellsPerBeat, null, preCellBeatWindow);


        for (CLI_ChordSymbol cliCs : simpleChordSequence)
        {

            GridChordContext gct = new GridChordContext(cliCs, simpleChordSequence, grid);
            ChordRenderingInfo cri = cliCs.getData().getRenderingInfo();

            if (!isProcessHoldShot(cri, hsMode))
            {
                // Nothing to do
                continue;
            }


//        LOGGER.log(Level.FINE, "processHoldShotChord() gct={0}, features={1}", new Object[]
//        {
//            gct, cri.getFeatures()
//        });
//        LOGGER.log(Level.FINE, "processHoldShotChord()   BEFORE grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 12));
            if (gct.grid.isEmpty(gct.chordCell))
            {
                // No existing notes, do nothing

            } else
            {
                // Existing notes, change their duration
                if (cri.hasOneFeature(Feature.HOLD))
                {
                    int holdCell = gct.cellRange.to;
                    gct.grid.changeDuration(new IntRange(gct.chordCell, gct.chordCell), holdCell, false, true);

                } else
                {
                    // SHOT
                    int holdCell = gct.grid.getCell(gct.chordPosInBeats + getShotNoteDuration(gct), false);
                    holdCell = Math.min(gct.cellRange.to, holdCell);
                    gct.grid.changeDuration(new IntRange(gct.chordCell, gct.chordCell), holdCell, true, false);

                }
            }

            // Remove next notes until next chord
            gct.grid.removeNotes(gct.afterCellRange);


            // If SHOT, make sure there is no long notes starting just before to avoid rhythmic confusion
            if (cri.hasOneFeature(Feature.SHOT) && !gct.beforeCellRange.isEmpty())
            {
                for (NoteEvent ne : gct.grid.getCellNotes(gct.chordCell - 1))
                {
                    float dur = ne.getDurationInBeats();
                    if (dur >= 2 * cellDuration)
                    {
                        LOGGER.fine("processHoldShotChord()   No accent note at chordCell but removed previous cell's parasite long notes");
                        gct.grid.removeNotes(gct.chordCell - 1);
                        break;
                    }
                }
            }

            // LOGGER.log(Level.FINE, "processHoldShotChord()   AFTER grid={0}", gct.grid.toString(gct.chordCell - 4, gct.chordCell + 12));

        }
    }

    /**
     * Process accents of a chord oriented phrase (polyphonic).
     * <p>
     *
     * @param p
     *
     */
    public void processAccentChord(Phrase p)
    {
        if (p == null || (!p.isEmpty() && p.getNotesBeatRange().from < cSeqBeatRange.from))
        {
            throw new IllegalArgumentException("p=" + p);   //NOI18N
        }

        LOGGER.log(Level.FINE, "processAccentChord() -- ");

        if (p.isEmpty())
        {
            LOGGER.fine("processAccentChord()   p is empty!");
            return;
        }


        // Prepare the grids
        Grid grid = new Grid(p, cSeqBeatRange, nbCellsPerBeat, null, preCellBeatWindow);


        for (CLI_ChordSymbol cliCs : simpleChordSequence)
        {

            GridChordContext gct = new GridChordContext(cliCs, simpleChordSequence, grid);
            ChordRenderingInfo cri = cliCs.getData().getRenderingInfo();

            if (cri.getAccentFeature() == null)
            {
                // Nothing to do
                continue;
            }


//        LOGGER.log(Level.FINE, "processAccentChord() gct={0}, features={1}", new Object[]
//        {
//            gct, cri.getFeatures()
//        });
//        LOGGER.log(Level.FINE, "processAccentChord()   BEFORE grid={0}", grid.toString(gzh.chordCell - 4, gzh.chordCell + 6));
            if (gct.grid.isEmpty(gct.chordCell))
            {
                // Nothing to do
                return;
            }

            // There are notes
            float velFactor;
            if (cri.hasOneFeature(Feature.ACCENT))
            {
                velFactor = 1.15f;
            } else if (cri.hasOneFeature(Feature.ACCENT_STRONGER))
            {
                velFactor = 1.3f;
            } else
            {
                throw new IllegalStateException("cri=" + cri);   //NOI18N
            }


            // Change velocity
            gct.grid.changeVelocity(new IntRange(gct.chordCell, gct.chordCell), v -> Math.round(v * velFactor));


            // Make room for the accent
            if (!gct.afterCellRange.isEmpty())
            {
                gct.grid.removeNotes(gct.afterCellRange.from);
            }


            // LOGGER.log(Level.FINE, "processAccentChord()   AFTER grid={0}", grid.toString(gct.chordCell - 4, gct.chordCell + 6));
        }
    }


    // ==============================================================================================================
    // Private methods
    // ==============================================================================================================
    /**
     * Also depends on tempo.
     *
     * @param gct
     * @param cri
     * @return A number of cells
     */
    private int getHoldShotDrumsPostSilenceDuration(GridChordContext gct, ChordRenderingInfo cri)
    {
        if (cri.hasOneFeature(Feature.EXTENDED_HOLD_SHOT))
        {
            return 10000;
        }
        int nbCells = gct.grid.getNbCellsPerBeat() * 2 - 1;
        if (TempoRange.SLOW.contains(tempo))
        {
            nbCells = Math.round(nbCells * 1f);
        } else if (TempoRange.MEDIUM_SLOW.contains(tempo))
        {
            nbCells = Math.round(nbCells * 1.2f);
        } else if (TempoRange.MEDIUM.contains(tempo))
        {
            nbCells = Math.round(nbCells * 1.4f);
        } else if (TempoRange.MEDIUM_FAST.contains(tempo))
        {
            nbCells = Math.round(nbCells * 1.6f);
        } else
        {
            // FAST
            nbCells = Math.round(nbCells * 2f);
        }

        return nbCells;
    }

    /**
     * Depend on getShotNoteDuration and context.
     *
     * @param gct The chord symbol context
     * @return
     */

    private float getAccentNoteMinDuration(GridChordContext gct)
    {
        float dur = getShotNoteDuration(gct) * 1.5f;
        dur = Math.min(dur, Math.max(gct.afterBeatRange.size() - 0.1f, cellDuration / 2));
        return dur;
    }

    /**
     * Depend on tempo and context.
     *
     * @param gct The chord symbol context
     * @return
     */
    private float getShotNoteDuration(GridChordContext gct)
    {
        float dur = cellDuration;
        if (TempoRange.SLOW.contains(tempo))
        {
            dur *= 1.8f;
        } else if (TempoRange.MEDIUM_SLOW.contains(tempo))
        {
            dur *= 1.9f;
        } else if (TempoRange.MEDIUM.contains(tempo))
        {
            dur *= 2.2f;
        } else if (TempoRange.MEDIUM_FAST.contains(tempo))
        {
            dur *= 2.5f;
        } else
        {
            // FAST
            dur *= 2.8f;
        }
        dur = Math.min(dur, Math.max(gct.afterBeatRange.size() - 0.1f, cellDuration / 2));
        return dur;
    }


    /**
     * Calculate the velocity of a new note to be inserted at cellIndex.
     * <p>
     * Calculation is based on notes around cellIndex and on cri parameters.
     *
     * @param grid
     * @param cellIndex
     * @param cri
     * @return
     */
    private int computeNewNoteVelocity(Grid grid, int cellIndex, ChordRenderingInfo cri)
    {
        final int NOTE_WINDOW_SIZE = 10;        // Nb of notes used

        // LOGGER.fine("computeNewNoteVelocity() -- cellIndex==" + cellIndex + " cri=" + cri);

        // Get the notes around our cell
        List<NoteEvent> nes = getAroundNotes(grid, cellIndex, NOTE_WINDOW_SIZE);

        // Get the velocity list
        List<Integer> velocities = nes.stream().map(ne -> ne.getVelocity()).toList();

        // Compute velocity
        int v = ComputeVelocityFromStats(velocities, cri, 60);

        // LOGGER.fine("computeNewNoteVelocity() v=" + v);
        return v;
    }

    /**
     * Calculate the absolute pitch of an accent note with relPitch at cellIndex for the specified phrase.
     * <p>
     * Calculation is based on the pitch of the closest note around the cell.
     *
     * @param grid
     * @param cellIndex
     * @param relPitch
     * @return
     */
    private int computeNewNoteAbsolutePitch(Grid grid, int cellIndex, int relPitch)
    {
        int res = 48 + relPitch;     // Default
        List<NoteEvent> nes = getAroundNotes(grid, cellIndex, 10);
        if (!nes.isEmpty())
        {
            int minDist = 1000;
            for (NoteEvent ne : nes)
            {
                int pitch = ne.getPitch();
                if (ne.getRelativePitch() == relPitch)
                {
                    res = pitch;
                    break;
                }
                int destPitch = ne.getClosestPitch(relPitch);
                int dist = Math.abs(destPitch - pitch);
                if (dist < minDist)
                {
                    minDist = dist;
                    res = destPitch;
                }
            }
        }
        return res;
    }

    /**
     * Select the pitch of a Drums accent note to be inserted at cellIndex.
     * <p>
     * Use the most used pitch in the cells around.
     *
     * @param grid      A grid limited to DRUMS_ACCENT_PITCHES.
     * @param cellIndex
     * @return One of the DRUMS_ACCENT_PITCHES pitches
     * @see DRUMS_ACCENT_PITCHES
     */
    private int computeDrumsAccentPitch(Grid grid, int cellIndex)
    {
        final int NOTE_WINDOW_SIZE = 10;
        int res = MidiConst.BASS_DRUM_1;

        List<NoteEvent> nes = getAroundNotes(grid, cellIndex, NOTE_WINDOW_SIZE);
        if (!nes.isEmpty())
        {
            HashMap<Integer, Integer> mapPitchCount = new HashMap<>();
            int maxCount = -1;
            for (NoteEvent ne : nes)
            {
                int pitch = ne.getPitch();
                Integer count = mapPitchCount.get(pitch);
                if (count == null)
                {
                    count = 0;
                }
                count++;
                mapPitchCount.put(pitch, count);
                if (count > maxCount)
                {
                    maxCount = count;
                    res = pitch;
                }
            }
        }
        return res;
    }

    /**
     * Calculate the velocity of a new drum accent note to be inserted at cellIndex.
     * <p>
     * Calculation is based on notes with same pitch around cellIndex and on cri.
     *
     * @param pitch
     * @param grid
     * @param cellIndex
     * @param cri
     * @return
     */
    private int computeNewDrumAccentNoteVelocity(int pitch, Grid grid, int cellIndex, ChordRenderingInfo cri)
    {
        final int NOTE_WINDOW_SIZE = 10;        // Nb of notes used

        // LOGGER.fine("computeNewDrumAccentNoteVelocity() -- pitch=" + pitch + " cellIndex=" + cellIndex + " cri=" + cri);

        // Get the notes around our cell
        List<NoteEvent> nes = getAroundNotes(grid, cellIndex, NOTE_WINDOW_SIZE);

        // Get the velocity list only for notes with same pitch (same instrument)
        List<Integer> velocities = nes.stream().filter(ne -> ne.getPitch() == pitch).map(ne -> ne.getVelocity()).toList();

        // Compute velocity
        int v = ComputeVelocityFromStats(velocities, cri, 55);

        // LOGGER.fine("computeNewDrumAccentNoteVelocity() v=" + v);
        return v;
    }

    /**
     * Calculate the velocity from an existing drum accent note.
     * <p>
     *
     * @param cliCs
     * @param accentNote
     * @return
     */
    private int computeExistingDrumAccentNoteVelocity(CLI_ChordSymbol cliCs, NoteEvent accentNote)
    {
        final int MEDIUM_VEL_OFFSET = 12;
        final int STRONG_VEL_OFFSET = 25;
        final int EXTRA_DOWN_BEAT = 5;
        ChordRenderingInfo cri = cliCs.getData().getRenderingInfo();
        Position pos = cliCs.getPosition();
        float vel = accentNote.getVelocity();
        int offset = cri.getAccentFeature().equals(Feature.ACCENT) ? MEDIUM_VEL_OFFSET : STRONG_VEL_OFFSET;
        int extra = pos.isFirstBarBeat() || pos.isHalfBarBeat(timeSignature, true) || pos.isHalfBarBeat(timeSignature, false) ? EXTRA_DOWN_BEAT : 0;

        vel += offset + extra;

        return (int) Math.min(120, vel);

    }


    /**
     * Get the recommended velocity for a new note amongst the specified velocity.
     *
     * @param velocities
     * @param cri
     * @param defaultVal If not enough notes around
     * @return
     */
    private int ComputeVelocityFromStats(List<Integer> velocities, ChordRenderingInfo cri, int defaultVal)
    {
        // We need a copy : we'll modify the list for the calculations
        velocities = new ArrayList<>(velocities);


//        final float MEDIUM_VEL_FACTOR = 1f;
//        final float STRONG_VEL_FACTOR = 1.3f;
        final int MEDIUM_VEL_OFFSET = 14;
        final int STRONG_VEL_OFFSET = 30;
        int vRef;
        int vMin, vMax;

        if (velocities.isEmpty())
        {
            vRef = defaultVal;
            vMin = vRef;
            vMax = vRef;
            LOGGER.log(Level.FINE, "ComputeVelocityFromStats() Empty list, using default velocity as reference={0}", vRef);

        } else
        {
            // Compute stats
            int sum = velocities.stream().mapToInt(vel -> vel).sum();
            Collections.sort(velocities);
            int size = velocities.size();
            vMin = velocities.get(0);
            vMax = velocities.get(size - 1);
            int vRange = vMax - vMin + 1;
            int vMean = Math.round(sum / size);
            int vMedian = Math.round((size % 2 != 0) ? velocities.get(size / 2) : (velocities.get((size - 1) / 2) + velocities.get(size / 2)) / 2f);
            float standardDev = 0;   // Standard Deviation
            for (Integer vel : velocities)
            {
                standardDev += Math.pow(vel - vMean, 2);
            };
            standardDev = size > 1 ? Math.round(Math.sqrt(standardDev) / (size - 1)) : 0; // Formula for standard variation of a sample, hence the /(n-1)


            if (cri.getAccentFeature() == null)
            {
                // No accent
                vRef = vMedian;
                // LOGGER.fine("ComputeVelocityFromStats() NORMAL NOTE  size=" + size + " stdDev=" + standardDev + " vRange=" + vRange + " vMedian=" + vMedian + " vRef=" + vRef);

            } else
            {
                // Accent note
                vRef = Math.round(vMedian + standardDev);
                // vRef = Math.max(vMedian, vMean);
                // LOGGER.fine("ComputeVelocityFromStats() ACCENT NOTE size=" + size + " stdDev=" + standardDev + " vRange=" + vRange + " vMedian=" + vMedian + " vRef=" + vRef);

                // Compute the value depending on the data spreading
//                // A wide-spread velocity means there are probably some accents already 
//                // IMPORTANT: thresholds calculated for NOTE_WINDOW_SIZE=10
//                final float LOW_WIDESPREAD_THRESHOLD = 5f;
//                final float MEDIUM_WIDESPREAD_THRESHOLD = 8f;
//
//                if (standardDev < LOW_WIDESPREAD_THRESHOLD)
//                {
//                    vRef = Math.round(vMax);
//                    LOGGER.fine("calculateVelocity() LOW_WIDESPREAD  size=" + size + " stdDev=" + standardDev + "vRange=" + vRange + " vMedian=" + vMedian + " vRef=" + vRef);
//                } else if (standardDev < MEDIUM_WIDESPREAD_THRESHOLD)
//                {
//                    vRef = Math.round(vMedian + 1.3f * standardDev);
//                    LOGGER.fine("calculateVelocity() MEDIUM_WIDESPREAD  size=" + size + " stdDev=" + standardDev + "vRange=" + vRange + " vMedian=" + vMedian + " vRef=" + vRef);
//                } else
//                {
//                    vRef = Math.round(vMedian + standardDev);
//                    LOGGER.fine("calculateVelocity() HIGH_WIDESPREAD  size=" + size + " stdDev=" + standardDev + "vRange=" + vRange + " vMedian=" + vMedian + " vRef=" + vRef);
//                }

                vRef = Math.min(vMax, vRef);        // vMedian+standardDev can be > vMax sometimes
            }
        }


        // Compute the final velocity
        int v;
        if (cri.getAccentFeature() == null)
        {
            v = vRef;
        } else
        {
            // Impact of accent level
            //float factor;
            int offset;
            if (cri.hasOneFeature(Feature.ACCENT))
            {
                // factor = MEDIUM_VEL_FACTOR;
                offset = MEDIUM_VEL_OFFSET;
            } else if (cri.hasOneFeature(Feature.ACCENT_STRONGER))
            {
                // factor = STRONG_VEL_FACTOR;                
                offset = STRONG_VEL_OFFSET;
            } else
            {
                throw new IllegalStateException("cri=" + cri);   //NOI18N
            }
            // v = Math.round(vRef * factor);
            v = vRef + offset;
        }


        v = Math.min(v, 120);
        v = Math.max(v, 10);

        // LOGGER.fine("ComputeVelocityFromStats()  ==> v=" + v);

        return v;
    }


    /**
     * Try to get notes at cell and the cells before.
     * <p>
     * If there is not enough notes before cell, add some notes following cell until reaching nbNotes.
     *
     * @param grid
     * @param cell
     * @param nbNotes The maximum number of notes to get
     * @return An array whose size is [0-nbNotes]
     */
    private List<NoteEvent> getAroundNotes(Grid grid, int cell, int nbNotes)
    {
        if (cell < 0 || cell > lastCellIndex || nbNotes < 0 || grid == null)
        {
            throw new IllegalArgumentException("grid=" + grid + " cellIndex=" + cell + " nbNotes=" + nbNotes);   //NOI18N
        }
        List<NoteEvent> nes = grid.getCellNotes(new IntRange(0, cell));
        if (nes.size() < nbNotes)
        {
            // Not enough before, complete with some notes after (if any)
            int i = cell + 1;
            while (nes.size() < nbNotes && i <= lastCellIndex)
            {
                nes.addAll(grid.getCellNotes(i));
                i++;
            }
        }
        if (!nes.isEmpty())
        {
            nes = nes.subList(Math.max(nes.size() - nbNotes, 0), nes.size());
        }
        return nes;
    }

    private boolean isProcessHoldShot(ChordRenderingInfo cri, HoldShotMode hsm)
    {
        if (!cri.hasOneFeature(Feature.HOLD, Feature.SHOT) || hsm.equals(HoldShotMode.IGNORE))
        {
            return false;
        }
        return !(hsm.equals(HoldShotMode.EXTENDED) && !cri.hasOneFeature(Feature.EXTENDED_HOLD_SHOT));
    }

    /**
     * Get the cell range from specified beatRange without taking into account pre-cell beat window for the 'to' bound.
     * <p>
     * If we want to remove notes, with 4 cells per beat, if beatRange stops at 0.22, no need to clear cell 1 [0.25-0.5], only cell 0 is enough.
     *
     * @param gct
     * @param beatRange
     * @return
     */
    private IntRange getSpecialCellRange(GridChordContext gct, FloatRange beatRange)
    {
        IntRange rg = gct.grid.getCellRange(beatRange, false);
        float lastCellStartPos = gct.grid.getStartPos(rg.to);
        if ((beatRange.to - lastCellStartPos) < 0 & rg.size() > 1)
        {
            // Upper bound is in the pre-cell beat window of next cell
            rg = rg.getTransformed(0, -1);
        }
        return rg;
    }


    // ---------------------------------------------------------------------------------------------------------------------
    // Private classes
    // ---------------------------------------------------------------------------------------------------------------------
    /**
     * Helper class to deal with drums.
     * <p>
     */
    private class GridDrumsHelper
    {

        private static final double CRASH_THRESHOLD_MIN = 0.2;         // 2/10 chances of adding a crash cymbal
        private static final double CRASH_THRESHOLD_NORMAL = 0.6;
        private static final double CRASH_THRESHOLD_MAX = 0.8;         // 8/10 chances of adding a crash cymbal
        private static final double SLIDING_WINDOW_CRASH_COUNT_NORMAL = 1; // If 1 crash in the sliding window use CRASH_THRESHOLD_NORMAL
        private static final int SLIDING_WINDOW_BEAT_SIZE = 4;
        private final LinkedList<Integer> slidingWindow = new LinkedList<>();   // Keep the cell of each previous crashes within the sliding window                
        private int slidingWindowSize;

        public static final int ACCENT_THRESHOLD_VEL_MIN = 40;
        public List<Integer> openHiHatPitches;
        public List<Integer> accentPitches;
        public List<Integer> crashPitches;
        public Grid gridCrashes;
        public Grid refGridCrashes;
        public Grid gridAccents;
        public Grid refGridAccents;
        public Grid gridOpenHiHats;


        public GridDrumsHelper(Phrase p, DrumKit kit)
        {
            if (p == null || kit == null)
            {
                throw new IllegalArgumentException("p=" + p + " kit=" + kit);   //NOI18N
            }

            slidingWindowSize = SLIDING_WINDOW_BEAT_SIZE * nbCellsPerBeat;

            accentPitches = kit.getKeyMap().getKeys(DrumKit.Subset.ACCENT);
            Predicate<NoteEvent> filterAccents = ne -> accentPitches.contains(ne.getPitch()) && ne.getVelocity() >= ACCENT_THRESHOLD_VEL_MIN;
            gridAccents = new Grid(p, cSeqBeatRange, nbCellsPerBeat, filterAccents, preCellBeatWindow);
            refGridAccents = gridAccents.clone(); // Preserved grid for velocity/pitch calculations        

            openHiHatPitches = kit.getKeyMap().getKeys(DrumKit.Subset.HI_HAT_OPEN);
            gridOpenHiHats = new Grid(p, cSeqBeatRange, nbCellsPerBeat, ne -> openHiHatPitches.contains(ne.getPitch()), preCellBeatWindow);

            crashPitches = kit.getKeyMap().getKeys(DrumKit.Subset.CRASH);
            gridCrashes = new Grid(p, cSeqBeatRange, nbCellsPerBeat, ne -> crashPitches.contains(ne.getPitch()), preCellBeatWindow);
            refGridCrashes = gridCrashes.clone();
        }

        /**
         * Should we insert a crash at specified cellIndex ?
         *
         * @param cellIndex
         * @param cri       A Strong accent has higher chances to use a crash.
         * @return
         */
        public boolean needCrash(int cellIndex, ChordRenderingInfo cri)
        {
            double threshold = CRASH_THRESHOLD_NORMAL;

            // Easy case
            if (cri.hasOneFeature(Feature.NO_CRASH))
            {
                return false;
            } else if (cri.hasOneFeature(Feature.CRASH))
            {
                return true;
            }

            // Not specified: use random
            updateSlidingWindow(cellIndex);
            int nbCrashes = slidingWindow.size();

            if (nbCrashes < SLIDING_WINDOW_CRASH_COUNT_NORMAL)
            {
                // There is less crashes than normal, make a crash more probable
                threshold += 0.1f * (SLIDING_WINDOW_CRASH_COUNT_NORMAL - nbCrashes);
            } else if (nbCrashes > SLIDING_WINDOW_CRASH_COUNT_NORMAL)
            {
                // There is already more crashes than normal, make a crash less probable
                threshold -= 0.15f * (nbCrashes - SLIDING_WINDOW_CRASH_COUNT_NORMAL);
            }

            float factor;
            if (cri.hasOneFeature(Feature.ACCENT))
            {
                factor = 1f;
            } else if (cri.hasOneFeature(Feature.ACCENT_STRONGER))
            {
                factor = 1.3f;
            } else
            {
                // No accent
                factor = 1f;
            }

            threshold *= factor;
            threshold = Math.min(threshold, CRASH_THRESHOLD_MAX);
            threshold = Math.max(threshold, CRASH_THRESHOLD_MIN);

            boolean b = Math.random() < threshold;

            return b;
        }

        /**
         * If not already present add a splash or a crash at specified cell.
         *
         * @param cellIndex  The cell where to add the cymbal
         * @param posInBeats The position of the crash
         * @param cri
         */
        public void addCrashCymbal(int cellIndex, float posInBeats, ChordRenderingInfo cri)
        {
            final int NOTE_WINDOW_SIZE = 5;

            if (!gridCrashes.isEmpty(cellIndex) || cri.hasOneFeature(Feature.NO_CRASH) || crashPitches.isEmpty())
            {
                return;
            }

            // Randomly choose cymbal
            int index = (int) Math.round(Math.random() * (crashPitches.size() - 1));
            int crashPitch = crashPitches.get(index);


            // Find velocity
            List<NoteEvent> nes = getAroundNotes(refGridCrashes, cellIndex, NOTE_WINDOW_SIZE);
            List<Integer> velocities = nes.stream().filter(ne -> ne.getPitch() == crashPitch).map(ne -> ne.getVelocity()).toList();
            int v = ComputeVelocityFromStats(velocities, cri, 60);


            // Add the cymbal
            float inCellPos = posInBeats - gridCrashes.getStartPos(cellIndex);   // Can be negative !)            
            gridCrashes.addNote(cellIndex, new Note(crashPitch, 0.1f, v), inCellPos);


            // Update the sliding window
            slidingWindow.offerFirst(cellIndex);

        }

        /**
         * Update the sliding window to discard elements out of the slidingWindow.
         *
         * @param crashCell The last cell where a crash cymbal was used
         */
        private void updateSlidingWindow(int crashCell)
        {
            Integer iCell;
            while ((iCell = slidingWindow.peekLast()) != null && iCell < crashCell - slidingWindowSize)
            {
                slidingWindow.pollLast();
            }
        }
    }
}

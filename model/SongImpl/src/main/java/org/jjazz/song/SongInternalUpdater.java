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
package org.jjazz.song;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.DeletedBarsEvent;
import org.jjazz.chordleadsheet.api.event.InsertedBarsEvent;
import org.jjazz.chordleadsheet.api.event.SectionAddedEvent;
import org.jjazz.chordleadsheet.api.event.SectionChangedEvent;
import org.jjazz.chordleadsheet.api.event.SectionMovedEvent;
import org.jjazz.chordleadsheet.api.event.SectionRemovedEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.midimix.MidiMixImpl;
import org.jjazz.midimix.MidiMixManagerImpl;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.rhythm.api.AdaptedRhythm;
import static org.jjazz.rhythm.api.Division.EIGHTH_SHUFFLE;
import static org.jjazz.rhythm.api.Division.EIGHTH_TRIPLET;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import static org.jjazz.rhythm.api.RhythmVoice.Type.DRUMS;
import static org.jjazz.rhythm.api.RhythmVoice.Type.PERCUSSION;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongPropertyChangeEvent;
import org.jjazz.songstructure.SongStructureImpl;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptRhythmChangedEvent;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.Exceptions;

/**
 * Manage the derived changes after a primary change on a Song component (Song, ChordLeadSheet, SongStructure).
 * <p>
 * For example, a ChordLeadSheet time signature change might require an update of a SongPart rhythm, which will impact the MidiMix, and possibly some chord
 * symbols position back in the ChordLeadSheet if rhythm division has changed.
 * <p>
 * SongInternalUpdater also allows to pre-check an operation: e.g. a time signature change should be vetoed if it ends up adding a new rhythm in a MidiMix with
 * not enough MIDI channels.
 */
class SongInternalUpdater
{

    private final Song song;
    private MidiMixImpl midiMix;
    private final SongStructureImpl songStructure;
    private final ChordLeadSheetImpl chordLeadSheet;
    private static final Logger LOGGER = Logger.getLogger(SongInternalUpdater.class.getSimpleName());

    public SongInternalUpdater(Song sg)
    {
        this.song = sg;
        this.songStructure = (SongStructureImpl) sg.getSongStructure();
        this.chordLeadSheet = (ChordLeadSheetImpl) sg.getChordLeadSheet();
    }


    /**
     * Check if a SongPropertyChangeEvent is authorized.
     *
     * @param event
     * @throws UnsupportedEditException If change is not authorized
     */
    public void preCheckChange(SongPropertyChangeEvent event) throws UnsupportedEditException
    {
        Objects.requireNonNull(event);
        assert event.getSource() == song;

        switch (event.getPropertyName())
        {
            case Song.PROP_USER_PHRASE ->
            {
                if (event.getOldValue() instanceof Phrase p)
                {
                    // It's an added phrase
                    String name = (String) event.getNewValue();
                    getDerivedAddUserPhrase(name, p.isDrums(), true);    // throws UnsupportedEditException
                }
            }
            default ->
            {
                // Nothing
            }
        }
    }

    /**
     * Check if a change is authorized.
     *
     * @param event
     * @throws UnsupportedEditException If change is not authorized
     */
    public void preCheckChange(ClsChangeEvent event) throws UnsupportedEditException
    {
        Objects.requireNonNull(event);
        switch (event)
        {
            case SectionAddedEvent e ->
            {
                if (e.getSameBarReplacedSection() != null)
                {
                    // Section replaces another one at same bar
                    getDerivedSectionReplaced(e.getSameBarReplacedSection(), e.getCLI_Section(), true);      // throws UnsupportedEditException
                } else
                {
                    getDerivedSectionAdded(e, true);        // throws UnsupportedEditException
                }
            }
            case SectionChangedEvent e ->
            {
                if (e.isTimeSignatureChanged())
                {
                    getDerivedSectionTimeSignatureChanged(e, true);   // throws UnsupportedEditException
                }
            }
            default ->
            {
                // Nothing
            }
        }
    }

    /**
     * Check if a change is authorized.
     *
     * @param event
     * @throws UnsupportedEditException If change is not authorized
     */
    public void preCheckChange(SgsChangeEvent event) throws UnsupportedEditException
    {
        Objects.requireNonNull(event);
        switch (event)
        {
            case SptRhythmChangedEvent srce ->
            {
                getDerivedSptRhythmChanged(srce, true);
            }
            case SptAddedEvent sae ->
            {
                getDerivedSptAdded(sae, true);
            }
            default ->
            {
                // Nothing
            }
        }
    }

    /**
     * Provide the WriteOperations (or ThrowingWriteOperations) required on other Song components following the source change event.
     * <p>
     *
     * @param results
     * @return Can be empty.
     */
    public List<Operation> getDerivedOperations(WriteOperationResults results)
    {
        Objects.requireNonNull(results);

        if (results.clsChangeEvent() == null && results.sgsChangeEvent() == null && results.pChangeEvent() == null)
        {
            return Collections.emptyList();
        }

        if (results.clsChangeEvent() != null)
        {
            return getDerivedOperations(results.clsChangeEvent());

        } else if (results.sgsChangeEvent() != null)
        {
            return getDerivedOperations(results.sgsChangeEvent());

        } else
        {
            return getDerivedOperations(results.pChangeEvent());
        }
    }

    // =============================================================================================================
    // Private methods
    // =============================================================================================================

    private List<Operation> getDerivedOperations(ClsChangeEvent srcEvent)
    {
        if (srcEvent.isUndoOrRedo())
        {
            // IMPORTANT : Each Song component generates his own undoableEdits,
            // so we must not forward changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice
            return Collections.emptyList();
        }

        List<Operation> res = null;
        try
        {
            switch (srcEvent)
            {
                case SizeChangedEvent e ->
                {
                    res = getDerivedClsSizeChanged(e);
                }
                case DeletedBarsEvent e ->
                {
                    res = getDerivedDeletedBars(e);
                }
                case InsertedBarsEvent e ->
                {
                    res = getDerivedInsertedBars(e);
                }
                case SectionAddedEvent e ->
                {
                    if (e.getSameBarReplacedSection() != null)
                    {
                        // Section replaces another one at same bar
                        res = getDerivedSectionReplaced(e.getSameBarReplacedSection(), e.getCLI_Section(), false);
                    } else
                    {
                        res = getDerivedSectionAdded(e, false);
                    }
                }
                case SectionChangedEvent e ->
                {
                    if (e.isNameChanged())
                    {
                        res = getDerivedSectionNameChanged(e);
                    }
                    if (e.isTimeSignatureChanged())
                    {
                        if (res == null)
                        {
                            res = getDerivedSectionTimeSignatureChanged(e, false);
                        } else
                        {
                            res.addAll(getDerivedSectionTimeSignatureChanged(e, false));
                        }
                    }
                }
                case SectionRemovedEvent e ->
                {
                    res = getDerivedSectionRemoved(e);
                }
                case SectionMovedEvent e ->
                {
                    res = getDerivedSectionMoved(e);
                }
                default ->
                {
                    res = Collections.emptyList();
                }
            }
        } catch (UnsupportedEditException ex)
        {
            // Should never happen : exception should have been caught by a previous call to preCheckChange() 
            Exceptions.printStackTrace(ex);
            throw new IllegalStateException("getNextOperations() Unexpected exception: ex=" + ex.getMessage());
        }

        return res;
    }

    private List<Operation> getDerivedOperations(SgsChangeEvent srcEvent)
    {

        if (srcEvent.isUndoOrRedo())
        {
            // IMPORTANT : Each Song component generates his own undoableEdits,
            // so we must not forward changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice            
            return Collections.emptyList();
        }


        List<Operation> res;
        try
        {
            switch (srcEvent)
            {
                case SptAddedEvent e ->
                {
                    res = getDerivedSptAdded(e, false);
                }
                case SptRemovedEvent e ->
                {
                    res = getDerivedSptRemoved(e);
                }
                case SptRhythmChangedEvent e ->
                {
                    res = getDerivedSptRhythmChanged(e, false);
                }
                default ->
                {
                    res = Collections.emptyList();
                }
            }
        } catch (UnsupportedEditException ex)
        {
            // Should never happen : exception should have been caught by a previous call to preCheckChange() 
            Exceptions.printStackTrace(ex);
            throw new IllegalStateException("getNextOperations() Unexpected exception: ex=" + ex.getMessage());
        }

        return res;
    }

    private List<Operation> getDerivedOperations(SongPropertyChangeEvent srcEvent)
    {

        if (srcEvent.isUndoOrRedo())
        {
            // IMPORTANT : Each Song component generates his own undoableEdits,
            // so we must not forward changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice            
            return Collections.emptyList();
        }


        List<Operation> res;
        try
        {
            switch (srcEvent.getPropertyName())
            {
                case Song.PROP_USER_PHRASE ->
                {
                    if (srcEvent.getNewValue() instanceof String name)
                    {
                        // Added phrase
                        Phrase p = (Phrase) srcEvent.getOldValue();
                        res = getDerivedAddUserPhrase(name, p.isDrums(), false);
                    } else
                    {
                        // Removed phrase
                        String name = (String) srcEvent.getOldValue();
                        res = getDerivedRemoveUserPhrase(name);
                    }
                }
                case Song.PROP_PHRASE_NAME ->
                {
                    String oldName = (String) srcEvent.getOldValue();
                    String newName = (String) srcEvent.getNewValue();
                    res = getDerivedRenameUserPhrase(oldName, newName);
                }
                default ->
                {
                    res = Collections.emptyList();
                }
            }
        } catch (UnsupportedEditException ex)
        {
            // Should never happen : exception should have been caught by a previous call to preCheckChange() 
            Exceptions.printStackTrace(ex);
            throw new IllegalStateException("getNextOperations() Unexpected exception: ex=" + ex.getMessage());
        }

        return res;

    }

    /**
     * A section was added to an empty bar.
     * <p>
     * - Insert a new SongPart for the added section<br>
     * - Reduce the size of the SongPart(s) whose parent section is the previous shortened section<br>
     *
     * @param evt
     * @param preCheck
     * @return
     * @throws UnsupportedEditException
     */
    private List<Operation> getDerivedSectionAdded(SectionAddedEvent evt, boolean preCheck) throws UnsupportedEditException
    {
        var cliSection = evt.getCLI_Section();
        int bar = cliSection.getPosition().getBar();
        assert bar > 0 : "evt=" + evt;
        CLI_Section prevSection = evt.getPreviousBarSection();
        assert prevSection != null : "evt=" + evt;


        SongPart spt = createSptAfterSection(cliSection, prevSection);
        SptAddedEvent event = new SptAddedEvent(songStructure, List.of(spt));

        if (preCheck)
        {
            getDerivedSptAdded(event, true);       //  // throws UnsupportedEditException
            return null;
        }


        List<Operation> res = new ArrayList<>();


        // Add the new SongPart
        res.add(songStructure.addSongPartsOperation(List.of(spt)));


        // Resize of SongParts linked to previous section
        var mapSptSize = getMapSptSize(prevSection);
        res.add(songStructure.resizeSongPartsOperation(mapSptSize));


        return res;
    }


    /**
     * A section was replaced by a new one (same bar).
     * <p>
     * - Update the parent section and possibly rename the impacted SongParts<br>
     *
     * @param oldSection
     * @param newSection
     * @param preCheck
     * @return
     * @throws UnsupportedEditException
     */
    private List<Operation> getDerivedSectionReplaced(CLI_Section oldSection, CLI_Section newSection, boolean preCheck) throws UnsupportedEditException
    {
        var oldSpts = getSongParts(oldSection);
        if (oldSpts.isEmpty())
        {
            return Collections.emptyList();
        }

        var oldName = oldSection.getData().getName();
        var oldTs = oldSection.getData().getTimeSignature();
        var newName = newSection.getData().getName();
        var newTs = newSection.getData().getTimeSignature();
        Rhythm newRhythm = newTs.equals(oldTs) ? null : songStructure.getRecommendedRhythm(newTs, oldSpts.get(0).getStartBarIndex());


        if (preCheck)
        {
            if (newRhythm != null)
            {
                var event = new SptRhythmChangedEvent(songStructure, newRhythm, oldSpts, oldSpts);
                getDerivedSptRhythmChanged(event, true);               // throws UnsupportedEditException
            }
            return null;
        }


        // Update SongParts rhythm and parentSection
        List<Operation> res = new ArrayList<>();
        res.add(songStructure.setSongPartsRhythmOperation(oldSpts, newRhythm, newSection));


        // Update SongPart name only if it was not customized by user
        List<SongPart> toBeRenamedSpts = oldSpts.stream()
                .filter(spt -> spt.getName().equalsIgnoreCase(oldName))
                .toList();
        res.add(songStructure.setSongPartsNameOperation(toBeRenamedSpts, newName));


        return res;
    }

    /**
     * A section time signature was changed.
     * <p>
     * - Update impacted SongParts rhythm<br>
     *
     * @param sce
     * @param preCheck
     * @return
     * @throws UnsupportedEditException
     */
    private List<Operation> getDerivedSectionTimeSignatureChanged(SectionChangedEvent sce, boolean preCheck) throws UnsupportedEditException
    {
        assert sce.isTimeSignatureChanged() : "sce=" + sce;


        var cliSection = sce.getCLI_Section();
        var spts = getSongParts(cliSection);
        if (spts.isEmpty())
        {
            return Collections.emptyList();
        }

        var newTs = sce.getNewSection().getTimeSignature();
        Rhythm newRhythm = songStructure.getRecommendedRhythm(newTs, spts.getFirst().getStartBarIndex());


        if (preCheck)
        {
            var event = new SptRhythmChangedEvent(songStructure, newRhythm, spts, spts);
            getDerivedSptRhythmChanged(event, true);               // throws UnsupportedEditException
            return null;
        }


        // Set rhythm of impacted SongParts
        List<Operation> res = new ArrayList<>();
        res.add(songStructure.setSongPartsRhythmOperation(spts, newRhythm, null));

        return res;

    }

    /**
     * A section name was changed.
     * <p>
     * - Update impacted SongPart(s) name only if it was not customized by user<br>
     *
     * @param sce
     * @return
     */
    private List<Operation> getDerivedSectionNameChanged(SectionChangedEvent sce)
    {
        assert sce.isNameChanged() : "sce=" + sce;


        var cliSection = sce.getCLI_Section();
        var spts = getSongParts(cliSection);
        if (spts.isEmpty())
        {
            return Collections.emptyList();
        }

        String oldName = sce.getOldSection().getName();
        String newName = sce.getNewSection().getName();
        List<Operation> res = new ArrayList<>();


        // Update SongPart name only if not customized by user
        List<SongPart> sptsToBeRenamed = getSongParts(cliSection).stream()
                .filter(spt -> spt.getName().equalsIgnoreCase(oldName))
                .toList();
        res.add(songStructure.setSongPartsNameOperation(sptsToBeRenamed, newName));


        return res;

    }

    /**
     * A section was removed.
     * <p>
     * - Remove the corresponding SongParts<br>
     * - Resize the SongParts for the previous section which got bigger<br>
     * - Possibly adjust the position of the (new) last chord symbols of the previous section (if the rhythm division of the corresponding song part has
     * changed)<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getDerivedSectionRemoved(SectionRemovedEvent evt)
    {
        var cliSection = evt.getCLI_Section();
        int bar = cliSection.getPosition().getBar();
        assert bar > 0 : "evt=" + evt;
        CLI_Section prevSection = evt.getPreviousBarSection();
        assert prevSection != null : "evt=" + evt;


        List<Operation> res = new ArrayList<>();


        // Remove the impacted SongParts
        var spts = getSongParts(cliSection);
        res.add(songStructure.removeSongPartsOperation(spts));


        // Resize the SongParts linked to previous section
        var mapSptSize = getMapSptSize(prevSection);
        res.add(songStructure.resizeSongPartsOperation(mapSptSize));


        // Update the position of the (new) ChordLeadSheetItems of prevSection  -might do nothing if no rhythm division change
        var prevSectionSpts = getSongParts(prevSection);
        if (!prevSectionSpts.isEmpty())
        {
            res.addAll(adjustChordLeadSheetItemsPosition(prevSection, prevSectionSpts.getFirst().getRhythm()));
        }

        return res;
    }

    /**
     * A section was moved.
     * <p>
     * - Resize the SongParts for the previous sections before/after the move<br>
     * - Possibly adjust chord symbols position if rhythm division has changed<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getDerivedSectionMoved(SectionMovedEvent evt)
    {
        var cliSection = evt.getCLI_Section();
        int newBarIndex = cliSection.getPosition().getBar();
        assert newBarIndex > 0 : "cliSection=" + cliSection;


        CLI_Section newBarPrevSection = chordLeadSheet.getSection(newBarIndex - 1);
        CLI_Section oldBarNewSection = chordLeadSheet.getSection(evt.getOldBar());
        List<Operation> res = new ArrayList<>();


        if (oldBarNewSection == newBarPrevSection || oldBarNewSection == cliSection)
        {
            // It's a "small move", do not cross any other section, so it's just resize operations
            var mapSptSize = getMapSptSize(cliSection);
            mapSptSize.putAll(getMapSptSize(newBarPrevSection));
            res.add(songStructure.resizeSongPartsOperation(mapSptSize));

        } else
        {
            // It's a "big move", which crosses at least another section: remove then re-add
            res.add(songStructure.removeSongPartsOperation(getSongParts(cliSection)));
            try
            {
                SongPart spt = createSptAfterSection(cliSection, newBarPrevSection);
                res.add(songStructure.addSongPartsOperation(List.of(spt)));
            } catch (UnsupportedEditException ex)
            {
                // Should never happen since we don't introduce new rhythm
                Exceptions.printStackTrace(ex);
            }

            // Resize impacted SongParts 
            var mapSptSize = getMapSptSize(oldBarNewSection);
            mapSptSize.putAll(getMapSptSize(newBarPrevSection));
            res.add(songStructure.resizeSongPartsOperation(mapSptSize));

        }

        // Possibly update the position of ChordLeadSheetItems, if a rhythm division has changed
        var spts = getSongParts(cliSection);
        if (!spts.isEmpty())
        {
            res.addAll(adjustChordLeadSheetItemsPosition(cliSection, spts.getFirst().getRhythm()));
        }
        spts = getSongParts(oldBarNewSection);
        if (!spts.isEmpty())
        {
            res.addAll(adjustChordLeadSheetItemsPosition(oldBarNewSection, spts.getFirst().getRhythm()));
        }

        return res;
    }

    /**
     * Some bars were deleted.
     * <p>
     * - Remove and possibly resize SongParts<br>
     * - Possibly adjust chord symbols position if rhythm division has changed<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getDerivedDeletedBars(DeletedBarsEvent evt)
    {
        List<Operation> res = new ArrayList<>();

        // Remove impacted SongParts
        var removedSections = evt.getItems(CLI_Section.class);
        for (var cliSection : removedSections)
        {
            var spts = getSongParts(cliSection);
            res.add(songStructure.removeSongPartsOperation(spts));
        }

        // Section before the deleted bars might have changed
        if (evt.getBarFrom() > 0)
        {
            // Resize song parts for previous section
            var mapSptSize = getMapSptSize(chordLeadSheet.getSection(evt.getBarFrom() - 1));
            songStructure.resizeSongParts(mapSptSize);
        } else if (evt.isInitSectionRemoved())
        {
            // Init section was replaced by the first shifted section: nothing to resize
        } else
        {
            // Init section was resized
            var mapSptSize = getMapSptSize(chordLeadSheet.getSection(0));
            songStructure.resizeSongParts(mapSptSize);
        }


        // Possibly adjust the position of ChordLeadSheetItems because of a rhythm division change
        if (!removedSections.isEmpty() && (chordLeadSheet.getSizeInBars() - 1) > evt.getBarFrom())
        {
            var cliSection = chordLeadSheet.getSection(evt.getBarFrom());
            var spts = getSongParts(cliSection);
            if (!spts.isEmpty())
            {
                res.addAll(adjustChordLeadSheetItemsPosition(cliSection, spts.getFirst().getRhythm()));
            }
        }

        return res;
    }

    /**
     * Some bars were inserted.
     * <p>
     * - Possibly resize SongParts<br>
     * - Create an init SongPart if insertion from bar 0<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getDerivedInsertedBars(InsertedBarsEvent evt)
    {
        List<Operation> res = new ArrayList<>();

        if (evt.getBarFrom() > 0)
        {
            // Section before the insertion is resized, resize impacted SongParts
            var cliSection = chordLeadSheet.getSection(evt.getBarFrom() - 1);
            var mapSptSize = getMapSptSize(cliSection);
            res.add(songStructure.resizeSongPartsOperation(mapSptSize));

        } else
        {
            // Insertion from bar 0, create an init section for the new initi section
            var cliSection = chordLeadSheet.getSection(0);
            var section = cliSection.getData();
            var nbBars = chordLeadSheet.getBarRange(cliSection).size();
            var spt0 = songStructure.getSongPart(0);
            SongPart newSpt;
            if (spt0 != null)
            {
                // Copy from existing
                newSpt = spt0.getCopy(null, 0, nbBars, cliSection);
            } else
            {
                // Empty SongStructure, create from scratch
                var r = songStructure.getRecommendedRhythm(section.getTimeSignature(), 0);
                newSpt = songStructure.createSongPart(r, section.getName(), 0, cliSection, true);
            }
            try
            {
                res.add(songStructure.addSongPartsOperation(List.of(newSpt)));
            } catch (UnsupportedEditException ex)
            {
                // Should never happen since we use a rhythm already in use
                LOGGER.log(Level.SEVERE, "getNextInsertedBars() Unexpected UnsupportedEditException={0}", ex.getMessage());
                Exceptions.printStackTrace(ex);
            }
        }

        return res;
    }

    /**
     * Some SongParts were added.
     * <p>
     * - if new rhythm update MidiMix<br>
     * - possibly adjust chord symbols position if rhythm division has changed<br>
     *
     * @param sae
     * @param preCheck
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    private List<Operation> getDerivedSptAdded(SptAddedEvent sae, boolean preCheck) throws UnsupportedEditException
    {
        Set<Rhythm> uniqueRhythms = getMidiMix().getUniqueRhythms();
        var spts = sae.getSongParts();


        if (preCheck)
        {
            spts.forEach(spt -> uniqueRhythms.add(spt.getRhythm()));
            checkForMidiChannelUnavailableError(uniqueRhythms);     // throws UnsupportedEditException
            return Collections.emptyList();
        }

        List<Operation> res = new ArrayList<>();


        // Possibly update MidiMix 
        for (var r : sae.getRhythms(true))
        {
            if (!uniqueRhythms.contains(r))
            {
                res.addAll(addRhythmToMidiMix(r));
            }
        }

        // Possibly update ChordLeadSheetItems position if rhythm division has changed
        res.addAll(adjustChordLeadSheetItemsPosition(spts));

        return res;
    }

    /**
     * Some SongPart were removed.
     * <p>
     * - Possibly update MidiMix if a rhythm becomes unused<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getDerivedSptRemoved(SptRemovedEvent evt)
    {
        var songRhythms = song.getSongStructure().getUniqueRhythms(true, false);


        List<Operation> res = new ArrayList<>();


        // Get rhythms from removed SongParts which are not used anymore in the song
        Set<Rhythm> rhythmsToBeRemoved = evt.getRhythms(true).stream()
                .filter(r -> !songRhythms.contains(r))
                .collect(Collectors.toSet());

        for (var r : rhythmsToBeRemoved)
        {
            res.addAll(removeRhythmFromMidiMix(r));
        }

        return res;
    }

    /**
     * The rhythm of some SongParts has changed.
     * <p>
     * - Update MidiMix if new rhythm<br>
     * - Update chord symbols position if new rhythm division has changed<br>
     *
     * @param srce
     * @param preCheck
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    private List<Operation> getDerivedSptRhythmChanged(SptRhythmChangedEvent srce, boolean preCheck) throws UnsupportedEditException
    {
        var spts = srce.getSongParts();
        var newRhythm = srce.getNewRhythm();

        if (preCheck)
        {
            if (newRhythm != null)
            {
                // Compute the remaining rhythms after the change
                Set<Rhythm> uniqueRhythms = new HashSet<>();
                uniqueRhythms.add(newRhythm);
                songStructure.getSongParts().stream()
                        .filter(spt -> !spts.contains(spt))
                        .forEach(spt -> uniqueRhythms.add(spt.getRhythm()));
                checkForMidiChannelUnavailableError(uniqueRhythms);      // throws UnsupportedEditException
            }
            return null;
        }


        List<Operation> res = new ArrayList<>();


        // Possibly update MidiMix 
        var uniqueRhythms = getMidiMix().getUniqueRhythms();
        for (var r : srce.getRhythms(true))
        {
            if (!uniqueRhythms.contains(r))
            {
                res.addAll(addRhythmToMidiMix(r));
            }
        }

        // Possibly update ChordLeadSheetItems position
        res.addAll(adjustChordLeadSheetItemsPosition(spts));


        return res;
    }


    /**
     * The cls size has changed.
     * <p>
     * - Possibly remove and resize song parts<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getDerivedClsSizeChanged(SizeChangedEvent evt)
    {
        List<Operation> res = new ArrayList<>();
        if (!evt.isGrowing())
        {
            // Size got smaller, possibly remove song parts
            List<CLI_Section> removedSections = evt.getItems(CLI_Section.class);
            List<SongPart> toBeRemovedSpts = new ArrayList<>();
            for (CLI_Section cliSection : removedSections)
            {
                toBeRemovedSpts.addAll(getSongParts(cliSection));
            }

            res.add(songStructure.removeSongPartsOperation(toBeRemovedSpts));
        }


        // Possibly resize last section
        var lastSection = getLastSection();
        var mapSptSize = getMapSptSize(lastSection);
        res.add(songStructure.resizeSongPartsOperation(mapSptSize));


        return res;
    }


    /**
     * A user phrase was removed from the song.
     * <p>
     * - Remove user channel from MidiMix.
     *
     * @param name
     * @return
     */
    private List<Operation> getDerivedRemoveUserPhrase(String name)
    {
        return List.of(getMidiMix().removeUserChannelOperation(name));
    }

    /**
     * A new user phrase was added to the song.
     * <p>
     * - Add a user channel to the MidiMix<br>
     *
     * @param name
     * @param isDrums  True if it's a new drums phrase
     * @param preCheck
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    private List<Operation> getDerivedAddUserPhrase(String name, boolean isDrums, boolean preCheck) throws UnsupportedEditException
    {
        if (preCheck)
        {
            if (getMidiMix().getUnusedChannels().isEmpty())
            {
                MidiMixManagerImpl.throwNotEnoughMidiChannelException();      //  throws UnsupportedEditException
            }
            if (getMidiMix().getUserRhythmVoice(name) != null)
            {
                MidiMixManagerImpl.throwSameNameUserChannelException(name);    //  throws UnsupportedEditException
            }
            return Collections.emptyList();
        }

        return List.of(getMidiMix().addUserChannelOperation(name, isDrums));

    }


    /**
     * A user phrase name was changed.
     * <p>
     * - Replace UserRhythmVoice in the MidiMix.
     *
     * @param oldName
     * @param newName
     * @return
     */
    private List<Operation> getDerivedRenameUserPhrase(String oldName, String newName)
    {
        UserRhythmVoice oldUrv = getMidiMix().getUserRhythmVoice(oldName);
        assert oldUrv != null : "oldName=" + oldName;
        var kit = oldUrv.getDrumKit();

        UserRhythmVoice newUrv = kit != null ? new UserRhythmVoice(newName, kit) : new UserRhythmVoice(newName);
        Operation operation = getMidiMix().replaceRhythmVoiceOperation(oldUrv, newUrv);
        return List.of(operation);
    }


    // ========================================================================================================
    // Helper methods
    // ========================================================================================================
    /**
     * Check if MidiMix can accomodate the specified rhythms (in addition to the user tracks).
     *
     * @param rhythms
     * @throws UnsupportedEditException
     */
    private void checkForMidiChannelUnavailableError(Set<Rhythm> rhythms) throws UnsupportedEditException
    {
        Objects.requireNonNull(rhythms);

        int nbVoices = getMidiMix().getUserChannels().size();      // Initialize with user rhythm voices
        for (var r : rhythms)
        {
            nbVoices += getSourceRhythm(r).getRhythmVoices().size();
        }

        if (nbVoices > MidiMix.NB_AVAILABLE_CHANNELS)
        {

        }
    }

    /**
     * Get all the songParts whose parent section is parentSection.
     *
     * @param parentSection
     * @return
     */
    private List<SongPart> getSongParts(CLI_Section parentSection)
    {
        return songStructure.getSongParts(cli -> cli.getParentSection() == parentSection);
    }

    private CLI_Section getLastSection()
    {
        return chordLeadSheet.getSection(chordLeadSheet.getSizeInBars() - 1);
    }

    /**
     * Get a SongPart-size map for all SongParts impacted by parentSection size change.
     *
     * @param resizedParentSection
     * @return
     */
    private Map<SongPart, Integer> getMapSptSize(CLI_Section resizedParentSection)
    {
        Map<SongPart, Integer> res = new HashMap<>();
        int size = chordLeadSheet.getBarRange(resizedParentSection).size();
        for (SongPart spt : getSongParts(resizedParentSection))
        {
            res.put(spt, size);
        }
        return res;
    }

    /**
     * Manage the AdaptedRhythm case.
     * <p>
     * @param r
     * @return A Rhythm instance which is not an AdaptedRhythm
     */
    private Rhythm getSourceRhythm(Rhythm r)
    {
        return (r instanceof AdaptedRhythm ar) ? ar.getSourceRhythm() : r;
    }

    /**
     * Create a SongPart for newSection and set it at the appropriate location.
     * <p>
     * If prevSection is not null:<br>
     * - if at least 1 corresponding SongPart is found, locate the new SongPart after the first series of these SongParts<br>
     * - if no SongPart is found, locate the new SongPart at the end of the SongStructure.<p>
     * If prevSection is null, locate the new SongPart at first position.
     *
     * @param newSection
     * @param prevSection The section before cliSection. Can be null.
     * @return The created SongPart, ready to be added to the SongStructure.
     */
    private SongPart createSptAfterSection(CLI_Section newSection, CLI_Section prevSection)
    {
        int sptBarIndex;
        if (prevSection == null)
        {
            sptBarIndex = 0;
        } else if (getSongParts(prevSection).isEmpty())
        {
            // Append
            sptBarIndex = songStructure.getSizeInBars();
        } else
        {
            // Locate the new SongPart after the first raw of consecutive SongParts for prevSection
            List<SongPart> prevSpts = getSongParts(prevSection); // can't be empty
            sptBarIndex = -1;
            for (int i = 0; i < prevSpts.size(); i++)
            {
                sptBarIndex = prevSpts.get(i).getStartBarIndex() + prevSpts.get(i).getNbBars();
                if (i < prevSpts.size() - 1 && prevSpts.get(i + 1).getStartBarIndex() != sptBarIndex)
                {
                    break;
                }
            }
            assert sptBarIndex != - 1 : "prevSpts=" + prevSpts + " prevSection=" + prevSection + " newSection=" + newSection;
        }
        // Choose rhythm
        Rhythm r = songStructure.getRecommendedRhythm(newSection.getData().getTimeSignature(), sptBarIndex);
        // Create the song part
        SongPart spt = songStructure.createSongPart(r, newSection.getData().getName(), sptBarIndex, newSection, true);
        return spt;
    }


    /**
     * Apply adjustChordLeadSheetItemsPosition(CLI_Section cliSection, Rhythm rhythm) for each parent section of the specified SongParts.
     *
     * @param spts Provide the parent sections to inspect
     * @return
     */
    private List<Operation> adjustChordLeadSheetItemsPosition(List<SongPart> spts)
    {
        List<Operation> res = new ArrayList<>();

        Set<CLI_Section> processedSections = new HashSet<>();
        for (var spt : spts)
        {
            var cliSection = spt.getParentSection();
            if (processedSections.contains(cliSection))
            {
                continue;
            }
            processedSections.add(cliSection);
            res.addAll(adjustChordLeadSheetItemsPosition(cliSection, spt.getRhythm()));
        }

        return res;
    }

    /**
     * Adjust position of ChordLeadSheetItems which do not match the reference rhythm division.
     * <p>
     * The reference rhythm is the rhythm of the first SongPart whose cliSection is the parent section.
     *
     * @param cliSection Adjust items of this section
     * @param rhythm
     * @return
     */
    private List<Operation> adjustChordLeadSheetItemsPosition(CLI_Section cliSection, Rhythm rhythm)
    {
        List<Operation> res = new ArrayList<>();

        var division = getSourceRhythm(rhythm).getFeatures().division();

        // Binary <> ternary change 
        Quantization q = switch (division)
        {
            case EIGHTH_SHUFFLE, EIGHTH_TRIPLET ->
                Quantization.ONE_THIRD_BEAT;
            default ->
                Quantization.ONE_QUARTER_BEAT;
        };


        // Update off-beat chord symbols position
        for (var cliCs : chordLeadSheet.getItems(cliSection, CLI_ChordSymbol.class))
        {
            var pos = cliCs.getPosition();
            if (!pos.isOffBeat())
            {
                continue;
            }
            var newPos = Quantizer.getQuantized(q, pos, cliSection.getData().getTimeSignature(), 1, pos.getBar());
            if (!newPos.equals(pos))
            {
                res.add(chordLeadSheet.moveItemOperation(cliCs, newPos));
            }
        }

        return res;
    }

    private MidiMixImpl getMidiMix()
    {
        if (midiMix == null)
        {
            try
            {
                midiMix = (MidiMixImpl) MidiMixManager.getDefault().findMix(song);
            } catch (UnsupportedEditException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
            }
        }
        return midiMix;
    }

    /**
     * Remove r from the MidiMix.
     *
     * @param r
     * @return
     */
    private List<Operation> removeRhythmFromMidiMix(Rhythm r)
    {
        var rvs = getMidiMix().getRhythmVoices();

        List<Operation> res = rvs.stream()
                .filter(rv -> rv.getContainer() == r)
                .map(rv -> (Operation) getMidiMix().setInstrumentMixOperation(getMidiMix().getChannel(rv), null, null))
                .toList();
        return res;
    }


    /**
     * Add a rhythm to the MidiMix.
     * <p>
     * Relies on adaptInstrumentMixes() to harmonize InstrumentSettings if there is already a rhythm in the song.
     *
     * @param newRhythm
     * @return
     * @see #adaptInstrumentMixes(org.jjazz.midimix.api.MidiMix, org.jjazz.midimix.api.MidiMix, org.jjazz.rhythm.api.Rhythm)
     */
    private List<Operation> addRhythmToMidiMix(Rhythm newRhythm)
    {
        Preconditions.checkArgument(newRhythm != null && !(newRhythm instanceof AdaptedRhythm), "r=%s", newRhythm);

        LOGGER.log(Level.FINE, "addRhythm() r={0}", new Object[]
        {
            newRhythm
        });

        List<Operation> res = new ArrayList();


        MidiMix mmNewRhythm = MidiMixManager.getDefault().findMix(newRhythm);
        if (!getMidiMix().getUniqueRhythms().isEmpty())
        {
            // Adapt mm to sound like the InstrumentMixes of r0           
            Rhythm r0 = getMidiMix().getUniqueRhythms().iterator().next();
            adaptInstrumentMixes(mmNewRhythm, getMidiMix(), r0);
        }

        List<Integer> usedChannelsNewRhythm = mmNewRhythm.getUsedChannels();
        if (getMidiMix().getUnusedChannels().size() < usedChannelsNewRhythm.size())
        {
            // Problem should have been caught earlier
            throw new IllegalStateException("getMidiMix()=" + getMidiMix() + " midiMixRhythm=" + mmNewRhythm);
        }

        // addInstrumentMixes(getMidiMix(), midiMixRhythm, r);

        for (Integer channelNewRhythm : usedChannelsNewRhythm)
        {
            RhythmVoice rvNewRhythm = mmNewRhythm.getRhythmVoice(channelNewRhythm);
            if (!(rvNewRhythm instanceof UserRhythmVoice))
            {
                int channelDest = getMidiMix().getUsedChannels().contains(channelNewRhythm) ? getMidiMix().findFreeChannel(rvNewRhythm.isDrums())
                        : channelNewRhythm;
                assert channelDest != -1;
                InstrumentMix insMixSrc = mmNewRhythm.getInstrumentMix(channelNewRhythm);
                res.add(getMidiMix().setInstrumentMixOperation(channelDest, rvNewRhythm, new InstrumentMix(insMixSrc)));
            }
        }

        return res;
    }


    /**
     * Adapt the InstrumentMixes of midiMixDest to "sound" like the InstrumentMixes of rSrc in midiMixSrc.
     *
     * @param midiMixDest
     * @param midiMixSrc
     * @param rSrc
     */
    private void adaptInstrumentMixes(MidiMix midiMixDest, MidiMix midiMixSrc, Rhythm rSrc)
    {
        LOGGER.log(Level.FINE, "adaptInstrumentMixes() midiMixDest={0} midiMixSrc={1} rSrc={2}", new Object[]
        {
            midiMixDest, midiMixSrc, rSrc
        });

        Map<String, InstrumentMix> mapKeyMix = new HashMap<>();
        Map<InstrumentFamily, InstrumentMix> mapFamilyMix = new HashMap<>();
        InstrumentMix rSrcInsMixDrums = null;
        InstrumentMix rSrcInsMixPerc = null;

        // First try to match InstrumentMixes using "key" = "3 first char of Rv.getName() + GM1 family"
        for (int channelSrc : midiMixSrc.getUsedChannels(rSrc))
        {
            // Build the keys from rSrc
            RhythmVoice rvSrc = midiMixSrc.getRhythmVoice(channelSrc);
            InstrumentMix insMixSrc = midiMixSrc.getInstrumentMix(channelSrc);
            if (rvSrc.isDrums())
            {
                // Special case, use the 2 special variables for Drums or Percussion                
                if (midiMixSrc.getDrumsReroutedChannels().contains(channelSrc))
                {
                    // If channel is rerouted, re-enable the disabled parameters
                    insMixSrc = new InstrumentMix(insMixSrc);
                    insMixSrc.setInstrumentEnabled(true);
                    insMixSrc.getSettings().setChorusEnabled(true);
                    insMixSrc.getSettings().setReverbEnabled(true);
                    insMixSrc.getSettings().setPanoramicEnabled(true);
                    insMixSrc.getSettings().setVolumeEnabled(true);
                }
                if (rvSrc.getType().equals(RhythmVoice.Type.DRUMS))
                {
                    rSrcInsMixDrums = insMixSrc;
                } else
                {
                    rSrcInsMixPerc = insMixSrc;
                }

            } else
            {
                GM1Instrument insGM1 = insMixSrc.getInstrument().getSubstitute();  // Might be null            
                InstrumentFamily family = insGM1 != null ? insGM1.getFamily() : null;
                String mapKey = Utilities.truncate(rvSrc.getName().toLowerCase(), 3) + "-" + ((family != null) ? family.name() : "");
                if (mapKeyMix.get(mapKey) == null)
                {
                    mapKeyMix.put(mapKey, insMixSrc);  // If several instruments have the same Type, save only the first one
                }
                if (family != null && mapFamilyMix.get(family) == null)
                {
                    mapFamilyMix.put(family, insMixSrc);       // If several instruments have the same family, save only the first one
                }
            }
        }

        // Try to convert using the keys
        HashSet<Integer> doneChannels = new HashSet<>();
        for (int channelDest : midiMixDest.getUsedChannels())
        {
            RhythmVoice rvDesr = midiMixDest.getRhythmVoice(channelDest);
            InstrumentMix insMixDest = midiMixDest.getInstrumentMix(channelDest);
            InstrumentMix insMix;

            switch (rvDesr.getType())
            {
                case DRUMS ->
                    insMix = rSrcInsMixDrums;
                case PERCUSSION ->
                    insMix = rSrcInsMixPerc;
                default ->
                {
                    GM1Instrument mmInsGM1 = insMixDest.getInstrument().getSubstitute();  // Can be null            
                    InstrumentFamily mmFamily = mmInsGM1 != null ? mmInsGM1.getFamily() : null;
                    String mapKey = Utilities.truncate(rvDesr.getName().toLowerCase(), 3) + "-" + ((mmFamily != null)
                            ? mmFamily.name() : "");
                    insMix = mapKeyMix.get(mapKey);
                }

            }

            if (insMix != null)
            {
                // Copy InstrumentMix data
                insMixDest.setInstrument(insMix.getInstrument());
                insMixDest.getSettings().set(insMix.getSettings());
                doneChannels.add(channelDest);
                LOGGER.log(Level.FINER, "adaptInstrumentMixes() set (1) channel {0} instrument setting to : {1}", new Object[]
                {
                    channelDest,
                    insMix.getSettings()
                });
            }

        }

        // Try to convert also the other channels by matching only the instrument family
        for (int channelDest : midiMixDest.getUsedChannels())
        {
            if (doneChannels.contains(channelDest))
            {
                continue;
            }
            InstrumentMix insMixDest = midiMixDest.getInstrumentMix(channelDest);
            GM1Instrument insGM1Dest = insMixDest.getInstrument().getSubstitute();  // Can be null          
            if (insGM1Dest == null || insGM1Dest == GMSynth.getInstance().getVoidInstrument())
            {
                continue;
            }
            InstrumentFamily mmFamily = insGM1Dest.getFamily();
            InstrumentMix insMix = mapFamilyMix.get(mmFamily);
            if (insMix != null)
            {
                // Copy InstrumentMix data
                insMixDest.setInstrument(insMix.getInstrument());
                insMixDest.getSettings().set(insMix.getSettings());
                LOGGER.log(Level.FINER, "adaptInstrumentMixes() set (2) channel {0} instrument setting to : {1}", new Object[]
                {
                    channelDest,
                    insMix.getSettings()
                });
            }
        }
    }


}

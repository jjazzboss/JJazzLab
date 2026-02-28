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
                    preCheckAddUserPhrase(name);
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
                    preCheckSectionReplaced(e.getSameBarReplacedSection(), e.getCLI_Section());
                } else
                {
                    preCheckSectionAdded(e);
                }
            }
            case SectionChangedEvent e ->
            {
                if (e.isTimeSignatureChanged())
                {
                    preCheckSectionTimeSignatureChanged(e);
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
                preCheckSptRhythmChanged(srce);
            }
            case SptAddedEvent sae ->
            {
                preCheckSptAdded(sae);
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
            return getClsDerivedOperations(results.clsChangeEvent());

        } else if (results.sgsChangeEvent() != null)
        {
            return getSgsDerivedOperations(results.sgsChangeEvent());

        } else
        {
            var event = results.pChangeEvent();
            if (event.getSource() == getMidiMix())
            {
                return getMidiMixDerivedOperations(results.pChangeEvent());
            } else if (event.getSource() == song)
            {
                return getSongDerivedOperations(results.pChangeEvent());
            } else
            {
                throw new IllegalStateException("event=" + event);
            }
        }
    }

    // =============================================================================================================
    // Private methods
    // =============================================================================================================

    private List<Operation> getClsDerivedOperations(ClsChangeEvent srcEvent)
    {
        if (srcEvent.isUndoOrRedo())
        {
            // IMPORTANT : Each Song component generates his own undoableEdits,
            // so we must not forward changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice
            return Collections.emptyList();
        }

        List<Operation> res = null;
        switch (srcEvent)
        {
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
                    res = getDerivedSectionReplaced(e.getSameBarReplacedSection(), e.getCLI_Section());
                } else
                {
                    res = getDerivedSectionAdded(e);
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
                        res = getDerivedSectionTimeSignatureChanged(e);
                    } else
                    {
                        res.addAll(getDerivedSectionTimeSignatureChanged(e));
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

        return res;
    }

    private List<Operation> getSgsDerivedOperations(SgsChangeEvent srcEvent)
    {

        if (srcEvent.isUndoOrRedo())
        {
            // IMPORTANT : Each Song component generates his own undoableEdits,
            // so we must not forward changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice            
            return Collections.emptyList();
        }


        List<Operation> res;
        switch (srcEvent)
        {
            case SptAddedEvent e ->
            {
                res = getDerivedSptAdded(e);
            }
            case SptRemovedEvent e ->
            {
                res = getDerivedSptRemoved(e);
            }
            case SptRhythmChangedEvent e ->
            {
                res = getDerivedSptRhythmChanged(e);
            }
            default ->
            {
                res = Collections.emptyList();
            }
        }

        return res;
    }

    private List<Operation> getMidiMixDerivedOperations(SongPropertyChangeEvent srcEvent)
    {
        return Collections.emptyList();
    }

    private List<Operation> getSongDerivedOperations(SongPropertyChangeEvent srcEvent)
    {

        if (srcEvent.isUndoOrRedo())
        {
            // IMPORTANT : Each Song component generates his own undoableEdits,
            // so we must not forward changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice            
            return Collections.emptyList();
        }


        List<Operation> res;
        switch (srcEvent.getPropertyName())
        {
            case Song.PROP_USER_PHRASE ->
            {
                if (srcEvent.getNewValue() instanceof String name)
                {
                    // Added phrase
                    Phrase p = (Phrase) srcEvent.getOldValue();
                    res = getDerivedAddUserPhrase(name, p.isDrums());
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

        return res;

    }

    /**
     * Pre-check if adding a section to an empty bar is authorized.
     *
     * @param evt
     * @throws UnsupportedEditException If not enough MIDI channels
     */
    private void preCheckSectionAdded(SectionAddedEvent evt) throws UnsupportedEditException
    {
        var cliSection = evt.getCLI_Section();
        CLI_Section prevSection = evt.getPreviousBarSection();
        assert cliSection.getPosition().getBar() > 0 : "evt=" + evt;
        assert prevSection != null : "evt=" + evt;

        var spts = songStructure.getSongParts();
        if (!spts.isEmpty())
        {
            // The rhythm which should be used for the inserted SongPart
            var sptBarIndex = getSptInsertionBar(prevSection, cliSection);
            var newRhythm = songStructure.getRecommendedRhythm(cliSection.getData().getTimeSignature(), sptBarIndex);

            // Collect all the unique rhythms
            Set<Rhythm> uniqueRhythms = new HashSet<>();
            uniqueRhythms.add(newRhythm);
            uniqueRhythms.addAll(songStructure.getUniqueRhythms(true, false));
            checkMidiMixForMidiChannelUnavailableError(uniqueRhythms);   // throws UnsupportedEditException
        }
    }

    /**
     * A section was added to an empty bar.
     * <p>
     * - Insert a new SongPart for the added section<br>
     * - Reduce the size of the SongPart(s) whose parent section is the previous shortened section<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getDerivedSectionAdded(SectionAddedEvent evt)
    {
        var cliSection = evt.getCLI_Section();
        CLI_Section prevSection = evt.getPreviousBarSection();
        assert cliSection.getPosition().getBar() > 0 : "evt=" + evt;
        assert prevSection != null : "evt=" + evt;

        List<Operation> res = new ArrayList<>();

        // Add the new SongPart
        SongPart spt = createSptAfterSection(cliSection, prevSection);
        res.add(songStructure.addSongPartsOperation(List.of(spt)));

        // Resize of SongParts linked to previous section
        var mapSptSize = getMapSptSize(prevSection);
        res.add(songStructure.resizeSongPartsOperation(mapSptSize));

        return res;
    }


    /**
     * Pre-check if replacing a section at the same bar is authorized.
     *
     * @param oldSection
     * @param newSection
     * @throws UnsupportedEditException If not enough MIDI channels
     */
    private void preCheckSectionReplaced(CLI_Section oldSection, CLI_Section newSection) throws UnsupportedEditException
    {
        var oldSpts = getSongParts(oldSection);
        if (oldSpts.isEmpty())
        {
            return;
        }

        var oldTs = oldSection.getData().getTimeSignature();
        var newTs = newSection.getData().getTimeSignature();
        Rhythm newRhythm = newTs.equals(oldTs) ? null : songStructure.getRecommendedRhythm(newTs, oldSpts.get(0).getStartBarIndex());

        if (newRhythm != null)
        {
            Set<Rhythm> uniqueRhythms = new HashSet<>();
            uniqueRhythms.add(newRhythm);
            uniqueRhythms.addAll(songStructure.getUniqueRhythms(true, false));
            checkMidiMixForMidiChannelUnavailableError(uniqueRhythms);   // throws UnsupportedEditException
        }
    }

    /**
     * A section was replaced by a new one (same bar).
     * <p>
     * - Update the parent section and possibly rename the impacted SongParts<br>
     *
     * @param oldSection
     * @param newSection
     * @return
     */
    private List<Operation> getDerivedSectionReplaced(CLI_Section oldSection, CLI_Section newSection)
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
     * Pre-check if a section time signature change is authorized.
     *
     * @param sce
     * @throws UnsupportedEditException If not enough MIDI channels
     */
    private void preCheckSectionTimeSignatureChanged(SectionChangedEvent sce) throws UnsupportedEditException
    {
        assert sce.isTimeSignatureChanged() : "sce=" + sce;

        var cliSection = sce.getCLI_Section();
        var spts = getSongParts(cliSection);
        if (spts.isEmpty())
        {
            return;
        }

        var newTs = sce.getNewSection().getTimeSignature();
        Rhythm newRhythm = songStructure.getRecommendedRhythm(newTs, spts.getFirst().getStartBarIndex());
        var event = new SptRhythmChangedEvent(songStructure, newRhythm, new HashMap<>(), spts);
        preCheckSptRhythmChanged(event);               // throws UnsupportedEditException
    }

    /**
     * A section time signature was changed.
     * <p>
     * - Update impacted SongParts rhythm<br>
     *
     * @param sce
     * @return
     */
    private List<Operation> getDerivedSectionTimeSignatureChanged(SectionChangedEvent sce)
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
        List<SongPart> sptsToBeRenamed = spts.stream()
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
        Rhythm cliSectionRhythm = null;


        if (oldBarNewSection == newBarPrevSection || oldBarNewSection == cliSection)
        {
            // It's a "small move", do not cross any other section, so it's just resize operations
            var mapSptSize = getMapSptSize(cliSection);
            mapSptSize.putAll(getMapSptSize(newBarPrevSection));
            res.add(songStructure.resizeSongPartsOperation(mapSptSize));

            var cliSectionSpts = getSongParts(cliSection);
            if (!cliSectionSpts.isEmpty())
            {
                cliSectionRhythm = cliSectionSpts.getFirst().getRhythm();
            }

        } else
        {
            // It's a "big move", which crosses at least another section: add new SongPart and remove the old ones
            SongPart spt = createSptAfterSection(cliSection, newBarPrevSection);    // Must be calculated before any change occurs
            res.add(songStructure.addSongPartsOperation(List.of(spt)));
            res.add(songStructure.removeSongPartsOperation(getSongParts(cliSection)));

            // Resize impacted SongParts 
            var mapSptSize = getMapSptSize(oldBarNewSection);
            mapSptSize.putAll(getMapSptSize(newBarPrevSection));
            res.add(songStructure.resizeSongPartsOperation(mapSptSize));

            // Use the new SongPart's rhythm, not the removed old ones
            cliSectionRhythm = spt.getRhythm();
        }

        // Possibly update the position of ChordLeadSheetItems, if a rhythm division has changed
        if (cliSectionRhythm != null)
        {
            res.addAll(adjustChordLeadSheetItemsPosition(cliSection, cliSectionRhythm));
        }
        var spts = getSongParts(oldBarNewSection);
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
            res.add(songStructure.resizeSongPartsOperation(mapSptSize));
        } else if (evt.isInitSectionRemoved())
        {
            // Init section was replaced by the first shifted section: nothing to resize
        } else
        {
            // Init section was resized
            var mapSptSize = getMapSptSize(chordLeadSheet.getSection(0));
            res.add(songStructure.resizeSongPartsOperation(mapSptSize));
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
            // Insertion from bar 0, create an init section for the new init section
            var newInitSection = evt.getNewInitSection();
            var section = newInitSection.getData();
            var nbBars = chordLeadSheet.getBarRange(newInitSection).size();
            var spt0 = songStructure.getSongPart(0);
            SongPart newSpt;
            if (spt0 != null)
            {
                // Copy from existing
                newSpt = spt0.getCopy(null, 0, nbBars, newInitSection);
            } else
            {
                // Empty SongStructure, create from scratch
                var r = songStructure.getRecommendedRhythm(section.getTimeSignature(), 0);
                newSpt = songStructure.createSongPart(r, section.getName(), 0, newInitSection, true);
            }
            res.add(songStructure.addSongPartsOperation(List.of(newSpt)));
        }

        return res;
    }

    /**
     * Pre-check if adding some SongParts is authorized.
     *
     * @param sae
     * @throws UnsupportedEditException If not enough MIDI channels
     */
    private void preCheckSptAdded(SptAddedEvent sae) throws UnsupportedEditException
    {
        Set<Rhythm> uniqueRhythms = getMidiMix().getUniqueRhythms();
        sae.getSongParts().forEach(spt -> uniqueRhythms.add(getSourceRhythm(spt.getRhythm())));
        checkMidiMixForMidiChannelUnavailableError(uniqueRhythms);     // throws UnsupportedEditException
    }

    /**
     * Some SongParts were added.
     * <p>
     * - if new rhythm update MidiMix<br>
     * - possibly adjust chord symbols position if rhythm division has changed<br>
     *
     * @param sae
     * @return
     */
    private List<Operation> getDerivedSptAdded(SptAddedEvent sae)
    {
        Set<Rhythm> uniqueRhythms = getMidiMix().getUniqueRhythms();
        var spts = sae.getSongParts();

        List<Operation> res = new ArrayList<>();

        // Possibly update MidiMix
        for (var r : sae.getRhythms(true))
        {
            if (!uniqueRhythms.contains(r))
            {
                res.add(getMidiMix().addRhythmOperation(r));
            }
        }

        // Possibly update ChordLeadSheetItems position if rhythm division has changed
        res.addAll(adjustChordLeadSheetItemsPosition(spts));

        return res;
    }

    /**
     * Some SongParts were removed.
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

        Set<Rhythm> rhythmsToBeRemoved = evt.getRhythms(true).stream()
                .filter(r -> !songRhythms.contains(r))
                .collect(Collectors.toSet());

        rhythmsToBeRemoved.forEach(r -> res.add(getMidiMix().removeRhythmOperation(r)));

        return res;
    }

    /**
     * Pre-check if changing the rhythm of some SongParts is authorized.
     *
     * @param srce
     * @throws UnsupportedEditException If not enough MIDI channels
     */
    private void preCheckSptRhythmChanged(SptRhythmChangedEvent srce) throws UnsupportedEditException
    {
        var spts = srce.getSongParts();
        var newRhythm = srce.getNewRhythm();

        if (newRhythm != null)
        {
            // Compute the remaining rhythms after the change
            Set<Rhythm> uniqueRhythms = new HashSet<>();
            uniqueRhythms.add(newRhythm);
            songStructure.getSongParts().stream()
                    .filter(spt -> !spts.contains(spt))
                    .forEach(spt -> uniqueRhythms.add(getSourceRhythm(spt.getRhythm())));
            checkMidiMixForMidiChannelUnavailableError(uniqueRhythms);      // throws UnsupportedEditException
        }
    }

    /**
     * The rhythm of some SongParts has changed.
     * <p>
     * - Update MidiMix: possibly remove some rhythms and possibly add new rhythm<br>
     * - Update chord symbols position if new rhythm division has changed<br>
     *
     * @param srce
     * @return
     */
    private List<Operation> getDerivedSptRhythmChanged(SptRhythmChangedEvent srce)
    {
        var spts = srce.getSongParts();
        var newRhythm = srce.getNewRhythm();

        List<Operation> res = new ArrayList<>();

        // Remove unused rhythms from MidiMix
        var midiMixRhythms = getMidiMix().getUniqueRhythms();
        var songRhythms = songStructure.getUniqueRhythms(true, false);
        var toBeRemovedRhythms = midiMixRhythms.stream()
                .filter(r -> !songRhythms.contains(r))
                .toList();
        toBeRemovedRhythms.forEach(r -> res.add(getMidiMix().removeRhythmOperation(r)));

        // Add new rhythm if required
        if (newRhythm != null && !midiMixRhythms.contains(newRhythm))
        {
            res.add(getMidiMix().addRhythmOperation(newRhythm));
        }

        // Possibly update ChordLeadSheetItems position
        res.addAll(adjustChordLeadSheetItemsPosition(spts));

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
     * Pre-check if adding a new user phrase is authorized.
     *
     * @param name The user phrase name
     * @throws UnsupportedEditException If no MIDI channel is available or name is already used
     */
    private void preCheckAddUserPhrase(String name) throws UnsupportedEditException
    {
        if (getMidiMix().getUnusedChannels().isEmpty())
        {
            MidiMixImpl.throwNotEnoughMidiChannelException();      //  throws UnsupportedEditException
        }
        if (getMidiMix().getUserRhythmVoice(name) != null)
        {
            MidiMixImpl.throwSameNameUserChannelException(name);    //  throws UnsupportedEditException
        }
    }

    /**
     * A new user phrase was added to the song.
     * <p>
     * - Add a user channel to the MidiMix<br>
     *
     * @param name
     * @param isDrums True if it's a new drums phrase
     * @return
     */
    private List<Operation> getDerivedAddUserPhrase(String name, boolean isDrums)
    {
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
     * Check if MidiMix can accomodate the specified rhythms in addition to the user tracks.
     *
     * @param rhythms
     * @throws UnsupportedEditException
     */
    private void checkMidiMixForMidiChannelUnavailableError(Set<Rhythm> rhythms) throws UnsupportedEditException
    {
        Objects.requireNonNull(rhythms);

        int nbVoices = getMidiMix().getUserChannels().size();      // Initialize with user rhythm voices
        for (var r : rhythms)
        {
            nbVoices += getSourceRhythm(r).getRhythmVoices().size();
        }

        if (nbVoices > MidiMix.NB_AVAILABLE_CHANNELS)
        {
            MidiMixImpl.throwNotEnoughMidiChannelException();
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
        int sptBarIndex = getSptInsertionBar(prevSection, newSection);
        Rhythm r = songStructure.getRecommendedRhythm(newSection.getData().getTimeSignature(), sptBarIndex);
        SongPart spt = songStructure.createSongPart(r, newSection.getData().getName(), sptBarIndex, newSection, true);
        return spt;
    }

    /**
     * Compute where to insert a new SongPart for newSection.
     *
     * @param newSection
     * @param prevSection The section before cliSection. Can be null.
     * @return
     */
    private int getSptInsertionBar(CLI_Section prevSection, CLI_Section newSection)
    {
        int res;
        if (prevSection == null)
        {
            res = 0;
        } else if (getSongParts(prevSection).isEmpty())
        {
            // Append
            res = songStructure.getSizeInBars();
        } else
        {
            // Locate the new SongPart after the first raw of consecutive SongParts for prevSection
            List<SongPart> prevSpts = getSongParts(prevSection); // can't be empty
            res = -1;
            for (int i = 0; i < prevSpts.size(); i++)
            {
                res = prevSpts.get(i).getStartBarIndex() + prevSpts.get(i).getNbBars();
                if (i < prevSpts.size() - 1 && prevSpts.get(i + 1).getStartBarIndex() != res)
                {
                    break;
                }
            }
            assert res != - 1 : "prevSpts=" + prevSpts + " prevSection=" + prevSection + " newSection=" + newSection;
        }
        return res;
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


}

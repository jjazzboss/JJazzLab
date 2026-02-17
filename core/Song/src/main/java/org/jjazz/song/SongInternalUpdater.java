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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.jjazz.midimix.api.MidiMix;
import static org.jjazz.midimix.api.MidiMix.NB_AVAILABLE_CHANNELS;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.rhythm.api.AdaptedRhythm;
import static org.jjazz.rhythm.api.Division.EIGHTH_SHUFFLE;
import static org.jjazz.rhythm.api.Division.EIGHTH_TRIPLET;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.SongStructureImpl;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRhythmChangedEvent;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.Exceptions;

/**
 * Manage the impact of ChordLeadSheet or SongStructure changes to other Song internal components.
 * <p>
 * For example, a ChordLeadSheet time signature change might require an update of a SongPart rhythm, which will impact the MidiMix, and possibly some chord
 * symbols position back in the ChordLeadSheet if rhythm division has changed.
 * <p>
 * SongInternalUpdater also allows to pre-check an operation: e.g. a time signature change could be vetoed by SongStructure/MidiMix because the new rhythm would
 * need too many Midi channels.
 */
class SongInternalUpdater
{

    private final Song song;
    private final SongStructureImpl songStructure;
    private final ChordLeadSheetImpl chordLeadSheet;

    public SongInternalUpdater(Song sg)
    {
        this.song = sg;
        this.songStructure = (SongStructureImpl) sg.getSongStructure();
        this.chordLeadSheet = (ChordLeadSheetImpl) sg.getChordLeadSheet();
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
                    getNextSectionReplaced(e.getSameBarReplacedSection(), e.getCLI_Section(), true);      // throws UnsupportedEditException
                } else
                {
                    getNextSectionAdded(e, true);        // throws UnsupportedEditException
                }
            }
            case SectionChangedEvent e ->
            {
                if (e.isTimeSignatureChanged())
                {
                    getNextSectionTimeSignatureChanged(e, true);   // throws UnsupportedEditException
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
                getNextSptRhythmChanged(srce, true);
            }
            case SptAddedEvent sae ->
            {
                getNextSptAdded(sae, true);
            }
            default ->
            {
                // Nothing
            }
        }
    }

    /**
     * Provide the WriteOperations required on other Song components following the source change event.
     * <p>
     *
     * @param results
     * @return Can be empty.
     */
    public List<Operation> getNextOperations(WriteOperationResults results)
    {
        Objects.requireNonNull(results);
        if (results.clsChangeEvent() == null && results.sgsChangeEvent() == null)
        {
            return Collections.emptyList();
        }
        if (results.clsChangeEvent() != null)
        {
            return getNextOperations(results.clsChangeEvent());
        } else
        {
            return getNextOperations(results.sgsChangeEvent());
        }
    }

    // =============================================================================================================
    // Private methods
    // =============================================================================================================

    private List<Operation> getNextOperations(ClsChangeEvent srcEvent)
    {
        List<Operation> res = null;
        if (srcEvent.isUndoOrRedo())
        {
            return Collections.emptyList();
        }
        try
        {
            switch (srcEvent)
            {
                case SizeChangedEvent e ->
                {
                    res = getNextClsSizeChanged(e);
                }
                case DeletedBarsEvent e ->
                {
                    res = getNextDeletedBars(e);
                }
                case InsertedBarsEvent e ->
                {
                    res = getNextInsertedBars(e);
                }
                case SectionAddedEvent e ->
                {
                    if (e.getSameBarReplacedSection() != null)
                    {
                        // Section replaces another one at same bar
                        res = getNextSectionReplaced(e.getSameBarReplacedSection(), e.getCLI_Section(), false);
                    } else
                    {
                        res = getNextSectionAdded(e, false);
                    }
                }
                case SectionChangedEvent e ->
                {
                    if (e.isNameChanged())
                    {
                        res = getNextSectionNameChanged(e);
                    }
                    if (e.isTimeSignatureChanged())
                    {
                        if (res == null)
                        {
                            res = getNextSectionTimeSignatureChanged(e, false);
                        } else
                        {
                            res.addAll(getNextSectionTimeSignatureChanged(e, false));
                        }
                    }
                }
                case SectionRemovedEvent e ->
                {
                    res = getNextSectionRemoved(e);
                }
                case SectionMovedEvent e ->
                {
                    res = getNextSectionMoved(e);
                }
                default ->
                {
                    res = new ArrayList<>();
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

    private List<Operation> getNextOperations(SgsChangeEvent srcEvent)
    {
        List<Operation> res;
        if (srcEvent.isUndoOrRedo())
        {
            return Collections.emptyList();
        }
        try
        {
            switch (srcEvent)
            {
                case SptAddedEvent sae ->
                {
                    res = getNextSptAdded(sae, false);
                }
                case SptRhythmChangedEvent srce ->
                {
                    res = getNextSptRhythmChanged(srce, false);
                }
                default ->
                {
                    res = new ArrayList<>();
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
     * Section added to an empty bar.
     * <p>
     * - Create the corresponding SongPart with possibly a new rhythm<br>
     * - Adjust the size of SongParts using previous section<br>
     *
     * @param evt
     * @param preCheck
     * @return
     * @throws UnsupportedEditException
     */
    private List<Operation> getNextSectionAdded(SectionAddedEvent evt, boolean preCheck) throws UnsupportedEditException
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
            getNextSptAdded(event, true);       // Only risk is a new rhythm with not enough available MIDI channels
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
     * A section was replaced by a new one in the same bar.
     * <p>
     * - Update parent section, name and rhythm of impacted SongParts<br>
     *
     * @param oldSection
     * @param newSection
     * @param preCheck
     * @return
     * @throws UnsupportedEditException
     */
    private List<Operation> getNextSectionReplaced(CLI_Section oldSection, CLI_Section newSection, boolean preCheck) throws UnsupportedEditException
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
                getNextSptRhythmChanged(event, true);               // throws UnsupportedEditException
            }
            return null;
        }


        // Update SongParts rhyhtm and parentSection
        List<Operation> res = new ArrayList<>();
        res.add(songStructure.setSongPartsRhythmOperation(oldSpts, newRhythm, newSection));


        // Update SongParts name
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
    private List<Operation> getNextSectionTimeSignatureChanged(SectionChangedEvent sce, boolean preCheck) throws UnsupportedEditException
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
            getNextSptRhythmChanged(event, true);               // throws UnsupportedEditException
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
     * - Update impacted SongParts name if relevant<br>
     *
     * @param sce
     * @return
     */
    private List<Operation> getNextSectionNameChanged(SectionChangedEvent sce)
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


        // Update name of an impacted SongPart only if not customized by user
        List<SongPart> sptsToBeRenamed = getSongParts(cliSection).stream()
                .filter(spt -> spt.getName().equalsIgnoreCase(oldName))
                .toList();
        res.add(songStructure.setSongPartsNameOperation(sptsToBeRenamed, newName));


        return res;

    }

    /**
     * Section was removed.
     * <p>
     * - Remove the corresponding SongPart<br>
     * - Resize the SongParts for the previous section<br>
     * - Possibly adjust chord symbols position if rhythm division has changed<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getNextSectionRemoved(SectionRemovedEvent evt)
    {
        var cliSection = evt.getCLI_Section();
        int bar = cliSection.getPosition().getBar();
        assert bar > 0 : "evt=" + evt;
        CLI_Section prevSection = evt.getPreviousBarSection();
        assert prevSection != null : "evt=" + evt;


        List<Operation> res = new ArrayList<>();


        // Remove the new SongPart
        var spts = getSongParts(cliSection);
        res.add(songStructure.removeSongPartsOperation(spts));


        // Resize of SongParts linked to previous section
        var mapSptSize = getMapSptSize(prevSection);
        res.add(songStructure.resizeSongPartsOperation(mapSptSize));


        // Possibly update ChordLeadSheetItems position of prevSection
        var prevSectionSpts = getSongParts(prevSection);
        if (!prevSectionSpts.isEmpty())
        {
            res.addAll(adjustChordLeadSheetItemsPosition(prevSection, prevSectionSpts.getFirst().getRhythm()));
        }

        return res;
    }

    /**
     * Section was moved.
     * <p>
     * - Resize the impacted SongParts<br>
     * - Possibly adjust chord symbols position if rhythm division has changed<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getNextSectionMoved(SectionMovedEvent evt)
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

        // Possibly update ChordLeadSheetItems position of impacted sections
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
     * - Possibly remove and resize SongParts<br>
     * - Possibly adjust chord symbols position if rhythm division has changed<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getNextDeletedBars(DeletedBarsEvent evt)
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


        // Possibly update ChordLeadSheetItems position of the impacted section
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
     * Bars were inserted.
     * <p>
     * - Possibly resize SongParts<br>
     *
     * @param evt
     * @return
     */
    private List<Operation> getNextInsertedBars(InsertedBarsEvent evt)
    {
        List<Operation> res = new ArrayList<>();

        // Section before the insertion is resized
        Map<SongPart, Integer> mapSptSize = new HashMap<>();
        if (evt.getBarFrom() > 0)
        {
            var cliSection = chordLeadSheet.getSection(evt.getBarFrom() - 1);
            fillMapSptSize(mapSptSize, cliSection);
            songStructure.resizeSongParts(mapSptSize);
        } else
        {
            // Insertion from init bar, a new init section was created
            var cliSection = chordLeadSheet.getSection(0);
            var section = cliSection.getData();
            var nbBars = chordLeadSheet.getBarRange(cliSection).size();
            var spt = songStructure.getSongPart(0);
            SongPart newSpt;
            if (spt != null)
            {
                newSpt = spt.getCopy(null, 0, nbBars, cliSection);
            } else
            {
                var r = songStructure.getRecommendedRhythm(section.getTimeSignature(), 0);
                newSpt = songStructure.createSongPart(r, section.getName(), 0, cliSection, true);
            }
            try
            {
                songStructure.addSongParts(List.of(newSpt));
            } catch (UnsupportedEditException ex)
            {
                // Should never happen since we use a rhythm already in use
                Exceptions.printStackTrace(ex);
            }
        }

        return res;
    }

    /**
     * New SongPart added.
     * <p>
     * - If new rhythm, update MidiMix and possibly chord symbols position if rhythm division has changed<br>
     *
     * @param sae
     * @param preCheck
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    private List<Operation> getNextSptAdded(SptAddedEvent sae, boolean preCheck) throws UnsupportedEditException
    {
        MidiMix midiMix = MidiMixManager.getDefault().findExistingMix(song);
        var uniqueRhythms = midiMix.getUniqueRhythms();
        var spts = sae.getSongParts();


        if (preCheck)
        {
            spts.forEach(spt -> uniqueRhythms.add(spt.getRhythm()));
            checkForMidiChannelUnavailable(uniqueRhythms);
            return null;
        }

        List<Operation> res = new ArrayList<>();


        // Possibly update MidiMix 
        for (var r : sae.getRhythms(true))
        {
            if (!uniqueRhythms.contains(r))
            {
                res.add(midiMix.addRhythmOperation(r));
            }
        }

        // Possibly update ChordLeadSheetItems position
        res.addAll(adjustChordLeadSheetItemsPosition(spts));

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
    private List<Operation> getNextSptRhythmChanged(SptRhythmChangedEvent srce, boolean preCheck) throws UnsupportedEditException
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
                checkForMidiChannelUnavailable(uniqueRhythms);      // throws UnsupportedEditException
            }
            return null;
        }


        List<Operation> res = new ArrayList<>();


        // Possibly update MidiMix 
        MidiMix midiMix = MidiMixManager.getDefault().findExistingMix(song);
        var uniqueRhythms = midiMix.getUniqueRhythms();
        for (var r : srce.getRhythms(true))
        {
            if (!uniqueRhythms.contains(r))
            {
                res.add(midiMix.addRhythmOperation(r));
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
    private List<Operation> getNextClsSizeChanged(SizeChangedEvent evt)
    {
        List<Operation> res = new ArrayList<>();
        if (!evt.isGrowing())
        {
            // Remove song parts
            java.util.List<CLI_Section> removedSections = evt.getItems(CLI_Section.class);
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
     * Check if MidiMix can accomodate the specified rhythms (in addition to the user tracks).
     *
     * @param rhythms
     * @throws UnsupportedEditException
     */
    private void checkForMidiChannelUnavailable(Set<Rhythm> rhythms) throws UnsupportedEditException
    {
        Objects.requireNonNull(rhythms);

        MidiMix midiMix = MidiMixManager.getDefault().findExistingMix(song);
        int nbVoices = midiMix.getUserChannels().size();      // Initialize with user rhythm voices
        for (var r : rhythms)
        {
            nbVoices += getSourceRhythm(r).getRhythmVoices().size();
        }

        if (nbVoices > NB_AVAILABLE_CHANNELS)
        {
            throw new UnsupportedEditException(ResUtil.getString(getClass(), "ERR_NotEnoughChannels"));
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
     * Adjust position of ChordLeadSheetItems which do not match the rhythm division.
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
     * Adjust position of ChordLeadSheetItems which do not match the rhythm division.
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

}

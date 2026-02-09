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
package org.jjazz.song;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.SectionMovedEvent;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsVetoableChangeEvent;
import org.jjazz.chordleadsheet.api.event.DeletedBarsEvent;
import org.jjazz.chordleadsheet.api.event.InsertedBarsEvent;
import org.jjazz.chordleadsheet.api.event.SectionAddedEvent;
import org.jjazz.chordleadsheet.api.event.SectionChangedEvent;
import org.jjazz.chordleadsheet.api.event.SectionRemovedEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.rhythm.api.Division;
import static org.jjazz.rhythm.api.Division.EIGHTH_SHUFFLE;
import static org.jjazz.rhythm.api.Division.EIGHTH_TRIPLET;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRhythmChanged;
import org.openide.util.Exceptions;

/**
 * Responsible for maintaining consistency between the ChordLeadSheet and the SongStructure of a Song.
 * <p>
 * For example if ChordLeadSheet size is shortened, some SongParts will be probably need to be removed or resized. Inversely, switching a SongPart to a
 * swing-feel rhythm might require adjusting some CLI_ChordSymbols position.
 */

public class ClsSgsUpdater implements ClsChangeListener, SgsChangeListener
{

    private final Song song;
    private final SongStructure songStructure;
    private final ChordLeadSheet chordLeadSheet;
    private static final Logger LOGGER = Logger.getLogger(ClsSgsUpdater.class.getSimpleName());

    public ClsSgsUpdater(Song song)
    {
        Objects.requireNonNull(song);
        this.song = song;
        this.songStructure = song.getSongStructure();
        this.chordLeadSheet = song.getChordLeadSheet();

        // We must be use synchronized listeners to guarantee global consistency between ChordLeadSheet and songStructure (they use the same lock).
        // Otherwise Song.getDeepCopy() could get a snapshot where the Song's SongStructure is not in sync with the Song's ChordLeadSheet.
        this.chordLeadSheet.addClsChangeSyncListener(this);
        this.songStructure.addSgsChangeListener(this);
    }

    public Song getSong()
    {
        return song;
    }

    // ============================================================================================= 
    // ClsChangeListener implementation
    // =============================================================================================      

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent evt) throws UnsupportedEditException
    {
        if (evt.isUndoOrRedo())
        {
            // IMPORTANT : SongStructure generates his own undoableEdits,
            // so we must not listen to chordleadsheet changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            LOGGER.log(Level.FINE, "chordLeadSheetChanged() undo/redo is in progress, exiting");
            return;
        }

        if (evt instanceof ClsVetoableChangeEvent vce)
        {
            var changeEvent = vce.getChangeEvent();
            processClsChangeEvent(changeEvent, true);       // throws UnsupportedEditException
            return;
        }


        try
        {
            processClsChangeEvent(evt, false);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen if it has been authorized first
            Exceptions.printStackTrace(ex);
            throw new IllegalStateException("evt=" + evt);
        }
    }
    // ============================================================================================= 
    // SgsChangeListener implementation
    // =============================================================================================    

    @Override
    public void songStructureChanged(SgsChangeEvent evt)
    {
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(songStructure);
        if (um != null && um.isUndoRedoInProgress())
        {
            // IMPORTANT : ChordLeadSheet generates his own undoableEdits,
            // so we must not listen to SongStructure changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            LOGGER.log(Level.FINE, "songStructureChanged() undo/redo is in progress, exiting");
            return;
        }


        switch (evt)
        {
            case SptRhythmChanged sre ->
            {
                processRhythmChanged(sre);
            }
            default ->
            {
                // Nothing
            }
        }

    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================      
    private void processClsChangeEvent(ClsChangeEvent evt, boolean authorizeOnly) throws UnsupportedEditException
    {
        LOGGER.log(Level.FINE, "processClsChangeEvent() evt={0} authorizeOnly={1}", new Object[]
        {
            evt, authorizeOnly
        });

        switch (evt)
        {
            case SizeChangedEvent e when !authorizeOnly -> processSizeChanged(e);
            case DeletedBarsEvent e when !authorizeOnly -> processDeletedBars(e);
            case InsertedBarsEvent e when !authorizeOnly -> processInsertedBars(e);
            case SectionRemovedEvent e when !authorizeOnly -> processSectionRemoved(e);
            case SectionAddedEvent e -> processSectionAdded(e, authorizeOnly);
            case SectionMovedEvent e when !authorizeOnly -> processSectionMoved(e);
            case SectionChangedEvent e ->
            {
                if (e.isTimeSignatureChanged())
                {
                    processSectionTimeSignatureChanged(e, authorizeOnly);
                }
                if (!authorizeOnly && e.isNameChanged())
                {
                    processSectionNameChanged(e);
                }
            }
            default ->
            {
            }
        }
    }


//----------------------------------------------------------------------------------------------------
// Private functions
//----------------------------------------------------------------------------------------------------  
    private void processSectionMoved(SectionMovedEvent evt)
    {
        assert !evt.isUndo() : "evt=" + evt;
        var cliSection = evt.getCLI_Section();
        int newBarIndex = cliSection.getPosition().getBar();
        assert newBarIndex > 0 : "cliSection=" + cliSection;


        CLI_Section newBarPrevSection = chordLeadSheet.getSection(newBarIndex - 1);
        CLI_Section oldBarNewSection = chordLeadSheet.getSection(evt.getOldBar());
        Map<SongPart, Integer> mapSptSize = new HashMap<>();

        if (oldBarNewSection == newBarPrevSection || oldBarNewSection == cliSection)
        {
            // It's a "small move", do not cross any other section, so it's just resize operations
            fillMapSptSize(mapSptSize, cliSection);
            fillMapSptSize(mapSptSize, newBarPrevSection);
            songStructure.resizeSongParts(mapSptSize);

        } else
        {
            // It's a "big move", which crosses at least another section

            // We remove and re-add
            songStructure.removeSongParts(getSongParts(cliSection));
            SongPart spt = createSptAfterSection(cliSection, chordLeadSheet.getBarRange(cliSection).size(), newBarPrevSection);
            try
            {
                songStructure.addSongParts(List.of(spt));
            } catch (UnsupportedEditException ex)
            {
                // Should never happen since we don't introduce new rhythm
                Exceptions.printStackTrace(ex);
            }

            // Resize impacted SongParts 
            fillMapSptSize(mapSptSize, oldBarNewSection);
            fillMapSptSize(mapSptSize, newBarPrevSection);
            songStructure.resizeSongParts(mapSptSize);

        }

    }

    private void processSizeChanged(SizeChangedEvent evt)
    {
        if (!evt.isGrowing())
        {
            // Possibly remove song parts
            var removedSections = evt.getItems(CLI_Section.class);
            for (var cliSection : removedSections)
            {
                var spts = getSongParts(cliSection);
                songStructure.removeSongParts(spts);
            }
        }

        // Last section might have changed
        Map<SongPart, Integer> mapSptSize = new HashMap<>();
        CLI_Section lastSection = getLastSection();
        fillMapSptSize(mapSptSize, lastSection);
        songStructure.resizeSongParts(mapSptSize);
    }

    private void processDeletedBars(DeletedBarsEvent evt)
    {
        // Possibly remove song parts
        // Init section will be removed too if evt.isInitSectionRemoved() == true
        for (var cliSection : evt.getItems(CLI_Section.class))
        {
            var spts = getSongParts(cliSection);
            songStructure.removeSongParts(spts);
        }

        // Section before the deleted bars might have changed
        Map<SongPart, Integer> mapSptSize = new HashMap<>();
        if (evt.getBarFrom() > 0)
        {
            var cliSection = chordLeadSheet.getSection(evt.getBarFrom() - 1);
            fillMapSptSize(mapSptSize, cliSection);
        } else if (!evt.isInitSectionRemoved())
        {
            // Init bar was deleted and the init section was not replaced by the first shifted section
            var cliSection = chordLeadSheet.getSection(0);
            fillMapSptSize(mapSptSize, cliSection);
        }
        songStructure.resizeSongParts(mapSptSize);
    }

    private void processInsertedBars(InsertedBarsEvent evt)
    {
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
    }

    private void processSectionRemoved(SectionRemovedEvent evt)
    {
        var cliSection = evt.getCLI_Section();

        // Remove SongParts
        songStructure.removeSongParts(getSongParts(cliSection));

        // Previous replacing section got bigger
        Map<SongPart, Integer> mapSptSize = new HashMap<>();
        fillMapSptSize(mapSptSize, evt.getPreviousBarSection());
        songStructure.resizeSongParts(mapSptSize);
    }

    private void processSectionAdded(SectionAddedEvent evt, boolean authorizeOnly) throws UnsupportedEditException
    {
        var cliSection = evt.getCLI_Section();
        CLI_Section sameBarReplacedSection = evt.getSameBarReplacedSection();

        if (sameBarReplacedSection != null)
        {
            // Special case, section replaces another one at same bar
            processSectionReplaced(sameBarReplacedSection, cliSection, authorizeOnly);
            return;
        }

        // Section added to an empty bar
        int bar = cliSection.getPosition().getBar();
        CLI_Section prevSection = evt.getPreviousBarSection();
        assert bar > 0 && prevSection != null : "evt=" + evt;

        if (authorizeOnly)
        {
            SongPart spt = createSptAfterSection(cliSection, getVirtualSectionSize(bar), prevSection);
            var event = new SptAddedEvent(songStructure, List.of(spt));
            songStructure.testChangeEventForVeto(event);          // throws UnsupportedEditException
            return;
        } else
        {
            SongPart spt = createSptAfterSection(cliSection, chordLeadSheet.getBarRange(cliSection).size(), prevSection);
            songStructure.addSongParts(Arrays.asList(spt));        // throws UnsupportedEditException
        }

        // Resize previous section
        Map<SongPart, Integer> mapSptSize = new HashMap<>();
        fillMapSptSize(mapSptSize, prevSection);
        songStructure.resizeSongParts(mapSptSize);
    }

    /**
     * A section was replaced by a new one in the same bar.
     *
     * @param oldSection
     * @param newSection
     * @param authorizeOnly
     * @throws UnsupportedEditException
     */
    private void processSectionReplaced(CLI_Section oldSection, CLI_Section newSection, boolean authorizeOnly) throws UnsupportedEditException
    {
        var oldSpts = getSongParts(oldSection);
        if (oldSpts.isEmpty())
        {
            return;
        }


        var oldName = oldSection.getData().getName();
        var oldTs = oldSection.getData().getTimeSignature();
        var newName = newSection.getData().getName();
        var newTs = newSection.getData().getTimeSignature();
        Rhythm newRhythm = newTs.equals(oldTs) ? null : songStructure.getRecommendedRhythm(newTs, oldSpts.get(0).getStartBarIndex());


        if (authorizeOnly)
        {
            if (newRhythm != null)
            {
                List<SongPart> newSpts = oldSpts.stream()
                        .map(spt -> spt.getCopy(newRhythm, spt.getStartBarIndex(), spt.getNbBars(), newSection))
                        .toList();
                var event = new SptRhythmChanged(songStructure, newRhythm, oldSpts, newSpts);
                songStructure.testChangeEventForVeto(event);          // throws UnsupportedEditException
            }
            return;
        }


        // Update SongStructure
        var newSpts = songStructure.setSongPartsRhythm(oldSpts, newRhythm);        // throws UnsupportedEditException
        List<SongPart> toBeRenamedSpts = newSpts.stream()
                .filter(spt -> spt.getName().equalsIgnoreCase(oldName))
                .toList();
        songStructure.setSongPartsName(toBeRenamedSpts, newName);

    }

    private void processSectionTimeSignatureChanged(SectionChangedEvent evt, boolean authorizeOnly) throws UnsupportedEditException
    {
        assert evt.isTimeSignatureChanged() : "evt=" + evt;

        var cliSection = evt.getCLI_Section();
        TimeSignature newTs = evt.getNewSection().getTimeSignature();


        // Need to replace all impacted SongParts based on this section
        List<SongPart> oldSpts = getSongParts(cliSection);
        if (oldSpts.isEmpty())
        {
            return;
        }


        // Get the new rhythm to use
        Rhythm newRhythm = songStructure.getRecommendedRhythm(newTs, oldSpts.get(0).getStartBarIndex());


        ArrayList<SongPart> newSpts = new ArrayList<>();
        for (SongPart oldSpt : oldSpts)
        {
            SongPart newSpt;
            if (authorizeOnly)
            {
                // Can't pass cliSection as argument
                // cliSection is not changed yet, getCopy would fail because time signature doesn't match newRhythm
                newSpt = oldSpt.getCopy(newRhythm, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), null);
            } else
            {
                newSpt = oldSpt.getCopy(newRhythm, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
            }
            newSpts.add(newSpt);
        }

        if (authorizeOnly)
        {
            var event = new SptRhythmChanged(songStructure, oldSpts, oldSpts);
            songStructure.testChangeEventForVeto(event);          // Possible exception here     
        } else
        {
            songStructure.replaceSongParts(oldSpts, newSpts);         // Possible exception here     
        }

    }

    private void processSectionNameChanged(SectionChangedEvent evt)
    {
        assert evt.isNameChanged() : "evt=" + evt;

        List<SongPart> spts = getSongParts(evt.getCLI_Section()).stream()
                .filter(spt -> spt.getName().equalsIgnoreCase(evt.getOldSection().getName()))
                .toList();

        songStructure.setSongPartsName(spts, evt.getNewSection().getName());
    }


    /**
     * Update ChordLeadSheet off-beat chord symbols position if a SongPart's rhythm switches between ternary and binary feel.
     *
     * @param evt
     */
    private void processRhythmChanged(SptRhythmChanged evt)
    {
        assert !evt.isUndo() : "evt=" + evt;
        Set<CLI_Section> processedSections = new HashSet<>();


        for (int i = 0; i < evt.getSongParts().size(); i++)
        {
            var oldSpt = evt.getSongParts().get(i);
            var newSpt = evt.getNewSpts().get(i);
            var cliSection = oldSpt.getParentSection();

            if (processedSections.contains(cliSection))
            {
                continue;
            }
            processedSections.add(cliSection);

            var oldDivision = oldSpt.getRhythm().getFeatures().division();
            var newDivision = newSpt.getRhythm().getFeatures().division();

            if ((oldDivision.isBinary() && (newDivision.isSwing() || newDivision == Division.EIGHTH_TRIPLET))
                    || (newDivision.isBinary() && (oldDivision.isSwing() || oldDivision == Division.EIGHTH_TRIPLET)))
            {
                // There is a rhythm change with a binary/ternary feel switch
                Quantization q = switch (newDivision)
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
                    var newPos = Quantizer.getQuantized(q, pos, oldSpt.getRhythm().getTimeSignature(), 1, pos.getBar());
                    chordLeadSheet.moveItem(cliCs, newPos);
                }
            }
        }
    }


    /**
     * Get all the songParts associated to specified parent section.
     *
     * @param parentSection
     * @return
     */
    private List<SongPart> getSongParts(CLI_Section parentSection)
    {
        return songStructure.getSongParts(cli -> cli.getParentSection() == parentSection);
    }

    /**
     * Add mapSptSize pairs in the specified hash for each SongPart associated to specified section.
     *
     * @param mapSptSize
     * @param parentSection
     */
    private void fillMapSptSize(Map<SongPart, Integer> mapSptSize, CLI_Section parentSection)
    {
        int size = chordLeadSheet.getBarRange(parentSection).size();
        for (SongPart spt : getSongParts(parentSection))
        {
            mapSptSize.put(spt, size);
        }
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
     * @param newSectionSize Size in bars
     * @param prevSection    The section before cliSection. Can be null.
     * @return The created SongPart, ready to be added to the SongStructure.
     */
    private SongPart createSptAfterSection(CLI_Section newSection, int newSectionSize, CLI_Section prevSection)
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
            List<SongPart> prevSpts = getSongParts(prevSection);        // can't be empty
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
        SongPart spt = songStructure.createSongPart(
                r,
                newSection.getData().getName(),
                sptBarIndex,
                newSection,
                true);
        return spt;
    }


    /**
     * Get the size of a section at sectionBar which is possibly not yet inserted in the parentChordLeadSheet.
     *
     * @param sectionBar
     * @return
     */
    private int getVirtualSectionSize(int sectionBar)
    {
        CLI_Section curSection = chordLeadSheet.getSection(sectionBar);
        return chordLeadSheet.getBarRange(curSection).to - sectionBar + 1;
    }

    private CLI_Section getLastSection()
    {
        return chordLeadSheet.getSection(chordLeadSheet.getSizeInBars() - 1);
    }
}

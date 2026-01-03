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
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.SectionMovedEvent;
import org.jjazz.chordleadsheet.api.event.ItemAddedEvent;
import org.jjazz.chordleadsheet.api.event.ItemBarShiftedEvent;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.chordleadsheet.api.event.ItemRemovedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.chordleadsheet.api.event.ClsVetoableChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.rhythm.api.Division;
import static org.jjazz.rhythm.api.Division.EIGHTH_SHUFFLE;
import static org.jjazz.rhythm.api.Division.EIGHTH_TRIPLET;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.openide.util.Exceptions;

/**
 * Responsible for maintaining consistency between the ChordLeadSheet and the SongStructure of a Song.
 * <p>
 * For example if ChordLeadSheet size is shortened, some SongParts will be probably need to be removed or resized. Inversely, switching a SongPart to a
 * swing-feel rhythm might require adjusting some CLI_ChordSymbols position.
 */
public class ClsSgsUpdater implements ClsChangeListener, SgsChangeListener
{

    private enum State
    {
        DEFAULT, INSERT_INIT_BARS
    };
    private final Song song;
    private State state;
    private SongStructure songStructure;
    private ChordLeadSheet chordLeadSheet;
    private static final Logger LOGGER = Logger.getLogger(ClsSgsUpdater.class.getSimpleName());

    public ClsSgsUpdater(Song song)
    {
        Objects.requireNonNull(song);
        state = State.DEFAULT;
        this.song = song;
        this.songStructure = song.getSongStructure();
        this.chordLeadSheet = song.getChordLeadSheet();
        this.chordLeadSheet.addClsChangeListener(this);
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
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(songStructure);
        if (um != null && um.isUndoRedoInProgress())
        {
            // IMPORTANT : SongStructure generates his own undoableEdits,
            // so we must not listen to chordleadsheet changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            LOGGER.log(Level.FINE, "chordLeadSheetChanged() undo is in progress, exiting");
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
            throw new IllegalStateException();
        }
    }
    // ============================================================================================= 
    // SgsChangeListener implementation
    // =============================================================================================    

    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(songStructure);
        if (um != null && um.isUndoRedoInProgress())
        {
            // IMPORTANT : ChordLeadSheet generates his own undoableEdits,
            // so we must not listen to SongStructure changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            LOGGER.log(Level.FINE, "songStructureChanged() undo is in progress, exiting");
            return;
        }


        switch (e)
        {
            case SptReplacedEvent sre ->
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


        // Get the sections in the event's items
        var cliSections = evt.getItems().stream()
                .filter(cli -> cli instanceof CLI_Section)
                .map(cli -> (CLI_Section) cli)
                .toList();

        switch (state)
        {
            case DEFAULT ->
            {
                if (evt instanceof ClsActionEvent cae
                        && cae.getApiId() == ClsActionEvent.API_ID.InsertBars
                        && !cae.isComplete()
                        && cae.getData().equals(Integer.valueOf(0)))
                {
                    // When inserting initial bars ("before" bar 0), ChordLeadSheet produces an unusual change event sequence because of the special init section. 
                    // This leads to song structure not updated as the user would expect (see Issue #459).
                    // So it's better to wait for the "insert initial bars ClsActionEvent" to be completed then update song structure properly.                    
                    state = State.INSERT_INIT_BARS;
                    
                } else if (evt instanceof SizeChangedEvent)
                {
                    processSizeChanged(authorizeOnly);

                } else if (!cliSections.isEmpty() && (evt instanceof ItemBarShiftedEvent ise))
                {
                    processSectionsShifted(ise, cliSections, authorizeOnly);

                } else if (!cliSections.isEmpty() && (evt instanceof ItemChangedEvent ice))
                {
                    processSectionChanged(ice, cliSections.get(0), authorizeOnly);

                } else if (!cliSections.isEmpty() && (evt instanceof ItemAddedEvent iae))
                {
                    processSectionsAdded(iae, cliSections, authorizeOnly);

                } else if (!cliSections.isEmpty() && (evt instanceof ItemRemovedEvent ire))
                {
                    processSectionsRemoved(ire, cliSections, authorizeOnly);

                } else if (evt instanceof SectionMovedEvent sme)
                {
                    if (authorizeOnly)
                    {
                        checkSectionMoveForVeto(sme, cliSections.get(0));
                    } else
                    {
                        processSectionMoved(sme, cliSections.get(0));
                    }

                } else
                {
                    LOGGER.fine("processClsChangeEvent() -> evt not handled");
                }
            }

            case INSERT_INIT_BARS ->
            {
                if (evt instanceof ClsActionEvent cae
                        && cae.getApiId() == ClsActionEvent.API_ID.InsertBars
                        && cae.isComplete())
                {
                    // Now we can update the song structure

                    // Reaffect the parent section of the song parts that were assigned to the initial section (before the insert bars action)
                    var initSection = chordLeadSheet.getSection(0);
                    var secondSection = (CLI_Section) chordLeadSheet.getNextItem(initSection);
                    assert secondSection != null : "initSection=" + initSection;
                    var oldSpts = songStructure.getSongParts(spt -> spt.getParentSection() == initSection);
                    var newSpts = oldSpts.stream()
                            .map(spt -> spt.getCopy(null, spt.getStartBarIndex(), spt.getNbBars(), secondSection))
                            .toList();
                    songStructure.replaceSongParts(oldSpts, newSpts);


                    // Now add the new initial song part linked to the new initial section
                    var ts = initSection.getData().getTimeSignature();
                    Rhythm r = songStructure.getLastUsedRhythm(ts);     // Might be null in special cases where songStructure is empty
                    if (r == null)
                    {
                        var rdb = RhythmDatabase.getDefault();
                        try
                        {
                            r = rdb.getRhythmInstance(rdb.getDefaultRhythm(ts));
                        } catch (UnavailableRhythmException ex)
                        {
                            LOGGER.log(Level.WARNING, "processChangeEvent() Can''t add initial song part with ts={0}. ex={1}", new Object[]
                            {
                                ts, ex.getMessage()
                            });
                        }
                    }
                    if (r != null)
                    {
                        var initSpt = songStructure.createSongPart(r, initSection.getData().getName(), 0, secondSection.getPosition().getBar(), initSection, false);
                        songStructure.addSongParts(List.of(initSpt));
                    }

                    // Go back to default behaviour
                    state = State.DEFAULT;
                }
            }
            default -> throw new AssertionError(state.name());

        }


    }


//----------------------------------------------------------------------------------------------------
// Private functions
//----------------------------------------------------------------------------------------------------  
    /**
     * CLI_Section was already moved.
     *
     * @param evt
     * @param cliSection
     * @param authorizeOnly
     * @throws UnsupportedEditException
     */
    private void processSectionMoved(SectionMovedEvent evt, CLI_Section cliSection) throws UnsupportedEditException
    {
        int newBarIndex = cliSection.getPosition().getBar();
        assert newBarIndex > 0 : "cliSection=" + cliSection;
        CLI_Section prevSection = chordLeadSheet.getSection(newBarIndex - 1);
        CLI_Section sectionPrevBar = chordLeadSheet.getSection(evt.getOldBar());
        Map<SongPart, Integer> mapSptSize = new HashMap<>();

        if (sectionPrevBar == prevSection || sectionPrevBar == cliSection)
        {
            // It's a "small move", do not cross any other section, so it's just resize operations
            fillMapSptSize(mapSptSize, cliSection);
            fillMapSptSize(mapSptSize, prevSection);
            songStructure.resizeSongParts(mapSptSize);

        } else
        {
            // It's a "big move", which crosses at least another section

            // We remove and re-add
            songStructure.removeSongParts(getSongParts(cliSection));
            SongPart spt = createSptAfterSection(cliSection, chordLeadSheet.getBarRange(cliSection).size(), prevSection);
            songStructure.addSongParts(Arrays.asList(spt));

            // Resize impacted SongParts 
            fillMapSptSize(mapSptSize, sectionPrevBar);
            fillMapSptSize(mapSptSize, prevSection);
            songStructure.resizeSongParts(mapSptSize);

        }

    }

    /**
     * CLI_Section has NOT moved yet.
     *
     * @param evt
     * @param cliSection
     * @throws UnsupportedEditException
     */
    private void checkSectionMoveForVeto(SectionMovedEvent evt, CLI_Section cliSection) throws UnsupportedEditException
    {
        int newBarIndex = evt.getNewBar();
        int oldBarIndex = evt.getOldBar();


        // Small move cases (no section crossed)
        if (newBarIndex == oldBarIndex)
        {
            return;
        } else if (newBarIndex > oldBarIndex && chordLeadSheet.getSection(newBarIndex) == cliSection)
        {
            return;
        } else if (newBarIndex < oldBarIndex && chordLeadSheet.getSection(newBarIndex) == chordLeadSheet.getSection(oldBarIndex - 1))
        {
            return;
        }

        // It's a "big move", which crosses at least another section

        // Test remove and re-add
        CLI_Section prevSection = chordLeadSheet.getSection(newBarIndex - 1);
        songStructure.testChangeEventForVeto(new SptRemovedEvent(songStructure, getSongParts(cliSection)));     // throws UnsupportedEditException

        SongPart spt = createSptAfterSection(cliSection, getVirtualSectionSize(newBarIndex), prevSection);
        songStructure.testChangeEventForVeto(new SptAddedEvent(songStructure, List.of(spt)));   // throws UnsupportedEditException

    }

    private void processSectionsRemoved(ItemRemovedEvent evt, List<CLI_Section> cliSections, boolean authorizeOnly) throws UnsupportedEditException
    {
        // Remove the linked SongParts and resize previous section
        for (CLI_Section cliSection : cliSections)
        {
            if (authorizeOnly)
            {
                var event = new SptRemovedEvent(songStructure, getSongParts(cliSection));
                songStructure.testChangeEventForVeto(event);          // Possible exception here    
            } else
            {
                songStructure.removeSongParts(getSongParts(cliSection));
            }


            int barIndex = cliSection.getPosition().getBar();
            if (barIndex > 0 && !authorizeOnly)
            {
                Map<SongPart, Integer> mapSptSize = new HashMap<>();
                CLI_Section prevSection = chordLeadSheet.getSection(barIndex - 1);
                fillMapSptSize(mapSptSize, prevSection);
                songStructure.resizeSongParts(mapSptSize);
            }
        }
    }

    private void processSectionsAdded(ItemAddedEvent evt, List<CLI_Section> cliSections, boolean authorizeOnly) throws UnsupportedEditException
    {
        // For each section add a SongPart, and resize the previous section
        for (CLI_Section cliSection : cliSections)
        {
            int barIndex = cliSection.getPosition().getBar();
            CLI_Section prevSection = (barIndex > 0) ? chordLeadSheet.getSection(barIndex - 1) : null;

            if (authorizeOnly)
            {
                SongPart spt = createSptAfterSection(cliSection, getVirtualSectionSize(barIndex), prevSection);
                var event = new SptAddedEvent(songStructure, List.of(spt));
                songStructure.testChangeEventForVeto(event);          // Possible exception here    
            } else
            {
                // Possible exception here !
                SongPart spt = createSptAfterSection(cliSection, chordLeadSheet.getBarRange(cliSection).size(), prevSection);
                songStructure.addSongParts(Arrays.asList(spt));
            }
            if (prevSection != null && !authorizeOnly)
            {
                // Resize previous section if there is one
                Map<SongPart, Integer> mapSptSize = new HashMap<>();
                fillMapSptSize(mapSptSize, prevSection);
                songStructure.resizeSongParts(mapSptSize);
            }
        }

    }

    private void processSectionChanged(ItemChangedEvent evt, CLI_Section cliSection, boolean authorizeOnly) throws UnsupportedEditException
    {
        TimeSignature newTs = ((Section) evt.getNewData()).getTimeSignature();
        TimeSignature oldTs = ((Section) evt.getOldData()).getTimeSignature();
        String newName = ((Section) evt.getNewData()).getName();
        String oldName = ((Section) evt.getOldData()).getName();


        if (!newTs.equals(oldTs))
        {
            // Time Signature has changed


            // Need to replace all impacted SongParts based on this section
            List<SongPart> oldSpts = getSongParts(cliSection);
            if (!oldSpts.isEmpty())
            {

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
                    var event = new SptReplacedEvent(songStructure, oldSpts, oldSpts);
                    songStructure.testChangeEventForVeto(event);          // Possible exception here     
                } else
                {
                    // Possible exception here
                    songStructure.replaceSongParts(oldSpts, newSpts);         // Possible exception here     
                }
            }
        }


        if (!newName.equals(oldName) && !authorizeOnly)
        {
            // CLI_Section name has changed : rename songparts which have not been renamed by user
            List<SongPart> spts = getSongParts(cliSection).stream()
                    .filter(spt -> spt.getName().equalsIgnoreCase(oldName))
                    .toList();
            songStructure.setSongPartsName(spts, newName);
        }
    }

    private void processSectionsShifted(ItemBarShiftedEvent evt, List<CLI_Section> cliSections, boolean authorizeOnly)
    {
        if (authorizeOnly)
        {
            return;
        }

        // Resize sections before and after the shifted bars.
        // Size of the section before the shifted items has changed
        Map<SongPart, Integer> mapSptSize = new HashMap<>();
        int firstBarIndex = cliSections.get(0).getPosition().getBar();
        if (firstBarIndex > 0)
        {
            CLI_Section prevSection = chordLeadSheet.getSection(firstBarIndex - 1);
            fillMapSptSize(mapSptSize, prevSection);
        }

        // Size of the last section of the shifted items has changed too
        int lastBarIndex = evt.getItems().get(evt.getItems().size() - 1).getPosition().getBar();
        CLI_Section lastSection = chordLeadSheet.getSection(lastBarIndex);
        fillMapSptSize(mapSptSize, lastSection);
        songStructure.resizeSongParts(mapSptSize);
    }

    private void processSizeChanged(boolean authorizeOnly)
    {
        if (authorizeOnly)
        {
            return;
        }

        // Need to update size of impacted SongParts
        Map<SongPart, Integer> mapSptSize = new HashMap<>();
        CLI_Section lastSection = chordLeadSheet.getSection(chordLeadSheet.getSizeInBars() - 1);
        fillMapSptSize(mapSptSize, lastSection);
        songStructure.resizeSongParts(mapSptSize);
    }

    /**
     * Update ChordLeadSheet off-beat chord symbols position if a SongPart's rhythm switches between ternary and binary feel.
     *
     * @param sre
     */
    private void processRhythmChanged(SptReplacedEvent sre)
    {
        Set<CLI_Section> processedSections = new HashSet<>();


        for (int i = 0; i < sre.getSongParts().size(); i++)
        {
            var oldSpt = sre.getSongParts().get(i);
            var newSpt = sre.getNewSpts().get(i);
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
        ArrayList<SongPart> spts = new ArrayList<>();
        for (SongPart spt : songStructure.getSongParts())
        {
            if (spt.getParentSection() == parentSection)
            {
                spts.add(spt);
            }
        }
        return spts;
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
                newSectionSize,
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
}

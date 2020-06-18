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
package org.jjazz.songstructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.event.SectionMovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemAddedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemBarShiftedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemRemovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.util.SmallMap;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.songstructure.api.SongPart;
import org.openide.util.Exceptions;

/**
 * Responsible for listening to sgs parentChordLeadSheet changes and update sgs accordingly.
 */
public class SgsUpdater implements ClsChangeListener
{

    private SongStructureImpl sgs;
    private ChordLeadSheet parentCls;
    private static final Logger LOGGER = Logger.getLogger(SgsUpdater.class.getSimpleName());

    protected SgsUpdater(SongStructureImpl sgs)
    {
        if (sgs == null)
        {
            throw new IllegalArgumentException("sgs=" + sgs);
        }
        this.sgs = sgs;
        parentCls = sgs.getParentChordLeadSheet();
        if (parentCls != null)
        {
            parentCls.addClsChangeListener(this);
        }
    }

    // ============================================================================================= 
    // ClsChangeListener implementation
    // =============================================================================================      

    @Override
    public void authorizeChange(ClsChangeEvent evt) throws UnsupportedEditException
    {
        processChangeEvent(evt, true);
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent evt)
    {
        try
        {
            processChangeEvent(evt, false);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen if it has been authorized first
            Exceptions.printStackTrace(ex);
            throw new IllegalStateException();
        }
    }

    private void processChangeEvent(ClsChangeEvent evt, boolean authorizeOnly) throws UnsupportedEditException
    {
        LOGGER.log(Level.FINE, "processChangeEvent() evt=" + evt + " authorizeOnly=" + authorizeOnly);


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        if (um != null && um.isUndoRedoInProgress())
        {
            // IMPORTANT : SongStructure generates his own undoableEdits,
            // so we must not listen to chordleadsheet changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            LOGGER.log(Level.FINE, "processChangeEvent() undo is in progress, exiting");
            return;
        }


        // Get the sections in the event's items
        var cliSections = evt.getItems().stream()
                .filter(cli -> cli instanceof CLI_Section)
                .map(cli -> (CLI_Section) cli)
                .collect(Collectors.toList());


        if (evt instanceof SizeChangedEvent)
        {
            processSizeChanged(authorizeOnly);

        } else if (!cliSections.isEmpty() && (evt instanceof ItemBarShiftedEvent))
        {
            processSectionsShifted((ItemBarShiftedEvent) evt, cliSections, authorizeOnly);

        } else if (!cliSections.isEmpty() && (evt instanceof ItemChangedEvent))
        {
            processSectionChanged((ItemChangedEvent) evt, cliSections, authorizeOnly);

        } else if (!cliSections.isEmpty() && (evt instanceof ItemAddedEvent))
        {
            processSectionsAdded((ItemAddedEvent) evt, cliSections, authorizeOnly);

        } else if (!cliSections.isEmpty() && (evt instanceof ItemRemovedEvent))
        {
            processSectionsRemoved((ItemRemovedEvent) evt, cliSections, authorizeOnly);

        } else if (evt instanceof SectionMovedEvent)
        {
            processSectionMoved((SectionMovedEvent) evt, cliSections, authorizeOnly);

        }
    }

    //----------------------------------------------------------------------------------------------------
    // Private functions
    //----------------------------------------------------------------------------------------------------  

    private void processSectionMoved(SectionMovedEvent evt, List<CLI_Section> cliSections, boolean authorizeOnly) throws UnsupportedEditException
    {

        CLI_Section cliSection = cliSections.get(0);
        int barIndex = cliSection.getPosition().getBar();
        assert barIndex > 0 : "cliSection=" + cliSection;
        CLI_Section prevSection = parentCls.getSection(barIndex - 1);
        CLI_Section sectionPrevBar = parentCls.getSection(evt.getPrevBar());
        SmallMap<SongPart, Integer> mapSptSize = new SmallMap<>();


        if (sectionPrevBar == prevSection || sectionPrevBar == cliSection)
        {
            if (!authorizeOnly)
            {
                // It's a "small move", do not cross any other section, so it's just resize operations
                fillMapSptSize(mapSptSize, cliSection);
                fillMapSptSize(mapSptSize, prevSection);
                sgs.resizeSongParts(mapSptSize);
            }

        } else
        {
            // It's a "big move", which crosses at least another section

            // We remove and re-add
            if (authorizeOnly)
            {
                sgs.authorizeRemoveSongParts(getSongParts(cliSection));
                SongPart spt = createSptAfterSection(cliSection, getVirtualSectionSize(cliSection), prevSection);
                sgs.authorizeAddSongParts(Arrays.asList(spt));
            } else
            {
                sgs.removeSongParts(getSongParts(cliSection));
                SongPart spt = createSptAfterSection(cliSection, parentCls.getSectionRange(cliSection).size(), prevSection);
                sgs.addSongParts(Arrays.asList(spt));
            }

            if (!authorizeOnly)
            {
                // Resize impacted SongParts 
                fillMapSptSize(mapSptSize, sectionPrevBar);
                fillMapSptSize(mapSptSize, prevSection);
                sgs.resizeSongParts(mapSptSize);
            }
        }

    }

    private void processSectionsRemoved(ItemRemovedEvent evt, List<CLI_Section> cliSections, boolean authorizeOnly) throws UnsupportedEditException
    {
        // Remove the linked SongParts and resize previous section
        for (CLI_Section cliSection : cliSections)
        {
            if (authorizeOnly)
            {
                sgs.authorizeRemoveSongParts(getSongParts(cliSection));
            } else
            {
                sgs.removeSongParts(getSongParts(cliSection));
            }


            int barIndex = cliSection.getPosition().getBar();
            if (barIndex > 0 && !authorizeOnly)
            {
                SmallMap<SongPart, Integer> mapSptSize = new SmallMap<>();
                CLI_Section prevSection = parentCls.getSection(barIndex - 1);
                fillMapSptSize(mapSptSize, prevSection);
                sgs.resizeSongParts(mapSptSize);
            }
        }
    }

    private void processSectionsAdded(ItemAddedEvent evt, List<CLI_Section> cliSections, boolean authorizeOnly) throws UnsupportedEditException
    {
        // For each section add a SongPart, and resize the previous section
        for (CLI_Section cliSection : cliSections)
        {
            int barIndex = cliSection.getPosition().getBar();
            CLI_Section prevSection = (barIndex > 0) ? parentCls.getSection(barIndex - 1) : null;

            if (authorizeOnly)
            {
                SongPart spt = createSptAfterSection(cliSection, getVirtualSectionSize(cliSection), prevSection);
                sgs.authorizeAddSongParts(Arrays.asList(spt));
            } else
            {
                // Possible exception here !
                SongPart spt = createSptAfterSection(cliSection, parentCls.getSectionRange(cliSection).size(), prevSection);
                sgs.addSongParts(Arrays.asList(spt));
            }
            if (prevSection != null && !authorizeOnly)
            {
                // Resize previous section if there is one
                SmallMap<SongPart, Integer> mapSptSize = new SmallMap<>();
                fillMapSptSize(mapSptSize, prevSection);
                sgs.resizeSongParts(mapSptSize);
            }
        }

    }

    private void processSectionChanged(ItemChangedEvent evt, List<CLI_Section> cliSections, boolean authorizeOnly) throws UnsupportedEditException
    {
        CLI_Section cliSection = cliSections.get(0);
        TimeSignature newTs = cliSection.getData().getTimeSignature();
        TimeSignature oldTs = ((Section) evt.getOldData()).getTimeSignature();
        String oldName = ((Section) evt.getOldData()).getName();


        if (!newTs.equals(oldTs))
        {
            // Time Signature has changed

            // Need to replace all impacted SongParts based on this section
            List<SongPart> oldSpts = getSongParts(cliSection);
            if (!oldSpts.isEmpty())
            {

                // Get the new rhythm to use
                SongPart prevSpt = oldSpts.get(0).getStartBarIndex() == 0 ? null : sgs.getSongPart(oldSpts.get(0).getStartBarIndex() - 1);
                Rhythm newRhythm = findNewRhythm(newTs, prevSpt);

                ArrayList<SongPart> newSpts = new ArrayList<>();
                for (SongPart oldSpt : oldSpts)
                {
                    SongPart newSpt = oldSpt.clone(newRhythm, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
                    newSpts.add(newSpt);
                }

                if (authorizeOnly)
                {
                    // Possible exception here                        
                    sgs.authorizeReplaceSongParts(oldSpts, newSpts);
                } else
                {
                    // Possible exception here
                    sgs.replaceSongParts(oldSpts, newSpts);
                }
            }
        }


        if (!cliSection.getData().getName().equals(oldName) && !authorizeOnly)
        {
            // Section name has changed : rename songparts which have not been renamed by user
            List<SongPart> spts = getSongParts(cliSection).stream()
                    .filter(spt -> spt.getName().equalsIgnoreCase(oldName))
                    .collect(Collectors.toList());
            sgs.setSongPartsName(spts, cliSection.getData().getName());
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
        SmallMap<SongPart, Integer> mapSptSize = new SmallMap<>();
        int firstBarIndex = cliSections.get(0).getPosition().getBar();
        if (firstBarIndex > 0)
        {
            CLI_Section prevSection = parentCls.getSection(firstBarIndex - 1);
            fillMapSptSize(mapSptSize, prevSection);
        }

        // Size of the last section of the shifted items has changed too
        int lastBarIndex = evt.getItems().get(evt.getItems().size() - 1).getPosition().getBar();
        CLI_Section lastSection = parentCls.getSection(lastBarIndex);
        fillMapSptSize(mapSptSize, lastSection);
        sgs.resizeSongParts(mapSptSize);
    }

    private void processSizeChanged(boolean authorizeOnly)
    {
        if (authorizeOnly)
        {
            return;
        }

        // Need to update size of impacted SongParts
        SmallMap<SongPart, Integer> mapSptSize = new SmallMap<>();
        CLI_Section lastSection = parentCls.getSection(parentCls.getSize() - 1);
        fillMapSptSize(mapSptSize, lastSection);
        sgs.resizeSongParts(mapSptSize);
    }

    /**
     * Find the rhythm to be used for the specified time signature and song parts.
     *
     * @param ts
     * @param prevSpt Can be null if new rhythm is for the first song part.
     * @return
     */
    private Rhythm findNewRhythm(TimeSignature ts, SongPart prevSpt)
    {
        if (ts == null)
        {
            throw new IllegalArgumentException("ts=" + ts + " prevSpt=" + prevSpt);
        }

        RhythmDatabase rdb = RhythmDatabase.getDefault();

        // Try to use the last used rhythm for this new time signature
        Rhythm newRhythm = sgs.getLastUsedRhythm(ts);

        // Try to use an AdaptedRhythm for the previous song part's rhythm
        if (newRhythm == null && prevSpt != null)
        {
            Rhythm prevRhythm = prevSpt.getRhythm();
            if (prevRhythm instanceof AdaptedRhythm)
            {
                prevRhythm = ((AdaptedRhythm) prevRhythm).getSourceRhythm();
            }
            newRhythm = rdb.getAdaptedRhythm(prevRhythm, ts);        // may be null
        }

        // Last option
        if (newRhythm == null)
        {
            newRhythm = rdb.getDefaultRhythm(ts);        // Can't be null
        }

        return newRhythm;
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
        for (SongPart spt : sgs.getSongParts())
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
    private void fillMapSptSize(SmallMap<SongPart, Integer> mapSptSize, CLI_Section parentSection)
    {
        int size = parentCls.getSectionRange(parentSection).size();
        for (SongPart spt : getSongParts(parentSection))
        {
            mapSptSize.putValue(spt, size);
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
     * @param prevSection The section before cliSection. Can be null.
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
            sptBarIndex = sgs.getSizeInBars();

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
        SongPart prevSpt = sptBarIndex == 0 ? null : sgs.getSongPart(sptBarIndex - 1);
        Rhythm r = findNewRhythm(newSection.getData().getTimeSignature(), prevSpt);

        // Create the song part       
        SongPart spt = sgs.createSongPart(
                r,
                sptBarIndex,
                newSectionSize,
                newSection);
        return spt;
    }


    /**
     * Get the size of a section which is not yet inserted in the parentChordLeadSheet.
     *
     * @param cliSection
     * @return
     */
    private int getVirtualSectionSize(CLI_Section cliSection)
    {
        int bar = cliSection.getPosition().getBar();
        CLI_Section curSection = parentCls.getSection(bar);
        return parentCls.getSectionRange(curSection).to - bar + 1;
    }
}

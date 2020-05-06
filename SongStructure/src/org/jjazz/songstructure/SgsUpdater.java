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
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.util.SmallMap;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * Responsible for listening to sgs parentChordLeadSheet changes and update sgs accordingly.
 */
public class SgsUpdater implements ClsChangeListener
{

    private SongStructure sgs;
    private ChordLeadSheet parentCls;
    private static final Logger LOGGER = Logger.getLogger(SgsUpdater.class.getSimpleName());

    protected SgsUpdater(SongStructure sgs)
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

    //------------------------------------------------------------------------------------
    // Implements ClsChangeListener
    //------------------------------------------------------------------------------------
    @Override
    public void chordLeadSheetChanged(ClsChangeEvent evt) throws UnsupportedEditException
    {
        LOGGER.log(Level.FINE, "chordLeadSheetChanged() evt=" + evt);

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        if (um != null && um.isUndoRedoInProgress())
        {
            // IMPORTANT : SongStructure generates his own undoableEdits,
            // so we must not listen to chordleadsheet changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            return;
        }

        // To store resize operations
        SmallMap<SongPart, Integer> mapSptSize = new SmallMap<>();

        // Check if there are sections in the event's items
        List<CLI_Section> cliSections = new ArrayList<>();
        for (ChordLeadSheetItem<?> cli : evt.getItems())
        {
            if (cli instanceof CLI_Section)
            {
                cliSections.add((CLI_Section) cli);
            }
        }

        if (evt instanceof SizeChangedEvent)
        {
            SizeChangedEvent e = (SizeChangedEvent) evt;
            // Need to update size of impacted SongParts
            CLI_Section lastSection = parentCls.getSection(parentCls.getSize() - 1);
            fillMapSptSize(mapSptSize, lastSection);
            sgs.resizeSongParts(mapSptSize);
        } else if (!cliSections.isEmpty() && (evt instanceof ItemBarShiftedEvent))
        {
            // Resize sections before and after the shifted bars.
            ItemBarShiftedEvent e = (ItemBarShiftedEvent) evt;

            // Size of the section before the shifted items has changed
            int firstBarIndex = cliSections.get(0).getPosition().getBar();
            if (firstBarIndex > 0)
            {
                CLI_Section prevSection = parentCls.getSection(firstBarIndex - 1);
                fillMapSptSize(mapSptSize, prevSection);
            }

            // Size of the last section of the shifted items has changed too
            int lastBarIndex = e.getItems().get(e.getItems().size() - 1).getPosition().getBar();
            CLI_Section lastSection = parentCls.getSection(lastBarIndex);
            fillMapSptSize(mapSptSize, lastSection);
            sgs.resizeSongParts(mapSptSize);
        } else if (!cliSections.isEmpty() && (evt instanceof ItemChangedEvent))
        {
            // Update rhythm if signature has changed
            CLI_Section cliSection = cliSections.get(0);
            ItemChangedEvent e = (ItemChangedEvent) evt;
            TimeSignature newTs = cliSection.getData().getTimeSignature();
            TimeSignature oldTs = ((Section) e.getOldData()).getTimeSignature();
            String oldName = ((Section) e.getOldData()).getName();
            if (!newTs.equals(oldTs))
            {
                // Time Signature has changed, need to replace all impacted SongParts based on this section
                List<SongPart> oldSpts = getSongParts(cliSection);
                if (!oldSpts.isEmpty())
                {
                    // Try to use the last used rhythm for this new time signature
                    Rhythm newRhythm = sgs.getLastUsedRhythm(newTs);

                    // Try to use an AdaptedRhythm if possible           
                    RhythmDatabase rdb = RhythmDatabase.getDefault();
                    if (newRhythm == null)
                    {
                        if (oldSpts.get(0).getStartBarIndex() > 0)
                        {
                            Rhythm prevRhythm = sgs.getSongPart(oldSpts.get(0).getStartBarIndex() - 1).getRhythm();
                            if (prevRhythm instanceof AdaptedRhythm)
                            {
                                prevRhythm = ((AdaptedRhythm) prevRhythm).getSourceRhythm();
                            }
                            newRhythm = rdb.getAdaptedRhythm(prevRhythm, newTs);        // may be null
                        }
                    }

                    // Last option
                    if (newRhythm == null)
                    {
                        newRhythm = rdb.getDefaultRhythm(newTs);        // Can't be null
                    }

                    ArrayList<SongPart> newSpts = new ArrayList<>();
                    for (SongPart oldSpt : oldSpts)
                    {
                        SongPart newSpt = oldSpt.clone(newRhythm, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
                        newSpts.add(newSpt);
                    }

                    // Possible exception here : to be handled by caller 
                    sgs.replaceSongParts(oldSpts, newSpts);

                } else
                {
                    // It's just a renaming: rename songparts which have not been renamed by user
                    List<SongPart> spts = getSongParts(cliSection);
                    for (SongPart spt : spts.toArray(new SongPart[0]))
                    {
                        if (!spt.getName().equalsIgnoreCase(oldName))
                        {
                            spts.remove(spt);
                        }
                    }
                    sgs.setSongPartsName(spts, cliSection.getData().getName());
                }
            }
        } else if (!cliSections.isEmpty() && (evt instanceof ItemAddedEvent))
        {
            // For each section add a SongPart, and resize the previous section
            for (CLI_Section cliSection : cliSections)
            {
                int barIndex = cliSection.getPosition().getBar();
                CLI_Section prevSection = (barIndex > 0) ? parentCls.getSection(barIndex - 1) : null;
                SongPart spt = createSptAfterSection(cliSection, prevSection);
                UnsupportedEditException exception = null;
                try
                {
                    // Possible exception here !
                    sgs.addSongParts(Arrays.asList(spt));
                } catch (UnsupportedEditException ex)
                {
                    // Delay the exception propagation later to do the previous section resize operation.
                    // This will ensure that we leave in a clean state that can be correctly undone
                    // by the caller who will handle the exception.              
                    exception = ex;
                }
                if (prevSection != null)
                {
                    // Resize previous section if there is one
                    fillMapSptSize(mapSptSize, prevSection);
                    sgs.resizeSongParts(mapSptSize);
                }
                if (exception != null)
                {
                    // We're in an clean undoable state, throw exception now
                    throw exception;
                }
            }
        } else if (!cliSections.isEmpty() && (evt instanceof ItemRemovedEvent))
        {
            // Remove the linked SongParts and resize previous section
            for (CLI_Section cliSection : cliSections)
            {
                sgs.removeSongParts(getSongParts(cliSection));
                int barIndex = cliSection.getPosition().getBar();
                if (barIndex > 0)
                {
                    CLI_Section prevSection = parentCls.getSection(barIndex - 1);
                    fillMapSptSize(mapSptSize, prevSection);
                    sgs.resizeSongParts(mapSptSize);
                }
            }
        } else if (evt instanceof SectionMovedEvent)
        {
            CLI_Section cliSection = cliSections.get(0);
            SectionMovedEvent e = (SectionMovedEvent) evt;
            int barIndex = cliSection.getPosition().getBar();
            assert barIndex > 0 : "cliSection=" + cliSection;
            CLI_Section prevSection = parentCls.getSection(barIndex - 1);
            CLI_Section sectionPrevBar = parentCls.getSection(e.getPrevBar());
            if (sectionPrevBar == prevSection || sectionPrevBar == cliSection)
            {
                // It's a "small move", do not cross any other section, so it's just resize operations
                fillMapSptSize(mapSptSize, cliSection);
                fillMapSptSize(mapSptSize, prevSection);
                sgs.resizeSongParts(mapSptSize);
            } else
            {
                // It's a "big move", which crosses at least another section

                // We remove and re-add
                sgs.removeSongParts(getSongParts(cliSection));
                SongPart spt = this.createSptAfterSection(cliSection, prevSection);
                UnsupportedEditException exception = null;
                try
                {
                    sgs.addSongParts(Arrays.asList(spt));
                } catch (UnsupportedEditException ex)
                {
                    // Delay the exception propagation later to do the resize operation anyway.
                    // This will ensure that we leave in a clean state that can be correctly undone
                    // by the caller who will handle the exception.                             
                    exception = ex;
                }
                // Resize impacted SongParts 
                fillMapSptSize(mapSptSize, sectionPrevBar);
                fillMapSptSize(mapSptSize, prevSection);
                sgs.resizeSongParts(mapSptSize);
                if (exception != null)
                {
                    // We're in an clean undoable state, throw exception now
                    throw exception;
                }
            }
        }
    }
    //----------------------------------------------------------------------------------------------------
    // Private functions
    //----------------------------------------------------------------------------------------------------  

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
        int size = parentCls.getSectionSize(parentSection);
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

        Rhythm r = sgs.getLastUsedRhythm(newSection.getData().getTimeSignature());
        if (r == null)
        {
            r = RhythmDatabase.getDefault().getDefaultRhythm(newSection.getData().getTimeSignature());
        }
        SongPart spt = sgs.createSongPart(
                r,
                sptBarIndex,
                parentCls.getSectionSize(newSection),
                newSection);
        return spt;
    }
}

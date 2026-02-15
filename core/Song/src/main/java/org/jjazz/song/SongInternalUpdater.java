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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SectionAddedEvent;
import org.jjazz.chordleadsheet.api.event.SectionChangedEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRhythmChangedEvent;

/**
 * Manage the impact of ChordLeadSheet or SongStructure changes to other Song internal components.
 * <p>
 * For example, a ChordLeadSheet time signature change might require an udpate of a SongPart rhythm, which will impact the MidiMix, and possibly some chord
 * symbols position back in the ChordLeadSheet if rhythm division has changed.
 * <p>
 * SongInternalUpdater also allows to pre-check an operation: e.g. a time signature change could be vetoed by SongStructure/MidiMix because the new rhythm would
 * need too many Midi channels.
 */
class SongInternalUpdater
{

    private final Song song;
    private final SongStructure songStructure;
    private final ChordLeadSheet chordLeadSheet;
    private MidiMix midiMix;

    public SongInternalUpdater(Song sg)
    {
        this.song = sg;
        this.songStructure = sg.getSongStructure();
        this.chordLeadSheet = sg.getChordLeadSheet();
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
            case SectionAddedEvent sae ->
            {
                checkSectionAdd(sae);
            }
            case SectionChangedEvent sce ->
            {
                checkSectionChange(sce);
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
                checkRhythmChange(srce);
            }
            case SptAddedEvent sae ->
            {
                checkAddSongPart(sae);
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
    public List<WriteOperation> getNextOperations(WriteOperationResults results)
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

    private List<WriteOperation> getNextOperations(ClsChangeEvent srcEvent)
    {
        List<WriteOperation> res;
        switch (srcEvent)
        {
            case SizeChangedEvent sce ->
            {
                res = processSizeChanged(sce);
            }
            case SectionAddedEvent sae ->
            {
                res = processSectionAdded(sae);
            }
            default ->
            {
                res = new ArrayList<>();
            }
        }
        return res;
    }

    private List<WriteOperation> getNextOperations(SgsChangeEvent srcEvent)
    {
    }

    private WriteOperation getNextOperation(ClsChangeEvent srcEvent)
    {
    }

    private WriteOperation getNextOperation(SgsChangeEvent srcEvent)
    {
    }

    private List<WriteOperation> processSectionAdded(SectionAddedEvent evt) throws UnsupportedEditException
    {
        org.jjazz.chordleadsheet.api.item.CLI_Section cliSection = evt.getCLI_Section();
        CLI_Section sameBarReplacedSection = evt.getSameBarReplacedSection();
        if (sameBarReplacedSection != null)
        {
            // Special case, section replaces another one at same bar
            processSectionReplaced(sameBarReplacedSection, cliSection, evt.isVetoableChangeEvent());
            return;
        }

        // Section added to an empty bar
        int bar = cliSection.getPosition().getBar();
        CLI_Section prevSection = evt.getPreviousBarSection();
        assert bar > 0 && prevSection != null : "evt=" + evt;
        if (evt.isVetoableChangeEvent())
        {
            SongPart spt = createSptAfterSection(cliSection, getVirtualSectionSize(bar), prevSection);
            org.jjazz.songstructure.api.event.SptAddedEvent event = new SptAddedEvent(songStructure, List.of(spt));
            songStructure.testChangeEventForVeto(event); // throws UnsupportedEditException
            return;
        } else
        {
            SongPart spt = createSptAfterSection(cliSection, chordLeadSheet.getBarRange(cliSection).size(), prevSection);
            songStructure.addSongParts(Arrays.asList(spt)); // throws UnsupportedEditException
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
                var event = new SptRhythmChangedEvent(songStructure, newRhythm, oldSpts, newSpts);
                songStructure.testChangeEventForVeto(event);          // throws UnsupportedEditException
            }
            return;
        }


        // Update SongStructure
        songStructure.setSongPartsRhythm(oldSpts, newRhythm, newSection);        // throws UnsupportedEditException
        List<SongPart> toBeRenamedSpts = oldSpts.stream()
                .filter(spt -> spt.getName().equalsIgnoreCase(oldName))
                .toList();
        songStructure.setSongPartsName(toBeRenamedSpts, newName);

    }

    private List<WriteOperation> processSizeChanged(SizeChangedEvent evt)
    {
        List<WriteOperation> res = new ArrayList<>();
        if (!evt.isGrowing())
        {
            // Possibly remove song parts
            java.util.List< // Possibly remove song parts
            org.jjazz.chordleadsheet.api.item.CLI_Section> removedSections = evt.getItems(CLI_Section.class);
            List<SongPart> toBeRemovedSpts = new ArrayList<>();
            for (org.jjazz.chordleadsheet.api.item.CLI_Section cliSection : removedSections)
            {
                toBeRemovedSpts.addAll(getSongParts(cliSection));
            }
            res.add(songStructure.removeSongPartsOperation(toBeRemovedSpts));
        }
        // Last section might have changed
        Map<SongPart, Integer> mapSptSize = new HashMap<>();
        CLI_Section lastSection = getLastSection();
        fillMapSptSize(mapSptSize, lastSection);
        res.add(songStructure.resizeSongPartsOperation(mapSptSize));
        return res;
    }

    private void checkSectionAdd(SectionAddedEvent sae) throws UnsupportedEditException
    {
        var cliSection = sae.getCLI_Section();
        CLI_Section sameBarReplacedSection = sae.getSameBarReplacedSection();

        if (sameBarReplacedSection != null)
        {
            // Special case, section replaces another one at same bar
            processSectionReplaced(sameBarReplacedSection, cliSection, authorizeOnly);
            return;
        }

        // Section added to an empty bar
        int bar = cliSection.getPosition().getBar();
        CLI_Section prevSection = sae.getPreviousBarSection();
        assert bar > 0 && prevSection != null : "evt=" + evt;

        if (authorizeOnly)
        {
            SongPart spt = createSptAfterSection(cliSection, prevSection);
            var event = new SptAddedEvent(songStructure, List.of(spt));
            songStructure.testChangeEventForVeto(event);          // throws UnsupportedEditException
            return;
        } else
        {
            SongPart spt = createSptAfterSection(cliSection, prevSection);
            songStructure.addSongParts(Arrays.asList(spt));        // throws UnsupportedEditException
        }

        // Resize previous section
        Map<SongPart, Integer> mapSptSize = new HashMap<>();
        fillMapSptSize(mapSptSize, prevSection);
        songStructure.resizeSongParts(mapSptSize);
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

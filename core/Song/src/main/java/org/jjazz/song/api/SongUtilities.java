/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.song.api;

import java.util.ArrayList;
import java.util.List;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.ClsUtilities;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.songstructure.api.SongPart;
import org.openide.util.Exceptions;

/**
 * Song utilities methods.
 */
public class SongUtilities
{

    /**
     * @see #getLinearizedSong(org.jjazz.song.api.Song, boolean)
     */
    static public final String SECTION_COPY_DELIMITER_CHAR = "#";

    /**
     * Half the chord leadsheet of the specified song.
     * <p>
     * Move all chord symbols and sections accordingly.
     *
     * @param song
     * @throws UnsupportedEditException
     */
    static public void halfChordLeadsheet(Song song) throws UnsupportedEditException
    {
        ChordLeadSheet cls = song.getChordLeadSheet();
        for (var cli : cls.getItems())
        {
            int bar = cli.getPosition().getBar();
            float beat = cli.getPosition().getBeat();
            float nbBeats = cls.getSection(bar).getData().getTimeSignature().getNbNaturalBeats();
            int newBar = bar / 2;
            float newBeat = (bar % 2 == 0) ? beat / 2f : (nbBeats + beat) / 2f;
            if (cli instanceof CLI_Section section)
            {
                if (bar > 0)
                {
                    CLI_Section destSection = cls.getSection(newBar);
                    if (destSection.getPosition().getBar() == newBar)
                    {
                        // There is already a section at destination bar, just remove the section
                        cls.removeSection(section);
                    } else
                    {
                        cls.moveSection(section, newBar);
                    }
                }
            } else
            {
                cls.moveItem(cli, new Position(newBar, newBeat));
            }
        }

        // Update size as well
        int size = cls.getSizeInBars();
        cls.setSizeInBars(size / 2 + size % 2);
    }

    /**
     * Double the chord leadsheet of the specified song.
     * <p>
     * Move all chord symbols and sections accordingly.
     *
     * @param song
     */
    static public void doubleChordLeadsheet(Song song)
    {
        if (song == null)
        {
            throw new NullPointerException("song");
        }
        ChordLeadSheet cls = song.getChordLeadSheet();

        try
        {
            // Update size
            cls.setSizeInBars(cls.getSizeInBars() * 2);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }

        // Move items
        var items = cls.getItems();
        for (int i = items.size() - 1; i >= 0; i--)
        {
            ChordLeadSheetItem<?> cli = items.get(i);
            int bar = cli.getPosition().getBar();
            float beat = cli.getPosition().getBeat();
            float nbBeats = cls.getSection(bar).getData().getTimeSignature().getNbNaturalBeats();
            int newBar = bar * 2;
            float newBeat = beat * 2;
            if (newBeat >= nbBeats)
            {
                newBar++;
                newBeat -= nbBeats;
            }
            if (cli instanceof CLI_Section section)
            {
                if (bar > 0)
                {
                    try
                    {
                        cls.moveSection(section, newBar);
                    } catch (UnsupportedEditException ex)
                    {
                        // Should never happen
                        Exceptions.printStackTrace(ex);
                    }
                }
            } else
            {
                cls.moveItem(cli, new Position(newBar, newBeat));
            }
        }
    }


    /**
     * Get a new song with the lead sheet linearized ("unfolded") according to the song structure.
     * <p>
     * Returns a song where each chord leadsheet Section represents only one SongPart. Linearized section name has the form
     * "original_section_nameSECTION_COPY_DELIMITER_CHARnumber", eg "chorus#1".
     *
     * @param song
     * @param register If true register the created song
     * @return
     */
    static public Song getLinearizedSong(Song song, boolean register)
    {
        if (song == null)
        {
            throw new IllegalArgumentException("song");
        }


        var cls = song.getChordLeadSheet();
        var ss = song.getSongStructure();
        if (ss.getSongParts().isEmpty())
        {
            // Special case
            return SongFactory.getInstance().getCopy(song, false, register);
        }


        // Create an empty song with the right leadsheet size
        var newSong = SongFactory.getInstance().createEmptySong(song.getName(), ss.getSizeInBars(), "A", TimeSignature.FOUR_FOUR, null);
        var newCls = newSong.getChordLeadSheet();
        var resSs = newSong.getSongStructure();
        try
        {
            // Remove all SongParts
            resSs.removeSongParts(resSs.getSongParts());
        } catch (UnsupportedEditException ex)
        {
            // Should never happen as we remove everything
            Exceptions.printStackTrace(ex);
        }


        // The created song parts
        List<SongPart> newSpts = new ArrayList<>();


        // Fill it from the original song data
        int sectionCounter = 1;
        for (SongPart spt : ss.getSongParts())
        {
            var parentCliSection = spt.getParentSection();
            int barIndex = spt.getStartBarIndex();


            CLI_Section resCliSection;


            // Update the initial section or create the corresponding parent section
            if (barIndex == 0)
            {
                resCliSection = newCls.getSection(0);
                newCls.setSectionName(resCliSection, parentCliSection.getData().getName() + SECTION_COPY_DELIMITER_CHAR + sectionCounter);
                try
                {
                    newCls.setSectionTimeSignature(resCliSection, parentCliSection.getData().getTimeSignature());
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen since we copy a valid song
                    Exceptions.printStackTrace(ex);
                }
            } else
            {
                // Create it
                String originalSectionName = parentCliSection.getData().getName();
                String name = originalSectionName + SECTION_COPY_DELIMITER_CHAR + sectionCounter;
                resCliSection = CLI_Factory.getDefault().createSection(name, parentCliSection.getData().getTimeSignature(), barIndex, null);
                resCliSection.getClientProperties().set(parentCliSection.getClientProperties());
                try
                {
                    resCliSection = newCls.addSection(resCliSection);
                    // The section creation automatically creates a SongPart, remove it
                    resSs.removeSongParts(resSs.getSongParts());
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen since we copy a valid song
                    Exceptions.printStackTrace(ex);
                }

            }


            // Fill the corresponding section with chord symbols copies
            for (var cliCs : cls.getItems(parentCliSection, CLI_ChordSymbol.class))
            {
                var pos = cliCs.getPosition();
                int resBar = barIndex + pos.getBar() - parentCliSection.getPosition().getBar();
                var cliCsCopy = cliCs.getCopy(null, new Position(resBar, pos.getBeat()));
                newCls.addItem(cliCsCopy);
            }


            // Create the corresponding SongPart
            SongPart resSpt = spt.getCopy(null, barIndex, spt.getNbBars(), resCliSection);
            newSpts.add(resSpt);

            sectionCounter++;

        }


        // Add all SongParts in one shot to avoid problem with AdaptedRhythms
        try
        {
            resSs.addSongParts(newSpts);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen since copy of existing song
            Exceptions.printStackTrace(ex);
        }

        // Copy client properties
        newSong.getClientProperties().set(song.getClientProperties());


        if (register)
        {
            SongFactory.getInstance().registerSong(newSong);
        }
        return newSong;
    }

    /**
     * Get a new song with a simplified lead sheet.
     * <p>
     * <p>
     * Created song is registered.
     *
     * @param song
     * @param register If true register the created song
     * @return
     */
    static public Song getSimplifiedLeadSheet(Song song, boolean register)
    {
        if (song == null)
        {
            throw new IllegalArgumentException("song");
        }

        // Create a full copy to preserve links between SongParts and Sections
        Song resSong = SongFactory.getInstance().getCopy(song, false, register);
        ChordLeadSheet resCls = resSong.getChordLeadSheet();


        // Get a working simplified copy and use it to update the new leadsheet
        ChordLeadSheet simplifiedCls = ClsUtilities.getSimplified(song.getChordLeadSheet());


        // Remove all chord symbols 
        for (var item : resCls.getItems(CLI_ChordSymbol.class))
        {
            resCls.removeItem(item);
        }

        // Copy chord symboles from the simplified cls
        for (var item : simplifiedCls.getItems(CLI_ChordSymbol.class))
        {
            resCls.addItem(item);
        }

        simplifiedCls.cleanup();

        return resSong;
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.song.api;

import java.util.List;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

/**
 * Song utilities methods.
 */
public class SongUtils
{

    /**
     * Half the chord leadsheet of the specified song.
     * <p>
     * Move all chord symbols and sections accordingly.
     *
     * @param song
     */
    static public void halfChordLeadsheet(Song song)
    {
        ChordLeadSheet cls = song.getChordLeadSheet();
        for (ChordLeadSheetItem<?> cli : cls.getItems())
        {
            int bar = cli.getPosition().getBar();
            float beat = cli.getPosition().getBeat();
            int nbBeats = cls.getSection(bar).getData().getTimeSignature().getNbNaturalBeats();
            int newBar = bar / 2;
            float newBeat = (bar % 2 == 0) ? beat / 2f : (nbBeats + beat) / 2f;
            if (cli instanceof CLI_Section)
            {
                if (bar > 0)
                {
                    CLI_Section destSection = cls.getSection(newBar);
                    CLI_Section section = (CLI_Section) cli;
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
        int size = cls.getSize();
        cls.setSize(size / 2 + size % 2);
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

        // Update size     
        cls.setSize(cls.getSize() * 2);

        // Move items
        List<ChordLeadSheetItem<?>> items = cls.getItems();
        for (int i = items.size() - 1; i >= 0; i--)
        {
            ChordLeadSheetItem<?> cli = items.get(i);
            int bar = cli.getPosition().getBar();
            float beat = cli.getPosition().getBeat();
            int nbBeats = cls.getSection(bar).getData().getTimeSignature().getNbNaturalBeats();
            int newBar = bar * 2;
            float newBeat = beat * 2;
            if (newBeat >= nbBeats)
            {
                newBar++;
                newBeat -= nbBeats;
            }
            if (cli instanceof CLI_Section)
            {
                CLI_Section section = (CLI_Section) cli;
                if (bar > 0)
                {
                    cls.moveSection(section, newBar);
                }
            } else
            {
                cls.moveItem(cli, new Position(newBar, newBeat));
            }
        }
    }
}

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
package org.jjazz.ui.cl_editor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.song.api.Song;
import static org.jjazz.ui.cl_editor.actions.Bundle.CTL_HalfLeadsheet;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Half the distance by 2 between chords, so chordleadsheet size is also divided by 2.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.halfleadsheet")
@ActionRegistration(displayName = "#CTL_HalfLeadsheet", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/Tools", position = 100)
        })
@Messages("CTL_HalfLeadsheet=Half leadsheet")
public class HalfLeadsheet implements ActionListener
{

    private final Song song;
    private static final String undoText = CTL_HalfLeadsheet();
    private static final Logger LOGGER = Logger.getLogger(HalfLeadsheet.class.getSimpleName());

    public HalfLeadsheet(Song context)
    {
        this.song = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert song != null;
        ChordLeadSheet cls = song.getChordLeadSheet();
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);
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
        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }
}

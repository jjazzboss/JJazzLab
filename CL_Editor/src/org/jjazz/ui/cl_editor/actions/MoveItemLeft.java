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
import java.util.List;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import static org.jjazz.ui.cl_editor.actions.Bundle.*;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.moveitemleft")
@ActionRegistration(displayName = "#CTL_MoveItemLeft")
@Messages("CTL_MoveItemLeft=Move item left")
public final class MoveItemLeft implements ActionListener
{

    private final List<ChordLeadSheetItem<?>> context;
    private String undoText = CTL_MoveItemLeft();

    public MoveItemLeft(List<ChordLeadSheetItem<?>> context)
    {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        assert !context.isEmpty() : "context=" + context;
        ChordLeadSheet cls = context.get(0).getContainer();
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);
        if (context.get(0) instanceof CLI_Section)
        {
            moveSections();
        } else if (context.get(0) instanceof CLI_ChordSymbol)
        {
            moveChordSymbols();
        } else
        {
            // Nothing
        }
        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }

    private void moveSections()
    {
        for (ChordLeadSheetItem<?> cli : ChordLeadSheetItem.Utilities.sortByPosition(context))
        {
            ChordLeadSheet model = cli.getContainer();
            int barIndex = cli.getPosition().getBar();
            if (barIndex > 1 && model.getSection(barIndex - 1).getPosition().getBar() < (barIndex - 1))
            {
                // No section on the previous bar, move ok   
                model.moveSection((CLI_Section) cli, barIndex - 1);
            }
        }
    }

    private void moveChordSymbols()
    {
        for (ChordLeadSheetItem<?> cli : ChordLeadSheetItem.Utilities.sortByPosition(context))
        {
            ChordLeadSheet model = cli.getContainer();
            Position pos = cli.getPosition();
            if (pos.getBar() > 0)
            {
                // No section on the previous bar, move ok   
                model.moveItem(cli, new Position(pos.getBar() - 1, pos.getBeat()));
            }
        }
    }
}

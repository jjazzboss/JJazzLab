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
import org.jjazz.harmony.ChordTypeDatabase;
import org.jjazz.harmony.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import static org.jjazz.ui.cl_editor.actions.Bundle.*;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.nextchordtype")
@ActionRegistration(displayName = "#CTL_NextChordType")
@Messages("CTL_NextChordType=Next chord type")
public final class NextChordType implements ActionListener
{

    private final List<CLI_ChordSymbol> context;
    private String undoText = CTL_NextChordType();

    public NextChordType(List<CLI_ChordSymbol> context)
    {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        ChordLeadSheet cls = context.get(0).getContainer();
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);
        for (CLI_ChordSymbol item : context)
        {
            ExtChordSymbol cs = item.getData();
            ChordRenderingInfo cri = cs.getRenderingInfo();
            ChordTypeDatabase ctd = ChordTypeDatabase.getInstance();
            int index = ctd.getChordTypeIndex(cs.getChordType());
            if (index >= 0)
            {
                int newIndex = (index == ctd.getSize() - 1) ? 0 : index + 1;
                ChordRenderingInfo newCri = new ChordRenderingInfo(cri, (StandardScaleInstance)null); // Discard scale
                ExtChordSymbol newCs = new ExtChordSymbol(cs.getRootNote(), cs.getBassNote(), ctd.getChordType(newIndex), newCri, cs.getAlternateChordSymbol(), cs.getAlternateFilter());
                item.getContainer().changeItem(item, newCs);
            }
        }
        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
    }
}

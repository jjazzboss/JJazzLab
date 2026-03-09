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
package org.jjazz.cl_editorimpl.actions;

import java.awt.event.ActionEvent;
import java.util.EnumSet;
import static javax.swing.Action.NAME;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_LoopRestartBar;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.harmony.api.Position;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;

/**
 * Set the loop restart bar.
 */
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.setlooprestartbar")
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1415),
        })
public final class SetLoopRestartBar extends CL_ContextAction
{

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_SetLoopRestartBar"));
        putValue(CL_ContextAction.LISTENING_TARGETS, EnumSet.of(ListeningTarget.BAR_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        var selBars = selection.getSelectedBars();
        assert selBars.size() == 1;
        var newBar = selBars.getFirst().getModelBarIndex();
        var oldCli = cls.getLoopRestartBarItem();
        var oldBar = oldCli.getPosition().getBar();
        if (newBar == oldBar)
        {
            return;
        }

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(getActionName());

        cls.moveItem(oldCli, new Position(newBar));

        um.endCEdit(getActionName());
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = false;
        if (selection.getSelectedBars().size() == 1 && selection.isBarSelectedWithinCls())
        {
            int bar = selection.getMinBarIndex();
            int oldBar = selection.getChordLeadSheet().getLoopRestartBarItem().getPosition().getBar();
            b = bar != oldBar;
        }
        setEnabled(b);
    }

    // ===================================================================================================================
    // Private methods
    // ===================================================================================================================    

}

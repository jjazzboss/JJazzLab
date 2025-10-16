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
import org.jjazz.cl_editor.api.CL_ContextAction;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.harmony.api.Note;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.cl_editor.api.CL_Selection;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.transposedown")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 410),
            @ActionReference(path = "Shortcuts", name = "D-DOWN")
        })
public final class TransposeDown extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_DOWN);
    private static final Logger LOGGER = Logger.getLogger(TransposeUp.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(TransposeUp.class, "CTL_TransposeDown"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION));
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        setEnabled(selection.isChordSymbolSelected());
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(getActionName());

        // Transpose up use FLAT            
        for (CLI_ChordSymbol cliCs : selection.getSelectedChordSymbols())
        {
            ExtChordSymbol ecs = cliCs.getData();
            ExtChordSymbol newEcs = ecs.getTransposedChordSymbol(-1, Note.Accidental.FLAT);
            cliCs.getContainer().changeItem(cliCs, newEcs);
        }

        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(getActionName());
    }


}

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
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.cl_editor.api.CL_Selection;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;

@ActionRegistration(displayName = "not_used", lazy = false)
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.setendbar")
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 205),
            @ActionReference(path = "Shortcuts", name = "D-E")
        })
public final class SetEndBar extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_E);
    private int endBar = -1;

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_SetEndBar"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.BAR_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(getActionName());

        try
        {
            cls.setSizeInBars(endBar + 1);
        } catch (UnsupportedEditException ex)
        {
            String msg = "Impossible to resize.\n" + ex.getLocalizedMessage();
            um.abortCEdit(getActionName(), msg);
            return;
        }

        um.endCEdit(getActionName());
    }


    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = false;
        endBar = -1;
        if (selection.getSelectedBars().size() == 1)
        {
            int bar = selection.getBarRange().from;
            b = bar != (getActiveChordLeadSheet().getSizeInBars() - 1);
            endBar = selection.getMinBarIndex();
        }
        setEnabled(b);
    }
}

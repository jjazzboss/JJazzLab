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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.removebar")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 400),
            @ActionReference(path = "Shortcuts", name = "S-DELETE")
        })
public class RemoveBar extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("shift DELETE");
    private static final Logger LOGGER = Logger.getLogger(RemoveBar.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_RemoveBar"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.BAR_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        int minBar = selection.getMinBarIndexWithinCls();
        int maxBar = selection.getMaxBarIndexWithinCls();
        int lastBar = selection.getChordLeadSheet().getSizeInBars() - 1;
        
        LOGGER.log(Level.FINE, "actionPerformed() minBar={0} cls={1}", new Object[]
        {
            minBar, cls
        });

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(getActionName());


        try
        {
            cls.deleteBars(minBar, Math.min(maxBar, lastBar));
        } catch (UnsupportedEditException ex)
        {
            String msg = "Impossible to remove bars.\n" + ex.getLocalizedMessage();
            um.abortCEdit(getActionName(), msg);
            return;
        }


        um.endCEdit(getActionName());
    }

    /**
     * Enable the action only if first selected bar is withing chordleadsheet, and not if all chordleadsheet selected.
     */
    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = false;
        if (selection.isContiguousBarboxSelectionWithinCls())
        {
            b = !isAllBarsSelected(selection);
        }
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
        setEnabled(b);
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        if (event instanceof SizeChangedEvent)
        {
            selectionChange(getSelection());
        }
    }

    private boolean isAllBarsSelected(CL_Selection selection)
    {
        int minBar = selection.getMinBarIndexWithinCls();
        int maxBar = selection.getMaxBarIndexWithinCls();
        int lastBar = selection.getChordLeadSheet().getSizeInBars() - 1;
        return minBar == 0 && maxBar == lastBar;
    }
}

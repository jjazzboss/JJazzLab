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
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.cl_editor.api.CL_ContextAction;
import static org.jjazz.cl_editor.api.CL_ContextAction.LISTENING_TARGETS;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.InsertBar")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/BarInsert", position = 100),
            @ActionReference(path = "Shortcuts", name ="I")
        })
public class InsertBar extends CL_ContextAction 
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("I");
    private int insertModelBarIndex;
    private int insertNbBars;
    private static final Logger LOGGER = Logger.getLogger(InsertBar.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_InsertBar"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(CL_ContextAction.ListeningTarget.BAR_SELECTION, CL_ContextAction.ListeningTarget.ACTIVE_CLS_CHANGES));        
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        assert insertModelBarIndex >= 0;
        InsertBarDialog dlg = InsertBarDialog.getInstance();
        dlg.preset(cls, insertModelBarIndex, insertNbBars);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        if (dlg.exitedOk())
        {
            JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(getActionName());
            cls.insertBars(insertModelBarIndex, dlg.getNbBars());
            JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(getActionName());
        }
        dlg.cleanup();
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = selection.isBarSelected();
        insertModelBarIndex = -1;
        insertNbBars = -1;
        if (b)
        {
            b = true;
            if (selection.isBarSelectedWithinCls())
            {
                insertModelBarIndex = selection.getMinBarIndexWithinCls();
                insertNbBars = selection.getSelectedBarIndexesWithinCls().size();
            } else
            {
                // Insert at the end of the leadsheet if no selected bar within the leadsheet
                insertModelBarIndex = selection.getChordLeadSheet().getSizeInBars();
                insertNbBars = selection.getSelectedBars().size();
            }
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
}

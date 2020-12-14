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

import org.jjazz.ui.cl_editor.api.CL_ContextActionListener;
import org.jjazz.ui.cl_editor.api.CL_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.util.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.windows.WindowManager;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.InsertBar")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 300)
        })
public class InsertBar extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private int insertModelBarIndex;
    private int insertNbBars;
    private final String undoText = ResUtil.getString(getClass(), "CTL_InsertBar");
    private static final Logger LOGGER = Logger.getLogger(InsertBar.class.getSimpleName());

    public InsertBar()
    {
        this(Utilities.actionsGlobalContext());
    }

    private InsertBar(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        LOGGER.log(Level.FINE, "InsertBar(context) context=" + context);   //NOI18N
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("I"));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        LOGGER.log(Level.FINE, "createContextAwareInstance()");   //NOI18N
        return new InsertBar(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert insertModelBarIndex >= 0;   //NOI18N
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        InsertBarDialog dlg = InsertBarDialog.getInstance();
        dlg.preset(cls, insertModelBarIndex, insertNbBars);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        if (dlg.exitedOk())
        {
            JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);
            cls.insertBars(insertModelBarIndex, dlg.getNbBars());
            JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
        }
        dlg.cleanup();
    }

    /**
     * Enable the action if a bar is selected.<br>
     */
    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = false;
        insertModelBarIndex = -1;
        insertNbBars = -1;
        if (selection.isBarSelected())
        {
            b = true;
            if (selection.isBarSelectedWithinCls())
            {
                insertModelBarIndex = selection.getMinBarIndexWithinCls();
                insertNbBars = selection.getSelectedBarIndexesWithinCls().size();
            } else
            {
                // Insert at the end of the leadsheet if no selected bar within the leadsheet
                insertModelBarIndex = selection.getChordLeadSheet().getSize();
                insertNbBars = selection.getSelectedBars().size();
            }
        }
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);   //NOI18N
        setEnabled(b);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }
}

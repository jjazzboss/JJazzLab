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
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.removebar")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 400),
        })
public class RemoveBar extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private final String undoText = ResUtil.getString(getClass(), "CTL_RemoveBar");
    private static final Logger LOGGER = Logger.getLogger(RemoveBar.class.getSimpleName());

    public RemoveBar()
    {
        this(Utilities.actionsGlobalContext());
        LOGGER.log(Level.FINE, "RemoveBar()");   //NOI18N
    }

    private RemoveBar(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("shift DELETE"));
        LOGGER.log(Level.FINE, "RemoveBar(context) context=" + context);   //NOI18N
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        LOGGER.log(Level.FINE, "createContextAwareInstance(context)");   //NOI18N
        return new RemoveBar(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        int minBar = selection.getMinBarIndexWithinCls();
        int maxBar = selection.getMaxBarIndexWithinCls();
        int lastBar = cls.getSizeInBars() - 1;


        LOGGER.log(Level.FINE, "actionPerformed() minBar=" + minBar + " cls=" + cls + " context=" + context);   //NOI18N

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(undoText);


        try
        {
            cls.deleteBars(minBar, Math.min(maxBar, lastBar));
        } catch (UnsupportedEditException ex)
        {
            String msg = "Impossible to remove bars.\n" + ex.getLocalizedMessage();
            um.handleUnsupportedEditException(undoText, msg);
            return;
        }


        um.endCEdit(undoText);
    }

    /**
     * Enable the action only if first selected bar is withing chordleadsheet, and not if all chordleadsheet selected.
     */
    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = false;
        ChordLeadSheet cls = selection.getChordLeadSheet();
        if (selection.isContiguousBarSelection() && (selection.getMinBarIndexWithinCls() > 0 || selection.getMaxBarIndexWithinCls() < cls.getSizeInBars() - 1))
        {
            b = true;
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

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
package org.jjazz.ui.ss_editor.actions;

import org.jjazz.ui.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ui.ss_editor.api.SongPartCopyBuffer;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;
import static org.jjazz.ui.utilities.api.Utilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.util.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.cut")
@ActionRegistration(displayName = "#CTL_Cut", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1000, separatorBefore = 900),
        })
public class Cut extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = ResUtil.getString(getClass(), "CTL_Cut");

    public Cut()
    {
        this(Utilities.actionsGlobalContext());
    }

    private Cut(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_X));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Cut(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_SelectionUtilities selection = cap.getSelection();
        SongPartCopyBuffer buffer = SongPartCopyBuffer.getInstance();
        buffer.put(selection.getSelectedSongParts());
        SongStructure sgs = selection.getModel();
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        um.startCEdit(undoText);
        try
        {
            sgs.removeSongParts(selection.getSelectedSongParts());
        } catch (UnsupportedEditException ex)
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantCut");
            msg += "\n" + ex.getLocalizedMessage();
            um.handleUnsupportedEditException(undoText, msg);
            return;
        }
        um.endCEdit(undoText);
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        setEnabled(selection.isSongPartSelected() && selection.isContiguousSptSelection());
    }
}

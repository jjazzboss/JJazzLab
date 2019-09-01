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

import org.jjazz.ui.ss_editor.api.RL_ContextActionSupport;
import org.jjazz.ui.ss_editor.api.RL_ContextActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.ui.ss_editor.api.CopyBuffer;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.ss_editor.api.RL_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.RL_SelectionUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongPart;

@ActionID(category = "JJazz", id = "org.jjazz.ui.rl_editor.actions.copy")
@ActionRegistration(displayName = "#CTL_Copy", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 1100),
        })
@NbBundle.Messages("CTL_Copy=Copy")
public class Copy extends AbstractAction implements ContextAwareAction, RL_ContextActionListener
{

    private Lookup context;
    private RL_ContextActionSupport cap;
    private String undoText = CTL_Copy();

    public Copy()
    {
        this(Utilities.actionsGlobalContext());
    }

    private Copy(Lookup context)
    {
        this.context = context;
        cap = RL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Copy(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        RL_SelectionUtilities selection = cap.getSelection();
        CopyBuffer buffer = CopyBuffer.getInstance();
        List<SongPart> spts = selection.getSelectedSongParts();
        buffer.put(spts);
        // Force a selection change so that the Paste action enabled status can be updated 
        // (otherwise Paste action will not see that CopyBuffer is no more empty)
        SS_Editor editor = RL_EditorTopComponent.getActive().getRL_Editor();
        selection.unselectAll(editor);
        for (SongPart spt : spts)
        {
            editor.selectSongPart(spt, true);
        }
    }

    @Override
    public void selectionChange(RL_SelectionUtilities selection)
    {
        setEnabled(selection.isSongPartSelected() && selection.isOneSectionSptSelection());
    }

}

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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.ui.flatcomponents.FlatTextEditDialog;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;
import org.jjazz.util.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.editsptname")
@ActionRegistration(displayName = "#CTL_EditSptName", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 50)
        })
public class EditSptName extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = ResUtil.getString(getClass(), "CTL_EditSptName");
    private static final Logger LOGGER = Logger.getLogger(EditSptName.class.getSimpleName());

    public EditSptName()
    {
        this(Utilities.actionsGlobalContext());
    }

    public EditSptName(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ENTER"));
        LOGGER.log(Level.FINE, "constructor called");   //NOI18N
        selectionChange(cap.getSelection());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_SelectionUtilities selection = cap.getSelection();
        LOGGER.log(Level.FINE, "actionPerformed() selection=" + selection.toString());   //NOI18N
        List<SongPart> spts = selection.getIndirectlySelectedSongParts();
        SongPart spt0 = spts.get(0);
        SongStructure sgs = selection.getModel();
        FlatTextEditDialog dlg = FlatTextEditDialog.getInstance();
        adjustDialogPosition(dlg, spt0);
        String name = spt0.getName();
        dlg.setText(name);
        dlg.setColumns(Math.max(name.length() + 2, 8));
        dlg.setVisible(true);
        String text = dlg.getText().trim();
        if (dlg.isExitOk() && text.length() > 0 && !(spts.size() == 1 && text.equals(spt0.getName())))
        {
            JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(undoText);
            for (SongPart spt : spts)
            {
                sgs.setSongPartsName(Arrays.asList(spt), text);
            }
            JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(undoText);
        }
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        boolean b = selection.isContiguousSptSelection();
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);   //NOI18N
        setEnabled(b);
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new EditSptName(context);
    }

    private void adjustDialogPosition(JDialog dialog, SongPart spt)
    {
        SS_Editor editor = SS_EditorTopComponent.getActive().getSS_Editor();
        Rectangle r = editor.getSptViewerRectangle(spt);
        Point p = r.getLocation();
        //int x = p.x - ((dialog.getWidth() - r.width) / 2);
        int x = p.x;
        int y = p.y - dialog.getHeight();
        dialog.setLocation(Math.max(x, 0), Math.max(y, 0));
    }

}

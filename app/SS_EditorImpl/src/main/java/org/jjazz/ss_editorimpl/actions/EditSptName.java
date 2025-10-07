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
package org.jjazz.ss_editorimpl.actions;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.flatcomponents.api.FlatTextEditDialog;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextAction;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.editsptname")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 50)
        })
public class EditSptName extends SS_ContextAction
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("ENTER");
    private static final Logger LOGGER = Logger.getLogger(EditSptName.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_EditSptName"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        LOGGER.log(Level.FINE, "actionPerformed() selection={0}", selection.toString());
        List<SongPart> spts = selection.getIndirectlySelectedSongParts();
        SongPart spt0 = spts.get(0);
        SongStructure sgs = selection.getModel();
        FlatTextEditDialog dlg = FlatTextEditDialog.getInstance();
        dlg.setTextHorizontalAlignment(JTextField.LEADING);
        String name = spt0.getName();
        dlg.setTextNbColumns(Math.max(name.length() + 2, 8));
        dlg.setText(name);
        adjustDialogPosition(dlg, spt0);
        dlg.setVisible(true);
        String text = dlg.getText().trim();
        if (dlg.isExitOk() && text.length() > 0 && !(spts.size() == 1 && text.equals(spt0.getName())))
        {
            JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(getActionName());
            for (SongPart spt : spts)
            {
                sgs.setSongPartsName(Arrays.asList(spt), text);
            }
            JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(getActionName());
        }
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = selection.isSongPartSelected() && selection.isContiguousSptSelection();
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
        setEnabled(b);
    }

    private void adjustDialogPosition(JDialog dialog, SongPart spt)
    {
        SS_Editor editor = SS_EditorTopComponent.getActive().getEditor();
        Rectangle r = editor.getSptViewerRectangle(spt);
        Point p = r.getLocation();
        //int x = p.x - ((dialog.getWidth() - r.width) / 2);
        int x = p.x;
        int y = p.y - dialog.getHeight();
        dialog.setLocation(Math.max(x, 0), Math.max(y, 0));
    }

}

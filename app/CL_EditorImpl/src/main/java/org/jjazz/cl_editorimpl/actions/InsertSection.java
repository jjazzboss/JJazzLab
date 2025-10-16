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
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.cl_editor.api.CL_ContextAction;
import static org.jjazz.cl_editor.api.CL_ContextAction.LISTENING_TARGETS;
import org.jjazz.cl_editor.spi.Preset;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.cl_editor.spi.Preset.Type;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * Insert a section at the first selected bar.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.insertsection")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/BarInsert", position = 110),
            @ActionReference(path = "Shortcuts", name = "T")
        })
public class InsertSection extends CL_ContextAction
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("T");
    private static final Logger LOGGER = Logger.getLogger(InsertSection.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_InsertSection"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(CL_ContextAction.ListeningTarget.BAR_SELECTION, CL_ContextAction.ListeningTarget.ACTIVE_CLS_CHANGES));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        int barIndex = selection.getMinBarIndexWithinCls();
        Preset preset = new Preset(Type.SectionNameEdit, null, (char) 0);
        Edit.editBarWithDialog(editor, barIndex, preset, cls, getActionName());
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = false;
        // Enabled if one bar selected and no section change on that bar
        ChordLeadSheet cls = selection.getChordLeadSheet();
        if (selection.getSelectedBarIndexesWithinCls().size() == 1)
        {
            int barIndex = selection.getMinBarIndexWithinCls();
            var cliSection = cls.getSection(barIndex);
            b = cliSection.getPosition().getBar() != barIndex;
        }
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

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
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.ui.cl_editor.spi.Preset;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.cl_editor.spi.Preset.Type;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

/**
 * Insert a section at the first selected bar.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.insertsection")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 210),     // Right after Set end bar
        })
public class InsertSection extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private final String undoText = ResUtil.getString(getClass(), "CTL_InsertSection");
    private static final Logger LOGGER = Logger.getLogger(InsertSection.class.getSimpleName());

    public InsertSection()
    {
        this(Utilities.actionsGlobalContext());
    }

    private InsertSection(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new InsertSection(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        CL_Editor editor = CL_EditorTopComponent.getActive().getCL_Editor();
        int barIndex = selection.getMinBarIndexWithinCls();
        Preset preset = new Preset(Type.SectionNameEdit, null, (char) 0);
        Edit.editBarWithDialog(editor, barIndex, preset, cls, undoText);
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
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
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }

}

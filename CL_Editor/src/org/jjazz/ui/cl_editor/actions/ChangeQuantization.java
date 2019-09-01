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
import static javax.swing.Action.NAME;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.quantizer.Quantization;
import static org.jjazz.ui.cl_editor.actions.Bundle.*;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.changequantization")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1400, separatorBefore = 1390),
        })
@Messages("CTL_ChangeQuantization=Quantization...")
public class ChangeQuantization extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private String undoText = CTL_ChangeQuantization();
    private static final Logger LOGGER = Logger.getLogger(ChangeQuantization.class.getSimpleName());

    public ChangeQuantization()
    {
        this(Utilities.actionsGlobalContext());
        LOGGER.log(Level.FINE, "ChangeQuantization()");
    }

    private ChangeQuantization(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        LOGGER.log(Level.FINE, "ChangeQuantization(context) context=" + context);
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        LOGGER.log(Level.FINE, "createContextAwareInstance(context)");
        return new ChangeQuantization(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        CL_Editor editor = CL_EditorTopComponent.getActive().getCL_Editor();

        // Selection must contain bars belonging to one section
        CLI_Section section = cls.getSection(selection.getMinBarIndexWithinCls());
        Quantization q = editor.getDisplayQuantizationValue(section);
        LOGGER.log(Level.FINE, "actionPerformed() initialize dialog with section=" + section + " q=" + q);
        ChangeQuantizationDialog dialog = ChangeQuantizationDialog.getInstance();
        dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dialog.preset(section, q);
        dialog.setVisible(true);
        if (dialog.getExitStatus().equals(ChangeQuantizationDialog.ExitStatus.OK_CURRENT_SECTION))
        {
            q = dialog.getQuantization();
            editor.setDisplayQuantizationValue(section, q);
            LOGGER.log(Level.FINE, "actionPerformed() apply q=" + q + " for section=" + section);
        } else if (dialog.getExitStatus().equals(ChangeQuantizationDialog.ExitStatus.OK_ALL_SECTIONS))
        {
            q = dialog.getQuantization();
            LOGGER.log(Level.FINE, "actionPerformed() apply q=" + q + " for all sections");
            for (CLI_Section aSection : cls.getItems(CLI_Section.class))
            {
                editor.setDisplayQuantizationValue(aSection, q);
            }
        }
        dialog.cleanup();
    }

    /**
     * Enable the action only if all selected bars belong to only one section.
     */
    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        ChordLeadSheet cls = selection.getChordLeadSheet();
        boolean b = false;
        if (selection.isBarSelectedWithinCls())
        {
            CLI_Section section = cls.getSection(selection.getMinBarIndexWithinCls());
            b = (section == cls.getSection(selection.getMaxBarIndexWithinCls()));
        }
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);
        setEnabled(b);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }
}

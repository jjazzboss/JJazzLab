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
import static javax.swing.Action.NAME;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.changequantization")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1400, separatorBefore = 1390),
            @ActionReference(path = "Actions/Section", position = 2200)
        })
public class ChangeQuantization extends CL_ContextAction
{

    private static final Logger LOGGER = Logger.getLogger(ChangeQuantization.class.getSimpleName());


    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_ChangeQuantization"));
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.CLS_ITEMS_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES));        
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        CL_Editor editor = CL_EditorTopComponent.get(cls).getEditor();

        // Selection must contain bars belonging to one section
        CLI_Section section = cls.getSection(selection.getMinBarIndexWithinCls());
        Quantization q = CL_EditorClientProperties.getSectionUserQuantization(section);
        LOGGER.log(Level.FINE, "actionPerformed() initialize dialog with section={0} q={1}", new Object[]
        {
            section, q
        });


        // Prepare and show quantization dialog
        ChangeQuantizationDialog dialog = ChangeQuantizationDialog.getInstance();
        dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dialog.preset(section, q);
        dialog.setVisible(true);


        // Analyze result
        switch (dialog.getExitStatus())
        {
            case OK_CURRENT_SECTION ->
            {
                q = dialog.getQuantization();
                LOGGER.log(Level.FINE, "actionPerformed() apply q={0} for section={1}", new Object[]
                {
                    q, section
                });
                if (selection.isSectionSelected())
                {
                    for (var cliSec : selection.getSelectedSections())
                    {
                        CL_EditorClientProperties.setSectionUserQuantization(cliSec, q);
                    }
                } else
                {
                    CL_EditorClientProperties.setSectionUserQuantization(section, q);
                }
            }

            case OK_ALL_SECTIONS ->
            {
                q = dialog.getQuantization();
                LOGGER.log(Level.FINE, "actionPerformed() apply q={0} for all sections", q);
                for (CLI_Section aSection : cls.getItems(CLI_Section.class))
                {
                    CL_EditorClientProperties.setSectionUserQuantization(aSection, q);
                }
            }
            case CANCEL ->
            {
                // Nothing
            }
            default ->
            {
                throw new IllegalStateException("dialog.getExitStatus()=" + dialog.getExitStatus());
            }
        }


        if (!dialog.getExitStatus().equals(ChangeQuantizationDialog.ExitStatus.CANCEL))
        {
            Analytics.logEvent("Quantization Change", Analytics.buildMap("Value", q == null ? "null" : q.toString()));
            editor.getSongModel().setSaveNeeded(true);
        }


        dialog.cleanup();
    }

    /**
     * If bars are selected, enable the action only if all selected bars belong to only one section.
     */
    @Override
    public void selectionChange(CL_Selection selection)
    {
        ChordLeadSheet cls = selection.getChordLeadSheet();
        boolean b = false;
        if (selection.isBarSelectedWithinCls())
        {
            CLI_Section section = cls.getSection(selection.getMinBarIndexWithinCls());
            b = (section == cls.getSection(selection.getMaxBarIndexWithinCls()));
        } else if (selection.isSectionSelected())
        {
            b = true;
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

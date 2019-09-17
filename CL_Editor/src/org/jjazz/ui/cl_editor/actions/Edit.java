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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import static org.jjazz.ui.cl_editor.actions.Bundle.*;
import org.jjazz.ui.cl_editor.spi.CL_BarEditorDialog;
import org.jjazz.ui.cl_editor.spi.Preset;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.cl_editor.spi.SectionEditorDialog;
import org.jjazz.ui.cl_editor.spi.ChordSymbolEditorDialog;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 * Edit the selected item or bar.
 * <p>
 * Use the following services if available in the global lookup: <br>
 * - ChordSymbolEditorDialog <br>
 * - SectionEditorDialog <br>
 * otherwise use default bar editor for edit operations.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.edit")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 100),
            @ActionReference(path = "Actions/ChordSymbol", position = 100),
            @ActionReference(path = "Actions/Bar", position = 100),
        })
@Messages(
        {
            "CTL_Edit=Edit...",
            "ERR_ChangeSection=Impossible to set section"
        })
public class Edit extends AbstractAction implements ContextAwareAction, CL_ContextActionListener
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private String undoText = CTL_Edit();
    private static final Logger LOGGER = Logger.getLogger(Edit.class.getSimpleName());

    public Edit()
    {
        this(Utilities.actionsGlobalContext());
    }

    private Edit(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, CTL_Edit());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ENTER"));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Edit(context);
    }

    /**
     * @param e If action triggered by a key press, e.getActionCommand() provide the key pressed.
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        final ChordLeadSheet cls = selection.getChordLeadSheet();
        final CL_Editor editor = CL_EditorTopComponent.getActive().getCL_Editor();
        char key = (char) 0;
        LOGGER.log(Level.FINE, "e=" + e);
        if (e != null && !e.getActionCommand().equalsIgnoreCase("Edit") && !e.getActionCommand().equalsIgnoreCase("Enter"))
        {
            key = e.getActionCommand().charAt(0);

        }
        if (selection.isItemSelected())
        {
            ChordLeadSheetItem<?> item = selection.getSelectedItems().get(0);
            int barIndex = item.getPosition().getBar();
            if (item instanceof CLI_ChordSymbol)
            {
                CLI_ChordSymbol csItem = (CLI_ChordSymbol) item;
                ChordSymbolEditorDialog dialog = ChordSymbolEditorDialog.getDefault();
                if (dialog != null)
                {
                    // Use specific editor if service is provided
                    editCSWithDialog(dialog, csItem, key, cls);
                } else
                {
                    // Otherwise use the standard Bar dialog
                    editBarWithDialog(editor, barIndex, new Preset(Preset.Type.ChordSymbolEdit, item, key), cls);
                }
            } else if (item instanceof CLI_Section)
            {
                CLI_Section sectionItem = (CLI_Section) item;
                SectionEditorDialog dialog = SectionEditorDialog.getDefault();
                if (dialog != null)
                {
                    // Use specific editor if service is provided               
                    editSectionWithDialog(dialog, sectionItem, key, cls);
                } else
                {
                    // Otherwise use the standard Bar dialog
                    editBarWithDialog(editor, barIndex, new Preset(Preset.Type.SectionNameEdit, item, (char) 0), cls);
                }
            }
        } else
        {
            assert selection.isBarSelectedWithinCls() == true : "selection=" + selection;
            int modelBarIndex = selection.getMinBarIndexWithinCls();
            editBarWithDialog(editor, modelBarIndex, new Preset(Preset.Type.BarEdit, null, key), cls);
        }
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b;
        if (selection.isItemSelected())
        {
            b = selection.getSelectedItems().size() == 1;
        } else
        {
            b = selection.getSelectedBarIndexesWithinCls().size() == 1;
        }
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);
        setEnabled(b);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }

    private void editSectionWithDialog(final SectionEditorDialog dialog, final CLI_Section sectionItem, final char key, final ChordLeadSheet cls)
    {
        // Use specific editor if service is provided
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                dialog.preset(sectionItem, key);
                dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                dialog.setVisible(true);
                if (dialog.exitedOk())
                {
                    Section newSection = dialog.getNewData();
                    assert newSection != null;
                    JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
                    try
                    {
                        cls.setSectionName(sectionItem, newSection.getName());
                        cls.setSectionTimeSignature(sectionItem, newSection.getTimeSignature());
                    } catch (UnsupportedEditException ex)
                    {
                        String msg = ERR_ChangeSection() + ": " + sectionItem.getData() + ".\n" + ex.getLocalizedMessage();
                        um.handleUnsupportedEditException(undoText, msg);
                    }
                }
            }
        };
        // IMPORTANT: Dialog must be shown using invokeLater(), otherwise we have the problem of random double chars 
        // when action is triggered by a key (InputMap/ActionMap) and key is used in the dialog.      
        // See complete explanation in my question on stackoverflow:
        // https://stackoverflow.com/questions/53073707/my-jdialog-sometimes-receives-a-redundant-keystroke-from-the-calling-app-code      
        SwingUtilities.invokeLater(run);
    }

    private void editCSWithDialog(final ChordSymbolEditorDialog dialog, final CLI_ChordSymbol csItem, final char key, final ChordLeadSheet cls)
    {
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                // Use specific editor if service is provided              
                Position pos = csItem.getPosition();
                dialog.preset("Edit Chord Symbol - " + csItem.getData() + " - bar:" + (pos.getBar() + 1) + " beat:" + pos.getBeatAsUserString(), csItem, key, true);
                dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                dialog.setVisible(true);
                if (dialog.exitedOk())
                {
                    ExtChordSymbol newCs = dialog.getData();
                    assert newCs != null;
                    JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
                    um.startCEdit(undoText);
                    cls.changeItem(csItem, newCs);
                    um.endCEdit(undoText);
                }
                dialog.cleanup();
            }
        };
        // IMPORTANT: Dialog must be shown using invokeLater(), otherwise we have the problem of random double chars
        // when action is triggered by a key (InputMap/ActionMap) and key is used in the dialog.      
        // See complete explanation in my question on stackoverflow:
        // https://stackoverflow.com/questions/53073707/my-jdialog-sometimes-receives-a-redundant-keystroke-from-the-calling-app-code      
        SwingUtilities.invokeLater(run);
    }

    private void editBarWithDialog(final CL_Editor editor, final int barIndex, final Preset preset, final ChordLeadSheet cls)
    {
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                // Prepare dialog
                final CL_BarEditorDialog dialog = CL_BarEditorDialog.getDefault();
                dialog.preset(preset, editor.getModel(), barIndex);
                adjustDialogPosition(dialog, barIndex);
                dialog.setVisible(true);
                LOGGER.fine("editBarWithDialog() right after setVisible(true)");
                if (!dialog.isExitOk())
                {
                    dialog.cleanup();
                    return;
                }

                JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
                um.startCEdit(undoText);

                // Manage section change
                CLI_Section resultSection = dialog.getSection();
                if (resultSection != null)
                {
                    CLI_Section currentSection = cls.getSection(barIndex);
                    if (currentSection.getPosition().getBar() == barIndex)
                    {
                        // Update existing section
                        try
                        {
                            cls.setSectionName(currentSection, resultSection.getData().getName());
                            cls.setSectionTimeSignature(currentSection, resultSection.getData().getTimeSignature());
                        } catch (UnsupportedEditException ex)
                        {
                            String msg = ERR_ChangeSection() + ": " + resultSection.getData() + ".\n" + ex.getLocalizedMessage();
                            um.handleUnsupportedEditException(undoText, msg);
                            // There are other things to do, restart edit
                            um.startCEdit(undoText);
                        }
                    } else
                    {
                        // Add new section
                        try
                        {
                            cls.addSection(resultSection);
                        } catch (UnsupportedEditException ex)
                        {
                            String msg = ERR_ChangeSection() + ": " + resultSection.getData() + ".\n" + ex.getLocalizedMessage();
                            um.handleUnsupportedEditException(undoText, msg);
                            // There are other things to do, restart edit
                            um.startCEdit(undoText);
                        }
                    }
                }

                // Manage added/removed/changed items
                List<ChordLeadSheetItem<?>> resultAddedItems = dialog.getAddedItems();
                for (ChordLeadSheetItem<?> item : resultAddedItems)
                {
                    cls.addItem(item);
                }
                List<ChordLeadSheetItem<?>> resultRemovedItems = dialog.getRemovedItems();
                for (ChordLeadSheetItem<?> item : resultRemovedItems)
                {
                    cls.removeItem(item);
                }
                Map<CLI_ChordSymbol, ExtChordSymbol> map = dialog.getUpdatedChordSymbols();
                for (CLI_ChordSymbol cliCs : map.keySet())
                {
                    cls.changeItem(cliCs, map.get(cliCs));
                }

                um.endCEdit(undoText);

                // Go to next bar if chords have changed
                boolean chordSymbolChange = !resultAddedItems.isEmpty() || !resultRemovedItems.isEmpty() || !map.isEmpty();
                if (barIndex < cls.getSize() - 1 && chordSymbolChange)
                {
                    CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());
                    selection.unselectAll(editor);
                    editor.setFocusOnBar(barIndex + 1);
                    editor.selectBars(barIndex + 1, barIndex + 1, true);
                }

                dialog.cleanup();
            }
        };
        // IMPORTANT: Dialog must be shown using invokeLater(), otherwise we have the problem of random double chars
        // when action is triggered by a key (InputMap/ActionMap) and key is used in the dialog.      
        // See complete explanation in my question on stackoverflow:
        // https://stackoverflow.com/questions/53073707/my-jdialog-sometimes-receives-a-redundant-keystroke-from-the-calling-app-code
        SwingUtilities.invokeLater(run);
    }

    private void adjustDialogPosition(JDialog dialog, int barIndex)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getCL_Editor();
        Rectangle r = editor.getBarRectangle(barIndex);
        Point p = r.getLocation();
        int x = p.x - ((dialog.getWidth() - r.width) / 2);
        int y = p.y - dialog.getHeight();
        dialog.setLocation(Math.max(x, 0), Math.max(y, 0));
    }
}

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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import static org.jjazz.ui.cl_editor.actions.Bundle.*;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

/**
 * Allow user to select a timesignature in a JPopupMenu when a CLI_Section is selected.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.SetTimeSignature")
@ActionRegistration(displayName = "#CTL_SetTimeSignature", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 200, separatorAfter = 250)
        })
@Messages(
        {
            "CTL_SetTimeSignature=Set time signature",
            "ERR_SetTimeSignature=Impossible to set time signature"
        })
public final class SetTimeSignature extends AbstractAction implements Presenter.Popup
{

    private MyDynamicMenu menu;
    private final String undoText = CTL_SetTimeSignature();
    private static final Logger LOGGER = Logger.getLogger(SetTimeSignature.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Useless
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (menu == null)
        {
            menu = new MyDynamicMenu();
            updateMenu(menu);
        }
        return menu;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================    
    private void updateMenu(JMenu menu)
    {
        // Prepare the TimeSignature subMenu
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        menu.removeAll();
        for (final TimeSignature ts : TimeSignature.values())
        {
            JMenuItem mi = new JMenuItem(ts.toString());
            mi.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    CL_Editor editor = CL_EditorTopComponent.getActive().getCL_Editor();
                    ChordLeadSheet cls = editor.getModel();
                    JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
                    um.startCEdit(undoText);
                    try
                    {
                        changeTimeSignature(editor, ts);
                    } catch (UnsupportedEditException ex)
                    {
                        String msg = ERR_SetTimeSignature() + ": " + ts + ".\n" + ex.getLocalizedMessage();
                        um.handleUnsupportedEditException(undoText, msg);
                        return;
                    }
                    um.endCEdit(undoText);
                }
            });
            boolean b = rdb.getDefaultRhythm(ts) != null;
            mi.setEnabled(b);
            menu.add(mi);
        }
    }

    private void changeTimeSignature(CL_Editor editor, TimeSignature ts) throws UnsupportedEditException
    {
        CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());


        if (selection.isItemSelected() && (selection.getSelectedItems().get(0) instanceof CLI_Section))
        {

            List<ChordLeadSheetItem<?>> items;
            var selItems = selection.getSelectedItems();
            var allSections = editor.getModel().getItems(CLI_Section.class);
            CLI_Section cliSection = (CLI_Section) selItems.get(0);


            if (selItems.size() == 1 && cliSection.getPosition().getBar() == 0 && allSections.size() > 1)
            {
                // If several sections but only first bar section changed, propose to change the whole song                

                if (ts.equals(cliSection.getData().getTimeSignature()))
                {
                    return;
                }
                
                String msg = "Set time signature " + ts.toString() + " for the whole song ?";
                NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_CANCEL_OPTION);
                
                
                Object result = DialogDisplayer.getDefault().notify(d);
                if (NotifyDescriptor.YES_OPTION == result)
                {
                    items = new ArrayList<>(allSections);

                } else if (NotifyDescriptor.NO_OPTION == result)
                {
                    items = Arrays.asList(cliSection);

                } else
                {
                    return;
                }
            } else
            {
                items = selItems;
            }


            for (ChordLeadSheetItem<?> item : items)
            {
                editor.getModel().setSectionTimeSignature((CLI_Section) item, ts);
            }
        }
    }
    // ============================================================================================= 
    // Private class
    // =============================================================================================    

    class MyDynamicMenu extends JMenu implements ChangeListener
    {

        public MyDynamicMenu()
        {
            super(CTL_SetTimeSignature());
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            rdb.addChangeListener(this);
        }

        @Override
        public void stateChanged(ChangeEvent e)
        {
            // We can be outside the EDT
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            if (e.getSource() == rdb)
            {
                Runnable run = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        updateMenu(MyDynamicMenu.this);
                    }
                };
                org.jjazz.ui.utilities.Utilities.invokeLaterIfNeeded(run);
            }
        }
    }
}

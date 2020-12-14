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
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.harmony.ChordType;
import org.jjazz.harmony.ChordTypeDatabase;
import org.jjazz.harmony.StandardScaleInstance;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.util.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

/**
 * To be used in CLI_ChordSymbol context popup menu.
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.SetChordType")
@ActionRegistration(displayName = "#CTL_SetChordType", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ChordSymbol", position = 200)
        })
@Messages(
        {

        })
public final class SetChordType extends AbstractAction implements Presenter.Menu, Presenter.Popup
{

    private final String undoText = ResUtil.getString(getClass(), "CTL_SetChordType");

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Useless
    }

    @Override
    public JMenuItem getPopupPresenter()
    {
        return getMenuPresenter();
    }

    @Override
    public JMenuItem getMenuPresenter()
    {
        // Need a fresh instance! It's ok since it is called only once for each use in the UI
        // (menu UI items can't be reused on multiple menus, Container.add() allows only one parent component
        return buildMenu();
    }

    private JMenu buildMenu()
    {
        // Prepare the ChordType subMenu
        JMenu menu = new JMenu();
        menu.setText(ResUtil.getString(getClass(), "CTL_SetChordType"));
        JMenu menuMajor = new JMenu(ResUtil.getString(getClass(), "CTL_Major"));
        JMenu menuMinor = new JMenu(ResUtil.getString(getClass(), "CTL_Minor"));
        JMenu menuSeventh = new JMenu(ResUtil.getString(getClass(), "CTL_Seventh"));
        JMenu menuDiminished = new JMenu(ResUtil.getString(getClass(), "CTL_Diminished"));
        JMenu menuSus4 = new JMenu(ResUtil.getString(getClass(), "CTL_Sus4"));

        menu.add(menuMajor);
        menu.add(menuSeventh);
        menu.add(menuMinor);
        menu.add(menuDiminished);
        menu.add(menuSus4);

        ChordTypeDatabase ctdb = ChordTypeDatabase.getInstance();
        for (final ChordType ct : ctdb.getChordTypes())
        {
            JMenuItem mi = new JMenuItem((ct.toString().length() == 0) ? " " : ct.toString());
            mi.setToolTipText(ct.toDegreeString());
            mi.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    CL_Editor editor = CL_EditorTopComponent.getActive().getCL_Editor();
                    ChordLeadSheet cls = editor.getModel();
                    JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(undoText);
                    changeChordType(editor, ct);
                    JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(undoText);
                }
            });

            switch (ct.getFamily())
            {
                case MAJOR:
                    menuMajor.add(mi);
                    break;
                case MINOR:
                    menuMinor.add(mi);
                    break;
                case SEVENTH:
                    menuSeventh.add(mi);
                    break;
                case DIMINISHED:
                    menuDiminished.add(mi);
                    break;
                case SUS:
                    menuSus4.add(mi);
                    break;
                default:
                    throw new IllegalStateException("ct=" + ct);   //NOI18N
            }
        }
        return menu;
    }

    private void changeChordType(CL_Editor editor, ChordType ct)
    {
        CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());
        if (selection.isItemSelected() && (selection.getSelectedItems().get(0) instanceof CLI_ChordSymbol))
        {
            for (ChordLeadSheetItem<?> item : selection.getSelectedItems())
            {
                CLI_ChordSymbol cliCs = (CLI_ChordSymbol) item;
                ExtChordSymbol oldCs = cliCs.getData();
                ChordRenderingInfo cri = oldCs.getRenderingInfo();
                ChordRenderingInfo newCri = new ChordRenderingInfo(cri, (StandardScaleInstance) null); // Discard scale             
                ExtChordSymbol newCs = new ExtChordSymbol(oldCs.getRootNote(), oldCs.getBassNote(), ct, newCri, oldCs.getAlternateChordSymbol(), oldCs.getAlternateFilter());
                editor.getModel().changeItem(cliCs, newCs);
            }
        }
    }
}

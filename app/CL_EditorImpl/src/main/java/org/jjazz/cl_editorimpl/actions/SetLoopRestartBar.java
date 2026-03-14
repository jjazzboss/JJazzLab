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
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import org.jjazz.chordleadsheet.api.item.CLI_LoopRestartBar;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.spi.item.CLI_Factory;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.harmony.api.Position;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;


/**
 * Toggle selected bar as restart bar in playback loop mode.
 * <p>
 * Action has no keyboard shortcut and is only showed in transient popup menus, no need for CL_ContextAction.
 */
@ActionRegistration(displayName = "not_used", lazy = false) // lazy can't be true because of Presenter.Popup implementation
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.setlooprestartbar")
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1415),
        })
public final class SetLoopRestartBar extends AbstractAction implements Presenter.Popup, ContextAwareAction
{

    private JCheckBoxMenuItem checkBoxMenuItem;
    private CL_Editor editor;
    private int selBar;
    private CLI_LoopRestartBar cliLoopRestart = null;
    private static final Logger LOGGER = Logger.getLogger(SetLoopRestartBar.class.getSimpleName());

    public SetLoopRestartBar()
    {
        // Not used besides for creating the ContextAwareAction
    }

    public SetLoopRestartBar(Lookup context)
    {
        Objects.requireNonNull(context);
        putValue(NAME, ResUtil.getString(getClass(), "CTL_LoopRestartBar"));
        checkBoxMenuItem = new JCheckBoxMenuItem(this);


        var selection = new CL_Selection(context);
        CL_EditorTopComponent tc = CL_EditorTopComponent.get(selection.getChordLeadSheet());
        assert tc != null : "cls=" + selection.getChordLeadSheet();
        editor = tc.getEditor();


        boolean b = selection.getSelectedBars().size() == 1 && selection.isBarSelectedWithinCls();
        setEnabled(b);


        b = false;
        selBar = selection.getMinBarIndexWithinCls();
        if (selBar >= 0)
        {
            cliLoopRestart = editor.getSongModel().getChordLeadSheet().getLoopRestartBarItem();
            int restartBar = cliLoopRestart == null ? 0 : cliLoopRestart.getPosition().getBar();
            b = selBar == restartBar;
        }
        checkBoxMenuItem.setSelected(b);
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new SetLoopRestartBar(lkp);
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        assert selBar != -1;
        var cls = editor.getSongModel().getChordLeadSheet();
        var actionName = (String) getValue(NAME);

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(actionName);


        if (cliLoopRestart == null)
        {
            // Add  CLI_LoopRestartBar
            cliLoopRestart = CLI_Factory.getDefault().createLoopRestartBar(selBar);
            cls.addItem(cliLoopRestart);

        } else if (selBar != cliLoopRestart.getPosition().getBar())
        {
            // Move CLI_LoopRestartBar
            cls.moveItem(cliLoopRestart, new Position(selBar));

        } else
        {
            // Remove CLI_LoopRestartBar
            cls.removeItem(cliLoopRestart);

        }

        um.endCEdit(actionName);
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        return checkBoxMenuItem;
    }

    // ======================================================================
    // Private methods
    // ======================================================================   

    private boolean isSelected(CL_Selection selection)
    {
        boolean b = false;

        return b;
    }

}

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;

/**
 * Action menu to set time signature of sections.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.SetTimeSignature")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 200)
        })
public final class SetTimeSignatureActionMenu extends AbstractAction implements Presenter.Popup, ContextAwareAction
{

    private JMenu menu;
    private static final Logger LOGGER = Logger.getLogger(SetTimeSignatureActionMenu.class.getSimpleName());

    public SetTimeSignatureActionMenu()
    {
        // Not used besides for creating the ContextAwareAction
    }

    public SetTimeSignatureActionMenu(Lookup context)
    {
        Objects.requireNonNull(context);
        menu = new JMenu(ResUtil.getString(getClass(), "CTL_SetTimeSignature"));


        var selection = new CL_Selection(context);
        boolean b = selection.isSectionSelected();
        setEnabled(b);
        menu.setEnabled(b);
        if (!b)
        {
            return;
        }

        prepareMenu(menu, selection);
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new SetTimeSignatureActionMenu(lkp);
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        // Not used
    }

    /**
     * Change the time signature of the specified sections.
     * <p>
     * If there is only 1 section and it's the initial section, ask user if he wants to apply the setting to all sections.
     *
     * @param cls
     * @param ts
     * @param cliSections
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    static public void changeTimeSignaturePossiblyForWholeSong(ChordLeadSheet cls, TimeSignature ts, List<CLI_Section> cliSections) throws UnsupportedEditException
    {
        if (cls == null || ts == null || cliSections == null)
        {
            throw new IllegalArgumentException("cls=" + cls + " ts=" + ts + " cliSections=" + cliSections);
        }
        if (cliSections.isEmpty())
        {
            return;
        }

        var allSections = cls.getItems(CLI_Section.class);
        CLI_Section cliSection = cliSections.get(0);
        List<CLI_Section> changedSections = new ArrayList<>();

        if (cliSections.size() == 1 && cliSection.getPosition().getBar() == 0 && allSections.size() > 1)
        {
            // If several sections but only first bar section changed, propose to change the whole song                

            if (ts.equals(cliSection.getData().getTimeSignature()))
            {
                return;
            }

            String msg = ResUtil.getString(SetTimeSignatureActionMenu.class, "CTL_SetTimeSignatureForWholeSong", ts);
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(d);

            if (NotifyDescriptor.YES_OPTION == result)
            {
                changedSections.addAll(allSections);

            } else if (NotifyDescriptor.NO_OPTION == result)
            {
                changedSections.add(cliSection);

            } else
            {
                return;
            }
        } else
        {
            changedSections.addAll(cliSections);
        }

        for (CLI_Section section : changedSections)
        {
            cls.setSectionTimeSignature(section, ts);
        }
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        return menu;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================    
    private void prepareMenu(JMenu menu, CL_Selection selection)
    {
        var rdb = RhythmDatabase.getDefault();
        var sortedTs = new ArrayList<>(rdb.getTimeSignatures());
        sortedTs.sort((ts1, ts2) -> Integer.compare(ts1.getUpper(), ts2.getUpper()));
        for (var ts : sortedTs)
        {
            JMenuItem mi = new JMenuItem(ts.toString());
            mi.addActionListener(ae -> updateSections(ts, selection));
            menu.add(mi);
        }
    }

    private void updateSections(TimeSignature ts, CL_Selection selection)
    {
        var cls = selection.getChordLeadSheet();
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);

        um.startCEdit(menu.getText());

        try
        {
            changeTimeSignaturePossiblyForWholeSong(cls, ts, selection.getSelectedSections());
        } catch (UnsupportedEditException ex)
        {
            String msg = ResUtil.getString(getClass(), "ERR_SetTimeSignature", ts);
            msg += "\n" + ex.getLocalizedMessage();
            um.abortCEdit(menu.getText(), msg);
            return;
        }

        um.endCEdit(menu.getText());

    }


}

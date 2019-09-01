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
package org.jjazz.ui.ss_editor.actions;

import org.jjazz.ui.ss_editor.api.RL_ContextActionSupport;
import org.jjazz.ui.ss_editor.api.RL_ContextActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.ui.ss_editor.api.RL_SelectionUtilities;
import org.jjazz.undomanager.JJazzUndoManager;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

@ActionID(category = "JJazz", id = "org.jjazz.ui.rl_editor.actions.insertspt")
@ActionRegistration(displayName = "#CTL_InsertSpt", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 300)
        })
@NbBundle.Messages(
        {
            "CTL_InsertSpt=Insert...",
            "ERR_InsertSpt=Impossible to insert Song Part"
        })

public class InsertSpt extends AbstractAction implements ContextAwareAction, RL_ContextActionListener
{

    private Lookup context;
    private RL_ContextActionSupport cap;
    private String undoText = CTL_InsertSpt();
    private static final Logger LOGGER = Logger.getLogger(InsertSpt.class.getSimpleName());

    public InsertSpt()
    {
        this(Utilities.actionsGlobalContext());
    }

    private InsertSpt(Lookup context)
    {
        this.context = context;
        cap = RL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("I"));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new InsertSpt(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        RL_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        List<SongPart> spts = sgs.getSongParts();
        ChordLeadSheet cls = null;
        cls = sgs.getParentChordLeadSheet();
        if (cls == null)
        {
            throw new IllegalStateException("sgs=" + sgs);
        }
        InsertSptDialog dlg = InsertSptDialog.getInstance();
        dlg.setClsModel(cls);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
        if (dlg.isExitOk())
        {
            CLI_Section parentSection = dlg.getParentSection();
            assert parentSection != null;
            Rhythm r = sgs.getDefaultRhythm(parentSection.getData().getTimeSignature());
            int startBarIndex = spts.get(selection.getMinStartSptIndex()).getStartBarIndex();
            int nbBars = cls.getSectionSize(parentSection);
            SongPart newSpt = sgs.createSongPart(r, startBarIndex, nbBars, parentSection);
            JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
            um.startCEdit(undoText);
            try
            {
                sgs.addSongPart(newSpt);
            } catch (UnsupportedEditException ex)
            {
                String msg = ERR_InsertSpt() + "\n" + ex.getLocalizedMessage();
                um.handleUnsupportedEditException(undoText, msg);
                return;
            }
            um.endCEdit(undoText);
        }
        dlg.cleanup();
    }

    @Override
    public void selectionChange(RL_SelectionUtilities selection)
    {
        boolean b = selection.isOneSectionSptSelection();
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);
        setEnabled(b);
    }
}

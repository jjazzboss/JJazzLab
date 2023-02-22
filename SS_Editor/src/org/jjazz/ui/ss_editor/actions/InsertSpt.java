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

import org.jjazz.ui.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.Arrays;
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
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;
import org.jjazz.util.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.insertspt")
@ActionRegistration(displayName = "#CTL_InsertSpt", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 300)
        })
public class InsertSpt extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = ResUtil.getString(getClass(), "CTL_InsertSpt");
    private static final Logger LOGGER = Logger.getLogger(InsertSpt.class.getSimpleName());

    public InsertSpt()
    {
        this(Utilities.actionsGlobalContext());
    }

    private InsertSpt(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
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
        // Prepare data
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        List<SongPart> spts = sgs.getSongParts();
        ChordLeadSheet cls = null;
        cls = sgs.getParentChordLeadSheet();
        if (cls == null)
        {
            throw new IllegalStateException("sgs=" + sgs);   
        }


        // Show dialog
        InsertSptDialog dlg = InsertSptDialog.getInstance();
        dlg.setClsModel(cls);
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);


        if (dlg.isExitOk())
        {
            // Perform change
            CLI_Section parentSection = dlg.getParentSection();
            assert parentSection != null;   


            // Create the new song part
            int startBarIndex = spts.get(selection.getMinStartSptIndex()).getStartBarIndex();
            Rhythm r = sgs.getRecommendedRhythm(parentSection.getData().getTimeSignature(), startBarIndex);
            int nbBars = cls.getBarRange(parentSection).size();
            SongPart newSpt = sgs.createSongPart(r, parentSection.getData().getName(), startBarIndex, nbBars, parentSection, true);


            JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
            um.startCEdit(undoText);


            try
            {
                sgs.addSongParts(Arrays.asList(newSpt));

            } catch (UnsupportedEditException ex)
            {
                String msg = ResUtil.getString(getClass(), "ERR_InsertSpt") + "\n" + ex.getLocalizedMessage();
                um.handleUnsupportedEditException(undoText, msg);
                return;
            }

            um.endCEdit(undoText);
        }
        dlg.cleanup();
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        boolean b = selection.isContiguousSptSelection();
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);   
        setEnabled(b);
    }
}

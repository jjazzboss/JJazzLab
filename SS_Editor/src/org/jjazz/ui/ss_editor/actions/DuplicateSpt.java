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
import javax.swing.SwingUtilities;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.ui.ss_editor.api.RL_SelectionUtilities;
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

@ActionID(category = "JJazz", id = "org.jjazz.ui.rl_editor.actions.duplicatespt")
@ActionRegistration(displayName = "#CTL_DuplicateSpt", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 320)
        })
@NbBundle.Messages("CTL_DuplicateSpt=Duplicate...")
public class DuplicateSpt extends AbstractAction implements ContextAwareAction, RL_ContextActionListener
{

    private Lookup context;
    private RL_ContextActionSupport cap;
    private String undoText = CTL_DuplicateSpt();
    private static final Logger LOGGER = Logger.getLogger(DuplicateSpt.class.getSimpleName());

    public DuplicateSpt()
    {
        this(Utilities.actionsGlobalContext());
    }

    private DuplicateSpt(Lookup context)
    {
        this.context = context;
        cap = RL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("D"));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new DuplicateSpt(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                RL_SelectionUtilities selection = cap.getSelection();
                SongStructure sgs = selection.getModel();
                List<SongPart> spts = selection.getIndirectlySelectedSongParts();
                DuplicateSptDialog dlg = DuplicateSptDialog.getInstance();

                dlg.setSptsModel(spts);

                dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                dlg.setVisible(
                        true);
                if (dlg.isExitOk())
                {
                    int nbCopies = dlg.getNbCopies();
                    int nextStartBarIndex = spts.get(spts.size() - 1).getStartBarIndex() + spts.get(spts.size() - 1).getNbBars();
                    JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(undoText);
                    for (int i = 0; i < nbCopies; i++)
                    {
                        for (SongPart spt : spts)
                        {
                            SongPart newSpt = spt.clone(null, nextStartBarIndex, spt.getNbBars(), spt.getParentSection());
                            try
                            {
                                sgs.addSongPart(newSpt);
                            } catch (UnsupportedEditException ex)
                            {
                                // We should not be here, we reuse an existing rhythm
                                throw new IllegalStateException("Unexpected 'UnsupportedEditException'.", ex);
                            }
                            nextStartBarIndex += spt.getNbBars();
                        }
                    }
                    JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(undoText);
                }

                dlg.cleanup();

            }
        };

        // IMPORTANT: Dialog must be shown using invokeLater(), otherwise we have the problem of action key which appears in the dialog
        // when action is triggered by a key (InputMap/ActionMap) and key is used in the dialog.      
        // See complete explanation in my question on stackoverflow:
        // https://stackoverflow.com/questions/53073707/my-jdialog-sometimes-receives-a-redundant-keystroke-from-the-calling-app-code
        SwingUtilities.invokeLater(run);
    }

    @Override
    public void selectionChange(RL_SelectionUtilities selection)
    {
        boolean b = selection.isOneSectionSptSelection();  // True whatever the selection SongParts or RhythmParameters
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);
        setEnabled(b);
    }
}

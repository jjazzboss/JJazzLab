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
package org.jjazz.ss_editorimpl.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextAction;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.duplicatespt")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 320)
        })
public class DuplicateSpt extends SS_ContextAction
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("D");
    private static final Logger LOGGER = Logger.getLogger(DuplicateSpt.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_DuplicateSpt"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        final String actionName = getActionName();
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                SongStructure sgs = selection.getModel();
                List<SongPart> spts = selection.getIndirectlySelectedSongParts();
                DuplicateSptDialog dlg = DuplicateSptDialog.getInstance();

                dlg.setSptsModel(spts);

                dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                dlg.setVisible(true);
                if (dlg.isExitOk())
                {
                    // Prepare the copies                    
                    int nbCopies = dlg.getNbCopies();
                    dlg.cleanup();
                    int nextStartBarIndex = spts.get(spts.size() - 1).getStartBarIndex() + spts.get(spts.size() - 1).getNbBars();
                    var sptCopies = new ArrayList<SongPart>();
                    for (int i = 0; i < nbCopies; i++)
                    {
                        for (SongPart spt : spts)
                        {
                            SongPart newSpt = spt.getCopy(null, nextStartBarIndex, spt.getNbBars(), spt.getParentSection());
                            sptCopies.add(newSpt);
                            nextStartBarIndex += spt.getNbBars();
                        }
                    }

                    JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
                    um.startCEdit(actionName);
                    try
                    {
                        sgs.addSongParts(sptCopies);
                    } catch (UnsupportedEditException ex)
                    {
                        // We should not be here, we reuse existing rhythms
                        String msg = ResUtil.getString(getClass(), "ERR_CantDuplicate");
                        msg += "\n" + ex.getLocalizedMessage();
                        um.abortCEdit(actionName, msg);
                        return;
                    }
                    um.endCEdit(actionName);
                }
            }
        };

        // IMPORTANT: Dialog must be shown using invokeLater(), otherwise we have the problem of action key which appears in the dialog
        // when action is triggered by a key (InputMap/ActionMap) and key is used in the dialog.      
        // See complete explanation in my question on stackoverflow:
        // https://stackoverflow.com/questions/53073707/my-jdialog-sometimes-receives-a-redundant-keystroke-from-the-calling-app-code
        SwingUtilities.invokeLater(run);
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = selection.isContiguousSptSelection();  // True whatever the selection SongParts or RhythmParameters
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
        setEnabled(b);
    }
}

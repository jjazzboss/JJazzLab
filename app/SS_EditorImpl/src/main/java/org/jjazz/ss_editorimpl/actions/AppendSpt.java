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
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.appendspt")
@ActionRegistration(displayName = "not_used", lazy = false) // always enabled action, lazy could be true but we want to show the accelerator key in the popupmenu
@ActionReferences(
        {
            @ActionReference(path = "Actions/SS_Editor", position = 300)
        })
public class AppendSpt extends AbstractAction
{

    public static final KeyStroke KEYSTROKE = getGenericControlKeyStroke(KeyEvent.VK_I);
    private final String undoText = ResUtil.getString(getClass(), "CTL_AppendSpt");
    private static final Logger LOGGER = Logger.getLogger(AppendSpt.class.getSimpleName());

    public AppendSpt()
    {
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KEYSTROKE);
    }

    @Override
    public boolean isEnabled()
    {
        return super.isEnabled();
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        LOGGER.log(Level.FINE, "actionPerformed() -- tc={0}", tc);
        if (tc == null)
        {
            return;
        }


        // Prepare data
        SongStructure sgs = tc.getEditor().getModel();
        List<SongPart> spts = sgs.getSongParts();
        ChordLeadSheet cls = sgs.getParentChordLeadSheet();
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


            // Create the song part
            int startBarIndex = 0;
            if (!spts.isEmpty())
            {
                SongPart lastSpt = spts.get(spts.size() - 1);
                startBarIndex = lastSpt.getStartBarIndex() + lastSpt.getNbBars();
            }

            // Choose rhythm
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
                // We should not be here, we reuse an existing rhythm
                String msg = ResUtil.getString(getClass(), "ERR_ImpossibleToAppend");
                msg += "\n" + ex.getLocalizedMessage();
                um.abortCEdit(undoText, msg);
                return;
            }

            um.endCEdit(undoText);
        }

        dlg.cleanup();
    }

}

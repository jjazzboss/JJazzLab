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

import org.jjazz.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextAction;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.insertspt")
@ActionRegistration(displayName = "#CTL_InsertSpt", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 300)
        })
public class InsertSpt extends SS_ContextAction
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("I");
    private static final Logger LOGGER = Logger.getLogger(InsertSpt.class.getSimpleName());

   @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_InsertSpt"));
        putValue(ACCELERATOR_KEY, KEYSTROKE);
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.SONG_PART_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, SS_Selection selection)
    {
        // Prepare data
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
            um.startCEdit(getActionName());


            try
            {
                sgs.addSongParts(Arrays.asList(newSpt));

            } catch (UnsupportedEditException ex)
            {
                String msg = ResUtil.getString(getClass(), "ERR_InsertSpt") + "\n" + ex.getLocalizedMessage();
                um.abortCEdit(getActionName(), msg);
                return;
            }

            um.endCEdit(getActionName());
        }
        dlg.cleanup();
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b = selection.isContiguousSptSelection();
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
        setEnabled(b);
    }
}

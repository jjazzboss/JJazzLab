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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import static org.jjazz.ui.utilities.Utilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.JJazzUndoManager;

@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.appendspt")
@ActionRegistration(displayName = "#CTL_AppendSpt", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SS_Editor", position = 300)
        })
@NbBundle.Messages("CTL_AppendSpt=Insert At The End...")
public class AppendSpt extends AbstractAction
{

    private String undoText = CTL_AppendSpt();
    private static final Logger LOGGER = Logger.getLogger(AppendSpt.class.getSimpleName());

    public AppendSpt()
    {
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_I));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // LOGGER.log(Level.FINE, "actionPerformed()");
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc == null)
        {
            return;
        }
        
        
        // Prepare data
        SongStructure sgs = tc.getSS_Editor().getModel();
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

            
            int nbBars = cls.getSectionRange(parentSection).size();
            SongPart newSpt = sgs.createSongPart(r, parentSection.getData().getName(), startBarIndex, nbBars, parentSection, true);            
            
            
            JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
            um.startCEdit(undoText);
                        

            try
            {
                sgs.addSongParts(Arrays.asList(newSpt));
                
            } catch (UnsupportedEditException ex)
            {
                // We should not be here, we reuse an existing rhythm
                String msg = "Impossible to append song part\n" + ex.getLocalizedMessage();
                um.handleUnsupportedEditException(undoText, msg);
                return;
            }
            
            um.endCEdit(undoText);
        }
        
        dlg.cleanup();
    }

}

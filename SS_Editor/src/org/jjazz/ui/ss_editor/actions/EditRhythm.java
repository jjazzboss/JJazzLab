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
import java.util.ArrayList;
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
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongManager;
import static org.jjazz.ui.ss_editor.actions.Bundle.*;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.ui.ss_editor.spi.RhythmSelectionDialog;
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
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;

@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.editrhythm")
@ActionRegistration(displayName = "#CTL_EditRhythm", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/SongPart", position = 80)
        })
@NbBundle.Messages(
        {
            "CTL_EditRhythm=Change Rhythm...",
            "ERR_EditRhythm=Impossible to set rhythm"
        })
public class EditRhythm extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    static private boolean dialogShown = false;
    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = CTL_EditRhythm();
    private static final Logger LOGGER = Logger.getLogger(EditRhythm.class.getSimpleName());

    public EditRhythm()
    {
        this(Utilities.actionsGlobalContext());
    }

    private EditRhythm(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("R"));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new EditRhythm(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        changeRhythm(cap.getSelection().getIndirectlySelectedSongParts());
    }

    /**
     * Show the rhythm selection dialog initialized with the 1st selected song part.
     * <p>
     * Then apply the new rhythm to selected song parts (only if timesignature matches).
     *
     * @param selectedSpts
     */
    static public void changeRhythm(final List<SongPart> selectedSpts)
    {
        LOGGER.fine("changeRhythm() -- selectedSpts=" + selectedSpts);

        List<SongPart> selSpts = new ArrayList<>(selectedSpts);               // Copy to avoid concurrent modifications
        SongPart spt0 = selSpts.get(0);
        SongStructure sgs = spt0.getContainer();
        List<SongPart> allSpts = new ArrayList<>(sgs.getSongParts());       // Copy to avoid concurrent modifications

        // Initialize and show dialog
        RhythmSelectionDialog dlg = RhythmSelectionDialog.getDefault();
        Rhythm r0 = spt0.getRhythm();
        dlg.preset(r0);
        dlg.setTitleLabel("Select a " + r0.getTimeSignature() + " rhythm");
        if (!dialogShown)
        {
            dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        }
        dlg.setVisible(true);
        dialogShown = true;

        if (!dlg.isExitOk())
        {
            dlg.cleanup();
            return;
        }

        // Get the new rhythm
        Rhythm newRhythm = dlg.getSelectedRhythm();
        LOGGER.fine("changeRhythm() selected newRhythm=" + newRhythm);

        // Start the edit : update the tempo (optional) and each songpart's rhythm
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        um.startCEdit(CTL_EditRhythm());

        // Change tempo if required
        if (dlg.isUseRhythmTempo())
        {
            Song song = SongManager.getInstance().findSong(sgs);
            assert song != null : "selSpts=" + selSpts;
            int tempo = newRhythm.getPreferredTempo();
            song.setTempo(tempo);
        }

        ArrayList<SongPart> oldSpts = new ArrayList<>();
        ArrayList<SongPart> newSpts = new ArrayList<>();

        if (dlg.isApplyRhythmToNextSongParts() && selSpts.size() == 1)
        {
            // Special case:
            // Apply new rhythm also to next song parts (although they were not selected)
            // Stop at first spt which does not share the same rhythm than spt0
            int index = allSpts.indexOf(spt0);
            if (newRhythm != r0)
            {
                // Get the spts and prepare the new spts
                for (int i = index; i < allSpts.size(); i++)
                {
                    SongPart spt = allSpts.get(i);
                    if (spt.getRhythm() != r0)
                    {
                        // Exit at first different spt
                        break;
                    }
                    oldSpts.add(spt);
                    SongPart newSpt = spt.clone(newRhythm, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection());
                    newSpts.add(newSpt);
                }
            }
        } else
        {
            // Normal case, just apply when needed to selected song parts with same timesignature
            for (SongPart oldSpt : selSpts)
            {
                if (!oldSpt.getRhythm().equals(newRhythm) && oldSpt.getRhythm().getTimeSignature().equals(newRhythm.getTimeSignature()))
                {
                    oldSpts.add(oldSpt);
                    SongPart newSpt = oldSpt.clone(newRhythm, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
                    newSpts.add(newSpt);
                }
            }
        }

        // Perform the rhythm change
        try
        {
            sgs.replaceSongParts(oldSpts, newSpts);
        } catch (UnsupportedEditException ex)
        {
            String msg = ERR_EditRhythm() + ": " + newRhythm.getName() + ".\n" + ex.getLocalizedMessage();
            um.handleUnsupportedEditException(CTL_EditRhythm(), msg);
            return;
        }

        um.endCEdit(CTL_EditRhythm());

        dlg.cleanup();
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        boolean b;
        b = !selection.isEmpty();
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);
        setEnabled(b);
    }
}

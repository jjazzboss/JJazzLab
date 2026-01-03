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
package org.jjazz.ss_editorimpl.api;

import org.jjazz.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoableEdit;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythmselectiondialog.spi.RhythmPreviewer;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.rhythmselectiondialog.api.RhythmSelectionDialog;
import org.jjazz.rhythmselectiondialog.spi.RhythmSelectionDialogProvider;
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
import org.jjazz.ss_editor.api.SS_ContextActionListener;
import org.jjazz.ss_editorimpl.rhythmselectiondialog.RhythmSelectionDialogCustomComp;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.editrhythm")
@ActionRegistration(displayName = "#CTL_EditRhythm", lazy = false)
@ActionReferences(
    {
        @ActionReference(path = "Actions/SongPart", position = 80)
    })
public class EditRhythm extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("R");
    private static boolean dialogShown = false;
    private static String undoText = ResUtil.getString(EditRhythm.class, "CTL_EditRhythm");
    private Lookup context;
    private SS_ContextActionSupport cap;

    private static final Logger LOGGER = Logger.getLogger(EditRhythm.class.getSimpleName());

    public EditRhythm()
    {
        this(Utilities.actionsGlobalContext());
    }

    private EditRhythm(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addWeakSelectionListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, KEYSTROKE);
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
        changeSongPartsRhythm(cap.getSelection().getIndirectlySelectedSongParts());
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        boolean b;
        b = !selection.isEmpty();
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);
        setEnabled(b);
    }

    /**
     * Show the rhythm selection dialog initialized with the 1st selected song part.
     * <p>
     * Then apply the new rhythm to selected song parts (only if timesignature matches).
     *
     * @param selectedSpts
     */
    static public void changeSongPartsRhythm(final List<SongPart> selectedSpts)
    {
        LOGGER.log(Level.FINE, "changeRhythm() -- selectedSpts={0}", selectedSpts);


        List<SongPart> selSpts = new ArrayList<>(selectedSpts);               // Copy to avoid concurrent modifications
        SongPart selSpt0 = selSpts.get(0);
        SongStructure sgs = selSpt0.getContainer();
        Song song = SongFactory.getInstance().findSong(sgs);
        List<SongPart> allSpts = new ArrayList<>(sgs.getSongParts());       // Copy to avoid concurrent modifications


        // Initialize and show dialog
        RhythmSelectionDialog dlg = RhythmSelectionDialogProvider.getDefault().getDialog();
        var customComp = RhythmSelectionDialogCustomComp.getInstance();
        dlg.setCustomComponent(customComp);
        Rhythm rSelSpt0 = selSpt0.getRhythm();
        RhythmPreviewer previewer = RhythmPreviewer.getDefault();
        if (previewer != null)
        {
            try
            {
                previewer.setContext(song, selSpt0);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.log(Level.WARNING, "changeRhythm() Can''t set context ex={0}. RhythmPreviewProvider disabled.", ex.getMessage());
                previewer = null;
            }
        }
        var rdb = RhythmDatabase.getDefault();
        RhythmInfo ri = rdb.getRhythm(rSelSpt0.getUniqueId());
        dlg.preset(ri, previewer, () -> customComp.isUseRhythmTempo());
        dlg.setTitleText("Select a " + ri.timeSignature() + " rhythm");
        if (!dialogShown)
        {
            dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        }
        dlg.setVisible(true);

        // Dialog exited
        if (previewer != null)
        {
            previewer.cleanup();
        }

        dialogShown = true;
        if (!dlg.isExitOk())
        {
            dlg.cleanup();
            return;
        }


        // Get the new rhythm
        RhythmInfo newRhythmInfo = dlg.getSelectedRhythm();
        LOGGER.log(Level.FINE, "changeRhythm() selected newRhythm={0}", newRhythmInfo);
        Rhythm newRhythm;
        try
        {
            newRhythm = rdb.getRhythmInstance(newRhythmInfo);
        } catch (UnavailableRhythmException ex)
        {
            LOGGER.log(Level.WARNING, "changeRhythm() can''t get Rhythm instance from RhythmInfo={0}", newRhythmInfo);
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Start the edit : update the tempo (optional) and each songpart's rhythm
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        um.startCEdit(undoText);


        // Change tempo if required
        int newTempo = newRhythm.getPreferredTempo();
        int oldTempo = song.getTempo();
        if (customComp.isUseRhythmTempo() && newTempo != oldTempo)
        {
            song.setTempo(newTempo);

            // Create an undoable edit so that undo restores previous rhythm AND previous tempo
            UndoableEdit edit = new SimpleEdit("Set tempo " + newTempo)
            {
                @Override
                public void undoBody()
                {
                    song.setTempo(oldTempo);
                }

                @Override
                public void redoBody()
                {
                    song.setTempo(newTempo);
                }
            };

            // Directly notify UndoManager
            um.undoableEditHappened(new UndoableEditEvent(song, edit));
        }


        ArrayList<SongPart> oldSpts = new ArrayList<>();
        ArrayList<SongPart> newSpts = new ArrayList<>();

        if (customComp.isApplyRhythmToNextSongParts() && selSpts.size() == 1)
        {
            // Special case:
            // Apply new rhythm also to next song parts (although they were not selected)
            // Stop at first spt which does not share the same rhythm than spt0
            int index = allSpts.indexOf(selSpt0);
            if (newRhythm != rSelSpt0)
            {
                // Get the spts and prepare the new spts
                for (int i = index; i < allSpts.size(); i++)
                {
                    SongPart spt = allSpts.get(i);
                    if (spt.getRhythm() != rSelSpt0)
                    {
                        // Exit at first different spt
                        break;
                    }
                    oldSpts.add(spt);
                    SongPart newSpt = spt.getCopy(newRhythm, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection());
                    newSpts.add(newSpt);
                }
            }
        } else
        {
            // Normal case, just apply when needed to selected song parts with same timesignature
            for (SongPart oldSpt : selSpts)
            {
                if (oldSpt.getRhythm() != newRhythm && oldSpt.getRhythm().getTimeSignature().equals(newRhythm.getTimeSignature()))
                {
                    oldSpts.add(oldSpt);
                    SongPart newSpt = oldSpt.getCopy(newRhythm, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
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
            String msg = undoText + ": " + newRhythm.getName() + ".\n" + ex.getLocalizedMessage();
            um.abortCEdit(undoText, msg);
            return;
        }


        um.endCEdit(undoText);


        dlg.cleanup();
    }

    // ================================================================================
    // Private methods
    // ================================================================================
    // ================================================================================
    // Private classes
    // ================================================================================
}

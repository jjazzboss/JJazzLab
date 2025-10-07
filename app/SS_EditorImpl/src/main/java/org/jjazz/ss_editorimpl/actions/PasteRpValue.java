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

import com.google.common.collect.Iterables;
import org.jjazz.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ss_editorimpl.RpValueCopyBuffer;
import org.jjazz.ss_editor.api.SS_ContextActionListener;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;

/**
 * Paste RpValue action.
 * <p>
 * This action is directly used when triggered by the RhythmParameter menu entry. Ctrl-V keyboard shortcut is handled by the Paste action
 * which reuses some of our methods if needed (when RhythmParameters are selected).
 */
@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.pasterpvalue")
@ActionRegistration(displayName = "#CTL_PasteRpValue", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 30, separatorAfter = 31)
        })
public class PasteRpValue extends AbstractAction implements ContextAwareAction, SS_ContextActionListener, ChangeListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private final static String UNDO_TEXT = ResUtil.getString(PasteRpValue.class, "CTL_PasteRpValue");

    public PasteRpValue()
    {
        this(Utilities.actionsGlobalContext());
    }

    private PasteRpValue(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addWeakSelectionListener(this);
        putValue(NAME, UNDO_TEXT);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_V));
        RpValueCopyBuffer buffer = RpValueCopyBuffer.getInstance();
        buffer.addChangeListener(this);
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new PasteRpValue(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_Selection selection = cap.getSelection();
        performAction(selection);
    
    }

    @Override
    public void selectionChange(SS_Selection selection)
    {
        setEnabled(isEnabled(selection));
    }

    /**
     * Make the method accessible to Paste action.
     *
     * @param selection
     * @return
     */
    static protected boolean isEnabled(SS_Selection selection)
    {
        RpValueCopyBuffer buffer = RpValueCopyBuffer.getInstance();
        boolean b = !getPastableSongParts(selection, buffer.getRhythm(), buffer.getRhythmParameter()).isEmpty();
        return b;
    }
    
     /**
     * Make the method accessible to Paste action.
     *
     * @param selection
     */
    static protected void performAction(SS_Selection selection)
    {
            SongStructure sgs = selection.getModel();

        var buffer = RpValueCopyBuffer.getInstance();
        var r = buffer.getRhythm();
        var rp = buffer.getRhythmParameter();
        List<Object> values = buffer.get();
        assert !values.isEmpty() : "buffer=" + buffer;
        var selSpts = getPastableSongParts(selection, r, rp);


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
        um.startCEdit(UNDO_TEXT);


        if (selSpts.size() == 1)
        {
            // Single spt selection is special case: we try to paste all buffer values on next compatible song parts
            var allSpts = sgs.getSongParts();
            int sptIndex = allSpts.indexOf(selSpts.get(0));

            for (var itValue = values.iterator(); itValue.hasNext();)
            {
                SongPart spt = allSpts.get(sptIndex);


                var crp = RhythmParameter.findFirstCompatibleRp(spt.getRhythm().getRhythmParameters(), rp);
                if (crp != null)
                {
                    Object value = itValue.next();
                    Object newValue = crp.convertValue((RhythmParameter) rp, value);
                    sgs.setRhythmParameterValue(spt, (RhythmParameter) crp, newValue);
                }

                sptIndex++;
                if (sptIndex >= allSpts.size())
                {
                    break;
                }
            }

        } else
        {
            // Multple spt selection : we paste only on the selected song parts, cycling through the buffer values if required
            Iterator<Object> itValue = Iterables.cycle(values).iterator();
            for (var spt : selSpts)
            {
                var crp = RhythmParameter.findFirstCompatibleRp(spt.getRhythm().getRhythmParameters(), rp);
                if (crp != null)
                {
                    Object value = itValue.next();
                    Object newValue = crp.convertValue((RhythmParameter) rp, value);
                    sgs.setRhythmParameterValue(spt, (RhythmParameter) crp, newValue);
                }
            }
        }


        um.endCEdit(UNDO_TEXT);
    }

    // =======================================================================
    // ChangeListener interface
    // =======================================================================
    /**
     * Called when the RpValueCopyBuffer buffer has changed
     *
     * @param e
     */
    @Override
    public void stateChanged(ChangeEvent e)
    {
        SS_EditorTopComponent tc = SS_EditorTopComponent.getActive();
        if (tc != null)
        {
            SS_Selection selection = new SS_Selection(tc.getEditor().getLookup());
            selectionChange(selection);
        }
    }


    // =======================================================================
    // Private methods
    // =======================================================================
    static private List<SongPart> getPastableSongParts(SS_Selection selection, Rhythm r, RhythmParameter<?> rp)
    {
        List<SongPart> res;

        if (selection.isRhythmParameterSelected())
        {
            res = selection.getSelectedSongPartParameters().stream()
                    .filter(spp -> spp.getRp().isCompatibleWith(rp))
                    .map(spp -> spp.getSpt())
                    .toList();
        } else
        {
            res = new ArrayList<>();
        }

        return res;
    }

}

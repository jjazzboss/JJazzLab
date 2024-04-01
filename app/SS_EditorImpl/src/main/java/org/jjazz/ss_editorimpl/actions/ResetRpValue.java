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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextActionListener;
import org.jjazz.utilities.api.ResUtil;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editorimpl.actions.resetrpvalue")
@ActionRegistration(displayName = "#CTL_ResetRpValue", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 600),
        })
public final class ResetRpValue extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{
    public static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke("Z");
    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = ResUtil.getString(getClass(), "CTL_ResetRpValue");
    private static final Logger LOGGER = Logger.getLogger(ResetRpValue.class.getSimpleName());

    public ResetRpValue()
    {
        this(Utilities.actionsGlobalContext());
    }

    public ResetRpValue(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);                          // For popupmenu 
        putValue(ACCELERATOR_KEY, KEYSTROKE);      // For popupmenu
    }

    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_SelectionUtilities selection = cap.getSelection();
        SongStructure sgs = selection.getModel();
        LOGGER.log(Level.FINE, "actionPerformed() sgs={0} selection={1}", new Object[]{sgs, selection});   
        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(undoText);

        if (selection.isRhythmParameterSelected())
        {
            for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
            {
                RhythmParameter rp = sptp.getRp();
                SongPart spt = sptp.getSpt();
                Object newValue = rp.getDefaultValue();
                sgs.setRhythmParameterValue(spt, rp, newValue);
            }
        } else
        {
            for (SongPart spt : selection.getSelectedSongParts())
            {
                for (RhythmParameter rp : spt.getRhythm().getRhythmParameters())
                {
                    Object newValue = rp.getDefaultValue();
                    sgs.setRhythmParameterValue(spt, rp, newValue);
                }
            }
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(undoText);
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        boolean b = !selection.isEmpty();
        setEnabled(b);
        LOGGER.log(Level.FINE, "selectionChange() b={0}", b);   
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new ResetRpValue(context);
    }
}

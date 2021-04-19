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
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.ss_editor.api.SS_ContextActionListener;
import static org.jjazz.ui.utilities.Utilities.getGenericControlKeyStroke;
import org.jjazz.util.ResUtil;
import org.jjazz.rhythm.api.Enumerable;

@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.nextrpvalue")
@ActionRegistration(displayName = "#CTL_NextRpValue", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 400),
        })
public final class NextRpValue extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = ResUtil.getString(getClass(), "CTL_NextRpValue");
    private static final Logger LOGGER = Logger.getLogger(NextRpValue.class.getSimpleName());

    public NextRpValue()
    {
        this(Utilities.actionsGlobalContext());
    }

    public NextRpValue(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, ResUtil.getString(getClass(), "CTL_NextRpValue"));                          // For popupmenu display only
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_UP));    // For popupmenu display only
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
        LOGGER.log(Level.FINE, "actionPerformed() sgs=" + sgs + " selection=" + selection);   //NOI18N
        JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(undoText);
        for (SongPartParameter sptp : selection.getSelectedSongPartParameters())
        {
            RhythmParameter rp = sptp.getRp();
            if (rp instanceof Enumerable)
            {
                SongPart spt = sptp.getSpt();
                Object newValue = ((Enumerable) rp).getNextValue(spt.getRPValue(rp));
                sgs.setRhythmParameterValue(spt, rp, newValue);
            }
        }
        JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(undoText);
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        boolean b = selection.isEnumerableRhythmParameterSelected();
        setEnabled(b);
        LOGGER.log(Level.FINE, "selectionChange() b=" + b);   //NOI18N
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new NextRpValue(context);
    }
}

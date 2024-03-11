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
package org.jjazz.ss_editor.actions;

import org.jjazz.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_SelectionUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.ss_editor.api.RpValueCopyBuffer;
import org.jjazz.ss_editor.api.SS_ContextActionListener;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;

/**
 * Copy RpValue action.
 * <p>
 * This action is directly used when triggered by the RhythmParameter menu entry. Ctrl-C keyboard shortcut is handled by the Copy action
 * which reuses some of our methods if needed (when RhythmParameters are selected).
 */
@ActionID(category = "JJazz", id = "org.jjazz.ss_editor.actions.copyrpvalue")
@ActionRegistration(displayName = "#CTL_CopyRpValue", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 20),
        })
public class CopyRpValue extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = ResUtil.getString(getClass(), "CTL_CopyRpValue");
    private static final Logger LOGGER = Logger.getLogger(CopyRpValue.class.getSimpleName());

    public CopyRpValue()
    {
        this(Utilities.actionsGlobalContext());
    }

    private CopyRpValue(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_C));       // Only for display
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new CopyRpValue(context);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_SelectionUtilities selection = cap.getSelection();
        performAction(selection);
    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        setEnabled(isEnabled(selection));
    }

    /**
     * Make the method accessible to Copy action.
     *
     * @param selection
     * @return
     */
    static protected void performAction(SS_SelectionUtilities selection)
    {
        assert selection.isRhythmParameterSelected() : "selection=" + selection;
        var sptps = selection.getSelectedSongPartParameters();

        // Collect data
        List<Object> values = new ArrayList<>();
        RhythmParameter<?> rp0 = sptps.get(0).getRp();
        SongPart spt0 = sptps.get(0).getSpt();
        for (SongPartParameter sptp : sptps)
        {
            RhythmParameter<?> rpi = sptp.getRp();
            SongPart spt = sptp.getSpt();
            values.add(spt.getRPValue(rpi));
        }


        // Copy in the buffer
        RpValueCopyBuffer buffer = RpValueCopyBuffer.getInstance();
        buffer.put(spt0.getRhythm(), (RhythmParameter) rp0, values);


        // Force a selection change so that the Paste action enabled status can be updated 
        // (otherwise Paste action will not see that the CopyBuffer is no more empty)
        SS_Editor editor = SS_EditorTopComponent.getActive().getEditor();
        editor.selectRhythmParameter(spt0, rp0, false);
        editor.selectRhythmParameter(spt0, rp0, true);
    }

    /**
     * Make the method accessible to Copy action.
     *
     * @param selection
     * @return
     */
    static protected boolean isEnabled(SS_SelectionUtilities selection)
    {
        boolean b = false;
        if (selection.isRhythmParameterSelected() && selection.isContiguousSptSelection())
        {
            var sptps = selection.getSelectedSongPartParameters();
            Rhythm r0 = sptps.get(0).getSpt().getRhythm();
            var opt = sptps.stream()
                    .skip(1)
                    .map(sptp -> sptp.getSpt().getRhythm())
                    .filter(r -> r != r0)
                    .findAny();
            b = opt.isEmpty();
        }
        return b;
    }

}

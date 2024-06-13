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
package org.jjazz.pianoroll.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import org.jjazz.pianoroll.HumanizeDialog;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.ResUtil;


/**
 * Open the Humanize dialog.
 */
public class Humanize extends AbstractAction
{
    public static final String ACTION_ID = "Humanize";
    public static final String KEYBOARD_SHORTCUT = "ctrl H";
    private final PianoRollEditorTopComponent editorTc;
    private static final Logger LOGGER = Logger.getLogger(Humanize.class.getSimpleName());

    public Humanize(PianoRollEditorTopComponent tc)
    {
        this.editorTc = tc;

        putValue(Action.NAME, ResUtil.getString(getClass(), "Humanize"));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "HumanizeTooltip") + " (" + KEYBOARD_SHORTCUT + ")");


        editorTc.getEditor().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(getGenericControlKeyStroke(KeyEvent.VK_H), ACTION_ID);
        editorTc.getEditor().getActionMap().put(ACTION_ID, this);


    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var dlg = new HumanizeDialog(editorTc);
        dlg.setVisible(true);
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
}

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

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ui.sptviewer.api.SptViewer;
import org.jjazz.ui.rpviewer.api.RpViewer;

public class ExtendSelectionRight extends AbstractAction
{

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_Editor editor = SS_EditorTopComponent.getActive().getSS_Editor();
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (c instanceof SptViewer)
        {
            SptViewer sptv = (SptViewer) c;
            MoveSelectionRight.moveSelectionRightSpt(editor, sptv.getModel(), true);
        } else if (c instanceof RpViewer)
        {
            RpViewer rpv = (RpViewer) c;
            MoveSelectionRight.moveSelectionRightRp(editor, rpv.getSptModel(), rpv.getRpModel(), true);
        }
    }
}

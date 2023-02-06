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
package org.jjazz.ui.cl_editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;

public class JumpToEnd extends AbstractAction
{

    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());
        selection.unselectAll(editor);
        int lastBarIndex = editor.getModel().getSizeInBars() - 1;
        editor.selectBars(lastBarIndex, lastBarIndex, true);
        editor.setFocusOnBar(lastBarIndex);
        editor.makeBarVisible(lastBarIndex);
    }
}

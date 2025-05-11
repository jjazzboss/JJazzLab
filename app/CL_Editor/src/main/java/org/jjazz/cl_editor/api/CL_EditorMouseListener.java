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
package org.jjazz.cl_editor.api;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;

/**
 * A listener for mouse events of interest.
 */
public interface CL_EditorMouseListener
{

    public void itemClicked(MouseEvent e, ChordLeadSheetItem<?> item, IR_Type irType);

    public void itemWheelMoved(MouseWheelEvent e, ChordLeadSheetItem<?> item, IR_Type irType);

    public void barClicked(MouseEvent e, int barIndex);

    public void barDragged(MouseEvent e, int barIndex);

    public void barReleased(MouseEvent e, int barIndex);

    public void editorWheelMoved(MouseWheelEvent e);
}

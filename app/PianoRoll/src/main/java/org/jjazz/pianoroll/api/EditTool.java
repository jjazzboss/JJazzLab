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
package org.jjazz.pianoroll.api;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Icon;


/**
 * An editor tool: selection, pencil, eraser, etc.
 */
public interface EditTool extends PianoRollEditorMouseListener
{

    /**
     * A 20x20 icon.
     *
     * @param selected
     * @return
     */
    Icon getIcon(boolean selected);

    Cursor getCursor();

    String getName();

    /**
     * If true user can draw a rectangle to edit multiple notes.
     *
     * @return
     * @see #editMultipleNotes(java.util.List)
     */
    boolean isEditMultipleNotesSupported();

    /**
     * Perform the edit on multiple notes (if operation is supported, see isEditMultipleNotesSupported()).
     * <p>
     * This method is called when user has selected one or more notes via the rectangle selection.
     *
     * @param noteViews
     * @see #isEditMultipleNotesSupported()
     */
    void editMultipleNotes(List<NoteView> noteViews);

    /**
     * Check if the specified MouseEvent uses keyboard modifiers (eg alt) that modify the current snap setting.
     *
     * @param e
     * @return
     */
    default boolean isOverrideSnapSetting(MouseEvent e)
    {
        return e.isAltDown();
    }
    
    default boolean isConstantPitchModifier(MouseEvent e)
    {
        return e.isShiftDown();
    }
}

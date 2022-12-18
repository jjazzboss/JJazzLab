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
package org.jjazz.pianoroll.api;

import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.edittools.SelectionTool;


/**
 * An editor tool: selection, pencil, eraser, etc.
 */
public interface EditTool extends PianoRollEditorMouseListener
{

    /**
     * Get the available EditTools.
     *
     * @param editor
     * @return
     */
    static public List<EditTool> getAvailableTools(PianoRollEditor editor)
    {
        return Arrays.asList(new SelectionTool(editor));
    }

    /**
     * A 20x20 icon.
     *
     * @param selected
     * @return
     */
    Icon getIcon(boolean selected);

    String getName();

    /**
     * Perform the edit on multiple notes.
     * <p>
     * This method is called e.g. when user has selected one or more notes via the rectangle selection.
     *
     * @param noteEvents
     */
    void editMultipleNotes(List<NoteEvent> noteEvents);


}

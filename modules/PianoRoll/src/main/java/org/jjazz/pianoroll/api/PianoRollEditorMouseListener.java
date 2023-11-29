/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU Genvral Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser Genvral Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser Genvral Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.pianoroll.api;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Listener to mouse events in a PianoRollEditor.
 */
interface PianoRollEditorMouseListener
{

    void editorClicked(MouseEvent e);

    void editorDragged(MouseEvent e);

    void editorReleased(MouseEvent e);

    public void editorWheelMoved(MouseWheelEvent evt);

    void noteClicked(MouseEvent e, NoteView nv);

    void noteWheelMoved(MouseWheelEvent e, NoteView nv);

    void noteMoved(MouseEvent e, NoteView nv);

    void noteEntered(MouseEvent e, NoteView nv);

    void noteExited(MouseEvent e, NoteView nv);

    void noteDragged(MouseEvent e, NoteView nv);

    void noteReleased(MouseEvent e, NoteView nv);

}

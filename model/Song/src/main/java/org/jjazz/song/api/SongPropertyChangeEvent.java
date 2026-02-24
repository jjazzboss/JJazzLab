/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.song.api;

import java.beans.PropertyChangeEvent;

/**
 * A PropertyChangeEvent which can indicate if event is part of a undo/redo.
 */
public class SongPropertyChangeEvent extends PropertyChangeEvent
{

    private static final Boolean UNDO = Boolean.TRUE;
    private static final Boolean REDO = Boolean.FALSE;
    private Boolean undoOrRedoOrNothing;

    public SongPropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue)
    {
        super(source, propertyName, oldValue, newValue);
        undoOrRedoOrNothing = null;
    }

    public void setIsUndo()
    {
        undoOrRedoOrNothing = UNDO;
    }

    public void setIsRedo()
    {
        undoOrRedoOrNothing = REDO;
    }

    public boolean isUndo()
    {
        return undoOrRedoOrNothing == UNDO;
    }

    public boolean isRedo()
    {
        return undoOrRedoOrNothing == REDO;
    }

    public boolean isUndoOrRedo()
    {
        return undoOrRedoOrNothing != null;
    }
}

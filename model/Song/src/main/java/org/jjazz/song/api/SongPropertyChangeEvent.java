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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A PropertyChangeEvent with additional features: undo/redo indicator and optional associated events.
 */
public class SongPropertyChangeEvent extends PropertyChangeEvent
{

    private static final Boolean UNDO = Boolean.TRUE;
    private static final Boolean REDO = Boolean.FALSE;
    private Boolean undoOrRedoOrNothing;
    private final List<PropertyChangeEvent> propertyChanges;

    public SongPropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue)
    {
        super(source, propertyName, oldValue, newValue);
        undoOrRedoOrNothing = null;
        propertyChanges = new ArrayList<>();
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

    /**
     * Add some related PropertyChangeEvent events to this change.
     *
     * @param changes
     */
    public void addRelatedPropertyChanges(List<PropertyChangeEvent> changes)
    {
        propertyChanges.addAll(changes);
    }

    /**
     * Get the related PropertyChangeEvent events.
     *
     * @return Can be empty
     */
    public List<PropertyChangeEvent> getRelatedPropertyChanges()
    {
        return Collections.unmodifiableList(propertyChanges);
    }
}

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
package org.jjazz.undomanager.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.undo.AbstractUndoableEdit;

/**
 * A convenience class to create insignificant undoableedits that can be combined into CompoundEdits.
 */
public abstract class SimpleEdit extends AbstractUndoableEdit
{

    private String presentationName;
    private static final Logger LOGGER = Logger.getLogger(SimpleEdit.class.getName());

    public SimpleEdit(String presentationName)
    {
        if (presentationName == null)
        {
            throw new NullPointerException("name=" + presentationName);   
        }
        this.presentationName = presentationName;
    }

    @Override
    public void undo()
    {
        super.undo();
        LOGGER.log(Level.FINE, "undo() edit: {0}", getPresentationName());   
        undoBody();
    }

    abstract public void undoBody();

    @Override
    public void redo()
    {
        super.redo();
        LOGGER.log(Level.FINE, "redo() edit: {0}", getPresentationName());   
        redoBody();
    }

    abstract public void redoBody();

    @Override
    public String getPresentationName()
    {
        return presentationName;
    }

    /**
     * Must be embedded in a significant CompoundEdit.
     *
     * @return False
     */
    @Override
    public boolean isSignificant()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return getPresentationName();
    }
}

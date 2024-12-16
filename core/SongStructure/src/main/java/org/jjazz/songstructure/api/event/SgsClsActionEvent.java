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
package org.jjazz.songstructure.api.event;

import org.jjazz.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.songstructure.api.SongStructure;

/**
 * A special SgsActionEvent used by SgsUpdater for SgsActionEvents which encapsulate a ClsActionEvent.
 */
public class SgsClsActionEvent extends SgsActionEvent
{

    public SgsClsActionEvent(SongStructure sgs, String actionId, boolean isComplete, boolean undo, ClsActionEvent clsActionEvent)
    {
        super(sgs, actionId, isComplete, undo, clsActionEvent);
    }

    public ClsActionEvent getClsActionEvent()
    {
        return (ClsActionEvent) getData();
    }

    @Override
    public String toString()
    {
        return "SgsClsActionEvent(" + getActionId() + ", complete=" + this.isActionComplete() + ", isUndo=" + isUndo() + ", clsActionEvent=" + getClsActionEvent() + ")";
    }
}

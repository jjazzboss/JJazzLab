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
package org.jjazz.songstructure.api.event;

import org.jjazz.songstructure.api.SongStructure;

/**
 * An event to indicate that a high-level SongStructure action that changes the song structure has started or is complete.
 * <p>
 * This can be used by listener to group lower-level change events by actionId. The actionId must be the corresponding method name
 * from the SongStructure interface, e.g. "addSongParts".
 * <p>
 * There is the guarantee that if a start SgsActionEvent is received, the complete sgsActionEvent will be received on the same
 * actionId. It's possible that no lower-level change event occur between 2 started/complete action events on the same actionId.
 */
public class SgsActionEvent extends SgsChangeEvent
{

    private final boolean startedOrComplete;      // false = started
    private final boolean isUndo;      // false = started
    private final String actionId;

    /**
     *
     * @param sgs
     * @param actionId The corresponding method name from the SongStructure interface which performs the change, e.g.
     * "addSongParts".
     * @param startedOrComplete False means action has started, true action is complete
     * @param undo True if we're actually undoing the action
     */
    public SgsActionEvent(SongStructure sgs, String actionId, boolean startedOrComplete, boolean undo)
    {
        super(sgs);
        if (actionId == null)
        {
            throw new IllegalArgumentException("sgs=" + sgs + " actionId=" + actionId + " startedOrComplete=" + startedOrComplete + " undo=" + undo);
        }
        this.startedOrComplete = startedOrComplete;
        this.actionId = actionId;
        this.isUndo = undo;
    }

    public boolean isActionStarted()
    {
        return !startedOrComplete;
    }

    public boolean isActionComplete()
    {
        return startedOrComplete;
    }

    public String getActionId()
    {
        return actionId;
    }

    public boolean isUndo()
    {
        return isUndo;
    }
    
     @Override
    public String toString()
    {
        return "SgsActionEvent(" + actionId + ", complete=" + startedOrComplete + ", isUndo=" + isUndo + ")";
    }
}

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

import com.google.common.base.Preconditions;
import org.jjazz.songstructure.api.SongStructure;

/**
 * A special vetoable event sent before performing a SongStructure change that can be vetoed by its listeners.
 * <p>
 * For example replaceSongParts() can be vetoed by a listener if there is not enough Midi channels.
 * <p>
 * All the SongStructure methods which throw UnsupportedEditException send a SgsVetoableChangeEvent. The listener is responsible to analyze the passed
 * SgsChangeEvent and throw an UnsupportedEditException (with an user error message) to veto the change.
 *
 * @see SongStructure
 * @see org.jjazz.chordleadsheet.api.UnsupportedEditException
 */
public class SgsVetoableChangeEvent extends SgsChangeEvent
{

    private final SgsChangeEvent changeEvent;

    public SgsVetoableChangeEvent(SongStructure src, SgsChangeEvent changeEvent)
    {
        super(src);
        Preconditions.checkArgument(
                !(changeEvent instanceof SgsActionEvent)
                && !(changeEvent instanceof SgsVetoableChangeEvent)
                && changeEvent.getSource() == src,
                "changeEvent=%s", changeEvent
        );
        this.changeEvent = changeEvent;
    }

    /**
     * @return The event corresponding to the change about to be performed -unless it is vetoed by a listener.
     */
    public SgsChangeEvent getChangeEvent()
    {
        return changeEvent;
    }

}

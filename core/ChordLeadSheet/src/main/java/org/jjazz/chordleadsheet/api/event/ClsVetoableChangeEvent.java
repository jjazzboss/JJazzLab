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
package org.jjazz.chordleadsheet.api.event;

import com.google.common.base.Preconditions;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;

/**
 * A special vetoable event sent before performing a ChordLeadSheet change that can be vetoed by its listeners.
 * <p>
 * For example changeSection() can be vetoed by a SongStructure listener if it can't find a Rhythm for the new TimeSignature, or if there is not enough Midi
 * channels.
 * <p>
 * All the ChordLeadSheet methods which throw UnsupportedEditException send a ClsVetoableChangeEvent. The listener is responsible to analyze the passed
 * ClsChangeEvent and throw an UnsupportedEditException (with an user error message) to veto the change.
 *
 * @see ChordLeadSheet
 * @see org.jjazz.chordleadsheet.api.UnsupportedEditException
 */
public class ClsVetoableChangeEvent extends ClsChangeEvent
{

    private final ClsChangeEvent changeEvent;

    public ClsVetoableChangeEvent(ChordLeadSheet src, ClsChangeEvent changeEvent)
    {
        super(src);
        Preconditions.checkArgument(
                !(changeEvent instanceof ClsActionEvent)
                && !(changeEvent instanceof ClsVetoableChangeEvent)
                && changeEvent.getSource() == src,
                "changeEvent=%s", changeEvent);
        this.changeEvent = changeEvent;
    }

    /**
     * @return The event corresponding to the change about to be performed -unless it is vetoed by a listener.
     */
    public ClsChangeEvent getChangeEvent()
    {
        return changeEvent;
    }

    @Override
    public String toString()
    {
        return "ClsVetoableChangeEvent->" + changeEvent;
    }
}

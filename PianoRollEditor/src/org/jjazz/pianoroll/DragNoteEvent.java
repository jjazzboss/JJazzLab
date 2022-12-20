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
package org.jjazz.pianoroll;

import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;
import org.jjazz.phrase.api.NoteEvent;

/**
 * Represents a dragged NoteEvent.
 * <p>
 * Use a special subclass which can not be equal/compare=0 with normal NoteEvents. This prevents problems when possibly
 * identitical NoteEvents are Map keys.
 */
public class DragNoteEvent extends NoteEvent
{

    private static final Logger LOGGER = Logger.getLogger(DragNoteEvent.class.getSimpleName());

    public DragNoteEvent(int pitch, float duration, int velocity, float posInBeats)
    {
        super(pitch, duration, velocity, posInBeats);
    }


    @Override
    public DragNoteEvent getCopyPos(float posInBeats)
    {
        DragNoteEvent res = new DragNoteEvent(getPitch(), getDurationInBeats(), getVelocity(), posInBeats);
        res.setClientProperties(this);
        return res;
    }

    @Override
    public DragNoteEvent getCopyPitch(int pitch)
    {
        DragNoteEvent res = new DragNoteEvent(pitch, getDurationInBeats(), getVelocity(), getPositionInBeats());
        res.setClientProperties(this);
        return res;
    }

    /**
     * Overridden to never equals a NoteEvent, except when compared to another DragNoteEvent
     */
    @Override
    public int compareTo​(Note n)
    {
        int res= super.compareTo(n);        
        if (res==0 && !(n instanceof DragNoteEvent))
        {
            res = 1;
        } 
        LOGGER.severe("compareTo​() this=" + this + " n=" + n + " res=" + res);
        return res;
    }

    /**
     * Overridden to never equals a NoteEvent, except when compared to another DragNoteEvent
     */
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof DragNoteEvent dne)
        {
            return super.equals(dne);
        }
        LOGGER.severe("compareTo​() this=" + this + " n=" + n + " res=" + res);
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 27 * super.hashCode();
        return hash;
    }


}

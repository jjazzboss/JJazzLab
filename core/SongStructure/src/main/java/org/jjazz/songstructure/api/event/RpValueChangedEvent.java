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

import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * A RhythmParameter value has changed.
 */
public class RpValueChangedEvent extends SgsChangeEvent
{

    private RhythmParameter<?> rhytmParameter;
    private Object oldValue;
    private Object newValue;


    /**
     * Create an event.
     *
     * @param src
     * @param spt
     * @param rp
     * @param oldValue
     * @param newValue Can be the same than newValue for mutable RP values.
     */
    public RpValueChangedEvent(SongStructure src, SongPart spt, RhythmParameter<?> rp, Object oldValue, Object newValue)
    {
        super(src, spt);
        if (spt == null || rp == null || newValue == null)
        {
            throw new IllegalArgumentException("spt=" + spt + " rp=" + rp + " oldValue=" + oldValue + " newValue=" + newValue);   
        }
        this.rhytmParameter = rp;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public RhythmParameter<?> getRhythmParameter()
    {
        return rhytmParameter;
    }

    public Object getOldValue()
    {
        return oldValue;
    }

    /**
     * Get the new Rhythm parameter value.
     * <p>
     * Note that newValue can be the same as oldValue if the RP value class is mutable.
     *
     * @return
     */
    public Object getNewValue()
    {
        return newValue;
    }
}

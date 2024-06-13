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
package org.jjazz.songstructure.api;

import java.util.Objects;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RpEnumerable;

/**
 * Store a RP and its associated SongPart.
 */
public class SongPartParameter
{

    private SongPart spt;
    private RhythmParameter<?> rp;

    public SongPartParameter(SongPart spt, RhythmParameter<?> rp)
    {
        this.spt = spt;
        this.rp = rp;
    }

    /**
     * @return Return value can be null.
     */
    public SongPart getSpt()
    {
        return spt;
    }

    /**
     * @return Return value can be null.
     */
    public RhythmParameter<?> getRp()
    {
        return rp;
    }

    /**
     * True if the RhythmParameter is an instance of RP_Enumerable.
     *
     * @return
     */
    public boolean isEnumerableRp()
    {
        return rp instanceof RpEnumerable<?>;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof SongPartParameter)
        {
            SongPartParameter s = (SongPartParameter) o;
            return spt == s.getSpt() && rp == s.getRp();
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.spt);
        hash = 67 * hash + Objects.hashCode(this.rp);
        return hash;
    }

    @Override
    public String toString()
    {
        return "Sptp[spt=" + spt + " rp=" + rp + "]";
    }
}

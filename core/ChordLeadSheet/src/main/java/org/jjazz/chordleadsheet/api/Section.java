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
package org.jjazz.chordleadsheet.api;

import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;

/**
 * A section in a leadsheet has a name and a TimeSignature.
 * <p>
 * This is an immutable class.
 */
public final class Section implements Cloneable
{

    private String name;
    private TimeSignature timeSignature;
    private static final Logger LOGGER = Logger.getLogger(Section.class.getName());

    public Section(String n, TimeSignature ts)
    {
        if (n == null || ts == null)
        {
            throw new NullPointerException("n=" + n + " ts=" + ts);   
        }
        name = n;
        timeSignature = ts;
    }

    public String getName()
    {
        return name;
    }

    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    @Override
    public Section clone()
    {
        return new Section(name, timeSignature);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o==null || o.getClass() != this.getClass())
        {
            return false;
        }
        Section section = (Section) o;
        return name.equals(section.name) && timeSignature.equals(section.timeSignature);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.timeSignature);
        return hash;
    }

    @Override
    public String toString()
    {
        return name + "(" + timeSignature + ")";
    }
}

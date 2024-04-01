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
package org.jjazz.ss_editorimpl;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;

/**
 * Singleton class to manage RhythmParameter values copy/paste operations.
 * <p>
 * Values must come from the same RhythmParameter.
 */
public class RpValueCopyBuffer
{

    static private RpValueCopyBuffer INSTANCE;
    /**
     * The buffer for values.
     */
    private final List<Object> valueBuffer = new ArrayList<>();
    private RhythmParameter<?> rhythmParameter = null;
    private Rhythm rhythm = null;
    private final ArrayList<ChangeListener> listeners = new ArrayList<>();

    private RpValueCopyBuffer()
    {
    }

    public static RpValueCopyBuffer getInstance()
    {
        synchronized (RpValueCopyBuffer.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RpValueCopyBuffer();
            }
        }
        return INSTANCE;
    }

    /**
     * Put the RhythmParameter and a copy of the values in the buffer.
     * <p>
     *
     * @param <E>
     * @param r
     * @param rp
     * @param values RhythmParameter values
     */
    public <E> void put(Rhythm r, RhythmParameter<E> rp, List<E> values)
    {
        if (r == null || rp == null || values == null)
        {
            throw new IllegalArgumentException("r=" + r + " rp=" + rp + " values=" + values);   
        }
        if (values.isEmpty())
        {
            return;
        }
        valueBuffer.clear();
        for (E value : values)
        {
            E newValue = rp.cloneValue(value);
            valueBuffer.add(rp.cloneValue(value));
        }
        rhythmParameter = rp;
        rhythm = r;
        fireStateChanged();
    }

    public void clear()
    {
        rhythmParameter = null;
        valueBuffer.clear();
        fireStateChanged();
    }

    /**
     * @return int The nb of RhyhmParts in the buffer.
     */
    public int getSize()
    {
        return valueBuffer.size();
    }

    public boolean isEmpty()
    {
        return valueBuffer.isEmpty();
    }

    /**
     * The RhythmParameter for which the buffer stores values.
     *
     * @return Null if buffer is empty
     */
    public RhythmParameter<?> getRhythmParameter()
    {
        return rhythmParameter;
    }

    /**
     * The Rhythm of the RhythmParameter.
     *
     * @return Null if buffer is empty
     */
    public Rhythm getRhythm()
    {
        return rhythm;
    }

    /**
     * Return the values in the buffer.
     * <p>
     * @return A list of copies of the values stored in the buffer
     */
    public List<Object> get()
    {
        ArrayList<Object> res = new ArrayList<>();
        for (Object value : valueBuffer)
        {
            res.add(((RhythmParameter) rhythmParameter).cloneValue(value));
        }
        return res;
    }

    public void addChangeListener(ChangeListener cl)
    {
        if (!listeners.contains(cl))
        {
            listeners.add(cl);
        }
    }

    public void removeChangeListener(ChangeListener cl)
    {
        listeners.remove(cl);
    }

    private void fireStateChanged()
    {
        for (ChangeListener cl : listeners)
        {
            cl.stateChanged(new ChangeEvent(this));
        }
    }

    @Override
    public String toString()
    {
        return "rhythm=" + rhythm + " rp=" + this.rhythmParameter + " valueBuffer=" + this.valueBuffer;
    }

}

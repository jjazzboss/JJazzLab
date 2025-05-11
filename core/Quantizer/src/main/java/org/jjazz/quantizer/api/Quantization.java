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
package org.jjazz.quantizer.api;

import com.google.common.primitives.Floats;
import java.util.List;
import java.util.TreeSet;
import org.jjazz.harmony.api.SymbolicDuration;

/**
 * Quantization possible values.
 */
public enum Quantization
{

    HALF_BAR(null), // For example beat 0-2 for 4/4, 0-1.5 for a waltz
    BEAT(SymbolicDuration.QUARTER), // This is the "natural beat", beat 0-1-2-3 for 4/4, every 3 eighth note in 12/8.
    HALF_BEAT(SymbolicDuration.EIGHTH),
    ONE_THIRD_BEAT(SymbolicDuration.EIGHTH_TRIPLET),
    ONE_QUARTER_BEAT(SymbolicDuration.SIXTEENTH),
    ONE_SIXTH_BEAT(SymbolicDuration.SIXTEENTH_TRIPLET),
    OFF(null);    // No quantization


    private final float[] beats;
    private final List<Float> beatsAsList;
    private final TreeSet<Float> beatsAsTreeSet;
    private final SymbolicDuration symbolicDuration;

    private Quantization(SymbolicDuration sd)
    {
        this.symbolicDuration = sd;
        this.beats = computeBeats(sd);
        this.beatsAsList = Floats.asList(beats);
        this.beatsAsTreeSet = new TreeSet(beatsAsList);
    }


    /**
     * Get the symbolic duration of on quantized unit.
     *
     * @return Null for OFF or HALF_BAR.
     */
    public SymbolicDuration getSymbolicDuration()
    {
        return symbolicDuration;
    }

    public boolean isTernary()
    {
        return this.equals(ONE_THIRD_BEAT);
    }

    /**
     * An array of the quantized values within a beat.
     * <p>
     * Example: [0, 1/3, 2/3, 1]
     *
     * @return An empty array for OFF and HALF_BAR.
     */
    public float[] getBeats()
    {
        return beats;
    }

    /**
     * Same as getBeats() but return a list.
     *
     * @return
     */
    public List<Float> getBeatsAsList()
    {
        return beatsAsList;
    }

    /**
     * Same as getBeats() but return a TreeSet.
     *
     * @return
     */
    public TreeSet<Float> getBeatsAsTreeSet()
    {
        return beatsAsTreeSet;
    }

    /**
     *
     * @param value
     * @return If true, using Quantization.valueOf(value) will not throw an exception.
     */
    static public boolean isValidStringValue(String value)
    {
        for (Quantization q : values())
        {
            if (q.name().equals(value))
            {
                return true;
            }
        }
        return false;
    }

    private float[] computeBeats(SymbolicDuration sd)
    {
        int nbNotes = sd == null ? 0 : Math.round(1f / sd.getDuration()) + 1;
        float[] res = new float[nbNotes];
        for (int i = 0; i < nbNotes - 1; i++)
        {
            res[i] = i * sd.getDuration();
        }
        if (nbNotes > 0)
        {
            res[nbNotes - 1] = 1f;
        }
        return res;
    }
}

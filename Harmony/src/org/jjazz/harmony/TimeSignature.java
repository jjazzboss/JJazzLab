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
package org.jjazz.harmony;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * The time signature of a bar, e.g. C or 4/4, 6/8 etc...
 */
public enum TimeSignature
{
    TWO_FOUR(2, 4, 1),
    THREE_FOUR(3, 4, 1),
    FOUR_FOUR(4, 4, 1),
    FIVE_FOUR(5, 4, 1),
    SIX_FOUR(6, 4, 1),
    SEVEN_FOUR(7, 4, 1),
    SIX_EIGHT(6, 8, 3),
    TWELVE_EIGHT(12, 8, 3);

    /**
     * Special position for a beat : ensure it's ALWAYS the last position for any bar
     */
    public static final float SYSTEM_END_BEAT = Float.MAX_VALUE;
    /**
     * The number of lower units that make a bar.
     */
    private final int upper;
    /**
     * The unit.
     */
    private final int lower;
    /**
     * The "natural" beat unit expressed in terms of lower value (e.g. for bars 4/4=>1, 12/8=>3).
     */
    private final int naturalBeat;
    private static final Logger LOGGER = Logger.getLogger(TimeSignature.class.getSimpleName());

    /**
     * @param upp The number of lower units that make a bar.
     * @param low The lower unit.
     * @param nBeat The "natural" beat unit expressed in terms of lower value (e.g. for bars 4/4=>1, 12/8=>3)
     */
    private TimeSignature(int upp, int low, int nBeat)
    {
        this.upper = upp;
        this.lower = low;
        this.naturalBeat = nBeat;
    }

    /**
     * Get the TimeSignature object represented by a string representation as the one returned by toString(, e.g. "3/4".
     *
     * @param s The string representation
     * @throws ParseException If syntax error encountered in the string.
     * @return TimeSignature Null if no valid time signature could be constructed.
     */
    static public TimeSignature parse(String s) throws ParseException
    {
        String[] strs = s.trim().split("\\s*/\\s*");

        if (strs.length != 2)
        {
            throw new ParseException("Syntax error in TimeSignature format \"" + s + "\"", 0);
        }

        int up, low;
        try
        {
            up = Integer.parseInt(strs[0]);
            low = Integer.parseInt(strs[1]);
        } catch (NumberFormatException e)
        {
            throw new ParseException("Syntax error in TimeSignature format \"" + s + "\"", 0);
        }
        return get(up, low);
    }

    /**
     * Get the TimeSignature objects represented by a string representation, e.g. "3/4".
     *
     * @param up The uppser value of the TimSignature, e.g. "3" for "3/4"
     *
     * @param low The lower value of the TimSignature, e.g. "4" for "3/4"
     * @return TimeSignature Null if no valid time signature could be constructed.
     */
    static public TimeSignature get(int up, int low)
    {
        TimeSignature result = null;
        for (TimeSignature ts : TimeSignature.values())
        {
            if (ts.getUpper() == up && ts.getLower() == low)
            {
                result = ts;
                break;
            }
        }
        return result;
    }

    public int getUpper()
    {
        return upper;
    }

    public int getLower()
    {
        return lower;
    }

    /**
     * The "natural" beat unit expressed in terms of the lower value. Examples : 4/4=>1, 12/8=>3
     *
     * @return
     */
    public int getNaturalBeat()
    {
        return naturalBeat;
    }

    /**
     * Calculate the most appropriate "half of the bar" position in terms of logical beats. It is used for instance to know where
     * to put the 2nd chord for a 2 chords bar. For even bars (e.g. 4/4) : half, e.g. beat 2 (start at 0) For 3/4 : 1.5 others odd
     * bars : ceil(half), e.g. beat 3 (start at 0) for a 5/4 bar.
     */
    public float getHalfBarBeat()
    {
        float half = getNbNaturalBeats() / 2f;
        if ((getNbNaturalBeats() != 3) && ((getNbNaturalBeats() % 2) != 0))
        {
            // odd bars
            half = (float) Math.ceil(half);
        }
        return half;
    }

    /**
     * @return how many logical beats is there in a bar (e.g. 4/4=>4, 7/4=>7 12/8=>4).
     */
    public int getNbNaturalBeats()
    {
        return upper / naturalBeat;
    }

    /**
     * Tell whether it's a valid beat for that time signature.
     */
    public boolean checkBeat(float beat)
    {
        // Special case
        if (beat == SYSTEM_END_BEAT)
        {
            return true;
        } // normal case
        return !((beat < 0) || (beat >= getNbNaturalBeats()));
    }

    /**
     * @return e.g "3/4" or "6/8"
     */
    @Override
    public String toString()
    {
        return upper + "/" + lower;
    }

    /**
     * The next TimeSignature if increased of 1 unit. For instance 4/4 => 5/4.
     *
     * @return
     * @todo Fix the naturalBeat calculation.
     */
    public TimeSignature getNextTimeSignature()
    {
        int index = -1;
        TimeSignature[] TS_ALL = TimeSignature.values();
        for (int i = 0; i < TS_ALL.length; i++)
        {
            TimeSignature ts = TS_ALL[i];
            if (this == ts)
            {
                index = i;
                break;
            }
        }
        if (index == -1)
        {
            return null;
        }
        return (index == TS_ALL.length - 1) ? TS_ALL[0] : TS_ALL[index + 1];
    }

    /**
     * The previous TimeSignature if decreased by 1 unit. For instance 4/4 => 3/4.
     *
     * @todo Fix the naturalBeat calculation.
     */
    public TimeSignature getPreviousTimeSignature()
    {
        int index = -1;
        TimeSignature[] TS_ALL = TimeSignature.values();
        for (int i = 0; i < TS_ALL.length; i++)
        {
            TimeSignature ts = TS_ALL[i];
            if (this == ts)
            {
                index = i;
                break;
            }
        }
        if (index == -1)
        {
            return null;
        }
        return (index == 0) ? TS_ALL[TS_ALL.length - 1] : TS_ALL[index - 1];
    }
}

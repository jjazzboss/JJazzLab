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
package org.jjazz.harmony.api;

import com.google.common.base.Preconditions;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.ResUtil;

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
     * The half bar position for a ternary 3/4 bar, 5/3=1.6666...
     */
    public static final float SWING_WALTZ_HALF_BAR = 5f / 3f;
    public static final float BEAT_WINDOW = 0.15f;
    /**
     * The number of lower units that make a bar.
     */
    private final int upper;
    /**
     * The unit.
     */
    private final int lower;
    /**
     * The "natural" beat unit expressed in terms of lower value (e.g. for bars 4/4=&gt;1, 12/8=&gt;3).
     */
    private final int naturalBeat;
    private final int nbNaturalBeats;
    private static final Logger LOGGER = Logger.getLogger(TimeSignature.class.getSimpleName());

    /**
     * @param upp   The number of lower units that make a bar.
     * @param low   The lower unit.
     * @param nBeat The "natural" beat unit expressed in terms of lower value (e.g. for bars 4/4=&gt;1, 12/8=&gt;3)
     */
    private TimeSignature(int upp, int low, int nBeat)
    {
        this.upper = upp;
        this.lower = low;
        this.naturalBeat = nBeat;
        this.nbNaturalBeats = upper / naturalBeat;
    }

    /**
     * Get the TimeSignature object represented by a string representation as the one returned by toString(), e.g. "3/4".
     *
     * @param s The string representation
     * @throws ParseException If no valid time signature could be constructed
     * @return
     */
    static public TimeSignature parse(String s) throws ParseException
    {
        String[] strs = s.trim().split("/");
        if (strs.length != 2)
        {
            throw new ParseException("Invalid time signature: " + s, 0);
        }

        TimeSignature res;

        int up, low;
        try
        {
            up = Integer.parseInt(strs[0]);
            low = Integer.parseInt(strs[1]);
            res = get(up, low);
            if (res == null)
            {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e)
        {
            throw new ParseException(ResUtil.getString(TimeSignature.class, "TimeSignature.ERR_Syntax", s), 0);
        }

        return res;
    }

    /**
     * Get the TimeSignature objects represented by a string representation, e.g. "3/4".
     *
     * @param up  The uppser value of the TimSignature, e.g. "3" for "3/4"
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
     * The "natural" beat unit expressed in terms of the lower value. Examples : 4/4=&gt;1, 12/8=&gt;3
     *
     * @return
     */
    public int getNaturalBeat()
    {
        return naturalBeat;
    }

    /**
     * Calculate the most appropriate "half of the bar" position in terms of logical beats.
     * <p>
     * It is used for instance to know where to put the 2nd chord for a 2 chords bar.
     * <p>
     * - For even bars (e.g. 4/4) : half, e.g. beat 2(start at 0)<br>
     * - For 3/4 : 1.5 or 5/3 <br>
     * - Other odd bars : ceil(half), e.g. beat 3 (start at 0) for a 5/4 bar.
     *
     * @param swing If true half-bar for a 3/4 waltz is SWING_WALTZ_HALF_BAR=5/3=1.666...
     * @return
     */
    public float getHalfBarBeat(boolean swing)
    {
        float half = getNbNaturalBeats() / 2f;
        if (getNbNaturalBeats() == 3 && swing)
        {
            half = SWING_WALTZ_HALF_BAR;
        }
        if ((getNbNaturalBeats() != 3) && ((getNbNaturalBeats() % 2) != 0))
        {
            // odd bars
            half = (float) Math.ceil(half);
        }
        return half;
    }

    /**
     * How many "logical" beats there is in a bar.
     * <p>
     * E.g. 4 for 4/4, 7 for 7/4, 4 for 12/8, 3.5 for 7/8.
     *
     * @return
     */
    public float getNbNaturalBeats()
    {
        return nbNaturalBeats;
    }


    /**
     * Tell whether it's a valid beat for that time signature.
     *
     * @param beat
     * @return
     */
    public boolean checkBeat(float beat)
    {
        // Special case
        if (beat == SYSTEM_END_BEAT)
        {
            return true;
        }

        // normal case
        return !((beat < 0) || (beat >= getNbNaturalBeats()));
    }

    /**
     * Check if the specified beat is a "downbeat" for the time signature.
     * <p>
     * Downbeats for 4/4 are 0 and 2. If TimeSignature is not handled, return false.
     *
     * @param beat Must be a valid beat value for this TimeSignature. Accept a BEAT_WINDOW error on the downbeat position.
     * @return
     */
    public boolean isDownBeat(float beat)
    {
        Preconditions.checkArgument(checkBeat(beat), "this=%s beat=%s", this, beat);
        final List<FloatRange> T44_DOWN_RANGES = List.of(new FloatRange(0, BEAT_WINDOW), new FloatRange(2 - BEAT_WINDOW, 2 + BEAT_WINDOW));
        final List<FloatRange> T34_DOWN_RANGES = List.of(new FloatRange(0, BEAT_WINDOW), new FloatRange(1 - BEAT_WINDOW, 1 + BEAT_WINDOW));
        final List<FloatRange> DEF_DOWN_RANGES = List.of(FloatRange.EMPTY_FLOAT_RANGE);

        List<FloatRange> downRanges = switch (this)
        {
            case FOUR_FOUR ->
                T44_DOWN_RANGES;
            case THREE_FOUR ->
                T34_DOWN_RANGES;
            default ->
                DEF_DOWN_RANGES;
        };

        boolean b = downRanges.stream().anyMatch(fr -> fr.contains(beat, false));

        return b;
    }

    /**
     * Check if the specified beat is an "upbeat" for the time signature.
     * <p>
     * Upbeats for 4/4 are 1 and 3. If TimeSignature is not handled, return false.
     *
     * @param beat Must be a valid beat value for this TimeSignature. Accept a BEAT_WINDOW error on the downbeat position.
     * @return
     */
    public boolean isUpBeat(float beat)
    {
        Preconditions.checkArgument(checkBeat(beat), "this=%s beat=%s", this, beat);

        final List<FloatRange> T44_UP_RANGES = List.of(new FloatRange(1 - BEAT_WINDOW, 1 + BEAT_WINDOW), new FloatRange(3 - BEAT_WINDOW, 3 + BEAT_WINDOW));
        final List<FloatRange> T34_UP_RANGES = List.of(new FloatRange(2 - BEAT_WINDOW, 2 + BEAT_WINDOW));
        final List<FloatRange> DEF_UP_RANGES = List.of(FloatRange.EMPTY_FLOAT_RANGE);

        List<FloatRange> downRanges = switch (this)
        {
            case FOUR_FOUR ->
                T44_UP_RANGES;
            case THREE_FOUR ->
                T34_UP_RANGES;
            default ->
                DEF_UP_RANGES;
        };

        boolean b = downRanges.stream().anyMatch(fr -> fr.contains(beat, false));

        return b;
    }


    /**
     * Check if beat is &lt;= (getNbNaturalBeats() - beatWindow).
     *
     * @param beat The relative beat within a bar
     * @param beatWindow 
     * @return
     */
    public boolean isEndOfBar(float beat, float beatWindow)
    {
        return beat >= nbNaturalBeats - beatWindow;
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
     * The next TimeSignature if increased of 1 unit. For instance 4/4 =&gt; 5/4.
     *
     * @return
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
     * The previous TimeSignature if decreased by 1 unit.
     * <p>
     * For instance 4/4 =&gt; 3/4
     *
     * @return
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

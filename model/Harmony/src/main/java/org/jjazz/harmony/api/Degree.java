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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * The possible degrees that make a chord.
 */
public enum Degree
{
    ROOT(Natural.ROOT, 0),
    NINTH_FLAT(Natural.NINTH, -1),
    NINTH(Natural.NINTH, 0),
    NINTH_SHARP(Natural.NINTH, 1), THIRD_FLAT(Natural.THIRD, -1),
    THIRD(Natural.THIRD, 0),
    FOURTH_OR_ELEVENTH(Natural.ELEVENTH, 0),
    ELEVENTH_SHARP(Natural.ELEVENTH, 1), FIFTH_FLAT(Natural.FIFTH, -1),
    FIFTH(Natural.FIFTH, 0),
    FIFTH_SHARP(Natural.FIFTH, 1), THIRTEENTH_FLAT(Natural.SIXTH, -1),
    SIXTH_OR_THIRTEENTH(Natural.SIXTH, 0), // We consider the sixth as a thirteenth in all cases
    SEVENTH_FLAT(Natural.SEVENTH, -1),
    SEVENTH(Natural.SEVENTH, 0);

    /**
     * The natural degrees.
     */
    public enum Natural
    {
        ROOT(1, 0), NINTH(9, 2), THIRD(3, 4), ELEVENTH(11, 5), FIFTH(5, 7), SIXTH(13, 9), SEVENTH(7, 11);
        private int intValue;
        private int pitch;

        private Natural(int value, int pitch)
        {
            this.intValue = value;
            this.pitch = pitch;
        }

        public int getIntValue()
        {
            return intValue;
        }

        public int getPitch()
        {
            return pitch;
        }

        public String toStringShort()
        {
            return String.valueOf(intValue);
        }

        /**
         * @param intValue Possible values = 1, 9, 3, 11, 5, 13, 7
         * @return The Natural which match intValue, or null.
         */
        static public Natural getFromIntValue(int intValue)
        {
            for (Natural b : Natural.values())
            {
                if (b.intValue == intValue)
                {
                    return b;
                }
            }
            return null;
        }

        /**
         * @param relPitch 0-11
         * @return The Natural which match relPitch, or null.
         */
        static public Natural get(int relPitch)
        {
            for (Natural b : Natural.values())
            {
                if (b.pitch == relPitch)
                {
                    return b;
                }
            }
            return null;
        }

    }

    private static final Logger LOGGER = Logger.getLogger(Degree.class.getSimpleName());

    private final Natural natural;
    private final int accidental;

    /**
     *
     * @param n
     * @param defaultAccidental
     * @param incompatibleDegrees Degrees usually "musically incompatible" with this Degree.
     */
    private Degree(Natural n, int defaultAccidental, Degree... incompatibleDegrees)
    {
        this.natural = n;
        this.accidental = defaultAccidental;
    }

    /**
     * The relative pitch of the degree
     *
     * @return [0-11]
     */
    public int getPitch()
    {
        return natural.getPitch() + accidental;
    }

    public Natural getNatural()
    {
        return natural;
    }

    /**
     * @return -1, 0 or 1. For ex. -1 for b9.
     */
    public int getAccidental()
    {
        return accidental;
    }

    /**
     * @return "0", "3", "b9", "#11", etc
     */
    public String toStringShort()
    {
        if (accidental == -1)
        {
            return "b" + natural.toStringShort();
        } else if (accidental == +1)
        {
            return "#" + natural.toStringShort();
        }
        return natural.toStringShort();
    }

    /**
     * Example: n=NINTH, alt=+1, return NINTH_SHARP
     *
     * @param n
     * @param alt
     * @return A ChordDegree or null if no degree match
     */
    static public Degree getDegree(Natural n, int alt)
    {
        Objects.requireNonNull(n, "n");
        Preconditions.checkArgument(alt >= -1 && alt <= 1, "n=%s alt=%s", n, alt);
        for (Degree d : Degree.values())
        {
            if (d.getNatural() == n && d.getAccidental() == alt)
            {
                return d;
            }
        }
        return null;
    }

    /**
     * The degrees who match the relative pitch. Ex. relPitch=4 =&gt; THIRD Ex. relPitch=3 =&gt; NINTH_SHARP and THIRD_FLAT
     *
     * @param relPitch
     * @return A Degree list with 1 or 2 degrees.
     */
    static public List<Degree> getDegrees(int relPitch)
    {
        Preconditions.checkArgument(relPitch >= 0 && relPitch <= 11, "relPitch=%s", relPitch);
        ArrayList<Degree> res = new ArrayList<>();
        for (Degree d : Degree.values())
        {
            if (d.getPitch() == relPitch)
            {
                res.add(d);
            }
        }
        return res;
    }

    /**
     * Provide the "most probable" degree for the specified relpitch.
     * <p>
     * Ex: relPitch=3 (Eb) =&gt; return THIRD_MINOR rather than NINTH_SHARP.
     *
     * @param relPitch 0-11
     * @return A Degree. Can't be null.
     */
    static public Degree getDegreeMostProbable(int relPitch)
    {
        Preconditions.checkArgument(relPitch >= 0 && relPitch <= 11, "relPitch=%s", relPitch);
        return switch (relPitch)
        {
            case 0 -> ROOT;
            case 1 -> NINTH_FLAT;
            case 2 -> NINTH;
            case 3 -> THIRD_FLAT;           // and not NINTH_SHARP
            case 4 -> THIRD;
            case 5 -> FOURTH_OR_ELEVENTH;
            case 6 -> FIFTH_FLAT;           // and not ELEVENTH_SHARP
            case 7 -> FIFTH;
            case 8 -> FIFTH_SHARP;          // and not THIRTEENTH_FLAT
            case 9 -> SIXTH_OR_THIRTEENTH;
            case 10 -> SEVENTH_FLAT;
            case 11 -> SEVENTH;
            default -> throw new IllegalArgumentException("relPitch=" + relPitch);
        };
    }


    /**
     * A special equals() which considers SIXTH_OR_THIRTEENTH and SEVENTH equal.
     *
     * @param d
     * @return
     */
    public boolean equalsSixthMajorSeventh(Degree d)
    {
        boolean b = this == d || (this == SIXTH_OR_THIRTEENTH && d == SEVENTH) || (d == SIXTH_OR_THIRTEENTH && this == SEVENTH);
        return b;
    }
}

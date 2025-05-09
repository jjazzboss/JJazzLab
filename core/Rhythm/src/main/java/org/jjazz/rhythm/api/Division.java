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
package org.jjazz.rhythm.api;


/**
 * The rhythm main division.
 */
public enum Division
{
    BINARY,
    /**
     * jazz swing with triplet division
     */
    EIGHTH_SHUFFLE,
    /**
     * Similar to 12/8 signature.
     */
    EIGHTH_TRIPLET,
    UNKNOWN;

    /**
     * True if BINARY.
     *
     * @return
     */
    public boolean isBinary()
    {
        return this == BINARY;
    }

    /**
     * True if EIGHTH_SHUFFLE or EIGHTH_TRIPLET.
     *
     * @return
     */
    public boolean isTernary()
    {
        return this == EIGHTH_SHUFFLE || this == EIGHTH_TRIPLET;
    }

    /**
     * True if EIGHTH_SHUFFLE.
     *
     * @return
     */
    public boolean isSwing()
    {
        return this == EIGHTH_SHUFFLE;
    }

    static public Division valueOf(String s, Division defaultValue)
    {
        try
        {
            return valueOf(s);
        } catch (IllegalArgumentException e)
        {
            return defaultValue;
        }
    }


    /**
     * Try to guess the division type from the specified parameters.
     *
     * @param genre If null return UNKNOWN
     * @param text  Ignored if null
     * @param tempo Ignored if negative value
     * @return UNKNOWN if no matching found.
     */
    static public Division guess(Genre genre, String text, int tempo)
    {
        Division res;
        if (null == genre)
        {
            res = Division.UNKNOWN;
        } else
        {
            res = switch (genre)
            {
                case UNKNOWN ->
                    safeContainsOneOf(text, "16beat", "16bt", "16-b", "8beat", "8bt", "8-b",
                    "bossa", "samba", "rumba", "mambo", "forro", "chorro", "salsa", "latin", "cuba", "calypso", "chach", "mereng", "world",
                    "pop", "rock", "funk", "soul", "r&b", "rnb", "hip", "country", "reggae",
                    "dance", "electro", "house", "garage")
                    ? BINARY : UNKNOWN;
                case JAZZ ->
                    safeContainsOneOf(text, "rock", "fusi", "acid", "smoot") ? BINARY : EIGHTH_SHUFFLE;
                default ->
                    BINARY;
            };
        }

        return res;
    }

    static private boolean safeContainsOneOf(String text, CharSequence... strs)
    {
        if (text == null)
        {
            return false;
        }
        for (var str : strs)
        {
            if (text.contains(str))
            {
                return true;
            }
        }
        return false;
    }


}

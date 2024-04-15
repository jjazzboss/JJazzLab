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
package org.jjazz.importers.api;

import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Beat;
import org.jjazz.rhythm.api.Feel;
import org.jjazz.rhythm.api.Intensity;
import org.jjazz.rhythm.api.Genre;

/**
 *
 */
public class BiabStyleFeatures
{

    public Genre genre;
    public Beat beat;
    public Intensity intensity;
    public Feel feel;
    public TimeSignature timeSignature;
    private static BiabStyleFeatures[] DATA;

    public BiabStyleFeatures(Genre g, Beat b, Intensity i, Feel f, TimeSignature ts)
    {
        genre = g;
        beat = b;
        intensity = i;
        feel = f;
        timeSignature = ts;
    }

    public static BiabStyleFeatures getStyleFeatures(int byteValue)
    {
        initData();
        if (byteValue >= DATA.length)
        {
            return null;
        }
        return DATA[byteValue - 1];
    }

    /**
     * Guess based on file extension.
     *
     * @return Can be UNKNOWN
     */

    public static Genre guessGenre(String fileName)
    {
        initData();
        char c = fileName.charAt(fileName.length() - 1);
        if (c > 'N' || c < '1' || (c > '9' && c < 'A'))
        {
            return Genre.UNKNOWN;
        }
        int index;
        if (c <= '9')
        {
            index = c - '1';
        } else
        {
            index = 9 + (c - 'A');
        }
        return DATA[index].genre;
    }


    static private void initData()
    {
        if (DATA != null)
        {
            return;
        }
        DATA = new BiabStyleFeatures[]
        {
            new BiabStyleFeatures(Genre.JAZZ, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.FOUR_FOUR), // 1
            new BiabStyleFeatures(Genre.COUNTRY, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.TWELVE_EIGHT), // 2
            new BiabStyleFeatures(Genre.COUNTRY, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR), // 3
            new BiabStyleFeatures(Genre.LATIN, Beat.EIGHT, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR), // 4
            new BiabStyleFeatures(Genre.WORLD, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// 5
            new BiabStyleFeatures(Genre.RB, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.FOUR_FOUR),// 6
            new BiabStyleFeatures(Genre.RB, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// 7
            new BiabStyleFeatures(Genre.BALLROOM, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.THREE_FOUR),// 8
            new BiabStyleFeatures(Genre.POP, Beat.UNKNOWN, Intensity.LIGHT, Feel.BINARY, TimeSignature.FOUR_FOUR),// 9
            new BiabStyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.FOUR_FOUR), // A
            new BiabStyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.LIGHT, Feel.BINARY, TimeSignature.FOUR_FOUR), // B
            new BiabStyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.MEDIUM, Feel.BINARY, TimeSignature.FOUR_FOUR), // C
            new BiabStyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.HEAVY, Feel.BINARY, TimeSignature.FOUR_FOUR),// D
            new BiabStyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.MEDIUM, Feel.BINARY, TimeSignature.FOUR_FOUR),// E
            new BiabStyleFeatures(Genre.POP, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// F
            new BiabStyleFeatures(Genre.FUNK, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// G
            new BiabStyleFeatures(Genre.JAZZ, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.THREE_FOUR),// H
            new BiabStyleFeatures(Genre.LATIN, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// I
            new BiabStyleFeatures(Genre.LATIN, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// J
            new BiabStyleFeatures(Genre.JAZZ, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.FOUR_FOUR),// K
            new BiabStyleFeatures(Genre.COUNTRY, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// L
            new BiabStyleFeatures(Genre.POP, Beat.UNKNOWN, Intensity.LIGHT, Feel.BINARY, TimeSignature.TWELVE_EIGHT),// M
            new BiabStyleFeatures(Genre.COUNTRY, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.TWELVE_EIGHT),// N
            new BiabStyleFeatures(Genre.REGGAE, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR)// O
        };
    }
}

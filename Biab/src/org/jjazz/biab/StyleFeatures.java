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
package org.jjazz.biab;

import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Beat;
import org.jjazz.rhythm.api.Feel;
import org.jjazz.rhythm.api.Intensity;
import org.jjazz.rhythm.api.Genre;

/**
 *
 */
public class StyleFeatures
{

    public Genre genre;
    public Beat beat;
    public Intensity intensity;
    public Feel feel;
    public TimeSignature timeSignature;
    private static StyleFeatures[] DATA;

    public StyleFeatures(Genre g, Beat b, Intensity i, Feel f, TimeSignature ts)
    {
        genre = g;
        beat = b;
        intensity = i;
        feel = f;
        timeSignature = ts;
    }

    public static StyleFeatures getStyleFeatures(int byteValue)
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
        DATA = new StyleFeatures[]
        {
            new StyleFeatures(Genre.JAZZ, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.FOUR_FOUR), // 1
            new StyleFeatures(Genre.COUNTRY, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.TWELVE_EIGHT), // 2
            new StyleFeatures(Genre.COUNTRY, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR), // 3
            new StyleFeatures(Genre.LATIN, Beat.EIGHT, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR), // 4
            new StyleFeatures(Genre.WORLD, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// 5
            new StyleFeatures(Genre.RB, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.FOUR_FOUR),// 6
            new StyleFeatures(Genre.RB, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// 7
            new StyleFeatures(Genre.BALLROOM, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.THREE_FOUR),// 8
            new StyleFeatures(Genre.POP, Beat.UNKNOWN, Intensity.LIGHT, Feel.BINARY, TimeSignature.FOUR_FOUR),// 9
            new StyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.FOUR_FOUR), // A
            new StyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.LIGHT, Feel.BINARY, TimeSignature.FOUR_FOUR), // B
            new StyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.MEDIUM, Feel.BINARY, TimeSignature.FOUR_FOUR), // C
            new StyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.HEAVY, Feel.BINARY, TimeSignature.FOUR_FOUR),// D
            new StyleFeatures(Genre.ROCK, Beat.UNKNOWN, Intensity.MEDIUM, Feel.BINARY, TimeSignature.FOUR_FOUR),// E
            new StyleFeatures(Genre.POP, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// F
            new StyleFeatures(Genre.FUNK, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// G
            new StyleFeatures(Genre.JAZZ, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.THREE_FOUR),// H
            new StyleFeatures(Genre.LATIN, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// I
            new StyleFeatures(Genre.LATIN, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// J
            new StyleFeatures(Genre.JAZZ, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.TERNARY, TimeSignature.FOUR_FOUR),// K
            new StyleFeatures(Genre.COUNTRY, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR),// L
            new StyleFeatures(Genre.POP, Beat.UNKNOWN, Intensity.LIGHT, Feel.BINARY, TimeSignature.TWELVE_EIGHT),// M
            new StyleFeatures(Genre.COUNTRY, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.TWELVE_EIGHT),// N
            new StyleFeatures(Genre.REGGAE, Beat.UNKNOWN, Intensity.UNKNOWN, Feel.BINARY, TimeSignature.FOUR_FOUR)// O
        };
    }
}

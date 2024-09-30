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
import org.jjazz.rhythm.api.Division;
import org.jjazz.rhythm.api.Genre;

/**
 * Bridge to JJazzLab RhythmFeatures.
 */
public class BiabStyleFeatures
{

    public Genre genre;
    public Division division;
    public TimeSignature timeSignature;
    private static BiabStyleFeatures[] DATA;

    public BiabStyleFeatures(Genre g, Division d, TimeSignature ts)
    {
        genre = g;
        division = d;
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
     * @param fileName
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
            new BiabStyleFeatures(Genre.JAZZ, Division.EIGHTH_SHUFFLE, TimeSignature.FOUR_FOUR), // 1
            new BiabStyleFeatures(Genre.COUNTRY, Division.EIGHTH_TRIPLET, TimeSignature.TWELVE_EIGHT), // 2
            new BiabStyleFeatures(Genre.COUNTRY, Division.BINARY, TimeSignature.FOUR_FOUR), // 3
            new BiabStyleFeatures(Genre.LATIN, Division.BINARY, TimeSignature.FOUR_FOUR), // 4
            new BiabStyleFeatures(Genre.WORLD, Division.BINARY, TimeSignature.FOUR_FOUR),// 5
            new BiabStyleFeatures(Genre.RnB, Division.EIGHTH_SHUFFLE, TimeSignature.FOUR_FOUR),// 6
            new BiabStyleFeatures(Genre.RnB, Division.BINARY, TimeSignature.FOUR_FOUR),// 7
            new BiabStyleFeatures(Genre.BALLROOM, Division.BINARY, TimeSignature.THREE_FOUR),// 8
            new BiabStyleFeatures(Genre.POP, Division.BINARY, TimeSignature.FOUR_FOUR),// 9
            new BiabStyleFeatures(Genre.ROCK, Division.EIGHTH_SHUFFLE, TimeSignature.FOUR_FOUR), // A
            new BiabStyleFeatures(Genre.ROCK, Division.BINARY, TimeSignature.FOUR_FOUR), // B
            new BiabStyleFeatures(Genre.ROCK, Division.BINARY, TimeSignature.FOUR_FOUR), // C
            new BiabStyleFeatures(Genre.ROCK, Division.BINARY, TimeSignature.FOUR_FOUR),// D
            new BiabStyleFeatures(Genre.ROCK, Division.BINARY, TimeSignature.FOUR_FOUR),// E
            new BiabStyleFeatures(Genre.POP, Division.BINARY, TimeSignature.FOUR_FOUR),// F
            new BiabStyleFeatures(Genre.FUNK, Division.BINARY, TimeSignature.FOUR_FOUR),// G
            new BiabStyleFeatures(Genre.JAZZ, Division.EIGHTH_SHUFFLE, TimeSignature.THREE_FOUR),// H
            new BiabStyleFeatures(Genre.LATIN, Division.BINARY, TimeSignature.FOUR_FOUR),// I
            new BiabStyleFeatures(Genre.LATIN, Division.BINARY, TimeSignature.FOUR_FOUR),// J
            new BiabStyleFeatures(Genre.JAZZ, Division.EIGHTH_SHUFFLE, TimeSignature.FOUR_FOUR),// K
            new BiabStyleFeatures(Genre.COUNTRY, Division.BINARY, TimeSignature.FOUR_FOUR),// L
            new BiabStyleFeatures(Genre.POP, Division.EIGHTH_TRIPLET, TimeSignature.TWELVE_EIGHT),// M
            new BiabStyleFeatures(Genre.COUNTRY, Division.EIGHTH_TRIPLET, TimeSignature.TWELVE_EIGHT),// N
            new BiabStyleFeatures(Genre.REGGAE, Division.BINARY, TimeSignature.FOUR_FOUR)// O
        };
    }
}

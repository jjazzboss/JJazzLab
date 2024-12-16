package org.jjazz.instrumentcomponents.guitardiagram.api;

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
 *  
 */
 /*
 * NOTE: code reused and modified from the TuxGuitar software (GNU Lesser GPL license), author: Julián Gabriel Casadesús
 */
/**
 * Stores the information about the chord name, structure and accidental abilities into a list
 *
 * @author Nikola Kolarovic &lt;nikola.kolarovic@gmail.com&gt;
 *
 */
public class TGChordDatabase
{

    /**
     * fills all the necessary data into chords List consisted out of ChordInfo-s
     * <p>
     * If you want to change it, please contact me on nikola.kolarovic@gmail.com
     */
    private static final ChordInfo[] data = new ChordInfo[]
    {

        // Major--------------------------   0
        new ChordInfo("M", new int[]
        {
            1, 5, 8
        }),
        // 7--------------------------    1
        new ChordInfo("7", new int[]
        {
            1, 5, 8, 11
        }),
        // 7M--------------------------   2
        // hard-coded index used in ChordRecognizer, below comment "determine seventh", line 315 now
        new ChordInfo("maj7", new int[]
        {
            1, 5, 8, 12
        }),
        // 6--------------------------   3
        new ChordInfo("6", new int[]
        {
            1, 5, 8, 10
        }),
        // m--------------------------   4
        // index 4 hard-coded in ChordRecognizer line 220, so it is not so unusual
        new ChordInfo("m", new int[]
        {
            1, 4, 8
        }),
        // m7--------------------------   5
        new ChordInfo("m7", new int[]
        {
            1, 4, 8, 11
        }),
        // m7M--------------------------  6
        new ChordInfo("m/maj7", new int[]
        {
            1, 4, 8, 12
        }),
        // m6--------------------------  7
        new ChordInfo("m6", new int[]
        {
            1, 4, 8, 10
        }),
        // sus2--------------------------  8
        new ChordInfo("sus2", new int[]
        {
            1, 3, 8
        }),
        // sus4--------------------------   9
        new ChordInfo("sus4", new int[]
        {
            1, 6, 8
        }),
        // 7sus2--------------------------   10
        new ChordInfo("7sus2", new int[]
        {
            1, 3, 8, 11
        }),
        // 7sus4--------------------------   11
        new ChordInfo("7sus4", new int[]
        {
            1, 6, 8, 11
        }),
        // below indexes are hard-coded in ChordRecognizer line 311 now

        // dim--------------------------   12
        new ChordInfo("dim", new int[]
        {
            1, 4, 7
        }),
        // dim7--------------------------   13
        new ChordInfo("dim7", new int[]
        {
            1, 4, 7, 10
        }),
        // aug--------------------------   14
        new ChordInfo("aug", new int[]
        {
            1, 5, 9
        }),
        // 5--------------------------   15
        // index <last> hard-coded in ChordRecognizer line 220, so it is not so unusual
        new ChordInfo("5", new int[]
        {
            1, 8
        }),
        // JJazzLab addition dim7M ---- 16
        new ChordInfo("dim7M", new int[]
        {
            1, 4, 7, 12
        })
    };

    public static int length()
    {
        return data.length;
    }

    public static ChordInfo get(int index)
    {
        return data[index];
    }

    /**
     * chord data structure, contains all info for chord formation *
     */
    public static class ChordInfo
    {

        private String name;
        private int[] requiredNotes;

        public ChordInfo(String name, int[] requiredNotes)
        {
            this.name = name;
            this.requiredNotes = requiredNotes;
        }

        public String getName()
        {
            return this.name;
        }

        public int[] getRequiredNotes()
        {
            return this.requiredNotes;
        }

        public int[] cloneRequireds()
        {
            int[] requiredNotes = new int[this.requiredNotes.length];
            for (int i = 0; i < requiredNotes.length; i++)
            {
                requiredNotes[i] = this.requiredNotes[i];
            }
            return requiredNotes;
        }
    }
}

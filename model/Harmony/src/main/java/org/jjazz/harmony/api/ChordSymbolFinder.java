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


import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.math.BigIntegerMath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.utilities.api.Utilities;

/**
 * Find matching chord symbol(s) from individual notes.
 */
public class ChordSymbolFinder
{


    static public final int MAX_NOTES = 5;
    static long[] positions3;
    static long[] positions4;
    static long[] positions5;
    static private List<ChordType> allChordTypes;
    private final int maxNbNotes;
    private static final Logger LOGGER = Logger.getLogger(ChordSymbolFinder.class.getSimpleName());  


    /**
     * Create an instance.
     * 
     * @param maxNbNotes The maximum number of chord notes supported : 3, 4 or 5.
     */
    public ChordSymbolFinder(int maxNbNotes)
    {
        checkArgument(maxNbNotes >= 3 && maxNbNotes <= MAX_NOTES, "maxNbNotes=%s", maxNbNotes);
        this.maxNbNotes = maxNbNotes;
    }

    /**
     * @return The maximum number of chord notes supported: 3, 4, or 5.
     */
    public int getMaxNbNotes()
    {
        return maxNbNotes;
    }

    /**
     * This must be called once before using any ChordFinder instance.
     * <p>
     * The method may take up to 2 or 3 seconds on very slow computers to complete. If called more than once, does nothing.
     */
    static public void buildStaticData()
    {
        if (allChordTypes == null)
        {
            allChordTypes = ChordTypeDatabase.getDefault().getChordTypes();
            buildPositionsDatabase();
        }
    }

    /**
     * Select one chord symbol from the provided chord symbols.
     * <p>
     * If one of the chord symbols root matches the first pitch, it is returned. Otherwise return the most "common" chord symbol.
     *
     * @param notes The original notes which led to the chordSymbols, ordered by ascending pitch.
     * @param chordSymbols The ChordSymbols to choose from. Can't be empty.
     * @param lowerNoteIsBass For ex. G-C-E pitches will return C/G.
     * @return
     * @see ChordSymbolFinder#find(java.util.List)
     */
    public ChordSymbol getChordSymbol(List<Note> notes, List<ChordSymbol> chordSymbols, boolean lowerNoteIsBass)
    {
        checkArgument(chordSymbols != null && notes != null && !notes.isEmpty(),
                "notes=%s, chordSymbols=%s, lowerNoteIsBass=%s", notes, chordSymbols, lowerNoteIsBass);

        ChordSymbol chordSymbol;
        Note firstNote = notes.get(0);

        if (chordSymbols.isEmpty())
        {
            return null;
        } else if (chordSymbols.size() == 1)
        {
            // Easy
            chordSymbol = chordSymbols.get(0);
        } else
        {
            // Search for a chord symbol with root matching
            chordSymbol = chordSymbols.stream()
                    .filter(cs -> cs.getRootNote().equalsRelativePitch(firstNote))
                    .findAny()
                    .orElse(null);

            // Find most common chord symbol
            if (chordSymbol == null)
            {
                chordSymbol = pick(chordSymbols);
            }
        }

        if (lowerNoteIsBass && !chordSymbol.getRootNote().equalsRelativePitch(firstNote))
        {
            chordSymbol = new ChordSymbol(chordSymbol.getRootNote(), firstNote, chordSymbol.getChordType());
        }

        return chordSymbol;
    }

    /**
     * Find the chord symbols which match the specified notes.
     *
     * @param notes
     * @return Can return max 4 chord symbols (e.g. for dim7 notes like C Eb Gb A)
     */
    public List<ChordSymbol> find(List<Note> notes)
    {
        checkStaticData();

        List<ChordSymbol> res = new ArrayList<>();
        if (notes.size() < 3 || notes.size() > getMaxNbNotes())
        {
            return res;
        }

        int index = computeIndex(notes);
        long value;
        switch (notes.size())
        {
            case 3:
                value = positions3[index];
                break;
            case 4:
                value = positions4[index];
                break;
            case 5:
                value = positions5[index];
                break;
            default:
                throw new IllegalStateException("pitches.size()=" + notes.size());
        }
        List<ChordSymbol> list = decodeChordSymbols(value);
        if (list != null)
        {
            res.addAll(list);
        }

        return res;
    }


    /**
     * Get the index in the positions database for the specified notes list.
     *
     * @param notes
     * @return
     */
    static public int computeIndex(List<Note> notes)
    {
        int index;
        int p0 = notes.get(0).getRelativePitch();
        int p1 = notes.get(1).getRelativePitch();
        int p2 = notes.get(2).getRelativePitch();
        int p3;
        int p4;
        switch (notes.size())
        {
            case 3:
                index = p0 + 12 * p1 + 12 * 12 * p2;
                break;
            case 4:
                p3 = notes.get(3).getRelativePitch();
                index = p0 + 12 * p1 + 12 * 12 * p2 + 12 * 12 * 12 * p3;
                break;
            case 5:
                p3 = notes.get(3).getRelativePitch();
                p4 = notes.get(4).getRelativePitch();
                index = p0 + 12 * p1 + 12 * 12 * p2 + 12 * 12 * 12 * p3 + 12 * 12 * 12 * 12 * p4;
                break;
            default:
                throw new IllegalStateException("pitches.size()=" + notes.size());
        }
        return index;
    }


    // =====================================================================================
    // Private methods
    // =====================================================================================
    /**
     * Build the internal database : all relative pitch permutations for each chord symbol.
     * <p>
     * Can take some time to complete, maybe 1 or 2 seconds on slow computers ?
     */
    static private void buildPositionsDatabase()
    {
        long startTime = System.nanoTime();

        positions3 = new long[12 * 12 * 12];             // 1.7k
        if (MAX_NOTES > 3)
        {
            positions4 = new long[12 * 12 * 12 * 12];          // 20k
        }
        if (MAX_NOTES > 4)
        {
            positions5 = new long[12 * 12 * 12 * 12 * 12];       // 248k
        }


        long positionCount = 0;

        // Each key
        for (int rootPitch = 0; rootPitch < 12; rootPitch++)
        {
            Note rootNote = new Note(rootPitch);


            // Each chord type
            for (ChordType ct : ChordTypeDatabase.getDefault().getChordTypes())
            {
                var chord = ct.getChord();
                int nbNotes = chord.size();
                if (nbNotes < 3 || nbNotes > MAX_NOTES)
                {
                    continue;
                }


                chord.transpose(rootPitch);
                ChordSymbol cs = new ChordSymbol(rootNote, ct);


                // Compute all possible positions for this chord symbol
                List<Integer> pitches = chord.getNotes().stream().map(n -> n.getRelativePitch()).collect(Collectors.toList());
                List<Integer[]> pitchPermutations = new ArrayList<>(BigIntegerMath.factorial(nbNotes).intValue());
                Utilities.heapPermutation(pitches.toArray(new Integer[nbNotes]), nbNotes, pitchPermutations);


                // Each position                
                for (Integer[] perm : pitchPermutations)
                {
                    int index;
                    long value;
                    switch (nbNotes)
                    {
                        case 3:
                            index = computeIndex(Arrays.asList(new Note(perm[0]), new Note(perm[1]), new Note(perm[2])));
                            value = positions3[index];
                            value = encodeChordSymbol(cs, value);
                            positions3[index] = value;
                            break;
                        case 4:
                            index = computeIndex(Arrays.asList(new Note(perm[0]), new Note(perm[1]), new Note(perm[2]), new Note(perm[3])));
                            value = positions4[index];
                            value = encodeChordSymbol(cs, value);
                            positions4[index] = value;
                            break;
                        case 5:
                            index = computeIndex(Arrays.asList(new Note(perm[0]), new Note(perm[1]), new Note(perm[2]), new Note(perm[3]), new Note(perm[4])));
                            value = positions5[index];
                            value = encodeChordSymbol(cs, value);
                            positions5[index] = value;
                            break;
                        default:
                            throw new IllegalStateException("nbNotes=" + nbNotes);
                    }
                    positionCount++;
                }

            }
        }
        double durationInMs = (System.nanoTime() - startTime) / 1000000d;
        LOGGER.log(Level.INFO, "buildPositionsDatabase() complete in {0}ms for {1} positions.", new Object[]{durationInMs, positionCount});
    }

    /**
     * Add a chordSymbol encoded in a long value (up to 4 chord symbols can be encoded).
     * <p>
     * 1 Chord Symbol = 16 bits = [bit15-4]:chordtype index, [bit 3-0]:rootNote+1<br>
     * If the 4 chord symbol slots are already used, nothing is done.
     *
     *
     * @param cs
     * @param v
     * @return The new value
     */
    static private long encodeChordSymbol(ChordSymbol cs, long v)
    {
        // Encode the chord symbol on 16 bits
        long data = cs.getRootNote().getRelativePitch() + 1;       // +1 so that 16-bit value==0 means no chord symbol defined
        int ctIndex = allChordTypes.indexOf(cs.getChordType());
        data |= ctIndex << 4;


        // Find a free slot (4 possibles)
        int i = 0;
        while (i < 4 && (v & (0xFFFFL << i * 16)) != 0)     // 0xFFFF MUST be a long too!
        {
            i++;
        }


        // If free slot, add our data
        long res = v;
        if (i < 4)
        {
            res |= (data << i * 16);
        }

        return res;
    }


    /**
     * Retrieve up to 4 chord symbols from the long value.
     *
     * @param value
     * @return Null if no chord symbols.
     * @see ChordSymbolFinder#encodeChordSymbol(org.jjazz.harmony.api.ChordSymbol, long)
     */
    private List<ChordSymbol> decodeChordSymbols(long value)
    {
        if (value == 0)
        {
            return null;
        }
        List<ChordSymbol> res = new ArrayList<>();

        // Loop on occupied slots
        int i = 0;
        while (i < 4 && (value & (0xFFFFL << (i * 16))) != 0)       // 0xFFFF MUST be a long too!
        {
            long data1 = value & (0xFFFFL << (i * 16));
            if (data1 == 0)
            {
                break;
            }

            long data2 = data1 >>> i * 16;
            int rootPitch = (int) ((data2 & 0xF) - 1);
            int ctIndex = (int) ((data2 & 0xFFF0) >>> 4);
            assert !(rootPitch < 0 || rootPitch > 11 || ctIndex < 0 || ctIndex >= allChordTypes.size()) : "rootPitch=" + rootPitch + " ctIndex=" + ctIndex + " value=b" + Long.toBinaryString(value) + " i=" + i
                    + " or " + value + "  data1=b" + Long.toBinaryString(data1) + " data2=b" + Long.toBinaryString(data2);
            ChordType ct = allChordTypes.get(ctIndex);
            res.add(new ChordSymbol(new Note(rootPitch), ct));

            i++;
        }


        return res;
    }

    /**
     * Choose a chord symbol amongst a list.
     * <p>
     * Rely on the actual possible chord type list (see test data at the bottom of the file in comments) to try to choose the most
     * common chord symbol.
     *
     * @param chordSymbols
     * @return
     */
    private ChordSymbol pick(List<ChordSymbol> chordSymbols)
    {
        assert chordSymbols.size() == 2 || chordSymbols.size() == 3 : "chordSymbols=" + chordSymbols;

        ChordSymbol res = null;

        ChordSymbol cs0 = chordSymbols.get(0);
        ChordSymbol cs1 = chordSymbols.get(1);
        ChordType ct0 = cs0.getChordType();
        ChordType ct1 = cs1.getChordType();

        if (chordSymbols.size() == 2)
        {

            if (ct0.getName().equals("m+") //  G C E => [Em+, C]  => C
                    || ct0.getName().equals("6") //  Eb F Ab C => [Ab6, Fm7]  => Fm7
                    || ct0.getName().equals("M713") //  Eb F G Ab C => [Fm9, AbM713] => Fm9
                    || ct0.getName().equals("m7b9") //  Eb F G A C => [Dm7b9, F13] => F13
                    || ct0.getName().equals("bm6") //  Bb C En Gb => [Cm7b5, Ebm6]  => m7b5
                    )
            {
                // Take the other one
                res = cs1;
            } else if (ct0.getName().equals("") //  G C E => [Em+, C]  => C
                    || ct0.getName().equals("m7") //  Eb F Ab C => [Ab6, Fm7]  => Fm7
                    || ct0.getName().equals("m9") //  Eb F G Ab C => [Fm9, AbM713] => Fm9
                    || ct0.getName().equals("13") //  Eb F G A C => [Dm7b9, F13] => F13
                    || ct0.getName().equals("m7b5") //  Bb C Eb Gb => [Cm7b5, Ebm6]  => m7b5                    
                    )
            {
                // Take this one
                res = cs0;
            }
        } else
        {
            // Bb69 => [Bb69, C9sus, Gm11]
            // Gm69 => [Em11b5, A7susb9, Gm69]
            res = chordSymbols.stream()
                    .filter(cs -> cs.getChordType().getName().contains("69")) // Take the 69 or m69
                    .findAny()
                    .orElse(null);
        }

        return res == null ? cs0 : res;
    }

    static private void checkStaticData()
    {
        if (positions3 == null)
        {
            throw new IllegalStateException("Static data not built yet!");
        }
    }

    // =====================================================================================
    // Private classes
    // =====================================================================================

}


//testAllChordTypes() 3-5 notes ========================
//Bb => [Bb, Dm+]
//D2 => [Gbm7#5, D2]
//Gb+ => [D+, Gb+, Bb+]
//E6 => [E6, Dbm7]
//Bb69 => [C9sus, Gm11, Bb69]
//BM7 => [BM7]
//EbM713 => [Cm9, EbM713]
//GbM9 => [GbM9]
//EM7b5 => [EM7b5]
//FM7#5 => [FM7#5]
//BbM7#11 => [BbM7#11]
//Ab7 => [Ab7]
//G9 => [G9]
//A13 => [A13, Gbm7b9]
//D7b5 => [Ab7b5, D7b5]
//Db9b5 => [Db9b5, Eb9#5]
//Eb7#5 => [Eb7#5]
//D9#5 => [C9b5, D9#5]
//Ab7b9 => [Ab7b9]
//E7#9 => [E7#9]
//A7#9#5 => [A7#9#5]
//E7b9#5 => [Dm9b5, E7b9#5]
//G7b9b5 => [Db7#11, G7b9b5]
//Eb7#9b5 => [Eb7#9b5, A13b5]
//D7#11 => [D7#11, Ab7b9b5]
//Db13b5 => [Db13b5, G7#9b5]
//Ebm => [Ebm]
//Bbm2 => [Bbm2]
//Dm+ => [Bb, Dm+]
//Abm6 => [Abm6, Fm7b5]
//Abm69 => [Bb7susb9, Fm11b5, Abm69]
//Gm7 => [Bb6, Gm7]
//Dbm7b9 => [Dbm7b9, E13]
//Gm713 => [Gm713]
//Am7#5 => [Am7#5, F2]
//Gm9 => [Gm9, BbM713]
//Ebm11 => [Ebm11, Ab9sus, Gb69]
//Abm11b5 => [Abm11b5, Db7susb9, Bm69]
//Gm7M => [Gm7M]
//Am97M => [Am97M]
//Abm7b5 => [Abm7b5, Bm6]
//Em9b5 => [Gb7b9#5, Em9b5]
//Abdim => [Abdim]
//Ebdim7 => [Cdim7, Adim7, Ebdim7, Gbdim7]
//Adim7M => [Adim7M]
//Absus => [Absus]
//A7sus => [A7sus]
//G9sus => [G9sus, F69, Dm11]
//C7susb9 => [Bbm69, Gm11b5, C7susb9]
//
//
//testAllChordTypes() 3-4 notes ========================
//Eb => [Eb, Gm+]
//Ab2 => [Cm7#5, Ab2]
//E+ => [Ab+, E+, C+]
//E6 => [E6, Dbm7]
//EM7 => [EM7]
//BbM7b5 => [BbM7b5]
//GbM7#5 => [GbM7#5]
//E7 => [E7]
//A7b5 => [Eb7b5, A7b5]
//F7#5 => [F7#5]
//Gbm => [Gbm]
//Dbm2 => [Dbm2]
//Em+ => [Em+, C]
//Dbm6 => [Bbm7b5, Dbm6]
//Abm7 => [Abm7, B6]
//Dbm7#5 => [Dbm7#5, A2]
//Dm7M => [Dm7M]
//Dm7b5 => [Dm7b5, Fm6]
//Abdim => [Abdim]
//Adim7 => [Cdim7, Adim7, Ebdim7, Gbdim7]
//Dbdim7M => [Dbdim7M]
//Absus => [Absus]
//E7sus => [E7sus]

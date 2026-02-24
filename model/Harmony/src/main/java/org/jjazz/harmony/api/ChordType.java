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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static org.jjazz.harmony.api.Degree.*;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.utilities.api.ResUtil;

/**
 * Represents a chord type like "m7", its aliases and its degrees.
 * <p>
 * A chordtype is split into a base and an extension, for example with "m7b5" base="m7" extension="b5".<br>
 * The family tag groups chords that may be similar from an harmonic point of view. <br>
 * This is an unmutable class.
 */
final public class ChordType
{

    protected static final Logger LOGGER = Logger.getLogger(ChordType.class.getSimpleName());

    /**
     * Constant used in the constructor to specify when a degree is Not Present.
     */
    public static final int NOT_PRESENT = 9;

    /**
     * All chord symbols must belong to 1 of these 5 groups.
     */
    public enum Family
    {
        MAJOR, SEVENTH, MINOR, DIMINISHED, SUS;

        /**
         * @return A user-friendly name such as "Major" or "Seventh", translated in the default locale when possible.
         */
        @Override
        public String toString()
        {
            var res = ResUtil.getString(ChordType.class, name());
            return res;
        }
    }

    /**
     * An ordered list of chord symbol degrees.
     */
    public enum DegreeIndex
    {
        ROOT, // Always defined
        THIRD_OR_FOURTH, // Always defined EXCEPT for the "C2" which chord has neither 3rd nor 4th
        FIFTH, // Always defined
        SIXTH_OR_SEVENTH,
        EXTENSION1, // 9, 11, or 13
        EXTENSION2, // 11 or 13
        EXTENSION3;  // 13

        public boolean isExtension()
        {
            return this == EXTENSION1 || this == EXTENSION2 || this == EXTENSION3;
        }
    }

    /**
     * If we need to represent this chord type with a limited number of notes, which degrees do we select ?
     */
    private List<DegreeIndex> mostImportantDegrees;

    private Family family;
    /**
     * The base name of the chordtype, e.g. "m7" for "m7b5"
     */
    private String base;
    /**
     * The extension name of the chordtype, e.g. "b5" for "m7b5"
     */
    private String extension;

    /**
     * Chord degrees that make this chord type
     */
    private final List<Degree> degrees = new ArrayList<>();

    /**
     * The corresponding Chord object (redundant with d0 d3 etc...).
     */
    private final Chord chord = new Chord();

    /**
     * [1, b3 5]
     */
    private String degreeString;

    /**
     * Build a ChordType.
     * <p>
     * Use NOT_PRESENT constant if a degree is not present.
     *
     * @param b Base of the chord type, e.g. "m7" for "m79"
     * @param e Extension of the chord type, e.g. "9" for "m79"
     * @param f The family to which this chordtype belongs to.
     * @param i3 An integer -1, 0 that represents the status (flat,natural) of degree 3.
     * @param i5 An integer -1, 0 or 1 that represents the status (flat,natural or sharp) of degree 5.
     * @param i7 An integer -1, 0 that represents the status (flat,natural) of degree 7.
     * @param i9 An integer -1, 0 or 1 that represents the status (flat,natural or sharp) of degree 9.
     * @param i11 An integer 0 or 1 that represents the status (natural or sharp) of degree 11.
     * @param i13 An integer -1, 0 that represents the status (flat,natural) of degree 13.
     *
     */
    public ChordType(String b, String e, Family f, int i9, int i3, int i11, int i5, int i13, int i7)
    {
        if ((b == null) || (e == null) || !checkDegree(i9) || !checkDegree(i3) || !checkDegree(i11)
            || !checkDegree(i5) || !checkDegree(i13) || !checkDegree(i7))
        {
            throw new IllegalArgumentException(
                "b=" + b + " e=" + e + " f=" + f + " i9=" + i9 + " i3=" + i3 + " i11=" + i11 + " i5=" + i5 + " i13=" + i13 + " i7=" + i7);
        }

        base = b;
        extension = e;
        family = f;

        // Build the corresponding Chord object
        // Add the root
        chord.add(new Note(0));
        degrees.add(Degree.ROOT);

        // Order is important to get the right ExtensionType
        if (i3 != NOT_PRESENT)
        {
            // THIRD
            assert i3 != +1;
            chord.add(new Note(4 + i3));
            degrees.add(Degree.getDegree(Degree.Natural.THIRD, i3));
        }

        if (i11 == 0 && i3 == NOT_PRESENT)
        {
            // FOURTH
            chord.add(new Note(5));
            Degree d = Degree.FOURTH_OR_ELEVENTH;
            degrees.add(d);
        }

        if (i5 != NOT_PRESENT)
        {
            // FIFTH
            chord.add(new Note(7 + i5));
            degrees.add(Degree.getDegree(Degree.Natural.FIFTH, i5));
        }

        if (i13 == 0 && i7 == NOT_PRESENT)
        {
            // SIXTH
            chord.add(new Note(9));
            Degree d = Degree.SIXTH_OR_THIRTEENTH;
            degrees.add(d);
        }

        if (i7 != NOT_PRESENT)
        {
            assert i7 != +1;
            chord.add(new Note(11 + i7));
            Degree d = Degree.getDegree(Degree.Natural.SEVENTH, i7);
            degrees.add(d);
        }

        if (i9 != NOT_PRESENT)
        {
            chord.add(new Note(2 + i9));
            Degree d = Degree.getDegree(Degree.Natural.NINTH, i9);
            degrees.add(d);
        }

        if (i11 != NOT_PRESENT && !(i11 == 0 && i3 == NOT_PRESENT))
        {
            // ELEVENTH, Cm11, C7M#11
            assert i11 != -1
                && !(i3 == 0 && i11 == 0) // Can't have a 3rd degree with a sus4 chord
                && !(i3 == NOT_PRESENT && i11 != 0);        // if no 3rd then it must be a sus4
            chord.add(new Note(5 + i11));
            Degree d = Degree.getDegree(Degree.Natural.ELEVENTH, i11);
            degrees.add(d);
        }

        if (i13 != NOT_PRESENT && !(i13 == 0 && i7 == NOT_PRESENT))
        {
            assert i13 != 1;
            chord.add(new Note(9 + i13));
            Degree d = Degree.getDegree(Degree.Natural.SIXTH, i13);
            degrees.add(d);
        }

        // Build degreeString
        degreeString = degrees.stream()
            .map(d -> d.toStringShort())
            .collect(Collectors.joining(" ", "[", "]"));
    }

    /**
     * The ordered list of DegreeIndexes used by this chord type starting from NINTH.
     *
     * @return
     */
    public List<DegreeIndex> getExtensionDegreeIndexes()
    {
        ArrayList<DegreeIndex> res = new ArrayList<>();
        int start = DegreeIndex.EXTENSION1.ordinal();
        if (isSpecial2Chord())
        {
            res.add(DegreeIndex.EXTENSION1);
        } else
        {
            for (int i = start; i < degrees.size(); i++)
            {
                res.add(DegreeIndex.values()[i]);
            }
        }
        return res;
    }

    /**
     * The degreeIndex of specified degree.
     *
     * @param d
     * @return Can be null if d is not a chord type degree.
     */
    public DegreeIndex getDegreeIndex(Degree d)
    {
        if (d == null)
        {
            throw new NullPointerException("d");
        }
        int index = degrees.indexOf(d);

        if (index != -1 && isSpecial2Chord())
        {
            // Exception no third, no sixth nor seventh !
            index = switch (d)
            {
                case ROOT ->
                    index;
                case FIFTH ->
                    index + 1;
                case NINTH ->
                    index + 2;
                default ->
                    throw new IllegalStateException("d=" + d);
            };
        }

        DegreeIndex res = null;
        if (index != -1)
        {
            res = DegreeIndex.values()[index];
        }
        return res;
    }

    /**
     * The degree corresponding to the specified DegreeIndex.
     * <p>
     * Ex: Cm9 EXTENSION1=NINTH, THIRD_OR_FOURTH=THIRD_FLAT<br>
     * Ex: F7 EXTENSION1=null<br>
     * Ex: F13 EXTENSION1=SIXTH_OR_SIXTEENTH<br>
     * Ex: C6 EXTENSION1=null<br>
     *
     * @param di
     * @return The degree corresponding to specified index. Can be null (even if di==THIRD_OR_FOURTH, see the C2 chord type)
     */
    public Degree getDegree(DegreeIndex di)
    {
        int ordinal = di.ordinal();


        if (isSpecial2Chord())
        {
            // Special case, no third, no six or seventh!
            ordinal = switch (di)
            {
                case ROOT ->
                    ordinal;
                case FIFTH ->
                    ordinal - 1;
                case EXTENSION1 ->
                    ordinal - 2;
                default ->
                    1000;   // so we return null
            };
        }

        Degree d = null;
        if (ordinal < degrees.size())
        {
            d = degrees.get(ordinal);
        }
        return d;
    }

    public Family getFamily()
    {
        return family;
    }

    /**
     * E.g. "7" for C7#11
     *
     * @return
     */
    public String getBase()
    {
        return base;
    }

    /**
     * E.g. "#11" for C7#11
     * <p>
     * This can be used by a chord symbol renderer to render the extension with an "exponent" format.
     *
     * @return
     */
    public String getExtension()
    {
        return extension;
    }

    /**
     * E.g. "7#11" for C7#11
     *
     * @return
     */
    public String getName()
    {
        return base + extension;
    }

    /**
     * The ordered list of each ChordDegree composing this chord.
     * <p>
     * Order is ROOT, THIRD or FOURTH, FIFTH, [SIXTH_OR_THIRTEENTH(if==sixth)], [SEVENTH], [NINTH], [ELEVENTH], [SIXTH_OR_THIRTEENTH(if==extension)].
     *
     * @return An unmodifiable list
     */
    public List<Degree> getDegrees()
    {
        return Collections.unmodifiableList(degrees);
    }

    /**
     * The number of degrees used by this ChordType.
     *
     * @return A value &gt;= 3
     */
    public int getNbDegrees()
    {
        return degrees.size();
    }

    /**
     * Find the chord type's degree who matches the relative pitch.
     * <p>
     * Ex. this=7#9, relPitch=3 return NINTH_SHARP<br>
     * Ex. this=b3, relPitch=3 return THIRD_FLAT<br>
     * Ex. this=b3, relPitch=4 return NULL<br>
     *
     * @param relPitch
     * @return The corresponding degree if it exists, null otherwise.
     */
    public Degree getDegree(int relPitch)
    {
        if (relPitch < 0 || relPitch > 11)
        {
            throw new IllegalArgumentException("relPitch=" + relPitch);
        }
        for (Degree d : degrees)
        {
            if (d.getPitch() == relPitch)
            {
                return d;
            }
        }
        return null;
    }

    /**
     * Find the chord type's degree who matches natural degree nd.
     *
     * @param nd
     * @return The corresponding degree if it exists, e.g. if C7#9, nd=NINTH return NINTH_SHARP, or null if no NINTH.
     */
    public Degree getDegree(Degree.Natural nd)
    {
        for (Degree d : degrees)
        {
            if (d.getNatural() == nd)
            {
                return d;
            }
        }
        return null;
    }

    /**
     * Count how many initial degrees are identical between 2 chord types.
     * <p>
     * Examples:<br>
     * C and Cm: 1, Cm and Cm7: 3, Cm7#9 and Cm9: 4
     *
     * @param ct
     * @param sixthMajorSeventhEqual if true we consider 6 and 7M degrees identical.
     * @return Minimum value is 1 (root always matches).
     */
    public int getNbCommonDegrees(ChordType ct, boolean sixthMajorSeventhEqual)
    {
        int res;
        for (res = 0; res < Math.min(degrees.size(), ct.degrees.size()); res++)
        {
            var d = degrees.get(res);
            var dCt = ct.degrees.get(res);
            if (sixthMajorSeventhEqual ? !d.equalsSixthMajorSeventh(dCt) : d != dCt)
            {
                assert res > 0;    // root must always match
                break;
            }
        }
        return res;
    }

    /**
     * Find the most probable degree corresponding to relative pitch for this chordtype.
     * <p>
     * First try to use getDegree(relPitch). If it returns null, make some assumptions based on the chord type to find the most probable degree.<br>
     * Ex: Cm7, relPitch=Eb=3, then returns THIRD_FLAT.<br>
     * Ex: C7, relPitch=Eb=3, then returns NINTH_SHARP.<br>
     * Ex: C7, relPitch=F=5, then returns FOURTH.<br>
     * Ex: Cm7, relPitch=F=5, then returns ELEVENTH.<br>
     *
     * @param relPitch relative pitch.
     * @return The most probable corresponding degree. Can't be null.
     */
    public Degree getDegreeMostProbable(int relPitch)
    {
        if (relPitch < 0 || relPitch > 11)
        {
            throw new IllegalArgumentException("relPitch=" + relPitch);
        }
        Degree d = getDegree(relPitch);
        if (d == null)
        {
            d = switch (relPitch)
            {
                case 0 ->
                    ROOT;
                case 1 ->
                    NINTH_FLAT;
                case 2 ->
                    NINTH;
                case 3 ->
                    isMajor() ? NINTH_SHARP : THIRD_FLAT;
                case 4 ->
                    THIRD;
                case 5 ->
                    FOURTH_OR_ELEVENTH;
                case 6 ->
                    ELEVENTH_SHARP;     // If we're here it's not a b5 chord since all chords define 1,3,5
                case 7 ->
                    FIFTH;
                case 8 ->
                    THIRTEENTH_FLAT;    // If we're here it's not a #5 chord since all chords define 1,3,5
                case 9 ->
                    SIXTH_OR_THIRTEENTH;
                case 10 ->
                    SEVENTH_FLAT;
                case 11 ->
                    SEVENTH;
                default ->
                    throw new IllegalArgumentException("relPitch=" + relPitch);
            };
        }
        return d;
    }

    /**
     * The list of "most important Degrees" indexes for this chord type, in the descending order.
     * <p>
     * If some notes need to be omitted, it's better to remove the less important ones first. <br>
     * Ex: C7=&gt; [THIRD_OR_FOURTH, SIXTH_OR_SEVENTH, FIFTH, ROOT] (ROOT is the less important).<br>
     * Ex: C6=&gt; [THIRD_OR_FOURTH, SIXTH_OR_SEVENTH, ROOT, FIFTH] (ROOT-SIXTH interval is important).<br>
     * Ex: C7b5=&gt; [THIRD_OR_FOURTH, FIFTH, SIXTH_OR_SEVENTH, ROOT] <br>
     * Ex: C9M=&gt; [THIRD_OR_FOURTH, SIXTH_OR_SEVENTH, EXTENSION1, FIFTH, ROOT] <br>
     * Ex: C9M#11=&gt; [THIRD_OR_FOURTH, SIXTH_OR_SEVENTH, EXTENSION1, FIFTH, ROOT, EXTENSION2]<br>
     * Ex: C13#11(9)=&gt; [THIRD_OR_FOURTH, SIXTH_OR_SEVENTH, EXTENSION1, FIFTH, ROOT, EXTENSION2, EXTENSION3]<br>
     *
     * @return An unmodifiable list
     */
    public List<DegreeIndex> getMostImportantDegreeIndexes()
    {
        if (mostImportantDegrees == null)
        {
            List<DegreeIndex> dis = new ArrayList<>();
            if (!isSpecial2Chord())
            {
                dis.add(DegreeIndex.THIRD_OR_FOURTH);
            }
            if (!getDegree(DegreeIndex.FIFTH).equals(Degree.FIFTH))         // If altered 5 it's important
            {
                dis.add(DegreeIndex.FIFTH);
            }
            if (getDegree(DegreeIndex.SIXTH_OR_SEVENTH) != null)
            {
                dis.add(DegreeIndex.SIXTH_OR_SEVENTH);
            }
            if (getDegree(DegreeIndex.EXTENSION1) != null)
            {
                dis.add(DegreeIndex.EXTENSION1);
            }
            if (base.contains("6"))
            {
                dis.add(DegreeIndex.ROOT);
                if (getDegree(DegreeIndex.FIFTH).equals(Degree.FIFTH))
                {
                    dis.add(DegreeIndex.FIFTH);
                }
            } else
            {
                if (getDegree(DegreeIndex.FIFTH).equals(Degree.FIFTH))
                {
                    dis.add(DegreeIndex.FIFTH);
                }
                dis.add(DegreeIndex.ROOT);
            }
            if (getDegree(DegreeIndex.EXTENSION2) != null)
            {
                dis.add(DegreeIndex.EXTENSION2);
            }
            if (getDegree(DegreeIndex.EXTENSION3) != null)
            {
                dis.add(DegreeIndex.EXTENSION3);
            }

            mostImportantDegrees = Collections.unmodifiableList(dis);
        }
        LOGGER.log(Level.FINE, "getMostImportantDegreeIndexes() this={0} result={1}", new Object[]
        {
            this, mostImportantDegrees
        });
        return mostImportantDegrees;
    }

    /**
     * Try to fit harmonically degree d to this chord type.
     * <p>
     * ex: d=THIRD, this=m7 =&gt; return THIRD_FLAT <br>
     * ex: d=ELEVENTH_SHARP, this=m7b5 =&gt; return FIFTH_FLAT <br>
     * ex: d=ELEVENTH_SHARP, this=7M =&gt; return null<br>
     * ex: d=SEVENTH, this 6 =&gt; return SIXTH_OR_THIRTEENTH
     *
     * @param d
     * @return A Degree representing the harmonic conversion of d for this chord type. Can be null if no match.
     */
    public Degree fitDegree(Degree d)
    {
        LOGGER.log(Level.FINE, "fitDegree() -- d={0} this={1}", new Object[]
        {
            d, this
        });

        // Try natural degree match 
        Degree destDegree = getDegree(d.getNatural());

        if (destDegree == null)
        {
            // Maybe different degrees but they can represent the same note, e.g. d=b5 and this="M7#11"         
            destDegree = getDegree(d.getPitch());
        } else if (extension.contains("6") && d.getNatural().equals(Degree.Natural.SEVENTH))
        {
            // Special case, we convert 7th to 6th
            destDegree = Degree.SIXTH_OR_THIRTEENTH;
        } else if (getDegree(Natural.SEVENTH) != null && d.getNatural().equals(Degree.Natural.SIXTH))
        {
            // Special case, we convert 6th to 7th
            destDegree = getDegree(Natural.SEVENTH);
        }

        LOGGER.log(Level.FINE, "fitDegree()  destDegree={0}", destDegree);
        return destDegree;
    }

    /**
     * Fit harmonically degree d to this chord type.
     * <p>
     * 1/ Try natural degree match using fitDegree()
     * .<p>
     * 2/ If no natural match is possible, fitDegree() on provided optScale (if non null).<br>
     * ex: d=NINTH_SHARP, scale=DORIAN =&gt; return NINTH<br>
     * ex: d=NINTH, scale=LOCRIAN =&gt; return NINTH_FLAT<p>
     * 3/ If scale did not help :<br>
     * Make some assumptions: use the "most common" scale usually associated to a chord type, or just try "best guess"<br>
     * ex: d=NINTH_FLAT, this=m7, assume scale=DORIAN =&gt; return NINTH<br>
     * ex: d=THIRTEENTH_FLAT, this=m7, assume scale=DORIAN =&gt; return SIXTH_OR_THIRTEENTH<br>
     *
     * @param d
     * @param optScale Optional scale instance. Can be null.
     * @return A Degree representing the harmonic conversion of d for this chord type. Can't be null.
     */
    public Degree fitDegreeAdvanced(Degree d, StandardScaleInstance optScale)
    {
        LOGGER.log(Level.FINE, "fitDegreeAdvanced() -- d={0} this={1} scales={2}", new Object[]
        {
            d, this, optScale
        });

        // Try natural degree match 
        Degree destDegree = fitDegree(d);

        if (destDegree == null && optScale != null)
        {
            // Try to use the provided scales if any
            Scale s = optScale.getScale();
            // First see if the degree's pitch is part of the scale
            destDegree = s.getDegree(d.getPitch());
            if (destDegree == null)
            {
                // If not try to get scale's derived degrees (e.g. NINTH => NINTH_SHARP etc.)
                List<Degree> scaleDegrees = s.getDegrees(d.getNatural());
                if (!scaleDegrees.isEmpty())
                {
                    // There can be one or two, take the first
                    destDegree = scaleDegrees.get(0);
                }
            }
        }

        if (destDegree == null)
        {
            // We coud not find a perfect fit. Now make some assumptions on most often used scale for a given chord type...
            Degree dTmp;
            switch (d)
            {
                case NINTH_FLAT, NINTH, NINTH_SHARP ->
                {
                    // If we are are, this chord has no 9 defined
                    // If d=#9 then this chord type is major, otherwise we would not be here
                    destDegree = Degree.NINTH;
                    if (extension.equals("m7b5"))
                    {
                        destDegree = Degree.NINTH_FLAT;
                    }
                }
                case THIRD_FLAT, THIRD -> // This chord type has no third defined (otherwise we wouldn't be here), then this chord must be a sus chord
                    destDegree = Degree.FOURTH_OR_ELEVENTH;
                case FOURTH_OR_ELEVENTH ->
                {
                    // 4th can only be naturally mapped to a sus4 or m11 chord type. If we're here, this chord type is different.
                    // 11th natural can only be naturally mapped to a 11, #11 or sus4. If we're here, this chord type is different.               
                    if (family == Family.MINOR || family == Family.DIMINISHED)
                    {
                        destDegree = Degree.FOURTH_OR_ELEVENTH;                      // 11th ok with all minors and diminished
                    } else if ((dTmp = getDegree(6)) != null)
                    {
                        destDegree = dTmp;                                 // If chord type has #11/b5 go there
                    } else if ((dTmp = getDegree(Degree.Natural.NINTH)) != null && dTmp.getAccidental() != 0)
                    {
                        destDegree = Degree.ELEVENTH_SHARP;                // 11# ok as a "transition note" if no #11 and no altered 9th
                    } else
                    {
                        destDegree = Degree.FOURTH_OR_ELEVENTH;             // 11th OK e.g. with C7M#5 or C7#5
                    }
                }

                case ELEVENTH_SHARP ->
                {
                    // 11th sharp can only be naturally mapped to a #11, 11 or b5. If we're here, this chord type is different.
                    if (getDegree(5) != null)
                    {
                        destDegree = Degree.FOURTH_OR_ELEVENTH;              //  This chord type must be a sus chord
                    } else
                    {
                        destDegree = getDegree(Degree.Natural.FIFTH);         // go to 5 or #5
                    }
                }
                case FIFTH_FLAT, FIFTH, FIFTH_SHARP -> // We should never be here : all chord types have a fifth defined
                    throw new IllegalStateException("We should not end up here ! d=" + d + " this=" + this + " scales=" + optScale);
                case THIRTEENTH_FLAT -> // Thirteenth natural can only be naturally mapped on b13, 13 or #5 chord types. If we're here this chord type is different.
                    destDegree = getDegree(Degree.Natural.FIFTH);            // go to 5 or b5
                case SIXTH_OR_THIRTEENTH ->
                {
                    // Thirteenth natural can only be naturally mapped on 13, b13 or 6 chord types. If we're here this chord type is different.   
                    if (extension.equals("m7b5") || extension.equals("m9b5"))
                    {
                        destDegree = Degree.THIRTEENTH_FLAT;                  // Assume locrian mode
                    } else if ((dTmp = getDegree(8)) != null)                // If chord type has a #5 go there
                    {
                        destDegree = dTmp;
                    } else
                    {
                        destDegree = Degree.SIXTH_OR_THIRTEENTH;                       // 13th ok with all majors, minors, diminished, seventh
                    }
                }
                case SEVENTH_FLAT ->
                {
                    // Seventh can only be naturally mapped on 7M, 7 or dim7M chords.
                    // If we're here this chord type is different: minor triad or m6, a major triad or 6, a sus triad, a dim triad or dim7.

                    destDegree = Degree.SEVENTH_FLAT;  // 7 by default, exceptions below

                    if (family == Family.MAJOR && getDegree(9) != null)
                    {
                        // 6 chord
                        destDegree = Degree.SEVENTH;    // Assume that a 6 chord is a I-chord
                    } else if (extension.equals("dim7"))
                    {
                        destDegree = Degree.SIXTH_OR_THIRTEENTH;  // In dim7 7 is actually a bb7=13
                    }
                }
                case SEVENTH ->
                {
                    // Seventh can only be naturally mapped on 7M, 7 or dim7M chords. 
                    // If we're here this chord type is different: minor triad or m6, a major triad or 6, a dim triad or dim7.

                    destDegree = Degree.SEVENTH;  // 7M by default, exceptions below

                    if (family == Family.SUS)
                    {
                        // Sus triad
                        destDegree = Degree.SEVENTH_FLAT;
                    } else if (family == Family.MINOR && getDegree(9) == null)
                    {
                        // Minor triad but not m6
                        destDegree = Degree.SEVENTH_FLAT;      // Assume dorian mode by default, this might be wrong sometimes
                    } else if (family == Family.DIMINISHED)
                    {
                        // If dim triad assume it's a semi-diminished chord
                        // if dim7 convert 7 to "diminished 7"=bb7=13
                        destDegree = getDegree(9) != null ? Degree.SIXTH_OR_THIRTEENTH : Degree.SEVENTH_FLAT;
                    }
                }
                default ->
                    throw new IllegalStateException("d=" + d + " this=" + this + " scales=" + optScale);
            }
        }

        assert destDegree != null : "destDegree is null ! d=" + d + " this=" + this + " scales=" + optScale;

        LOGGER.log(Level.FINE, "fitDegreeAdvanced()  destDegree={0}", destDegree);
        return destDegree;
    }

    /**
     * Rely on fitDegreeAdvanced(Degree d, optScales).
     * <p>
     * If di does not directly correspond to one of these ChordType degrees, make some assumptions, e.g. if di==DegreeIndex.SIXTH_OR_SEVENTH then try
     * to fit to the seventh degree of this ChordType.
     *
     * @param di
     * @param optScale Optional, can be null.
     * @return
     */
    public Degree fitDegreeAdvanced(DegreeIndex di, StandardScaleInstance optScale)
    {
        Degree d = getDegree(di);
        if (d == null)
        {
            d = switch (di)
            {
                case ROOT, FIFTH -> // We should not be here because all chords must have those degrees defined 
                    throw new IllegalStateException("di=" + di);
                case THIRD_OR_FOURTH ->  // It can only be a "2" chord
                {
                    if (!isSpecial2Chord())
                    {
                        throw new IllegalArgumentException("this=" + this + " di=" + di + " d=" + d);
                    }
                    yield fitDegreeAdvanced(Degree.FOURTH_OR_ELEVENTH, optScale);
                }
                case SIXTH_OR_SEVENTH ->
                    fitDegreeAdvanced(Degree.SEVENTH, optScale);       // 7 suits most chords...
                case EXTENSION1 ->
                    fitDegreeAdvanced(Degree.NINTH, optScale);         // 9 suits most chords
                case EXTENSION2, EXTENSION3 ->
                    fitDegreeAdvanced(Degree.SIXTH_OR_THIRTEENTH, optScale);
                default ->
                    throw new IllegalStateException("di=" + di);
            };
        }
        return d;
    }

    /**
     * Get a simplified ChordType by keeping only the first nbMaxDegrees degrees.
     *
     * @param nbMaxDegrees Must be &gt; 2
     * @return Can't be null
     */
    public ChordType getSimplified(int nbMaxDegrees)
    {
        checkArgument(nbMaxDegrees >= 3, "nbMaxDegrees=%s", nbMaxDegrees);

        var res = this;

        if (degrees.size() > nbMaxDegrees)
        {
            var resDegrees = getDegrees().stream()
                .limit(nbMaxDegrees)
                .toList();
            res = ChordTypeDatabase.getDefault().getChordType(resDegrees);
            if (res == null)
            {
                LOGGER.log(Level.FINE, "getSimplified() ChordType {0} can not be simplified with only {1} degrees", new Object[]
                {
                    this, nbMaxDegrees
                });
                res = this;
            }
        }

        return res;
    }

    /**
     * Compute how much "similar" is the specified ChordType with this object.
     * <p>
     * Equal ChordTypes have a score of 63. Score is reduced when ChordType Degrees differ, according to the table below:
     * <p>
     * DegreeIndex.THIRD_OR_FOURTH: -32 (a score &gt;=32 means third or fourth matches)<br>
     * DegreeIndex.FIFTH: -16 (a score &gt;=48 means third+fifth match) <br>
     * DegreeIndex.SIXTH_SEVENTH: -8 (a score &gt;=56 means third+fifth+six_seventh match)<br>
     * DegreeIndex.EXTENSION1: -4 (a score &gt;=60 means third+fifth+six_seventh match+ext1 match)<br>
     * DegreeIndex.EXTENSION2: -2 (a score &gt;=62 means third+fifth+six_seventh match+ext1+ext2 match)<br>
     * DegreeIndex.EXTENSION3: -1 (a score ==63 means equal ChordTypes)<br>
     * <p>
     * If acceptAbsentDegrees is true and chord types do not have the same number of Degrees (e.g. C and C69), score is reduced by 1 for each "extra"
     * Degree. This way C and F6 (1 extra degree) are a bit less similar than C and F, and a bit more similar than C and F69 (2 extra degrees).
     * <p>
     * <p>
     * Special handling: 6 and 7M are considered similar.
     * <p>
     * <p>
     * Examples:<br>
     * - C and E, Fm69 and Ebm69 = 63. This is the max value for identical ChordTypes.<br>
     * - C7 and Cm6 = 63-32-8 = 23 <br>
     * - C7 and Cm69 = 63-32-8-1 = 22 if acceptAbsentDegrees==true , or 63-32-8-4 = 19 if acceptAbsentDegrees==false<br>
     * - C7 and C9 = 63-1=62 if acceptAbsentDegrees==true , or 63-4 = 59 if acceptAbsentDegrees==false<br>
     * - C and F13b9 = 63-3=60 if acceptAbsentDegrees==true, or 63-8-4-2 = 49 if acceptAbsentDegrees==false <br>
     * - Cm6 and Cm7m = 63<br>
     * - Cm6 and Cm7 = 55<br>
     *
     *
     * @param ct
     * @param acceptAbsentDegrees If true absent degrees in one of the ChordType only have a minor impact on the similarity score
     * @return [0-63]
     */
//    public int getSimilarityScore(ChordType ct, boolean acceptAbsentDegrees)
//    {
//        int res = 63;
//        int weight = 32;
//
//        for (int i = 1; i <= 6; i++)
//        {
//            Degree d = i < degrees.size() ? degrees.get(i) : null;
//            Degree dct = i < ct.degrees.size() ? ct.degrees.get(i) : null;
//            if (!Objects.equals(d, dct))
//            {
//                if ((d == null || dct == null) && acceptAbsentDegrees)
//                {
//                    res -= 1;   // This way C and F6 will be 62, C and F69 will be 61
//                } else if ((d == Degree.SIXTH_OR_THIRTEENTH && dct == Degree.SEVENTH)
//                        || (dct == Degree.SIXTH_OR_THIRTEENTH && d == Degree.SEVENTH))
//                {
//                    // Do nothing
//                } else
//                {
//                    res -= weight;
//                }
//            }
//            weight /= 2;
//        }
//
//        return res;
//    }
    /**
     * Calculate the pitch of degree nd if chord's root=rootPitch and chord's type=this.
     *
     * @param rootPitch
     * @param nd
     * @return A Midi pitch, or -1 if nd is not present in this chord pitch.
     */
    public int getPitch(Degree.Natural nd, int rootPitch)
    {
        Degree d = getDegree(nd);
        int pitch = -1;
        if (d != null)
        {
            pitch = rootPitch + d.getPitch();
        }
        return pitch;
    }

    /**
     * The special C2 chord which has no third no fourth no 6/7, but has a ninth.
     * <p>
     * E.g. true for a "C2" chord.
     *
     * @return
     */
    public boolean isSpecial2Chord()
    {
        return getName().equals("2");
    }

    /**
     *
     * @return True for Cm7, C°, etc.
     */
    public boolean isMinor()
    {
        return Degree.THIRD_FLAT.equals(getDegree(Natural.THIRD));
    }

    /**
     *
     * @return True if e.g. C7, C, C6, etc. False if e.g. Cm, C7sus etc.
     */
    public boolean isMajor()
    {
        return Degree.THIRD.equals(getDegree(Natural.THIRD));
    }

    /**
     *
     * @return True for e.g. C7, C7alt, etc.
     */
    public boolean isSeventhMinor()
    {
        return Degree.SEVENTH_FLAT.equals(getDegree(Natural.SEVENTH));
    }

    /**
     * @return True for e.g. C7M, C9M#11, etc.
     */
    public boolean isSeventhMajor()
    {
        return Degree.SEVENTH.equals(getDegree(Natural.SEVENTH));
    }

    /**
     *
     * @return True if a seventh (minor or major) is present.
     */
    public boolean isSeventh()
    {
        return getDegree(Natural.SEVENTH) != null;
    }

    /**
     *
     * @return True for e.g. C, C7, Cm6
     */
    public boolean isFifthNatural()
    {
        return Degree.FIFTH.equals(getDegree(Natural.FIFTH));
    }

    /**
     *
     * @return True for e.g. C+, C7#5, etc.
     */
    public boolean isFifthSharp()
    {
        return Degree.FIFTH_SHARP.equals(getDegree(Natural.FIFTH));
    }

    /**
     *
     * @return True for e.g. Cdim, C7b5, etc.
     */
    public boolean isFifthFlat()
    {
        return Degree.FIFTH_FLAT.equals(getDegree(Natural.FIFTH));
    }

    /**
     *
     * @return True if a eleventh (natural or altered) is present.
     */
    public boolean isEleventh()
    {
        return getDegree(Natural.ELEVENTH) != null;
    }

    /**
     *
     * @return True for e.g. Cm11. Note that C7sus will return false, see isSus().
     * @see isSus()
     */
    public boolean isEleventhNatural()
    {
        return !isSus() && Degree.FOURTH_OR_ELEVENTH.equals(getDegree(Natural.ELEVENTH));
    }

    /**
     *
     * @return True for e.g. C7M#11, C7#11.
     */
    public boolean isEleventhSharp()
    {
        return Degree.ELEVENTH_SHARP.equals(getDegree(Natural.ELEVENTH));
    }

    /**
     * True if family is Family.SUS (no third degree).
     *
     * @return True for e.g. Csus, C7sus, C2, ...
     */
    public boolean isSus()
    {
        return getFamily().equals(Family.SUS);
    }

    /**
     *
     * @return True if chord does not have a seventh degree (minor or major) but has the SIXTH_OR_THIRTEENTH degree.
     */
    public boolean isSixth()
    {
        return getDegree(Natural.SEVENTH) == null && Degree.SIXTH_OR_THIRTEENTH.equals(getDegree(Natural.SIXTH));
    }

    /**
     *
     * @return True if chord has a seventh degree (minor or major) and the SIXTH_OR_THIRTEENTH degree.
     */
    public boolean isThirteenth()
    {
        return getDegree(Natural.SEVENTH) != null && Degree.SIXTH_OR_THIRTEENTH.equals(getDegree(Natural.SIXTH));
    }

    /**
     *
     * @return True if a ninth (natural or altered) is present.
     */
    public boolean isNinth()
    {
        return getDegree(Natural.NINTH) != null;
    }

    /**
     *
     * @return True for e.g. C9, Dbm9
     */
    public boolean isNinthNatural()
    {
        return Degree.NINTH.equals(getDegree(Natural.NINTH));
    }

    /**
     *
     * @return True for e.g. C7#9
     */
    public boolean isNinthSharp()
    {
        return Degree.NINTH_SHARP.equals(getDegree(Natural.NINTH));
    }

    /**
     *
     * @return True for e.g. C7b9, etc.
     */
    public boolean isNinthFlat()
    {
        return Degree.NINTH_FLAT.equals(getDegree(Natural.NINTH));
    }

    @Override
    public String toString()
    {
        return getName();
    }

    /**
     * @return A string like "[1 3b 5 7b 9]" for ChordType "m79"
     */
    public String toDegreeString()
    {
        return degreeString;
    }

    /**
     * @return A copy of the corresponding Chord with a default C root and flat accidental (if accidental is needed).
     */
    public Chord getChord()
    {
        return chord.clone();
    }

    /**
     * Same than equals() except that we consider 6th and 7M identical degrees.
     *
     * @param o
     * @return
     */
    public boolean equalsSixthMajorSeventh(Object o)
    {
        boolean b = false;
        if (o instanceof ChordType ct)
        {
            var ctDegrees = ct.getDegrees();
            if (ctDegrees.size() == degrees.size())
            {
                b = true;
                for (int i = 0; i < degrees.size(); i++)
                {
                    if (!ctDegrees.get(i).equalsSixthMajorSeventh(degrees.get(i)))
                    {
                        b = false;
                        break;
                    }
                }
            }
        }
        return b;
    }

    // =============================================================================================
    // Private methods
    // =============================================================================================
    /**
     * Return true if d equals -1, 0, 1 or NOT_PRESENT
     *
     * @param d
     * @return
     */
    private boolean checkDegree(int d)
    {
        return (d == -1) || (d == 0) || (d == 1) || (d == NOT_PRESENT);
    }
}

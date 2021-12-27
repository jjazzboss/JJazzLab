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
package org.jjazz.harmony.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import static org.jjazz.harmony.api.Degree.*;

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
    }

    /**
     * An ordered list of chord symbol degrees.
     */
    public enum DegreeIndex
    {
        ROOT,
        THIRD_OR_FOURTH, // Can't have both in the same time
        FIFTH,
        SIXTH_OR_SEVENTH, // can be considered also as EXTENSION0
        EXTENSION1, // 9, 11, or 13
        EXTENSION2, // 11 or 13
        EXTENSION3  // 13
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
    private ArrayList<Degree> degrees = new ArrayList<>();

    /**
     * The corresponding Chord object (redundant with d0 d3 etc...).
     */
    private Chord chord = new Chord();

    /**
     * [1, b3 5]
     */
    private String degreeString;

    /**
     * Constructed from a factory.
     *
     * @param b Base of the chord type, e.g. "m7" for "m79"
     * @param e Extension of the chord type, e.g. "9" for "m79"
     * @param f An integer representing the family to which this chordtype belongs to.
     * @param i3 An integer -1, 0 or 1 that represent the status (flat,natural or sharp) of degree 3.
     * @param i5 An integer -1, 0 or 1 that represent the status (flat,natural or sharp) of degree 5.
     * @param i7 An integer -1, 0 or 1 that represent the status (flat,natural or sharp) of degree 7.
     * @param i9 An integer -1, 0 or 1 that represent the status (flat,natural or sharp) of degree 9.
     * @param i11 An integer -1, 0 or 1 that represent the status (flat,natural or sharp) of degree 11.
     * @param i13 An integer -1, 0 or 1 that represent the status (flat,natural or sharp) of degree 13. Use NOT_PRESENT constant
     * if a degree is not present.
     */
    protected ChordType(String b, String e, Family f, int i9, int i3, int i11, int i5, int i13, int i7)
    {
        if ((b == null) || (e == null) || !checkDegree(i9) || !checkDegree(i3) || !checkDegree(i11)
                || !checkDegree(i5) || !checkDegree(i13) || !checkDegree(i7))
        {
            throw new IllegalArgumentException("b=" + b + " e=" + e + " f=" + f + " i9=" + i9 + " i3=" + i3 + " i11=" + i11 + " i5=" + i5 + " i13=" + i13 + " i7=" + i7);   //NOI18N
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
            assert i3 != +1;   //NOI18N
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
            assert i7 != +1;   //NOI18N
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
            assert i11 != -1 //NOI18N
                    && !(i3 == 0 && i11 == 0) // Can't have a 3rd degree with a sus4 chord
                    && !(i3 == NOT_PRESENT && i11 != 0);        // if no 3rd then it must be a sus4
            chord.add(new Note(5 + i11));
            Degree d = Degree.getDegree(Degree.Natural.ELEVENTH, i11);
            degrees.add(d);
        }

        if (i13 != NOT_PRESENT && !(i13 == 0 && i7 == NOT_PRESENT))
        {
            assert i13 != 1;   //NOI18N
            chord.add(new Note(9 + i13));
            Degree d = Degree.getDegree(Degree.Natural.SIXTH, i13);
            degrees.add(d);
        }

        // Build degreeString
        StringBuilder sb = new StringBuilder("[");
        for (Degree d : degrees)
        {
            sb.append(d.toStringShort()).append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        degreeString = sb.toString();
    }

    /**
     * The ordered list of DegreeIndexes starting from SIXTH_OR_SEVENTH.
     *
     * @return
     */
    public List<DegreeIndex> getExtensionDegreeIndexes()
    {
        ArrayList<DegreeIndex> res = new ArrayList<>();
        for (int i = DegreeIndex.SIXTH_OR_SEVENTH.ordinal(); i < degrees.size(); i++)
        {
            res.add(DegreeIndex.values()[i]);
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
            throw new NullPointerException("d");   //NOI18N
        }
        int index = degrees.indexOf(d);
        if (index == -1)
        {
            return null;
        }
        return DegreeIndex.values()[index];
    }

    /**
     * The degree corresponding to the specified DegreeIndex.
     * <p>
     * Ex: Cm9 EXTENSION1=NINTH, THIRD_OR_FOURTH=THIRD_FLAT<br>
     * Ex: F7 EXTENSION1=null<br>
     *
     * @param di
     * @return The degree corresponding to specified index. Can be null.
     */
    public Degree getDegree(DegreeIndex di)
    {
        Degree d = null;
        if (di.ordinal() < degrees.size())
        {
            d = degrees.get(di.ordinal());
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
     * Order is ROOT, THIRD or FOURTH, FIFTH, [SIXTH_OR_THIRTEENTH(if==sixth)], [SEVENTH], [NINTH], [ELEVENTH],
     * [SIXTH_OR_THIRTEENTH(if==extension)].
     *
     * @return
     */
    public List<Degree> getDegrees()
    {
        return new ArrayList<>(degrees);
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
            throw new IllegalArgumentException("relPitch=" + relPitch);   //NOI18N
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
     * Find the most probable degree corresponding to relative pitch for this chordtype.
     * <p>
     * First try to use getDegree(relPitch). If it returns null, make some assumptions based on the chord type to find the most
     * probable degree.<br>
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
            throw new IllegalArgumentException("relPitch=" + relPitch);   //NOI18N
        }
        Degree d = getDegree(relPitch);
        if (d == null)
        {
            switch (relPitch)
            {
                case 0:
                    d = ROOT;
                    break;
                case 1:
                    d = NINTH_FLAT;
                    break;
                case 2:
                    d = NINTH;
                    break;
                case 3:
                    d = isMinor() ? THIRD_FLAT : NINTH_SHARP;
                    break;
                case 4:
                    d = THIRD;
                    break;
                case 5:
                    d = FOURTH_OR_ELEVENTH;
                    break;
                case 6:
                    d = ELEVENTH_SHARP;     // If we're here it's not a b5 chord since all chords define 1,3,5
                    break;
                case 7:
                    d = FIFTH;
                    break;
                case 8:
                    d = THIRTEENTH_FLAT;    // If we're here it's not a #5 chord since all chords define 1,3,5
                    break;
                case 9:
                    d = SIXTH_OR_THIRTEENTH;
                    break;
                case 10:
                    d = SEVENTH_FLAT;
                    break;
                case 11:
                    d = SEVENTH;
                    break;
            }
        }
        return d;
    }

    /**
     * The list of "most important Degrees" indexes for this chord type, in the descending order.
     * <p>
     * If some notes need to be omitted, it's better to remove the less important ones first. <br>
     * Ex: C7=&gt; [THIRD_OR_FOURTH, SIXTH_OR_SEVENTH, FIFTH, ROOT] (ROOT is the less important).<br>
     * Ex: C6=&gt; [THIRD_OR_FOURTH, SIXTH_OR_SEVENTH, ROOT, FIFTH] (ROOT-SIXTH interval is important).<br>
     * Ex: C7b5=&gt; [THIRD_OR_FOURTH, FIFTH, SEVENTH, ROOT] <br>
     * Ex: C9M=&gt; [THIRD_OR_FOURTH, SEVENTH, EXTENSION1, FIFTH, ROOT] <br>
     * Ex: C9M#11=&gt; [THIRD_OR_FOURTH, SEVENTH, EXTENSION1, FIFTH, ROOT, EXTENSION2]<br>
     * Ex: C13#11(9)=&gt; [THIRD_OR_FOURTH, SEVENTH, EXTENSION1, FIFTH, ROOT, EXTENSION2, EXTENSION3]<br>
     *
     * @return
     */
    public List<DegreeIndex> getMostImportantDegreeIndexes()
    {
        if (mostImportantDegrees == null)
        {
            mostImportantDegrees = new ArrayList<>();
            mostImportantDegrees.add(DegreeIndex.THIRD_OR_FOURTH);
            if (!getDegree(DegreeIndex.FIFTH).equals(Degree.FIFTH))         // If altered 5 it's important
            {
                mostImportantDegrees.add(DegreeIndex.FIFTH);
            }
            if (getDegree(DegreeIndex.SIXTH_OR_SEVENTH) != null)
            {
                mostImportantDegrees.add(DegreeIndex.SIXTH_OR_SEVENTH);
            }
            if (getDegree(DegreeIndex.EXTENSION1) != null)
            {
                mostImportantDegrees.add(DegreeIndex.EXTENSION1);
            }
            if (base.contains("6"))
            {
                mostImportantDegrees.add(DegreeIndex.ROOT);
                if (getDegree(DegreeIndex.FIFTH).equals(Degree.FIFTH))
                {
                    mostImportantDegrees.add(DegreeIndex.FIFTH);
                }
            } else
            {
                if (getDegree(DegreeIndex.FIFTH).equals(Degree.FIFTH))
                {
                    mostImportantDegrees.add(DegreeIndex.FIFTH);
                }
                mostImportantDegrees.add(DegreeIndex.ROOT);
            }
            if (getDegree(DegreeIndex.EXTENSION2) != null)
            {
                mostImportantDegrees.add(DegreeIndex.EXTENSION2);
            }
            if (getDegree(DegreeIndex.EXTENSION3) != null)
            {
                mostImportantDegrees.add(DegreeIndex.EXTENSION3);
            }
        }
        LOGGER.fine("getMostImportantDegreeIndexes() this=" + this + " result=" + mostImportantDegrees);   //NOI18N
        return new ArrayList<DegreeIndex>(mostImportantDegrees);
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
        LOGGER.fine("fitDegree() -- d=" + d + " this=" + this);   //NOI18N

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

        LOGGER.fine("fitDegree()  destDegree=" + destDegree);   //NOI18N
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
        LOGGER.fine("fitDegreeAdvanced() -- d=" + d + " this=" + this + " scales=" + optScale);   //NOI18N

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
                case NINTH_FLAT:        // Handled below
                case NINTH:
                case NINTH_SHARP:
                    // If we are are, this chord has no 9 defined
                    // If d=#9 then this chord type is major, otherwise we would not be here
                    destDegree = Degree.NINTH;
                    if (extension.equals("m7b5"))
                    {
                        destDegree = Degree.NINTH_FLAT;
                    }
                    break;

                case THIRD_FLAT:    // Handled below
                case THIRD:
                    // This chord type has no third defined (otherwise we wouldn't be here), then this chord must be a sus chord
                    destDegree = Degree.FOURTH_OR_ELEVENTH;
                    break;
                case FOURTH_OR_ELEVENTH:
                    // 4th can only be naturally mapped to a sus4 or m11 chord type. If we're here, this chord type is different.
                    // 11th natural can only be naturally mapped to a 11, #11 or sus4. If we're here, this chord type is different.               
                    if (family == Family.MINOR || family == Family.DIMINISHED)
                    {
                        destDegree = Degree.FOURTH_OR_ELEVENTH;                      // 11th ok with all minors and diminished
                    } else if ((dTmp = getDegree(6)) != null)
                    {
                        destDegree = dTmp;                                 // If chord type has #11/b5 go there
                    } else if ((dTmp = getDegree(Degree.Natural.NINTH)) != null && dTmp.getAlteration() != 0)
                    {
                        destDegree = Degree.ELEVENTH_SHARP;                // 11# ok as a "transition note" if no #11 and no altered 9th
                    } else
                    {
                        destDegree = Degree.FOURTH_OR_ELEVENTH;             // 11th OK e.g. with C7M#5 or C7#5
                    }
                    break;
                case ELEVENTH_SHARP:
                    // 11th sharp can only be naturally mapped to a #11, 11 or b5. If we're here, this chord type is different.               
                    if (getDegree(5) != null)
                    {
                        destDegree = Degree.FOURTH_OR_ELEVENTH;              //  This chord type must be a sus chord
                    } else
                    {
                        destDegree = getDegree(Degree.Natural.FIFTH);         // go to 5 or #5
                    }
                    break;
                case FIFTH_FLAT:    // Handled below
                case FIFTH:
                case FIFTH_SHARP:
                    // We should never be here : all chord types have a fifth defined
                    throw new IllegalStateException("We should not end up here ! d=" + d + " this=" + this + " scales=" + optScale);   //NOI18N
                case THIRTEENTH_FLAT:
                    // Thirteenth natural can only be naturally mapped on b13, 13 or #5 chord types. If we're here this chord type is different.
                    destDegree = getDegree(Degree.Natural.FIFTH);            // go to 5 or b5
                    break;
                case SIXTH_OR_THIRTEENTH:
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
                    break;
                case SEVENTH_FLAT:
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

                    break;
                case SEVENTH:
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
                    break;
                default:
                    throw new IllegalStateException("d=" + d + " this=" + this + " scales=" + optScale);   //NOI18N
            }
        }

        assert destDegree != null : "destDegree is null ! d=" + d + " this=" + this + " scales=" + optScale;   //NOI18N

        LOGGER.fine("fitDegreeAdvanced()  destDegree=" + destDegree);   //NOI18N
        return destDegree;
    }

    /**
     * Rely on fitDegreeAdvanced(Degree d, optScales).
     * <p>
     * If di does not directly correspond to one of these ChordType degrees, make some assumptions, e.g. if
     * di==DegreeIndex.SIXTH_OR_SEVENTH then try to fit to th seventh degree of this ChordType.
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
            switch (di)
            {
                case ROOT:
                case THIRD_OR_FOURTH:
                case FIFTH:
                    // We should not be here because all chords must have those degrees defined 
                    throw new IllegalStateException("di=" + di);   //NOI18N
                case SIXTH_OR_SEVENTH:
                    d = fitDegreeAdvanced(Degree.SEVENTH, optScale);       // 7 suits most chords...
                    break;
                case EXTENSION1:
                    d = fitDegreeAdvanced(Degree.NINTH, optScale);         // 9 suits most chords
                    break;
                case EXTENSION2:
                case EXTENSION3:
                    d = fitDegreeAdvanced(Degree.SIXTH_OR_THIRTEENTH, optScale);
                    break;
                default:
                    throw new IllegalStateException("di=" + di);   //NOI18N
            }
        }
        return d;
    }

    /**
     * Compute how much "similar" is the specified ChordType with this object.
     * <p>
     * Index is calculated by adding the weights below until a mismatch is found. Identical ChordTypes have a similarity index of
     * 63. For example C7 and Cm7 have a similarity index=0 (different families). C7 and C9 have a similarity index=32+16+8=56 (same
     * family, same fifth, same sixth_seventh, but extension1 mismatch).
     * <p>
     * Same ChordType.FAMILY:32<br>
     * Same DegreeIndex.FIFTH: 16<br>
     * Same DegreeIndex.SIXTH_SEVENTH: 8<br>
     * Same DegreeIndex.EXTENSION1: 4<br>
     * Same DegreeIndex.EXTENSION2: 2<br>
     * Same DegreeIndex.EXTENSION3: 1<br>
     *
     * @param ct
     * @return
     */
    public int getSimilarityIndex(ChordType ct)
    {
        int res = 0;
        if (!family.equals(ct.family))
        {
            return res;
        }
        res = 32;

        for (int i = 2; i <= 6; i++)
        {
            Degree d = i < degrees.size() ? degrees.get(i) : null;
            Degree dct = i < ct.degrees.size() ? ct.degrees.get(i) : null;
            if (!Objects.equals(d, dct))
            {
                return res;
            }
            res += 2 ^ (6 - i);
        }
        return res;
    }

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
     * @return True for e.g. Csus, C7sus
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
     * @return A corresponding Chord with a default C root.
     */
    public Chord getChord()
    {
        return chord.clone();
    }

    @Override
    public boolean equals(Object o)
    {
        boolean b = false;
        if (o instanceof ChordType)
        {
            ChordType ct = (ChordType) o;
            b = getDegrees().equals(ct.getDegrees());
        }
        return b;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(this.degrees);
        return hash;
    }

    // =============================================================================================
    // Private methods
    // =============================================================================================
    /**
     * Return true if d equals -1, 0, 1 or NOT_PRESENT
     */
    private boolean checkDegree(int d)
    {
        return (d == -1) || (d == 0) || (d == 1) || (d == NOT_PRESENT);
    }
}

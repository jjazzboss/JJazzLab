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

import java.text.ParseException;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordType.DegreeIndex;
import org.jjazz.harmony.api.Note.Alteration;
import org.jjazz.utilities.api.ResUtil;

/**
 * A jazz chord symbol.
 * <p>
 * Examples : "Cm7", "F7b9", "Dbalt", "Amaj7/F#".
 * <p>
 * This is an immutable class.
 * <p>
 */
public class ChordSymbol implements Cloneable
{

    /**
     * The original name of the chord which has been used at ChordSymbol creation.
     */
    private String originalName;
    /**
     * The "official" name of the chord as recognized by JJazz. It may differ from originalName used a ChordType alias. E.g. originalName="EMIN7" and
     * name="Em7".
     */
    private String name;

    /**
     * Root degree e.g. E in Em7.
     */
    private Note rootNote;
    /**
     * Bass degree e.g. A in Em7/A.
     */
    private Note bassNote;
    /**
     * The ChordType e.g. "m7" in Em7.
     */
    private ChordType chordType;

    private static final Logger LOGGER = Logger.getLogger(ChordSymbol.class.getSimpleName());

    public ChordSymbol()
    {
        this(new Note(0), ChordTypeDatabase.getInstance().getChordType(0));
    }

    public ChordSymbol(Note rootDg, ChordType ct)
    {
        this(rootDg, rootDg, ct);
    }

    /**
     *
     * @param rootDg
     * @param bassDg If null reuse rootDg
     * @param ct
     */
    public ChordSymbol(Note rootDg, Note bassDg, ChordType ct)
    {
        if ((rootDg == null) || (ct == null))
        {
            throw new IllegalArgumentException("rootDg=" + rootDg + " bassDg=" + bassDg + " ct=" + ct);
        }
        rootNote = buildStdNote(rootDg);
        bassNote = (bassDg != null) ? buildStdNote(bassDg) : rootNote;
        chordType = ct;
        name = computeName();
        originalName = name;
    }

    /**
     * A protected constructor to alter the originalName.
     *
     * @param rootDg
     * @param bassDg       If null reuse rootDg
     * @param ct
     * @param originalName No check is performed on this originalName, so caller must be careful.
     */
    protected ChordSymbol(Note rootDg, Note bassDg, ChordType ct, String originalName)
    {
        this(rootDg, bassDg, ct);
        if (originalName == null || originalName.isBlank())
        {
            throw new IllegalArgumentException("rootDg=" + rootDg + " bassDg=" + bassDg + " ct=" + ct + " originalName=" + originalName);
        }
        this.originalName = originalName;
    }

    /**
     * Construct a ChordSymbol from a string.
     *
     * @param str A string like "Cm7", "Abmaj7", "Bm7b5", "G#MAJ7", "C/F"
     * @throws ParseException
     */
    public ChordSymbol(String str) throws ParseException
    {
        if (str == null || str.isBlank())
        {
            throw new IllegalArgumentException("str=\"" + str + "\"");
        }
        str = str.trim();

        // Save the original name of the chord symbol, making sure first letter is uppercase
        originalName = str.substring(0, 1).toUpperCase() + str.substring(1);

        int bass_index;
        StringBuilder sb = new StringBuilder(originalName);

        // Get the optional bass note
        bass_index = originalName.lastIndexOf('/');
        if (bass_index != -1)
        {
            // There is a bass degree e.g. "Am7/D"
            try
            {
                bassNote = buildStdNote(new Note(originalName.substring(bass_index + 1)));
            } catch (ParseException e)
            {
                throw new ParseException(ResUtil.getString(getClass(), "CTL_InvalidChordSymbol") + ": " + originalName + ". " + e.getLocalizedMessage(), 0);
            }
            // continue with bass degree removed
            sb.delete(bass_index, sb.length());
        }

        String errorInvalid = ResUtil.getString(getClass(), "CTL_InvalidChordSymbol");

        // Get the root note
        try
        {
            rootNote = buildStdNote(new Note(sb.toString()));
        } catch (ParseException e)
        {
            throw new ParseException(errorInvalid + ": " + originalName + ". " + e.getLocalizedMessage(), 0);
        }

        // Remove the root note
        sb.delete(0, 1);
        if (sb.length() > 0 && (sb.charAt(0) == 'b' || sb.charAt(0) == '#'))
        {
            sb.delete(0, 1);
        }

        // if no bass specified, bass degree = root degree
        if (bassNote == null)
        {
            bassNote = rootNote;
        }

        // Fint the ChordType of the chord
        chordType = ChordTypeDatabase.getInstance().getChordType(sb.toString());

        if (chordType == null)
        {
            // Chord type not recognized
            throw new ParseException(errorInvalid + ": " + originalName, 0);
        } else
        {
            name = computeName();
        }
    }

    @Override
    public ChordSymbol clone()
    {
        ChordSymbol s = new ChordSymbol();
        //  There are all immutable objects      
        s.originalName = originalName;
        s.name = name;
        s.chordType = chordType;
        s.rootNote = rootNote;
        s.bassNote = bassNote;
        return s;
    }

    public Note getRootNote()
    {
        return rootNote;        // Clone() not needed, immutable class
    }

    /**
     * If no bass note defined return getRootNote().
     *
     * @return
     */
    public Note getBassNote()
    {
        return bassNote;        // Clone() not needed, immutable class
    }

    public ChordType getChordType()
    {
        return chordType;      // Clone() not needed, immutable class
    }

    /**
     * The standard ChordSymbol recognized by JJazzLab.
     * <p>
     * This may differ from the original name.
     *
     * @return
     * @see getOriginalName()
     */
    public String getName()
    {
        return name;
    }

    /**
     * Return the name used at creation if the ChordType(String) constructor has been used.
     * <p>
     * It may differ from the getName() if an chord type alias was used. First char is always upper case.
     *
     * @return
     */
    public String getOriginalName()
    {
        return originalName;
    }

    @Override
    public String toString()
    {
        return name;
    }

    /**
     * Print the ChordSymbol, its aliases and degrees
     */
    public void dump()
    {
        System.out.print("ChordSymbol " + this.toString());
        System.out.println(" " + chordType.toDegreeString());
    }

    /**
     * Get a transposed ChordSymbol.
     * <p>
     * If this ChordSymbol uses a specific originalName, it will be reused in the returned value.
     *
     * @param t   The amount of transposition in semi-tons.
     * @param alt If null, alteration of returned root & bass notes is unchanged. If not null use alt as root & bass notes alteration.
     * @return A new transposed ChordSymbol.
     */
    public ChordSymbol getTransposedChordSymbol(int t, Note.Alteration alt)
    {
        Note root = alt == null ? rootNote : new Note(rootNote, alt);
        Note bass = alt == null ? bassNote : new Note(bassNote, alt);
        Note transposedRoot = root.getTransposedWithinOctave(t);
        ChordSymbol cs = new ChordSymbol(transposedRoot, bass.getTransposedWithinOctave(t), chordType);

        // If current chord symbol has a special original name, reuse it
        if (!name.equals(originalName))
        {
            String strEnd = originalName.substring(rootNote.toRelativeNoteString().length());
            cs.originalName = transposedRoot.toRelativeNoteString() + strEnd;
        }

        return cs;
    }

    /**
     * Get a simplified ChordSymbol by keeping only the first nbMaxDegrees degrees.
     *
     * @param nbMaxDegrees Must be &gt; 2
     * @return Can't be null
     */
    public ChordSymbol getSimplified(int nbMaxDegrees)
    {
        var res = this;
        var ct = chordType.getSimplified(nbMaxDegrees);
        if (ct != chordType)
        {
            res = new ChordSymbol(rootNote, bassNote, ct);
        }
        return res;
    }

    /**
     * Get the chord corresponding to this ChordSymbol.
     * <p>
     * The method chooses to use flat or sharp notes depending on the ChordSymbol, using the most "common" tonality associated to the ChordSymbol.
     *
     * @return
     */
    public Chord getChord()
    {
        final int C = 0;
        final int Db = 1;
        final int D = 2;
        final int Eb = 3;
        final int E = 4;
        final int F = 5;
        final int Gb = 6;
        final int G = 7;
        final int Ab = 8;
        final int A = 9;
        final int Bb = 10;
        final int B = 11;


        Chord c = new Chord();      // Use flats by default        

        Alteration defaultAlt = Alteration.FLAT;
        if (name.length() >= 2 && name.charAt(1) == '#')
        {
            defaultAlt = Alteration.SHARP;
            for (var n : c.getNotes())
            {
                // Change all notes
                c.removeNote(n.getPitch());
                c.add(new Note(n, Alteration.SHARP));
            }
        }


        for (Degree d : chordType.getDegrees())
        {
            Alteration alt = defaultAlt;
            int extensionOffset = 0;
            switch (d)
            {
                case ROOT ->
                {
                    // Nothing 
                }
                case NINTH_FLAT ->
                {
                    extensionOffset = 12;
                }
                case NINTH ->
                {
                    extensionOffset = 12;
                    alt = switch (rootNote.getRelativePitch())
                    {
                        case E, B ->
                            Alteration.SHARP;
                        default ->
                            defaultAlt;
                    };
                }
                case NINTH_SHARP ->
                {
                    extensionOffset = 12;
                    alt = switch (rootNote.getRelativePitch())
                    {
                        case C, Eb, G, Bb ->
                            Alteration.SHARP;
                        default ->
                            defaultAlt;
                    };
                }
                case THIRD_FLAT ->
                {
                    // Always flat
                }
                case THIRD ->
                {
                    alt = switch (rootNote.getRelativePitch())
                    {
                        case D, E, A, B ->
                            Alteration.SHARP;
                        default ->
                            defaultAlt;
                    };
                }
                case FOURTH_OR_ELEVENTH ->
                {
                    // Always flat                     
                }
                case ELEVENTH_SHARP ->
                {
                    extensionOffset = 12;
                    alt = switch (rootNote.getRelativePitch())
                    {
                        case C, D, E, G, A ->
                            Alteration.SHARP;
                        default ->
                            defaultAlt;
                    };
                }
                case FIFTH_FLAT ->
                {
                    // Nothing     
                }
                case FIFTH ->
                {
                    alt = switch (rootNote.getRelativePitch())
                    {
                        case B ->
                            Alteration.SHARP;
                        default ->
                            defaultAlt;
                    };
                }
                case FIFTH_SHARP ->
                {
                    alt = switch (rootNote.getRelativePitch())
                    {
                        case C, D, F, G, Bb ->
                            Alteration.SHARP;
                        default ->
                            defaultAlt;
                    };
                }
                case THIRTEENTH_FLAT ->
                {
                    extensionOffset = 12;
                }
                case SIXTH_OR_THIRTEENTH ->
                {
                    extensionOffset = 12;
                    alt = switch (rootNote.getRelativePitch())
                    {
                        case E, A, B ->
                            Alteration.SHARP;
                        default ->
                            defaultAlt;
                    };
                }
                case SEVENTH_FLAT ->
                {
                    // NOthing
                }
                case SEVENTH ->
                {
                    alt = switch (rootNote.getRelativePitch())
                    {
                        case D, E, G, A, B ->
                            Alteration.SHARP;
                        default ->
                            defaultAlt;
                    };
                }
                default -> throw new AssertionError(d.name());

            }


            int pitch = d.getPitch() + rootNote.getRelativePitch() + extensionOffset;
            c.add(new Note(pitch, 1f, 64, alt));
        }

        return c;
    }

    /**
     * @return E.g. for D7 return "[D, F#, A, C]"
     */
    /**
     * @return E.g. for D7 return "[D, F#, A, C]"
     */
    public String toNoteString()
    {
        return getChord().toRelativeNoteString(rootNote.getAlterationDisplay());
    }

    /**
     * Compare chord types of 2 chord symbols.
     *
     * @param cs A chord symbol
     *
     * @return True if chords types are equivalent, e.g. for "Am7" and "Ebm-7"
     */
    public boolean isSameChordType(ChordSymbol cs)
    {
        if (cs == null)
        {
            throw new NullPointerException("cs");
        }
        // We can use "==" equality (and not equals) because ChordTypes are immutable objects
        // that only come from the ChordType.database
        return chordType == cs.chordType;
    }

    /**
     * Get the equivalent of relPitch but for the specified destination chord symbol.
     * <p>
     * Ex: this=Dbm7, destCs=F, relPitch=4=E =&gt; return 8=Ab
     *
     * @param relPitch A relative pitch
     * @param destCs
     * @return A relative pitch.
     */
    public int getRelativePitch(int relPitch, ChordSymbol destCs)
    {
        if (relPitch < 0 || relPitch > 11 || destCs == null)
        {
            throw new IllegalArgumentException("relPitch=" + relPitch + " destCs=" + destCs);
        }
        int srcRelPitchToRoot = Note.getNormalizedRelPitch(relPitch - getRootNote().getRelativePitch());
        int destRelPitch = Note.getNormalizedRelPitch(destCs.getRootNote().getRelativePitch() + srcRelPitchToRoot);
        return destRelPitch;
    }

    /**
     * Return the relative pitch corresponding to the specified degree index for this chord symbol.
     * <p>
     * Ex: this=E7, degreeIndex=THIRD_OR_FOURTH, return G#=8.
     *
     * @param di
     * @return -1 if no such degreeIndex.
     */
    public int getRelativePitch(DegreeIndex di)
    {
        int relPitch = -1;
        Degree d = chordType.getDegree(di);
        if (d != null)
        {
            relPitch = Note.getNormalizedRelPitch(rootNote.getRelativePitch() + d.getPitch());
        }
        return relPitch;
    }

    /**
     * Return the relative pitch corresponding to the specified degree of this chord symbol.
     * <p>
     * Ex: this=E7, degree=THIRT_FLAT, return G
     *
     * @param d
     * @return A relative pitch.
     */
    public int getRelativePitch(Degree d)
    {
        if (d == null)
        {
            throw new NullPointerException("d");
        }
        int relPitch = Note.getNormalizedRelPitch(rootNote.getRelativePitch() + d.getPitch());
        return relPitch;
    }

    /**
     * Comparison is based on rootNote and bassNote relative pitch, ChordType and originalName.
     * <p>
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final ChordSymbol other = (ChordSymbol) obj;
        if (!Objects.equals(this.originalName, other.originalName))
        {
            return false;
        }
        if (!Objects.equals(this.rootNote, other.rootNote))
        {
            return false;
        }
        if (!Objects.equals(this.bassNote, other.bassNote))
        {
            return false;
        }
        return Objects.equals(this.chordType, other.chordType);
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.originalName);
        hash = 23 * hash + Objects.hashCode(this.rootNote);
        hash = 23 * hash + Objects.hashCode(this.bassNote);
        hash = 23 * hash + Objects.hashCode(this.chordType);
        return hash;
    }

    /**
     * Create a random chord symbol: random root note and chord type, sometimes with a different bass note.
     *
     * @return
     */
    static public ChordSymbol getRandom()
    {
        int rootPitch = (int) Math.round(Math.random() * 11);
        int bassPitch = Math.round(1) > 0.7 ? rootPitch : (int) Math.round(Math.random() * 11);
        var chordTypes = ChordTypeDatabase.getInstance().getChordTypes();
        int index = (int) Math.round(Math.random() * (chordTypes.length - 1));
        ChordType ct = chordTypes[index];
        ChordSymbol res = new ChordSymbol(new Note(rootPitch), new Note(bassPitch), ct);
        return res;
    }

    // --------------------------------------------------------------------- 
    // Private methods
    // ---------------------------------------------------------------------
    /**
     * Compute default name from ChordSymbol components.
     *
     * @return
     */
    protected String computeName()
    {
        String s = rootNote.toRelativeNoteString() + chordType.getName() + (bassNote.equalsRelativePitch(rootNote) ? "" : ("/" + bassNote.
                toRelativeNoteString()));
        return s;
    }

    private Note buildStdNote(Note n)
    {
        return new Note(n.getRelativePitch(), 1, 64, n.getAlterationDisplay());
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // ---------------------------------------------------------------------
//    private Object writeReplace()
//    {
//        return new SerializationProxy(this);
//    }
//
//    private void readObject(ObjectInputStream stream)
//            throws InvalidObjectException
//    {
//        throw new InvalidObjectException("Serialization proxy required");
//    }
//
//    private static class SerializationProxy implements Serializable
//    {
//
//        private static final long serialVersionUID = 199237687633L;
//        private final int spVERSION = 2;
//        private final String spName;   
//        private final String spOriginalName;
//        // XStream can't deserialize the ° char : little hack to avoid the issue
//        private static final String DOT_REPLACEMENT = "_UpperDot_";
//
//        private SerializationProxy(ChordSymbol cs)
//        {
//            spName = cs.getOriginalName().replace("°", DOT_REPLACEMENT);
//            spOriginalName = cs.getOriginalName().replace("°", DOT_REPLACEMENT);
//        }
//
//        private Object readResolve() throws ObjectStreamException
//        {
//            String s = spOriginalName == null ? spName.replace(DOT_REPLACEMENT, "°") : spOriginalName.replace(DOT_REPLACEMENT, "°");
//            ChordSymbol cs;
//            try
//            {
//                cs = new ChordSymbol(s);
//            } catch (ParseException e)
//            {
//                LOGGER.log(Level.WARNING, spName + ": Invalid chord symbol, " + e.getLocalizedMessage() + ". Using 'C' ChordSymbol instead.");
//                cs = new ChordSymbol();
//            }
//
//
//            return cs;
//        }
//    }
}

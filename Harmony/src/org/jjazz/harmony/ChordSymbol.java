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
package org.jjazz.harmony;

import java.text.ParseException;
import java.util.Objects;
import java.util.logging.Logger;
import org.openide.util.NbBundle.Messages;
import static org.jjazz.harmony.Bundle.*;
import org.jjazz.harmony.ChordType.DegreeIndex;

/**
 * A jazz chord symbol.
 * <p>
 * Examples : "Cm7", "F7b9", "Dbalt", "Amaj7/F#".
 * <p>
 * This is an immutable class.
 * <p>
 */
@Messages(
        {
            "CTL_InvalidChordSymbol=Invalid chord symbol",
        }
)
public class ChordSymbol implements Cloneable
{

    /**
     * The original name of the chord which has been used at ChordSymbol creation.
     */
    private String originalName;
    /**
     * The "official" name of the chord as recognized by JJazz. It may differ from originalName used a ChordType alias. E.g.
     * originalName="EMIN7" and name="Em7".
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

    private static final Logger LOGGER = Logger.getLogger(ChordSymbol.class.getName());

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
            throw new IllegalArgumentException("rootDg=" + rootDg + " bassDg=" + bassDg + " ct=" + ct);   //NOI18N
        }
        rootNote = rootDg;
        bassNote = (bassDg != null) ? bassDg : rootDg;
        chordType = ct;
        name = computeName();
        originalName = name;
    }

    /**
     * A protected constructor to alter the originalName.
     *
     * @param rootDg
     * @param bassDg If null reuse rootDg
     * @param ct
     * @param originalName No check is performed on this originalName, so caller must be careful.
     */
    protected ChordSymbol(Note rootDg, Note bassDg, ChordType ct, String originalName)
    {
        this(rootDg, bassDg, ct);
        if (originalName == null || originalName.isBlank())
        {
            throw new IllegalArgumentException("rootDg=" + rootDg + " bassDg=" + bassDg + " ct=" + ct + " originalName=" + originalName);   //NOI18N
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
            throw new IllegalArgumentException("str=\"" + str + "\"");   //NOI18N
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
                bassNote = new Note(originalName.substring(bass_index + 1));
            } catch (ParseException e)
            {
                throw new ParseException(CTL_InvalidChordSymbol() + ": " + originalName + ". " + e.getLocalizedMessage(), 0);
            }
            // continue with bass degree removed
            sb.delete(bass_index, sb.length());
        }

        // Get the root note
        try
        {
            rootNote = new Note(sb.toString());
        } catch (ParseException e)
        {
            throw new ParseException(CTL_InvalidChordSymbol() + ": " + originalName + ". " + e.getLocalizedMessage(), 0);
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
            throw new ParseException(CTL_InvalidChordSymbol() + ": " + originalName, 0);
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
     * @param t The amount of transposition in semi-tons.
     * @param alt If null, alteration of returned root & bass notes is unchanged. If not null use alt as root & bass notes
     * alteration.
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
     * @return A Chord object corresponding to this ChordSymbol
     */
    public Chord getChord()
    {
        Chord c = chordType.getChord();
        c.transpose(rootNote.getRelativePitch());
        return c;
    }

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
            throw new NullPointerException("cs");   //NOI18N
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
            throw new IllegalArgumentException("relPitch=" + relPitch + " destCs=" + destCs);   //NOI18N
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
            throw new NullPointerException("d");   //NOI18N
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
        if (!Objects.equals(this.chordType, other.chordType))
        {
            return false;
        }
        return true;
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

    // --------------------------------------------------------------------- 
    // Private methods
    // ---------------------------------------------------------------------
    /**
     * Compute default name from ChordSymbol components.
     */
    protected String computeName()
    {
        String s = rootNote.toRelativeNoteString() + chordType.getName() + (bassNote.equalsRelativePitch(rootNote) ? "" : ("/" + bassNote.
                toRelativeNoteString()));
        return s;
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

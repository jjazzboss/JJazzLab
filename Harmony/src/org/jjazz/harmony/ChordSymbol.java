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

import java.io.*;
import java.text.ParseException;
import java.util.logging.Level;
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
public class ChordSymbol implements Serializable, Cloneable
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
            throw new IllegalArgumentException("rootDg=" + rootDg + " bassDg=" + bassDg + " ct=" + ct);
        }
        rootNote = rootDg;
        bassNote = (bassDg != null) ? bassDg : rootDg;
        chordType = ct;
        name = computeName();
        originalName = name;
    }

    /**
     * Construct a ChordSymbol from a string.
     *
     * @param str A string like "Cm7", "Abmaj7", "Bm7b5", "G#MAJ7", "C/F"
     * @throws ParseException
     */
    public ChordSymbol(String str) throws ParseException
    {
        if (str == null)
        {
            throw new IllegalArgumentException("str=\"" + str + "\"");
        }

        // save the original name of the chord symbol
        originalName = str.trim();

        if (originalName.length() == 0)
        {
            throw new IllegalArgumentException("str=" + str);
        }

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
     * Return the name used at creation if the ChordType(String) constructor or valueOf(String) function has been used. I
     * <p>
     * In this case this string may differ from the name. Otherwise it's same as name.
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
     *
     * @param t The amount of transposition in semi-tons.
     * @return A new transposed ChordSymbol.
     */
    public ChordSymbol getTransposedChordSymbol(int t)
    {
        ChordSymbol cs = new ChordSymbol(rootNote.getTransposedWithinOctave(t), bassNote.getTransposedWithinOctave(t), chordType);
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
        // We can use "==" equality (and not equals) because ChordTypes are immutable objects
        // that only come from the ChordType.database
        return (chordType == cs.chordType);
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
     * Comparison is based on rootNote and bassNote relative pitch and the ChordType.
     * <p>
     * Note that ChordSymbol names are not taken into account.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof ChordSymbol)
        {
            ChordSymbol cs = (ChordSymbol) o;

            return rootNote.equalsRelativePitch(cs.rootNote) && bassNote.equalsRelativePitch(cs.bassNote) && isSameChordType(cs);
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int rootRelPitch = rootNote != null ? rootNote.getRelativePitch() : 0;
        int bassRelPitch = bassNote != null ? bassNote.getRelativePitch() : 0;
        int hash = 3;
        hash = 17 * hash + rootRelPitch;
        hash = 17 * hash + bassRelPitch;
        hash = 17 * hash + (chordType != null ? chordType.hashCode() : 0);
        return hash;
    }

    /* --------------------------------------------------------------------- Private methods
    * --------------------------------------------------------------------- */
    /**
     * Compute default name from ChordSymbol components.
     */
    protected String computeName()
    {
        String s = rootNote.toRelativeNoteString() + chordType.getName() + (bassNote.equalsRelativePitch(rootNote) ? "" : ("/" + bassNote.
                toRelativeNoteString()));
        return s;
    }

    /* --------------------------------------------------------------------- Serialization
    * --------------------------------------------------------------------- */
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 199237687633L;
        private final int spVERSION = 1;
        private final String spName;

        private SerializationProxy(ChordSymbol cs)
        {
            spName = cs.getOriginalName();
        }

        private Object readResolve() throws ObjectStreamException
        {
            ChordSymbol cs;
            try
            {
                cs = new ChordSymbol(spName);
            } catch (ParseException e)
            {
                LOGGER.
                        log(Level.WARNING, spName + ": Invalid chord symbol, " + e.getLocalizedMessage() + ". Using 'C' ChordSymbol instead.");
                cs = new ChordSymbol();
            }
            return cs;
        }
    }

}

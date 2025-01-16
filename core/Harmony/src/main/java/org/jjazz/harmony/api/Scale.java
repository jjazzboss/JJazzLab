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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A list of notes.
 * <p>
 * Example: name=MajorPentatonic and startNote=Eb represent the scale Eb, F, G, Bb, C
 */
public class Scale
{

    private String name;
    /**
     * The notes starting on C0
     */
    private final ArrayList<Note> notes0 = new ArrayList<>();
    private final ArrayList<Degree> degrees = new ArrayList<>();
    private final transient ArrayList<Integer> intervals = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(Scale.class.getSimpleName());

    /**
     * Create a scale from a list of degrees.
     *
     * @param name
     * @param degs The ascending unique degrees starting with ROOT.
     */
    public Scale(String name, Degree... degs)
    {
        if (name == null || name.isEmpty() || degs == null || degs.length == 0 || degs[0] != Degree.ROOT)
        {
            throw new IllegalArgumentException("name=" + name + " degrees=" + degs);
        }
        this.name = name;
        this.degrees.add(Degree.ROOT);
        this.notes0.add(new Note(0));
        // Save degrees, notes and intervals
        int lastPitch = 0;
        for (int i = 1; i < degs.length; i++)
        {
            int newPitch = degs[i].getPitch();
            if (newPitch <= lastPitch)
            {
                throw new IllegalArgumentException("Degrees must unique and ascending. name=" + name + " degs=" + degs);
            }
            this.degrees.add(degs[i]);
            this.notes0.add(new Note(degs[i].getPitch()));
            this.intervals.add(newPitch - lastPitch);
            lastPitch = newPitch;
        }
    }

    public String getName()
    {
        return name;
    }

    /**
     * The list of notes starting at pitch 0.
     *
     * @return
     */
    public List<Note> getNotes()
    {
        return new ArrayList<>(notes0);
    }

    /**
     * The list of ascending notes starting on the specified note.
     *
     * @param startNote
     * @return Returned notes reuse startNote's duration, velocity and accidental.
     */
    public List<Note> getNotes(Note startNote)
    {
        if (startNote.getRelativePitch() == 0)
        {
            return getNotes();
        }
        List<Note> scale = new ArrayList<>();
        int pitch = startNote.getPitch();
        for (Note n : notes0)
        {
            scale.add(new Note(pitch + n.getPitch(), startNote.getDurationInBeats(), startNote.getVelocity(),
                    startNote.getAccidental()));
        }
        return scale;
    }

    /**
     * The list of degrees composing this scale.
     *
     * @return
     */
    public List<Degree> getDegrees()
    {
        return new ArrayList<>(degrees);
    }

    /**
     * For example for scale [C,D,E,G,A], intervals=[2,2,3,2]
     *
     * @return A list of intervals between notes (size=nb of notes-1)
     */
    public List<Integer> getIntervals()
    {
        return new ArrayList<>(intervals);
    }

    /**
     * Get the scale degree corresponding to specified relative pitch, if any.
     *
     * @param relPitch
     * @return A degree or null.
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
     * Get the scale degrees corresponding to a natural degree.<p>
     * Ex: n=NINTH, scale=MAJOR/IONIAN =&gt; return NINTH<br>
     * Ex: n=NINTH, scale=PHRYGIAN =&gt; return NINTH_FLAT<br>
     * Ex: n=NINTH, scale=ALTERED =&gt; return NINTH_FLAT and NINTH_SHARP<br>
     * Ex: n=THIRTEENH, scale=PENTATONIC_MINOR =&gt; return an empty list<br>
     *
     * @param n A natural degree
     * @return A list of degrees. List size is usually 1, possibly 2 or 0.
     */
    public List<Degree> getDegrees(Degree.Natural n)
    {
        ArrayList<Degree> res = new ArrayList<>();
        for (Degree d : degrees)
        {
            if (d.getNatural() == n)
            {
                res.add(d);
            }
        }
        return res;
    }

    @Override
    public String toString()
    {
        return name + ":" + getDegrees();
    }

}

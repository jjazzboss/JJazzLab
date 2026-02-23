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
package org.jjazz.phrase.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.ObservableProperties;
import org.openide.util.Exceptions;

/**
 * A Note with a position and optional client properties.
 * <p>
 * This is an immutable class EXCEPT for the client properties.<p>
 * Two different NoteEvent instances can not be equal. If you need NoteEvent map keys to be considered equal when they share the same Note attributes, use the
 * AsNoteKey inner class.
 */
public class NoteEvent extends Note implements Cloneable, Comparable<Note>
{

    /**
     * System property used to customize NoteEvent.toString().
     * <p>
     * Used as String.format() parameter with arguments 1=toPianoOctaveString(), 2=position, 3=durationInBeats, 4=velocity, 5=identityHashCode
     * <p>
     * Example: "[%1$s, dur=%3$.2f]"
     */
    public static final String SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT = "NoteEventToStringFormat";

    /**
     * If true this note is an "adjusting note".
     * <p>
     * "Adjusting" means it's a temporary note used by an action which will eventually replace the temporary note by a non-adjusting note. For example if user
     * is mouse-dragging a note in an editor, editor might generate "adjusting notes" until drag operation is complete.
     * <p>
     * Note that this property is not directly used by the NoteEvent class.
     */
    public static final String PROP_IS_ADJUSTING = "PropIsAdjusting";


    private float position;
    protected ObservableProperties<Object> clientProperties;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(NoteEvent.class.getSimpleName());

    /**
     * Create a new NoteEvent.
     *
     * @param pitch
     * @param duration
     * @param velocity
     * @param posInBeats
     * @param acc
     */
    public NoteEvent(int pitch, float duration, int velocity, float posInBeats, Accidental acc)
    {
        super(pitch, duration, velocity, acc);
        if (posInBeats < 0)
        {
            throw new IllegalArgumentException("posInBeats=" + posInBeats);
        }
        position = posInBeats;
        clientProperties = new ObservableProperties<>();
    }

    /**
     * Create a new NoteEvent with Accidental.FLAT.
     * <p>
     * @param pitch
     * @param duration
     * @param velocity
     * @param posInBeats
     */
    public NoteEvent(int pitch, float duration, int velocity, float posInBeats)
    {
        this(pitch, duration, velocity, posInBeats, Accidental.FLAT);
    }

    /**
     * Create a new NoteEvent from a Note at specified position.
     * <p>
     *
     * @param n
     * @param posInBeats
     */
    public NoteEvent(Note n, float posInBeats)
    {
        this(n.getPitch(), n.getDurationInBeats(), n.getVelocity(), posInBeats, n.getAccidental());
    }


    /**
     * Set or reset the note as isAdjusting.
     *
     * @param note
     * @param b    If true note is marked isAdjusting.
     * @see #PROP_IS_ADJUSTING
     */
    static public void markIsAdjustingNote(NoteEvent note, boolean b)
    {
        Boolean value = b == true ? Boolean.TRUE : null;
        note.getClientProperties().put(NoteEvent.PROP_IS_ADJUSTING, value);
    }

    /**
     * Check if note is marked isAdjusting.
     *
     * @param ne
     * @return
     */
    static public boolean isAdjustingNote(NoteEvent ne)
    {
        return Boolean.TRUE.equals(ne.getClientProperties().get(PROP_IS_ADJUSTING));
    }

    /**
     * Create a new and possibly modified NoteEvent from this instance.
     * <p>
     * @param pitch          if &lt; 0 reuse this instance's pitch, otherwise use the parameter value
     * @param duration       if &lt; 0 reuse this instance's duration, otherwise use the parameter value
     * @param velocity       if &lt; 0 reuse this instance's velocity, otherwise use the parameter value
     * @param posInBeats     if &lt; 0 reuse this instance's position, otherwise use the parameter value
     * @param acc            if null reuse this instance's accidental, otherwise use the parameter value
     * @param copyProperties if true copy the properties
     * @return
     */
    public NoteEvent setAll(int pitch, float duration, int velocity, float posInBeats, Accidental acc, boolean copyProperties)
    {
        var res = new NoteEvent(pitch < 0 ? getPitch() : pitch,
                duration < 0 ? getDurationInBeats() : duration,
                velocity < 0 ? getVelocity() : velocity,
                posInBeats < 0 ? getPositionInBeats() : posInBeats,
                acc == null ? getAccidental() : acc
        );
        if (copyProperties)
        {
            res.getClientProperties().set(getClientProperties());
        }
        return res;
    }

    /**
     * Get a copy with the pitch parameter modified.
     * <p>
     * Client properties are copied.
     *
     * @param newPitch       The new pitch
     * @param copyProperties
     * @return
     */
    public NoteEvent setPitch(int newPitch, boolean copyProperties)
    {
        var res = setAll(newPitch, -1, -1, -1, null, copyProperties);
        return res;
    }

    /**
     * Get a copy with the duration parameter modified.
     * <p>
     *
     * @param newDurationInBeats
     * @param copyProperties     If true copy the properties
     * @return
     */
    public NoteEvent setDuration(float newDurationInBeats, boolean copyProperties)
    {
        var res = setAll(-1, newDurationInBeats, -1, -1, null, copyProperties);
        return res;
    }

    /**
     * Get a copy with the velocity parameter modified.
     * <p>
     *
     * @param newVelocity
     * @param copyProperties If true copy the properties
     * @return
     */
    public NoteEvent setVelocity(int newVelocity, boolean copyProperties)
    {
        var res = setAll(-1, -1, newVelocity, -1, null, copyProperties);
        return res;
    }

    /**
     * Get a copy with the position parameter modified.
     * <p>
     *
     * @param newPositionInBeats
     * @param copyProperties     If true copy the properties
     * @return
     */
    public NoteEvent setPosition(float newPositionInBeats, boolean copyProperties)
    {
        var res = setAll(-1, -1, -1, newPositionInBeats, null, copyProperties);
        return res;
    }

    /**
     * Get a copy with the alteration parameter modified.
     * <p>
     *
     * @param newAccidental
     * @param copyProperties If true copy the properties
     * @return
     */
    public NoteEvent setAccidental(Accidental newAccidental, boolean copyProperties)
    {
        var res = setAll(-1, -1, -1, -1, newAccidental, copyProperties);
        return res;
    }

    /**
     * Get the client properties.
     *
     * @return
     */
    public ObservableProperties<Object> getClientProperties()
    {
        return clientProperties;
    }

    /**
     * Convert a note into 2 MidiEvents (NoteON and NoteOFF).
     *
     * @param channel
     * @return
     */
    public List<MidiEvent> toMidiEvents(int channel)
    {
        List<MidiEvent> events = new ArrayList<>();
        try
        {
            ShortMessage smOn = new ShortMessage(ShortMessage.NOTE_ON, channel, getPitch(), getVelocity());
            long tickOn = Math.round(position * MidiConst.PPQ_RESOLUTION);
            ShortMessage smOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, getPitch(), 0);
            long tickOff = Math.round((position + getDurationInBeats()) * MidiConst.PPQ_RESOLUTION);
            events.add(new MidiEvent(smOn, tickOn));
            events.add(new MidiEvent(smOff, tickOff));
        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        return events;
    }

    public boolean isBefore(NoteEvent mne)
    {
        return position < mne.position;
    }

    public float getPositionInBeats()
    {
        return position;
    }

    public FloatRange getBeatRange()
    {
        return new FloatRange(position, position + getDurationInBeats());
    }


    /**
     * Test if this note is near the specified position.
     * <p>
     * A "near" position is in the interval [posInBeats-nearWindow;posInBeats+nearWindow[.
     *
     * @param posInBeats
     * @param nearWindow Must be &gt;= 0
     * @return
     */
    public boolean isNear(float posInBeats, float nearWindow)
    {
        Preconditions.checkArgument(nearWindow >= 0);

        boolean res;
        if (nearWindow == 0)
        {
            res = Float.floatToIntBits(getPositionInBeats()) == Float.floatToIntBits(posInBeats);
        } else if (getPositionInBeats() < (posInBeats - nearWindow))
        {
            res = false;
        } else if (getPositionInBeats() >= (posInBeats + nearWindow))
        {
            res = false;
        } else
        {
            res = true;
        }
        return res;
    }


    /**
     * Check for equality as a Note tolerating slight differences in position and duration.
     * <p>
     * If the positions are equals +/- beatWindow, positions are considered equal. If the durations are equals +/- 2*beatWindow, durations are considered equal.
     * <p>
     * ClientProperties are ignored.
     *
     * @param ne
     * @param nearWindow Must be &gt;= 0
     * @return
     */
    public boolean equalsAsNoteNearPosition(NoteEvent ne, float nearWindow)
    {
        Preconditions.checkNotNull(ne);
        Preconditions.checkNotNull(nearWindow>=0, "nearWindow=%s", nearWindow);
        
        if (ne.getPitch() != getPitch())
        {
            return false;
        }
        if (ne.getVelocity() != getVelocity())
        {
            return false;
        }
        if (!ne.isNear(position, nearWindow))
        {
            return false;
        }
        if (ne.getDurationInBeats() < (getDurationInBeats() - 2 * nearWindow) || ne.getDurationInBeats() > (getDurationInBeats() + 2 * nearWindow))
        {
            return false;
        }
        return true;
    }

    /**
     * Compare this NoteEvent to a Note or NoteEvent.
     * <p>
     *
     * @param n
     * @return 0 only if this == n.
     */
    @Override
    public int compareTo​(Note n)
    {
        if (n == this)
        {
            return 0;
        }
        int res;
        if (n instanceof NoteEvent ne)
        {
            res = Float.compare(position, ne.position);
            if (res == 0)
            {
                res = super.compareTo(n);
            }
        } else
        {
            res = super.compareTo(n);
        }
        if (res == 0)
        {
            res = Integer.compare(System.identityHashCode(this), System.identityHashCode(n));
        }
        return res;
    }

    /**
     * Compare using only position.
     * <p>
     *
     * @param n
     * @return
     */
    public int compareToAsPosition​(NoteEvent n)
    {
        int res = Float.compare(position, n.position);
        return res;
    }

    /**
     * Get a new Note instance built from this object's pitch, duration, velocity and accidental.
     *
     * @return
     */
    public Note toNote()
    {
        return new Note(getPitch(), getDurationInBeats(), getVelocity(), getAccidental());
    }

    /**
     * Get the "as Note" map key for this instance.
     *
     * @return
     * @see AsNoteKey
     */
    public AsNoteKey getAsNoteKey()
    {
        return new AsNoteKey(this);
    }

    /**
     * Return false unless o is the same object.
     * <p>
     * Use equalsAsNoteNearPosition(ne, 0) for equality based on the NoteEvent musical attributes.
     *
     * @param o
     * @return
     * @see #equalsAsNoteNearPosition(org.jjazz.phrase.api.NoteEvent, float)
     */
    @Override
    public boolean equals(Object o)
    {
        return this == o;
    }

    /**
     * Return the identify hash code.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        return System.identityHashCode(this);
    }


    /**
     * Clone this instance, including the client properties.
     *
     * @return
     */
    @Override
    public NoteEvent clone()
    {
        NoteEvent ne = setAll(-1, -1, -1, -1, null, true);
        return ne;
    }

//    @Override
//    public String toString()
//    {
//        return "[" + super.toString() + ",p=" + String.format("%.3f", position) + ",d=" + String.format("%.3f", getDurationInBeats()) + "]";
//    }
    @Override
    public String toString()
    {
        final String defaultFormat = "[%1$s, p=%2$.3f, d=%3$.3f, v=%4$d, id=%5$d]";
        String f = System.getProperty(SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, defaultFormat);
        return String.format(f, toPianoOctaveString(), position, getDurationInBeats(), getVelocity(), System.identityHashCode(this));
    }

    public void addClientPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removeClientPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    /**
     * Save the specified NoteEvent as a string.
     * <p>
     * NOTE: client properties are NOT saved.
     *
     * @param ne
     * @return
     * @see loadAsString(String)
     */
    static public String saveAsString(NoteEvent ne)
    {
        return Note.saveAsString(ne, true) + ":" + ne.position;
    }

    /**
     * Create a NoteEvent from the specified string.
     * <p>
     * Example "60,FLAT,102,2.5:1.25" means pitch=60, AccidentalDisplay=FLAT, velocity=102, duration=2.5 beats, and position=1.25 beats
     *
     * @param s
     * @return
     * @throws ParseException If s is not a valid string.
     * @see saveAsString(NoteEvent)
     */
    static public NoteEvent loadAsString(String s) throws ParseException
    {
        NoteEvent ne = null;
        String[] strs = s.split(":");
        if (strs.length == 2)
        {
            try
            {
                Note n = Note.loadAsString(strs[0]);
                float pos = Float.parseFloat(strs[1]);
                ne = new NoteEvent(n, pos);
            } catch (IllegalArgumentException | ParseException ex)   // Will catch NumberFormatException too
            {
                LOGGER.log(Level.WARNING, "loadAsString() Catched ex={0}", ex.getMessage());
            }
        }
        if (ne == null)
        {
            throw new ParseException("NoteEvent.loadAsString() Invalid NoteEvent string s=" + s, 0);
        }
        return ne;
    }


    /**
     * A NoteEvent wrapper to be used as hash/map key when 2 different NoteEvent instances need to be considered as equal when their note attributes are equal
     * (except client properties).
     */
    static public class AsNoteKey
    {

        private final NoteEvent noteEvent;

        public AsNoteKey(NoteEvent noteEvent)
        {
            this.noteEvent = noteEvent;
        }

        public NoteEvent getNoteEvent()
        {
            return noteEvent;
        }


        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 67 * hash + noteEvent.getPitch();
            hash = 67 * hash + Float.floatToIntBits(noteEvent.getDurationInBeats());
            hash = 67 * hash + noteEvent.getVelocity();
            hash = 67 * hash + Float.floatToIntBits(noteEvent.getPositionInBeats());
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            boolean res = false;
            if (obj instanceof AsNoteKey nk)
            {
                res = Float.floatToIntBits(noteEvent.getPositionInBeats()) == Float.floatToIntBits(nk.noteEvent.getPositionInBeats())
                        && noteEvent.getPitch() == nk.noteEvent.getPitch()
                        && noteEvent.getVelocity() == nk.noteEvent.getVelocity()
                        && Float.floatToIntBits(noteEvent.getDurationInBeats()) == Float.floatToIntBits(nk.noteEvent.getDurationInBeats());
            }
            return res;
        }

        @Override
        public String toString()
        {
            return "ANK#" + noteEvent.toString();
        }

    }
}

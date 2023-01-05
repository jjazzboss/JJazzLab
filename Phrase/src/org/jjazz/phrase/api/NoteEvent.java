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
package org.jjazz.phrase.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import org.jjazz.harmony.api.Note;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.util.api.FloatRange;
import org.openide.util.Exceptions;

/**
 * A Note with a position and optional client properties.
 * <p>
 * This is an immutable class except for the client properties. Two different NoteEvent instances can not be equal. If you need
 * NoteEvent map keys to be considered equal when they share the same attributes, use the AsNoteKey class.
 */
public class NoteEvent extends Note implements Cloneable, Comparable<Note>
{

    private float position;
    protected Map<String, Object> clientProperties;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(NoteEvent.class.getSimpleName());

    /**
     * Create a new NoteEvent.
     *
     * @param pitch
     * @param duration
     * @param velocity
     * @param posInBeats
     */
    public NoteEvent(int pitch, float duration, int velocity, float posInBeats)
    {
        super(pitch, duration, velocity);
        if (posInBeats < 0)
        {
            throw new IllegalArgumentException("posInBeats=" + posInBeats);   //NOI18N
        }
        position = posInBeats;
    }

    /**
     * Create a new NoteEvent from a Note at specified position.
     *
     * @param n
     * @param posInBeats
     */
    public NoteEvent(Note n, float posInBeats)
    {
        this(n.getPitch(), n.getDurationInBeats(), n.getPitch(), posInBeats);
    }

    /**
     * Create a new NoteEvent from another NoteEvent.
     * <p>
     * Client properties are also copied.
     *
     * @param ne
     * @param pitch      if &lt; 0 reuse ne's pitch, otherwise use this value
     * @param duration   if &lt; 0 reuse ne's duration, otherwise use this value
     * @param velocity   if &lt; 0 reuse ne's velocity, otherwise use this value
     * @param posInBeats if &lt; 0 reuse ne's position, otherwise use this value
     */
    public NoteEvent(NoteEvent ne, int pitch, float duration, int velocity, float posInBeats)
    {
        this(pitch < 0 ? ne.getPitch() : pitch, duration < 0 ? ne.getDurationInBeats() : duration, velocity < 0 ? ne.getVelocity() : velocity, posInBeats < 0 ? ne.getPositionInBeats() : posInBeats);
        setClientProperties(ne);
    }

    /**
     * Get a copy with one parameter modified.
     * <p>
     * Client properties are also copied.
     *
     * @param pitch
     * @return 
     */
    public NoteEvent getCopyPitch(int pitch)
    {
        NoteEvent res = new NoteEvent(pitch, getDurationInBeats(), getVelocity(), getPositionInBeats());
        res.setClientProperties(this);
        return res;
    }

    /**
     * Get a copy with one parameter modified.
     * <p>
     * Client properties are also copied.
     *
     * @param durationInBeats
     * @return
     */
    public NoteEvent getCopyDur(float durationInBeats)
    {
        NoteEvent res = new NoteEvent(getPitch(), durationInBeats, getVelocity(), getPositionInBeats());
        res.setClientProperties(this);
        return res;
    }

    /**
     * Get a copy with one parameter modified.
     * <p>
     * Client properties are also copied.
     *
     * @param velocity
     * @return
     */
    public NoteEvent getCopyVel(int velocity)
    {
        NoteEvent res = new NoteEvent(getPitch(), getDurationInBeats(), velocity, getPositionInBeats());
        res.setClientProperties(this);
        return res;
    }

    /**
     * Get a copy with one parameter modified.
     * <p>
     * Client properties are also copied.
     *
     * @param posInBeats
     * @return
     */
    public NoteEvent getCopyPos(float posInBeats)
    {
        NoteEvent res = new NoteEvent(getPitch(), getDurationInBeats(), getVelocity(), posInBeats);
        res.setClientProperties(this);
        return res;
    }

    /**
     * Get a copy with the specified parameters modified.
     * <p>
     * Client properties are also copied.
     *
     * @param durationInBeats
     * @param posInBeats
     * @return
     */
    public NoteEvent getCopyDurPos(float durationInBeats, float posInBeats)
    {
        NoteEvent res = new NoteEvent(getPitch(), durationInBeats, getVelocity(), posInBeats);
        res.setClientProperties(this);
        return res;
    }

    /**
     * Get a copy with the specified parameters modified.
     * <p>
     * Client properties are also copied.
     *
     * @param pitch
     * @param velocity
     * @return
     */
    public NoteEvent getCopyPitchVel(int pitch, int velocity)
    {
        NoteEvent res = new NoteEvent(pitch, getDurationInBeats(), velocity, getPositionInBeats());
        res.setClientProperties(this);
        return res;
    }

    /**
     * Get a copy with the specified parameters modified.
     * <p>
     * Client properties are also copied.
     *
     * @param pitch
     * @param posInBeats
     * @return
     */
    public NoteEvent getCopyPitchPos(int pitch, float posInBeats)
    {
        NoteEvent res = new NoteEvent(pitch, getDurationInBeats(), getVelocity(), posInBeats);
        res.setClientProperties(this);
        return res;
    }

    /**
     * Put a client property.
     * <p>
     * Fire a propertyName change event.
     *
     * @param propertyName
     * @param value        If null, the property is removed.
     */
    public void putClientProperty(String propertyName, Object value)
    {
        if (value == null)
        {
            if (clientProperties != null)
            {
                Object old = clientProperties.remove(propertyName);
                if (old != null)
                {
                    pcs.firePropertyChange(propertyName, old, null);
                }
            }
        } else
        {
            if (clientProperties == null)
            {
                clientProperties = new HashMap<>();
            }
            Object old = clientProperties.get(propertyName);
            clientProperties.put(propertyName, value);
            pcs.firePropertyChange(propertyName, old, value);
        }
    }

    /**
     * Get a client property.
     *
     * @param propertyName
     * @return Can be null.
     */
    public Object getClientProperty(String propertyName)
    {
        return clientProperties != null ? clientProperties.get(propertyName) : null;
    }

    /**
     * Replace the current properties by the properties from ne.
     * <p>
     * Fire 0, 1 or more client property change events as required.
     *
     * @param ne
     */
    public final void setClientProperties(NoteEvent ne)
    {
        if (clientProperties == null && ne.clientProperties == null)
        {
            // Easy
            return;
        }

        // Could be simplier but we want to minimize the number of property change events
        Set<String> processedProps = new HashSet<>();
        if (clientProperties != null)
        {
            for (var prop : clientProperties.keySet())
            {
                Object v = ne.clientProperties == null ? null : ne.clientProperties.get(prop);
                putClientProperty(prop, v);
                processedProps.add(prop);
            }
        }
        if (ne.clientProperties != null)
        {
            for (var prop : ne.clientProperties.keySet())
            {
                if (!processedProps.contains(prop))
                {
                    putClientProperty(prop, ne.clientProperties.get(prop));
                }
            }
        }
    }

    /**
     * Remove all client properties.
     */
    public void removeClientProperties()
    {
        for (var prop : clientProperties.keySet().toArray(new String[0]))
        {
            putClientProperty(prop, null);  // this will fire an event
        }
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
     * @param nearWindow
     * @return
     */
    public boolean isNear(float posInBeats, float nearWindow)
    {
        Preconditions.checkArgument(nearWindow >= 0);

        boolean res;
        if (nearWindow == 0)
        {
            res = Float.floatToIntBits(getPositionInBeats()) == Float.floatToIntBits(posInBeats);
        } else if (getPositionInBeats() < posInBeats - nearWindow)
        {
            res = false;
        } else if (getPositionInBeats() >= posInBeats + nearWindow)
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
     * If the positions are equals +/- beatWindow, positions are considered equal. If the durations are equals +/- 2*beatWindow,
     * durations are considered equal.
     * <p>
     * ClientProperties are ignored.
     *
     * @param ne
     * @param nearWindow Must be &gt; 0
     * @return
     */
    public boolean equalsAsNoteNearPosition(NoteEvent ne, float nearWindow)
    {
        Preconditions.checkNotNull(ne);
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
        if (ne.getDurationInBeats() < getDurationInBeats() - 2 * nearWindow || ne.getDurationInBeats() > getDurationInBeats() + 2 * nearWindow)
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
     * Get a new Note instance built from this object's pitch, duration, velocity and alteration.
     *
     * @return
     */
    public Note toNote()
    {
        return new Note(getPitch(), getDurationInBeats(), getVelocity(), getAlterationDisplay());
    }

    /**
     * Get the "as Note" key for this instance.
     *
     * @return
     */
    public AsNoteKey getAsNoteKey()
    {
        return new AsNoteKey(this);
    }

    /**
     * Return false unless o is the same object.
     *
     * @param o
     * @return
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
     * Also clone the client properties.
     *
     * @return
     */
    @Override
    public NoteEvent clone()
    {
        NoteEvent ne = new NoteEvent(this, -1, -1, -1, -1);
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
        // return String.format("[%s, p=%.3f, d=%.3f, v=%d]", toPianoOctaveString(), position, getDurationInBeats(), getVelocity());
        return String.format("[%s, p=%f, d=%f, v=%d, id=%d]", toPianoOctaveString(), position, getDurationInBeats(), getVelocity(), System.identityHashCode(this));
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
     * Example "60,FLAT,102,2.5:1.25" means pitch=60, AlterationDisplay=FLAT, velocity=102, duration=2.5 beats, and position=1.25
     * beats
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
                ne = new NoteEvent(n.getPitch(), n.getDurationInBeats(), n.getVelocity(), pos);
            } catch (IllegalArgumentException | ParseException ex)   // Will catch NumberFormatException too
            {
                LOGGER.warning("loadAsString() Catched ex=" + ex.getMessage());
            }
        }

        if (ne == null)
        {
            throw new ParseException("NoteEvent.loadAsString() Invalid NoteEvent string s=" + s, 0);
        }
        return ne;
    }


    /**
     * A NoteEvent wrapper to be used as hash/map key when 2 different NoteEvent instances need to be considered as equal when
     * their attributes are equal (except client properties).
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

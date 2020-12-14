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
package org.jjazz.rhythmmusicgeneration;

import java.util.HashMap;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import org.jjazz.harmony.Note;
import org.jjazz.midi.MidiConst;
import org.jjazz.util.FloatRange;
import org.openide.util.Exceptions;

/**
 * A Note with a position and optional client properties.
 * <p>
 * This is an immutable class except for the client properties.
 */
public class NoteEvent extends Note implements Cloneable
{

    private float position;
    protected HashMap<String, Object> clientProperties;

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
     * Create a MidiNoteEvent from an existing one but using the specified pitch.
     * <p>
     * Client properties are cloned from ne.
     *
     * @param ne
     * @param pitch
     */
    public NoteEvent(NoteEvent ne, int pitch)
    {
        this(pitch, ne.getDurationInBeats(), ne.getVelocity(), ne.getPositionInBeats());
        setClientProperties(ne);
    }

    /**
     * Create a MidiNoteEvent from an existing one but using the specified duration.
     * <p>
     * Client properties are cloned from ne.
     *
     * @param ne
     * @param durationInBeats
     */
    public NoteEvent(NoteEvent ne, float durationInBeats)
    {
        this(ne.getPitch(), durationInBeats, ne.getVelocity(), ne.getPositionInBeats());
        setClientProperties(ne);
    }

    /**
     * Create a MidiNoteEvent from an existing one but using the specified duration and position.
     * <p>
     * Client properties are cloned from ne.
     *
     * @param ne
     * @param durationInBeats
     * @param posInBeats
     */
    public NoteEvent(NoteEvent ne, float durationInBeats, float posInBeats)
    {
        this(ne.getPitch(), durationInBeats, ne.getVelocity(), posInBeats);
        setClientProperties(ne);
    }

    /**
     * Create a MidiNoteEvent from an existing one but using the specified pitch, duration and position.
     * <p>
     * Client properties are cloned from ne.
     *
     * @param ne
     * @param pitch
     * @param durationInBeats
     * @param posInBeats
     */
    public NoteEvent(NoteEvent ne, int pitch, float durationInBeats, float posInBeats)
    {
        this(pitch, durationInBeats, ne.getVelocity(), posInBeats);
        setClientProperties(ne);
    }

    /**
     * Create a MidiNoteEvent from an existing one but using the specified pitch, duration, velocity.
     * <p>
     * Client properties are cloned from ne.
     *
     * @param ne
     * @param pitch
     * @param durationInBeats
     * @param velocity
     */
    public NoteEvent(NoteEvent ne, int pitch, float durationInBeats, int velocity)
    {
        this(pitch, durationInBeats, velocity, ne.getPositionInBeats());
        setClientProperties(ne);
    }

    /**
     * Reset all current properties and copy all properties from ne.
     *
     * @param ne
     */
    public final void setClientProperties(NoteEvent ne)
    {
        clientProperties = (ne.clientProperties == null) ? null : new HashMap<>(ne.clientProperties);
    }

    /**
     * Put a client property.
     *
     * @param propertyName
     * @param value If null, the property is removed.
     */
    public void putClientProperty(String propertyName, Object value)
    {
        if (value == null)
        {
            if (clientProperties != null)
            {
                clientProperties.remove(propertyName);
            }
        } else
        {
            if (clientProperties == null)
            {
                clientProperties = new HashMap<>();
            }
            clientProperties.put(propertyName, value);
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
     * Convert a note into 2 MidiEvents (NoteON and NoteOFF).
     *
     * @param channel
     * @return
     */
    public MidiEvent[] toMidiEvents(int channel)
    {
        MidiEvent[] events = new MidiEvent[2];
        try
        {
            ShortMessage smOn = new ShortMessage(ShortMessage.NOTE_ON, channel, getPitch(), getVelocity());
            long tickOn = Math.round(position * MidiConst.PPQ_RESOLUTION);
            ShortMessage smOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, getPitch(), 0);
            long tickOff = Math.round((position + getDurationInBeats()) * MidiConst.PPQ_RESOLUTION);
            events[0] = new MidiEvent(smOn, tickOn);
            events[1] = new MidiEvent(smOff, tickOff);
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
     * Client properties are ignored.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == null || !(o instanceof NoteEvent))
        {
            return false;
        }
        NoteEvent ne = (NoteEvent) o;
        return ne.position == position && super.equals(o);
    }

    /**
     * Client properties are ignored.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = super.hashCode();
        hash = 29 * hash + Float.floatToIntBits(this.position);
        return hash;
    }

    /**
     * Also clone the client properties.
     *
     * @return
     */
    @Override
    public NoteEvent clone()
    {
        NoteEvent ne = new NoteEvent(this, getPitch());
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
        return String.format("[%s, p=%.3f, d=%.3f, v=%d]", toAbsoluteNoteString(), position, getDurationInBeats(), getVelocity());
    }

}

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
package org.jjazz.rhythmmusicgeneration.api;

import java.util.HashMap;
import java.util.logging.Logger;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;

/**
 * A special NoteEvent class to describe a pitchbend from fromPitch to destNote's pitch.
 * <p>
 */
public class PitchBendEvent extends NoteEvent
{

    private final int fromPitch;
    private static final Logger LOGGER = Logger.getLogger(PitchBendEvent.class.getSimpleName());

    /**
     * Describe a pitchbend from fromPitch to destNote's pitch.
     *
     * @param destNote
     * @param fromPitch
     */
    public PitchBendEvent(NoteEvent destNote, int fromPitch)
    {
        super(destNote.getPitch(), destNote.getDurationInBeats(), destNote.getVelocity(), destNote.getPositionInBeats());
        this.fromPitch = fromPitch;
    }

    public int getFromPitch()
    {
        return fromPitch;
    }

    /**
     * Clone also the clientProperties.
     *
     * @return
     */
    @Override
    public PitchBendEvent clone()
    {
        PitchBendEvent pbe = new PitchBendEvent(this, fromPitch);
        clientProperties = (pbe.clientProperties == null) ? null : new HashMap<>(pbe.clientProperties);
        return pbe;
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
        if (o == null || !(o instanceof PitchBendEvent))
        {
            return false;
        }
        PitchBendEvent pbe = (PitchBendEvent) o;
        return pbe.fromPitch == fromPitch && super.equals(o);
    }

    @Override
    public int hashCode()
    {
        int hash = super.hashCode();
        hash = 97 * hash + this.fromPitch;
        return hash;
    }

    /**
     * Generate the pitch bend Midi events.
     *
     * @param channel
     * @return
     */
    @Override
    public MidiEvent[] toMidiEvents(int channel)
    {
        LOGGER.warning("toMidiEvents() NOT VALIDED YET !");   //NOI18N
        int pitchShift = getPitch() - fromPitch;

        ShortMessage[] mes = MidiUtilities.getPitchBendMessages(channel, pitchShift);
        MidiEvent[] events = new MidiEvent[2 * mes.length];
        long tickStart = (long) (getPositionInBeats() * MidiConst.PPQ_RESOLUTION);
        int i = 0;
        for (ShortMessage me : mes)
        {
            events[i] = new MidiEvent(me, tickStart);
            i++;
        }

        mes = MidiUtilities.getPitchBendMessages(channel, 0);
        long tickEnd = (long) ((getPositionInBeats() + getDurationInBeats()) * MidiConst.PPQ_RESOLUTION) - 1;
        for (ShortMessage me : mes)
        {
            events[i] = new MidiEvent(me, tickEnd);
            i++;
        }

        return events;
    }

}

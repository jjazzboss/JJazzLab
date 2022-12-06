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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.ScaleManager;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;

/**
 * Convenience Phrase-based static methods.
 */
public class PhraseUtilities
{

    /**
     * Build a Phrase with 12 notes C-D-E-F-G-A-B-C that fit into nbBeats.
     *
     * @param channel
     * @param startPos Position of the 1st note 'C'
     * @param nbBeats
     * @return
     */
    static public Phrase getCscalePhrase(int channel, float startPos, float nbBeats)
    {
        Phrase p = new Phrase(channel);
        float noteDur = nbBeats / 8f;
        float pos = startPos;
        for (Note n : ScaleManager.MAJOR.getNotes())
        {
            NoteEvent ne = new NoteEvent(n.getPitch() + 60, noteDur, n.getVelocity(), pos);
            pos += noteDur;
            p.add(ne);
        }
        // Add octave note at this end
        NoteEvent ne = new NoteEvent(72, noteDur, Note.VELOCITY_STD, pos);
        p.add(ne);
        return p;
    }

    /**
     * Get a phrase with random notes at random positions.
     *
     * @param channel
     * @param nbBars Number of 4/4 bars.
     * @param nbNotes Number of random notes to generate.
     * @return
     */
    static public Phrase getRandomPhrase(int channel, int nbBars, int nbNotes)
    {
        Phrase p = new Phrase(channel);

        for (int i = 0; i < nbNotes; i++)
        {
            int pitch = (int) (40 + Math.round(20 * Math.random()));
            int vel = (int) (50 + Math.round(20 * Math.random()));
            float pos = Math.max(0, Math.round(nbBars * 4 * Math.random()) - 2);
            float dur = Math.random() > 0.5d ? 0.5f : 1f;
            p.add(new NoteEvent(pitch, dur, vel, pos));
        }

        return p;
    }

    /**
     * Get a basic drums phrase.
     *
     * @param startPosInBeats
     * @param nbBars
     * @param ts
     * @param channel The channel of the returned phrase
     * @return
     */
    static public Phrase getBasicDrumPhrase(float startPosInBeats, int nbBars, TimeSignature ts, int channel)
    {
        if (ts == null || !MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("nbBars=" + nbBars + " ts=" + ts + " channel=" + channel);   //NOI18N
        }
        Phrase p = new Phrase(channel);
        float duration = 0.25f;
        for (int bar = 0; bar < nbBars; bar++)
        {
            for (int beat = 0; beat < ts.getNbNaturalBeats(); beat++)
            {
                // 2 Hi Hat per beat
                NoteEvent ne = new NoteEvent(MidiConst.CLOSED_HI_HAT, duration, 80, startPosInBeats);
                p.add(ne);
                ne = new NoteEvent(MidiConst.CLOSED_HI_HAT, duration, 80, startPosInBeats + 0.5f);
                p.add(ne);

                // Bass drums or Snare
                int pitch;
                int velocity = 70;
                switch (beat)
                {
                    case 0:
                        pitch = MidiConst.ACOUSTIC_BASS_DRUM;
                        velocity = 120;
                        break;
                    case 1:
                    case 3:
                    case 5:
                    case 7:
                        pitch = MidiConst.ACOUSTIC_SNARE;
                        break;
                    default:
                        pitch = MidiConst.ACOUSTIC_BASS_DRUM;
                }
                ne = new NoteEvent(pitch, duration, velocity, startPosInBeats);
                p.add(ne);

                // Next beat
                startPosInBeats++;
            }
        }
        return p;
    }


    /**
     * Parse all tracks to build one phrase per used channel.
     * <p>
     * A track can use notes from different channels. Notes from a given channel can be on several tracks.
     *
     * @param tracksPPQ The Midi PPQ resolution (pulses per quarter) used in the tracks.
     * @param tracks
     * @param channels Get phrases only for the specified channels. If empty, get phrases for all channels.
     * @return
     */
    static public List<Phrase> getPhrases(int tracksPPQ, Track[] tracks, Integer... channels)
    {
        Map<Integer, Phrase> mapRes = new HashMap<>();
        var selectedChannels = channels.length > 0 ? Arrays.asList(channels) : Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);

        for (Track track : tracks)
        {
            // Get all the events at the appropriate resolution
            var trackEvents = MidiUtilities.getMidiEvents(track,
                    ShortMessage.class,
                    sm -> sm.getCommand() == ShortMessage.NOTE_OFF || sm.getCommand() == ShortMessage.NOTE_ON,
                    null);
            trackEvents = MidiUtilities.getMidiEventsAtPPQ(trackEvents, tracksPPQ, MidiConst.PPQ_RESOLUTION);


            for (int channel : MidiUtilities.getUsedChannels(track))
            {
                if (selectedChannels.contains(channel))
                {
                    Phrase p = mapRes.get(channel);
                    if (p == null)
                    {
                        p = new Phrase(channel);
                        mapRes.put(channel, p);
                    }
                    p.add(trackEvents, 0, false);
                }
            }
        }

        return new ArrayList<>(mapRes.values());
    }
}

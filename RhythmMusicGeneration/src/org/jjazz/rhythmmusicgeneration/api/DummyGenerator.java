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
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.Family;
import org.jjazz.rhythm.api.*;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.api.IntRange;

/**
 * A dummy generator that generate simple tracks for test purposes.
 */
public class DummyGenerator implements MusicGenerator
{

    private Rhythm rhythm;
    protected static final Logger LOGGER = Logger.getLogger(DummyGenerator.class.getSimpleName());

    public DummyGenerator(Rhythm r)
    {
        if (r == null)
        {
            throw new NullPointerException("r=" + r);   //NOI18N
        }
        rhythm = r;
    }
  

    @Override
    public HashMap<RhythmVoice, Phrase> generateMusic(SongContext context)
    {
        HashMap<RhythmVoice, Phrase> res = new HashMap<>();

        // Loop only on song parts belonging to context
        for (SongPart spt : context.getSongParts())
        {
            Rhythm r = spt.getRhythm();
            if (!r.equals(rhythm))
            {
                // Not for us
                continue;
            }

            TimeSignature ts = r.getTimeSignature();
            IntRange sptRange = context.getSptBarRange(spt); // Context bars can start/end in the middle of a song part
            float sptPosInBeats = context.getSong().getSongStructure().getPositionInNaturalBeats(sptRange.from);

            // Get the ChordSequence corresponding to the song part
            SongContext rContext = new SongContext(context, sptRange);
            ContextChordSequence cSeq = new ContextChordSequence(rContext);
            for (RhythmVoice rv : rhythm.getRhythmVoices())
            {
                // Get or create the resulting phrase for this RhythmVoice
                int destChannel = context.getMidiMix().getChannel(rv);
                Phrase pRes = res.get(rv);
                if (pRes == null)
                {
                    pRes = new Phrase(destChannel);
                    res.put(rv, pRes);
                }
                if (rv.isDrums())
                {
                    LOGGER.fine("generateMusic() generate dummy drums track for RhythmVoice: " + rv.getName());   //NOI18N
                    Phrase p = getBasicDrumPhrase(sptPosInBeats, sptRange.size(), ts, destChannel);
                    pRes.add(p);
                } else
                {
                    if (rv.getPreferredInstrument().getSubstitute().getFamily().equals(Family.Bass))
                    {
                        LOGGER.fine("generateMusic() generate dummy bass track for RhythmVoice: " + rv.getName());   //NOI18N
                        Phrase p = getBasicBassPhrase(sptPosInBeats, cSeq, ts, destChannel);
                        pRes.add(p);
                    } else
                    {
                        LOGGER.fine("generateMusic() music generation not supported for this RhythmVoice: " + rv.getName());   //NOI18N
                    }
                }
            }
        }

        return res;
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
                p.addOrdered(ne);
                ne = new NoteEvent(MidiConst.CLOSED_HI_HAT, duration, 80, startPosInBeats + 0.5f);
                p.addOrdered(ne);

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
                p.addOrdered(ne);

                // Next beat
                startPosInBeats++;
            }
        }
        return p;
    }

    /**
     * Get a basic bass phrase.
     *
     * @param startPosInBeats
     * @param cSeq
     * @param ts
     * @param channel The channel of the returned phrase
     * @return
     */
    static public Phrase getBasicBassPhrase(float startPosInBeats, ChordSequence cSeq, TimeSignature ts, int channel)
    {
        if (cSeq == null || ts == null || !MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("cSeq=" + cSeq + " ts=" + ts + " channel=" + channel);   //NOI18N
        }
        Phrase p = new Phrase(channel);
        for (int i = 0; i < cSeq.size(); i++)
        {
            CLI_ChordSymbol cli = cSeq.get(i);
            int bassPitch = 3 * 12 + cli.getData().getBassNote().getRelativePitch(); // stay on the 3rd octave            
            float duration = cSeq.getChordDuration(i, ts);
            float posInBeats = cSeq.toPositionInBeats(cli.getPosition(), ts, startPosInBeats);
            NoteEvent ne = new NoteEvent(bassPitch, duration, 80, posInBeats);
            p.addOrdered(ne);
        }
        return p;
    }

// ====================================================================================================
// Private methods
// ====================================================================================================
}

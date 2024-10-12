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
package org.jjazz.rhythmmusicgeneration.api;

import java.util.Collections;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.songcontext.api.SongContext;
import java.util.HashMap;
import java.util.logging.Level;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Chord;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.phrase.api.PhraseSamples;
import org.jjazz.rhythm.api.*;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.IntRange;

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
            throw new NullPointerException("r=" + r);
        }
        rhythm = r;
    }

    @Override
    public HashMap<RhythmVoice, Phrase> generateMusic(SongContext sgContext, RhythmVoice... rvs) throws MusicGenerationException
    {
        HashMap<RhythmVoice, Phrase> res = new HashMap<>();

        // Loop only on song parts belonging to context
        for (SongPart spt : sgContext.getSongParts())
        {
            Rhythm r = spt.getRhythm();
            if (!r.equals(rhythm))
            {
                // Not for us
                continue;
            }

            TimeSignature ts = r.getTimeSignature();
            IntRange sptRange = sgContext.getSptBarRange(spt); // Context bars can start/end in the middle of a song part
            float sptPosInBeats = sgContext.getSong().getSongStructure().toPositionInNaturalBeats(sptRange.from);

            // Get the SimpleChordSequence corresponding to the song part
            SongChordSequence scSeq = new SongChordSequence(sgContext.getSong(), sptRange);     // throw UserErrorGenerationException
            SimpleChordSequence cSeq = new SimpleChordSequence(scSeq, ts);


            for (RhythmVoice rv : rhythm.getRhythmVoices())
            {
                // Get or create the resulting phrase for this RhythmVoice
                int destChannel = sgContext.getMidiMix().getChannel(rv);
                Phrase pRes = res.get(rv);
                if (pRes == null)
                {
                    pRes = new Phrase(destChannel, rv.isDrums());
                    res.put(rv, pRes);
                }
                if (rv.isDrums())
                {
                    LOGGER.log(Level.FINE, "generateMusic() generate dummy drums track for RhythmVoice: {0}", rv.getName());
                    Phrase p = PhraseSamples.getBasicDrumPhrase(sptPosInBeats, sptRange.size(), ts, destChannel);
                    pRes.add(p);
                } else
                {
                    if (rv.getPreferredInstrument().getSubstitute().getFamily().equals(InstrumentFamily.Bass))
                    {
                        LOGGER.log(Level.FINE, "generateMusic() generate dummy bass track for RhythmVoice: {0}", rv.getName());
                        Phrase p = getBasicBassPhrase(sptPosInBeats, cSeq, destChannel);
                        pRes.add(p);
                    } else
                    {
                        LOGGER.log(Level.FINE, "generateMusic() generate dummy melodic track for RhythmVoice: {0}", rv.getName());
                        Phrase p = getBasicMelodicPhrase(sptPosInBeats, cSeq, destChannel);
                        pRes.add(p);
                    }
                }
            }
        }

        return res;
    }

    /**
     * Get a basic bass phrase.
     *
     * @param startPosInBeats
     * @param cSeq
     * @param channel         The channel of the returned phrase
     * @return
     */
    static public Phrase getBasicBassPhrase(float startPosInBeats, SimpleChordSequence cSeq, int channel)
    {
        if (cSeq == null || !MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("cSeq=" + cSeq + " channel=" + channel);
        }
        Phrase p = new Phrase(channel, false);
        for (var cliCs : cSeq)
        {
            int bassPitch = 3 * 12 + cliCs.getData().getBassNote().getRelativePitch(); // stay on the 3rd octave            
            float duration = cSeq.getChordDuration(cliCs);
            float posInBeats = cSeq.toPositionInBeats(cliCs.getPosition(), startPosInBeats);
            NoteEvent ne = new NoteEvent(bassPitch, duration, 80, posInBeats);
            p.add(ne);
        }
        return p;
    }

    /**
     * Get a basic random phrase for a melodic instrument (use chord notes).
     *
     * @param startPosInBeats
     * @param cSeq
     * @param channel         The channel of the returned phrase
     * @return
     */
    static public Phrase getBasicMelodicPhrase(float startPosInBeats, SimpleChordSequence cSeq, int channel)
    {
        if (cSeq == null || !MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("cSeq=" + cSeq + " channel=" + channel);
        }
        Phrase p = new Phrase(channel, false);
        for (var cliCs : cSeq)
        {
            float chordDuration = cSeq.getChordDuration(cliCs);
            Chord c = cliCs.getData().getChord();
            float noteDuration = (chordDuration / c.size()) - 0.001f;
            float posInBeats = cSeq.toPositionInBeats(cliCs.getPosition(), startPosInBeats);
            
            var notes = c.getNotes();
            Collections.shuffle(notes);
            for (var n : notes)
            {
                int pitch = 5 * 12 + n.getRelativePitch();
                NoteEvent ne = new NoteEvent(pitch, noteDuration, 65, posInBeats);
                p.add(ne);
                posInBeats += noteDuration;
            }
        }
        return p;
    }

// ====================================================================================================
// Private methods
// ====================================================================================================
}

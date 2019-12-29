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
import org.jjazz.rhythmmusicgeneration.spi.MidiMusicGenerator;
import java.util.logging.Logger;
import javax.sound.midi.Track;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.*;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.Range;

/**
 * A dummy generator that generate simple tracks for test purposes.
 */
public class DummyGenerator implements MidiMusicGenerator
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
    public Rhythm getRhythm()
    {
        return rhythm;
    }

    @Override
    public void generateMusic(MusicGenerationContext context, HashMap<RhythmVoice, Track> mapRvTracks) throws MusicGenerationException
    {
        // Loop only on song parts belonging to context
        for (SongPart spt : context.getSongParts())
        {
            Rhythm r = spt.getRhythm();
            TimeSignature ts = r.getTimeSignature();
            Range sptRange = context.getSptRange(spt); // Context bars can start/end in the middle of a song part
            long tick = context.getSptStartTick(spt);
            if (r.equals(rhythm))
            {
                // This is our rhythm         
                // Get the ChordSequence corresponding to the song part
                MusicGenerationContext rContext = new MusicGenerationContext(context, sptRange);
                ContextChordSequence cSeq = new ContextChordSequence(rContext);
                for (RhythmVoice rv : rhythm.getRhythmVoices())
                {
                    // Fill the track for each supported RhythmVoice
                    Track track = mapRvTracks.get(rv);
                    int channel = context.getMidiMix().getChannel(rv);
                    switch (rv.getType())
                    {
                        case Bass:
                            LOGGER.fine("generateMusic() generate dummy bass track for RhythmVoice: " + rv.getName() + " size=" + track.size());
                            Utilities.addBassNoteEvents(track, channel, tick, cSeq, ts);
                            break;
                        case Drums:
                            LOGGER.fine("generateMusic() generate dummy drums track for RhythmVoice: " + rv.getName() + " size=" + track.size());
                            Utilities.addDrumsNoteEvents(track, channel, tick, sptRange.size(), ts);
                            break;
                        default:
                            LOGGER.fine("generateMusic() music generation not supported for this RhythmVoice: " + rv.getName());
                    }
                }
            }
        }
    }

    // ====================================================================================================
    // Private methods
    // ====================================================================================================
}

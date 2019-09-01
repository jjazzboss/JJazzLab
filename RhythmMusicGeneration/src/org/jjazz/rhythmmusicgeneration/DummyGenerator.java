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
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerationContext;
import java.util.logging.Logger;
import javax.sound.midi.Track;
import org.jjazz.midimix.MidiMix;
import org.jjazz.rhythm.api.*;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerationException;
import org.jjazz.song.api.Song;

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
        Song song = context.getSong();
        MidiMix mix = context.getMidiMix();

        for (RhythmVoice rv : rhythm.getRhythmVoices())
        {
            Rhythm r = rv.getContainer();
            Track track = mapRvTracks.get(rv);
            int channel = mix.getChannel(rv);
            if (rv.getType() == RvType.Bass)
            {
                Utilities.addBassNoteEvents(track, channel, song, r);
                LOGGER.fine("generateMusic() generate dummy bass track for RhythmVoice: " + rv.getName() + " size=" + track.size());
            } else if (rv.getType() == RvType.Drums)
            {
                Utilities.addDrumsNoteEvents(track, channel, song, r);
                LOGGER.fine("generateMusic() generate dummy drums track for RhythmVoice: " + rv.getName() + " size=" + track.size());
            } else
            {
                LOGGER.info("generateMusic() music generation not supported for this RhythmVoice: " + rv.getName());
            }
        }
    }
}

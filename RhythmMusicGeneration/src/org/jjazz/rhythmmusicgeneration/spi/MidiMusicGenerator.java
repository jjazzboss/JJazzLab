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
package org.jjazz.rhythmmusicgeneration.spi;

import java.util.HashMap;
import javax.sound.midi.Track;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * A music generator producing Midi sequence tracks.
 */
public interface MidiMusicGenerator
{

    /**
     * @return The Rhythm for which we generate music.
     */
    Rhythm getRhythm();

    /**
     * Fill the specified Midi tracks with Midi events to produce a rhythm accompaniment.
     * <p>
     * The service provider must compute Midi music data (notes) for the specified context. Resulting Midi messages must be stored
     * in the tracks provided by the mapRvTracks map, one track per RhythmVoice/Midi channel.
     * <p>
     * The authorized Midi events are note on/off and pitch wheel changes. Midi events timing must be based on a PPQ
     * resolution=MidiConst.PPQ_RESOLUTION. The first note of the first bar (usually on beat 0) should start at tick 0, even if
     * the context specifies a first bar (fromBar) which is greater than 0.
     * <p>
     * The MidiMix from the context is used to retrieve the unique Midi channel associated to each RhythmVoice (see method
     * MidiMix.getChannel(RhythmVoice)). If the context song contains several rhythms, the method must add Midi events ONLY for
     * bars which use this MidiMusicGenerator's rhythm.
     * <p>
     * If context bar range
     * <p>
     * Some features are directly managed by the framework (for example by post-processing the generated Midi tracks), so the
     * method shall NOT implement them:<br>
     * - Instrument selection and settings (Program changes, Midi controller messages such as bank select, volume, reverb,
     * panoramic, etc.) <br>
     * - RP_SYS_Mute rhythm parameter handling (muting a specific track for a specific SongPart)<br>
     * - Handling of the channel's specific velocity shift<br>
     * - Handling of the instrument's specific transposition<br>
     *
     * @param context     The information to be used for music generation
     * @param mapRvTracks The tracks ready to be filled, one track per rhythm voice/channel.
     *
     * @throws MusicGenerationException If generator could not produce the expected music. The framework is responsible for
     *                                  notifying the user of the error message associated to the exception.
     *
     */
    void generateMusic(MusicGenerationContext context, HashMap<RhythmVoice, Track> mapRvTracks) throws MusicGenerationException;
}

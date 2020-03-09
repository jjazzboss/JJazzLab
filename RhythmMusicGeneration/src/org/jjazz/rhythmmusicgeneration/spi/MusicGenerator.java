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

import org.jjazz.rhythmmusicgeneration.MusicGenerationException;
import org.jjazz.rhythmmusicgeneration.MusicGenerationContext;
import java.util.HashMap;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.Phrase;

/**
 * A music generator for a given rhythm.
 */
public interface MusicGenerator
{

    /**
     * Post-processor of Phrases returned by generateMusic().
     */
    public interface PostProcessor
    {

        /**
         * If several PostProcessor instances exist, use the priority to set the order of execution.
         *
         * @return 0 is highest priority, Integer.MAX is lowest priority
         */
        int getPriority();

        /**
         * Apply some post-processing to the specified phrases.
         *
         * @param mapRvPhrase
         * @return True if some changes have been done.
         * @throws MusicGenerationException
         */
        boolean postProcess(HashMap<RhythmVoice, Phrase> mapRvPhrase) throws MusicGenerationException;
    }

    /**
     * @return The Rhythm for which we generate music.
     */
    Rhythm getRhythm();

    /**
     * Generate the note Phrases which correspond to a musical accompaniment for a given rhythm.
     * <p>
     * The service provider must compute notes for the specified context, one Phrase per RhythmVoice/Midi channel.
     * <p>
     * The first note of the first bar (usually on beat 0) must start at position 0, even if the context specifies a first bar
     * (fromBar) which is greater than 0.
     * <p>
     * The MidiMix from <code>context</code> is used to retrieve the unique Midi channel associated to each RhythmVoice (see
     * method MidiMix.getChannel(RhythmVoice)). If the context song contains several rhythms, the method must add notes ONLY for
     * bars which use this MidiMusicGenerator's rhythm.
     * <p>
     * Note that some features are directly managed by the JJazzLab framework :<br>
     * - Instrument selection and settings (Program changes, Midi controller messages such as bank select, volume, reverb,
     * panoramic, etc.) <br>
     * - RP_SYS_Mute rhythm parameter handling (muting a specific track for a specific SongPart)<br>
     * - Handling of the channel's specific velocity shift<br>
     * - Handling of the instrument's specific transposition<br>
     * - Post-processing<br - etc. <p>
     * @p
     *
     *
     *
     *
     *
     * aram context The information to be used for music generation
     * @return One Phrase per rhythm voice/channel.
     *
     * @throws MusicGenerationException If generator could not produce the expected music. The framework is responsible for
     * notifying the user of the error message associated to the exception.
     *
     */
    HashMap<RhythmVoice, Phrase> generateMusic(MusicGenerationContext context) throws MusicGenerationException;
}

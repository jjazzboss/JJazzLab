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
package org.jjazz.rhythmmusicgeneration.spi;

import org.jjazz.songcontext.api.SongContext;
import java.util.Map;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * The music generation capability of a Rhythm.
 */
public interface MusicGenerator
{
    /**
     * Generate the note Phrases which correspond to a musical accompaniment for a given rhythm.
     * <p>
     * The service provider must compute one Phrase per RhythmVoice/Midi channel. The MidiMix from <code>context</code> provides the Midi channel associated to
     * each RhythmVoice via <code>MidiMix.getChannel(RhythmVoice)</code>. Phrases must be provided for all RhythmVoices (though a Phrase can be empty),
     * including for the possible RhythmVoiceDelegates used by the Rhythm.
     * <p>
     * If the context song contains several rhythms, the method must add notes ONLY for bars which use this MusicGenerator's rhythm. For example, if context
     * range is bars 3-4 with rhythm1 on bar3 and rhythm2 on bar4, then the rhythm1 generator must add notes for bar 3 only.
     * <p>
     * IMPORTANT: the following features are directly managed by the JJazzLab framework, notably by postprocessing the output of generateMusic():<br>
     * - Midi Instrument selection and settings (Program changes, Midi controller messages such as bank select, volume, reverb, panoramic) <br>
     * - RP_SYS_Mute rhythm parameter handling (muting a specific track for a specific SongPart)<br>
     * - RP_SYS_DrumsMix rhythm parameter handling (adjusting some drums track instruments velocity)<br>
     * - RP_SYS_CustomPhrase rhythm parameter handling (substitute a user-supplied phrase for a rhythm track)<br>
     * - RP_STD_Fill rhythm parameter handling for the "fade_out" value (MusicGenerator must handle the other values)<br>
     * - Handling of the channel's specific velocity shift<br>
     * - Handling of the instrument's specific transposition<br>
     * - Drums channel rerouting<br>
     * - NC chord symbols (produce no sound)<br>
     *
     * @param context      The information to be used for music generation
     * @param rhythmVoices Generate music only for these RhythmVoices, or for all RhythmVoices if nothing specified
     * @return One Phrase per rhythm voice/channel.
     *
     * @throws MusicGenerationException If generator could not produce the expected music for some reason.
     *
     */

    Map<RhythmVoice, Phrase> generateMusic(SongContext context, RhythmVoice... rhythmVoices) throws MusicGenerationException;       
}

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

import org.jjazz.songcontext.api.SongContext;
import java.util.Map;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * Define the music generation capability of a Rhythm.
 */
public interface MusicGenerator
{

    /**
     * Generate the note Phrases which correspond to a musical accompaniment for a given rhythm.
     * <p>
     * The service provider must compute notes for the specified context, one Phrase per RhythmVoice/Midi channel. Phrases must be
     * generated for the standard RhythmVoices and, if used by some rhythm, the RhythmVoiceDelegates.<p>
     * Notes must be generated for the context bars which use this generator's rhythm. For example, if context range is bars 3-4
     * with rhythm1 on bar3 and rhythm2 on bar4, then the rhythm1 generator must add notes for bar 3 only.
     * <p>
     * The MidiMix from <code>context</code> is used to retrieve the Midi channel associated to each RhythmVoice. Use
     * <code>MidiMix.getChannel(RhythmVoice)</code> for a standard <code>RhythmVoice</code>, and
     * <code>MidiMix.getChannel(RhythmVoiceDelegate.getSource())</code> for a <code>RhythmVoiceDelegate</code>.
     * <p>
     * If the context song contains several rhythms, the method must add notes ONLY for bars which use this MidiMusicGenerator's
     * rhythm.
     * <p>
     * Note that some features are directly managed by the JJazzLab framework (by postprocessing the output of generateMusic())
     * :<br>
     * - Instrument selection and settings (Program changes, Midi controller messages such as bank select, volume, reverb,
     * panoramic, etc.) <br>
     * - RP_SYS_Mute rhythm parameter handling (muting a specific track for a specific SongPart)<br>
     * - RP_SYS_DrumsMix rhythm parameter handling (adjusting some drums track instruments velocity)<br>
     * - Handling of the channel's specific velocity shift<br>
     * - Handling of the instrument's specific transposition<br>
     *
     * @param context The information to be used for music generation
     * @return One Phrase per rhythm voice/channel.
     *
     * @throws MusicGenerationException If generator could not produce the expected music. The framework is responsible for
     * notifying the user of the error message associated to the exception.
     *
     */
    Map<RhythmVoice, Phrase> generateMusic(SongContext context) throws MusicGenerationException;
}

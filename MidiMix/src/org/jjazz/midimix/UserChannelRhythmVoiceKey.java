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
package org.jjazz.midimix;

import org.jjazz.harmony.TimeSignature;
import org.jjazz.midi.synths.GM1Bank;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midi.InstrumentSettings;
import org.jjazz.midi.synths.Family;
import org.jjazz.rhythm.api.DummyRhythm;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * A special RhythmVoice instance used by MidiMix as the RhythmVoice key for the special User channel.
 */
public class UserChannelRhythmVoiceKey extends RhythmVoice
{

    private static UserChannelRhythmVoiceKey INSTANCE;

    static public UserChannelRhythmVoiceKey getInstance()
    {
        synchronized (UserChannelRhythmVoiceKey.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new UserChannelRhythmVoiceKey();
            }
        }
        return INSTANCE;
    }

    private UserChannelRhythmVoiceKey()
    {
        super(new DummyRhythm("UserChannelDummyRhythm", TimeSignature.FOUR_FOUR), Type.CHORD1, "USER_CHANNEL", StdSynth.getGM1Bank().getDefaultInstrument(Family.Piano), new InstrumentSettings(), 0);
    }

}

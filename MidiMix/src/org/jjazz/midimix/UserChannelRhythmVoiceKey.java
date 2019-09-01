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
import org.jjazz.midi.GM1Instrument;
import org.jjazz.midi.GMSynth;
import org.jjazz.midi.InstrumentSettings;
import org.jjazz.rhythm.api.AbstractRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RvType;
import org.jjazz.rhythm.api.TempoRange;

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
        super(new DummyRhythm(), RvType.Other, "USER_CHANNEL", GMSynth.getInstance().getGM1Bank().getInstruments().get(0), new InstrumentSettings(), 0);
    }

    /**
     * Used to create the special RhythmVoice for user channel.
     */
    private static class DummyRhythm extends AbstractRhythm
    {

        public DummyRhythm()
        {
            super("x", "name", "desc.", "author", "ver", Rhythm.Feel.BINARY, TimeSignature.FOUR_FOUR, 120, TempoRange.MEDIUM, null);
        }

    }
}

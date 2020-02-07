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

import java.util.prefs.Preferences;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midi.InstrumentSettings;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.synths.Family;
import org.jjazz.rhythm.api.DummyRhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.openide.util.NbPreferences;

/**
 * A special RhythmVoice instance used by MidiMix as the RhythmVoice key for the special User channel.
 */
public class UserChannelRvKey extends RhythmVoice
{

    private static final String PREF_USER_CHANNEL = "PrefUserChannel";
    private static UserChannelRvKey INSTANCE;
    private static Preferences prefs = NbPreferences.forModule(UserChannelRvKey.class);

    static public UserChannelRvKey getInstance()
    {
        synchronized (UserChannelRvKey.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new UserChannelRvKey();
            }
        }
        return INSTANCE;
    }

    private UserChannelRvKey()
    {
        super(new DummyRhythm("UserChannelDummyRhythm", TimeSignature.FOUR_FOUR), Type.CHORD1, "User", StdSynth.getInstance().getGM1Bank().getDefaultInstrument(Family.Piano), new InstrumentSettings(), 0);
    }

    /**
     * The default Midi channel to be used when User channel is enabled.
     * <p>
     * 0 by default.
     *
     * @return
     */
    public int getPreferredUserChannel()
    {
        return prefs.getInt(PREF_USER_CHANNEL, 0);
    }

    /**
     * Set the preferred Midi channel for the user channel.
     *
     * @param c Can't be the channel reserved for drums (10/9)
     */
    public void setPreferredUserChannel(int c)
    {
        if (!MidiConst.checkMidiChannel(c) || c == MidiConst.CHANNEL_DRUMS)
        {
            throw new IllegalArgumentException("c=" + c);
        }
        prefs.putInt(PREF_USER_CHANNEL, c);
    }

}

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
package org.jjazz.outputsynth.api;

import org.jjazz.midi.api.MidiSynth;
import org.jjazz.outputsynth.api.OutputSynth.UserSettings.SendModeOnUponPlay;

/**
 * The builtin OutputSynth for the JJazzLabSoundFont in GS mode.
 */
public class OS_JJazzLabSoundFont_GM2 extends OutputSynth
{

    private static OS_JJazzLabSoundFont_GM2 INSTANCE;

    public static OS_JJazzLabSoundFont_GM2 getInstance()
    {
        synchronized (OS_JJazzLabSoundFont_GM2.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new OS_JJazzLabSoundFont_GM2();
            }
        }
        return INSTANCE;
    }

    private OS_JJazzLabSoundFont_GM2()
    {
        super(new MultiSynth(MultiSynthManager.getInstance().getMidiSynth(MultiSynthManager.JJAZZLAB_SOUNDFONT_GM2_SYNTH_NAME)));
        getUserSettings().setSendModeOnUponPlay(SendModeOnUponPlay.GM2);
    }


    /**
     * The synth associated to the JJazzLab soundfont.
     *
     * @return
     */
    public final MidiSynth getJJazzLabSoundFontSynth()
    {
        return MultiSynthManager.getInstance().getMidiSynth(MultiSynthManager.JJAZZLAB_SOUNDFONT_GM2_SYNTH_NAME);
    }

}

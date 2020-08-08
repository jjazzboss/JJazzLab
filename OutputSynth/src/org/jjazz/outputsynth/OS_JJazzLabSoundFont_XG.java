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
package org.jjazz.outputsynth;

import java.io.File;
import java.util.logging.Logger;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midisynthmanager.api.MidiSynthManager;

/**
 * The builtin OutputSynth for the JJazzLabSoundFont in XG mode.
 */
public class OS_JJazzLabSoundFont_XG extends OutputSynth
{


    private static OS_JJazzLabSoundFont_XG INSTANCE;
    private final MidiSynth midiSynth;
    private static final Logger LOGGER = Logger.getLogger(OS_JJazzLabSoundFont_XG.class.getSimpleName());

    public static OS_JJazzLabSoundFont_XG getInstance()
    {
        synchronized (OS_JJazzLabSoundFont_XG.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new OS_JJazzLabSoundFont_XG();
            }
        }
        return INSTANCE;
    }

    private OS_JJazzLabSoundFont_XG()
    {
        midiSynth = MidiSynthManager.getDefault().getMidiSynth(MidiSynthManager.JJAZZLAB_SOUNDFONT_XG_SYNTH_NAME);

        // Adjust settings
        addCustomSynth(midiSynth);
        removeCompatibleStdBank(StdSynth.getInstance().getGM1Bank());
        setSendModeOnUponPlay(OutputSynth.SendModeOnUponStartup.XG);
    }

    /**
     * Overridden : forbidden method on this preset object.
     *
     * @param f
     */
    @Override
    public void setFile(File f)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * The synth associated to the JJazzLab soundfont.
     *
     * @return
     */
    public MidiSynth getJJazzLabSoundFontSynth()
    {
        return midiSynth;
    }

}

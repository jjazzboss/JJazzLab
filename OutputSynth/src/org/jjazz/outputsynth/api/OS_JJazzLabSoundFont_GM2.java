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

import java.io.File;
import java.util.logging.Logger;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.synths.StdSynth;
import org.jjazz.midisynthmanager.api.MidiSynthManager;

/**
 * The builtin OutputSynth for the JJazzLabSoundFont in GS mode.
 */
public class OS_JJazzLabSoundFont_GM2 extends OutputSynth
{    
    
    private static OS_JJazzLabSoundFont_GM2 INSTANCE;
    private final MidiSynth midiSynth;
    private static final Logger LOGGER = Logger.getLogger(OS_JJazzLabSoundFont_GM2.class.getSimpleName());
    
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
       midiSynth = MidiSynthManager.getDefault().getMidiSynth(MidiSynthManager.JJAZZLAB_SOUNDFONT_GM2_SYNTH_NAME);

        // Adjust settings
        addCustomSynth(midiSynth);
        removeCompatibleStdBank(StdSynth.getInstance().getGM1Bank());
        setSendModeOnUponPlay(OutputSynth.SendModeOnUponStartup.GM2);
    }
    
    /**
     * Overridden : forbidden method on this preset object.
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

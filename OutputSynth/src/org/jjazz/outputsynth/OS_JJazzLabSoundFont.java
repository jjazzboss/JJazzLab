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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.midi.synths.StdSynth;

/**
 * The builtin OutputSynth for the JJazzLabSoundFont.
 */
public class OS_JJazzLabSoundFont extends OutputSynth
{
    
    private static final String JJAZZLAB_SOUNDFONT_SYNTH_PATH = "resources/JJazzLabSoundFontSynth.ins";
    private static OS_JJazzLabSoundFont INSTANCE;
    private final MidiSynth midiSynth;
    private static final Logger LOGGER = Logger.getLogger(OS_JJazzLabSoundFont.class.getSimpleName());
    
    public static OS_JJazzLabSoundFont getInstance()
    {
        synchronized (OS_JJazzLabSoundFont.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new OS_JJazzLabSoundFont();
            }
        }
        return INSTANCE;
    }
    
    private OS_JJazzLabSoundFont()
    {
        // Read the synth from the .ins file
        InputStream is = getClass().getResourceAsStream(JJAZZLAB_SOUNDFONT_SYNTH_PATH);
        assert is != null : "JJAZZLAB_SOUNDFONT_SYNTH_PATH=" + JJAZZLAB_SOUNDFONT_SYNTH_PATH;
        MidiSynthFileReader r = MidiSynthFileReader.Util.getReader("ins");
        assert r != null;
        try
        {
            List<MidiSynth> synths = r.readSynthsFromStream(is, null);
            assert synths.size() == 1;
            midiSynth = synths.get(0);
        } catch (IOException ex)
        {
            throw new IllegalStateException("Unexpected error", ex);
        }

        // Adjust settings
        addCustomSynth(midiSynth);
        removeCompatibleStdBank(StdSynth.getInstance().getGM1Bank());
        setSendModeOnUponPlay(OutputSynth.SendModeOnUponStartup.GS);
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

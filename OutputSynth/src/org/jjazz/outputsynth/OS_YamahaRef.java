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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.MidiAddress;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midisynthmanager.api.MidiSynthManager;

/**
 * The builtin OutputSynth for the Yamaha Tyros/PSR reference synth.
 */
public class OS_YamahaRef extends OutputSynth
{

    private static OS_YamahaRef INSTANCE;
    private final MidiSynth midiSynth;
    private static final Logger LOGGER = Logger.getLogger(OS_YamahaRef.class.getSimpleName());

    public static OS_YamahaRef getInstance()
    {
        synchronized (OS_YamahaRef.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new OS_YamahaRef();
            }
        }
        return INSTANCE;
    }

    private OS_YamahaRef()
    {
        midiSynth = MidiSynthManager.getDefault().getMidiSynth(MidiSynthManager.YAMAHA_REF_SYNTH_NAME);

        // Adjust settings
        addCustomSynth(midiSynth);
        addCompatibleStdBank(StdSynth.getInstance().getGM2Bank());
        addCompatibleStdBank(StdSynth.getInstance().getXGBank());
    }

    /**
     * The YamahaRef synth.
     *
     * @return
     */
    public MidiSynth getYamahaRefSynth()
    {
        return midiSynth;
    }

    /**
     * The Drums instrument of this synth to be used by default.
     *
     * @return
     */
    public Instrument getDefaultDrumsInstrument()
    {
        // This is the standard kit
        return midiSynth.getInstrument(new MidiAddress(0, 127, 0, MidiAddress.BankSelectMethod.MSB_LSB));
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
}

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

/**
 * The builtin OutputSynth for the Yamaha Tyros/PSR reference synth.
 */
public class OS_YamahaRef extends OutputSynth
{

    private static final String YAMAHA_REF_SYNTH_PATH = "resources/YamahaRefSynth.ins";

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
        // Read the synth from the .ins file
        InputStream is = getClass().getResourceAsStream(YAMAHA_REF_SYNTH_PATH);
        assert is != null : "YAMAHA_REF_SYNTH_PATH=" + YAMAHA_REF_SYNTH_PATH;
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

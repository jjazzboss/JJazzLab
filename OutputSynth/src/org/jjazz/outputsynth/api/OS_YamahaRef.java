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

import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiSynth;


/**
 * The builtin OutputSynth for the Yamaha Tyros/PSR reference synth.
 */
public class OS_YamahaRef extends OutputSynth
{

    private static OS_YamahaRef INSTANCE;

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
        super(MidiSynthManager.getInstance().getMidiSynth(MidiSynthManager.YAMAHA_REF_SYNTH_NAME));
    }

     /**
     * The YamahaRef synth.
     *
     * @return
     */
    public MidiSynth getYamahaRefSynth()
    {
        return MidiSynthManager.getInstance().getMidiSynth(MidiSynthManager.YAMAHA_REF_SYNTH_NAME);
    }

    /**
     * The Drums instrument of this synth to be used by default.
     *
     * @return
     */
    public Instrument getDefaultDrumsInstrument()
    {
        // This is the standard kit
        return getYamahaRefSynth().getInstrument(new MidiAddress(0, 127, 0, MidiAddress.BankSelectMethod.MSB_LSB));
    }


}

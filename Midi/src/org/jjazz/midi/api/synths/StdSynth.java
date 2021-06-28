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
package org.jjazz.midi.api.synths;

import java.util.logging.Logger;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiSynth;

/**
 * A shared instance of a standard MidiSynth for standard banks: GM, GM2, XG.
 */
public class StdSynth extends MidiSynth
{
    public static String NAME = "Standard";
    public static String MANUFACTURER = "JJazz";
    private static StdSynth INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(StdSynth.class.getSimpleName());

    public static StdSynth getInstance()
    {
        synchronized (StdSynth.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new StdSynth();
            }
        }
        return INSTANCE;
    }

    private StdSynth()
    {
        super(NAME, MANUFACTURER);
        addBank(getGM1Bank());
        addBank(getGM2Bank());
        addBank(getXGBank());
        addBank(NotSetBank.getInstance());
    }

    public GM1Bank getGM1Bank()
    {
        return GM1Bank.getInstance();
    }

    public GM2Bank getGM2Bank()
    {
        return GM2Bank.getInstance();
    }

    public XGBank getXGBank()
    {
        return XGBank.getInstance();
    }

    public VoidInstrument getVoidInstrument()
    {
        return NotSetBank.getInstance().getVoidInstrument();
    }

    /**
     * Overridden to be clever in the instrument search.
     *
     * @param address
     * @return Can be null.
     */
    @Override
    public Instrument getInstrument(MidiAddress address)
    {
        Instrument ins = null;
        int pc = address.getProgramChange();
        int bankMSB = address.getBankMSB();
        int bankLSB = address.getBankLSB();
        if (bankMSB == 0 && bankLSB == 0)
        {
            // GM 
            ins = getGM1Bank().getInstrument(pc);
            assert ins != null;   //NOI18N
        } else if (bankMSB == 121 || bankMSB == 120)
        {
            // GM2 
            ins = getGM2Bank().getInstrument(address);
        } else if (bankMSB == 0 || bankMSB == 126 || bankMSB == 127)
        {
            // XG 
            ins = getXGBank().getInstrument(address);
        }
        return ins;
    }
}

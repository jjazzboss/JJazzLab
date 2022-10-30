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
import org.jjazz.midi.api.MidiSynth;

/**
 * A synth which only contains the GM bank.
 * <p>
 */
public class GMSynth extends MidiSynth
{

    public static String NAME = "GM Synth";
    public static String MANUFACTURER = "JJazz";
    private static GMSynth INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(GMSynth.class.getSimpleName());

    public static GMSynth getInstance()
    {
        synchronized (GMSynth.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new GMSynth();
            }
        }
        return INSTANCE;
    }

    private GMSynth()
    {
        super(NAME, MANUFACTURER);
        addBank(getGM1Bank());
        addBank(NotSetBank.getInstance());
        setCompatibility(true, false, false, false);
    }

    public final GM1Bank getGM1Bank()
    {
        return GM1Bank.getInstance();
    }

    /**
     * A special "empty" GM1Instrument: when used, no Midi messages are sent (no bank select/program change).
     * 
     * This special instrument is part of the special InstrumentBank NotSetBank.
     * @return
     */
    public final VoidInstrument getVoidInstrument()
    {
        return NotSetBank.getInstance().getVoidInstrument();
    }



}

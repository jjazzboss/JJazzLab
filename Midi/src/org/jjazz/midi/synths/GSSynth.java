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
package org.jjazz.midi.synths;

import java.util.logging.Logger;
import org.jjazz.midi.MidiSynth;

/**
 * The GS synth for the GS banks.
 * <p>
 * NOTE: GS banks are NOT compatible with GM2/XG in general, some identical MidiAddresses result in completly different patches.
 */
public class GSSynth extends MidiSynth
{

    public static String NAME = "GS Synth";
    public static String MANUFACTURER = "JJazz";
    private static GSSynth INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(GSSynth.class.getSimpleName());

    public static GSSynth getInstance()
    {
        synchronized (GSSynth.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new GSSynth();
            }
        }
        return INSTANCE;
    }

    private GSSynth()
    {
        super(NAME, MANUFACTURER);
        addBank(getGSBank());
        // addBank(getGS_SC88Pro_Bank());
    }

    public GSBank getGSBank()
    {
        return GSBank.getInstance();
    }

    public GSBank_SC88Pro getGS_SC88Pro_Bank()
    {
        return GSBank_SC88Pro.getInstance();
    }
 
}

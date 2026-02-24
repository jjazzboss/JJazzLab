/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.midi.api.synths;

import java.util.logging.Logger;
import org.jjazz.midi.api.MidiSynth;

/**
 * A synth which only contains the GM2 bank.
 * <p>
 */
public class GM2Synth extends MidiSynth
{

    public static String NAME = "GM2 Synth";
    public static String MANUFACTURER = "JJazz";
    private static GM2Synth INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(GM2Synth.class.getSimpleName());

    public static GM2Synth getInstance()
    {
        synchronized (GM2Synth.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new GM2Synth();
            }
        }
        return INSTANCE;
    }

    private GM2Synth()
    {
        super(NAME, MANUFACTURER);
        addBank(getGM2Bank());
        setCompatibility(true, true, false, false);
    }

    public final GM2Bank getGM2Bank()
    {
        return GM2Bank.getInstance();
    }

  

}

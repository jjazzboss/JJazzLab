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
 * A synth which only contains the XG bank.
 * <p>
 */
public class XGSynth extends MidiSynth
{

    public static String NAME = "XG Synth";
    public static String MANUFACTURER = "JJazz";
    private static XGSynth INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(XGSynth.class.getSimpleName());

    public static XGSynth getInstance()
    {
        synchronized (XGSynth.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new XGSynth();
            }
        }
        return INSTANCE;
    }

    private XGSynth()
    {
        super(NAME, MANUFACTURER);
        addBank(getXGBank());
        setCompatibility(true, false, true, false);
    }

    public final XGBank getXGBank()
    {
        return XGBank.getInstance();
    }

}

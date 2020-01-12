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

import java.util.logging.*;
import org.jjazz.midi.AbstractInstrumentBank;
import org.jjazz.midi.Instrument;

/**
 * Utility bank to store the Void instrument.
 *
 * @param <T>
 */
public final class NotSetBank<T extends Instrument> extends AbstractInstrumentBank<Instrument>
{

    public static final String BANKNAME = "NotSet Bank";
    private static NotSetBank<Instrument> INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(NotSetBank.class.getSimpleName());

    /**
     * Use JJazzSynth to get access to this instance.
     *
     * @return
     */
    protected static NotSetBank<Instrument> getInstance()
    {
        synchronized (NotSetBank.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new NotSetBank<>();
            }
        }
        return INSTANCE;
    }

    private NotSetBank()
    {
        super(BANKNAME, 0, 0);
        addInstrument(getVoidInstrument());
    }

    /**
     * An empty GM1Instrument: when used, no Midi message is sent.
     *
     * @return
     */
    public VoidInstrument getVoidInstrument()
    {
        return VoidInstrument.getInstance();
    }
}

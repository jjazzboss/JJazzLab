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
package org.jjazz.defaultinstruments;

import java.util.logging.*;
import static org.jjazz.defaultinstruments.Bundle.CTL_NotSetBank;
import org.jjazz.midi.AbstractInstrumentBank;
import org.jjazz.midi.Instrument;
import org.openide.util.NbBundle;

/**
 * JJazzBank utility bank to store the Void instrument.
 *
 * @param <T>
 */
@NbBundle.Messages(
        {
            "CTL_NotSetBank=NotSet Bank"
        })
public final class NotSetBank<T extends Instrument> extends AbstractInstrumentBank<Instrument>
{

    public static final String BANKNAME = CTL_NotSetBank();
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
        super(BANKNAME, null, 0, 0);
        addInstrument(getVoidInstrument());
        addInstrument(getVoidDrumsInstrument());
    }

    public VoidInstrument getVoidInstrument()
    {
        return VoidInstrument.getInstance();
    }

    public VoidDrumsInstrument getVoidDrumsInstrument()
    {
        return VoidDrumsInstrument.getInstance();
    }
}

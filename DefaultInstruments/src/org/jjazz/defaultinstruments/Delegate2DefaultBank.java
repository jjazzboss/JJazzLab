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
import static org.jjazz.defaultinstruments.Bundle.CTL_DefaultBank;
import org.jjazz.midi.AbstractInstrumentBank;
import org.jjazz.midi.Instrument;
import org.jjazz.rhythm.api.RvType;
import org.openide.util.NbBundle;

/**
 * Bank to store the Delegate2DefaultInstruments associated to each RhythmVoice.Type.
 *
 * @param <T>
 */
@NbBundle.Messages(
        {
            "CTL_DefaultBank=Default Instruments Bank"
        })
public final class Delegate2DefaultBank<T extends Instrument> extends AbstractInstrumentBank<Instrument>
{

    public static final String BANKNAME = CTL_DefaultBank();
    private static Delegate2DefaultBank<Instrument> INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(Delegate2DefaultBank.class.getSimpleName());

    /**
     * Use JJazzSynth to get access to this instance.
     *
     * @return
     */
    protected static Delegate2DefaultBank<Instrument> getInstance()
    {
        synchronized (Delegate2DefaultBank.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new Delegate2DefaultBank<Instrument>();
            }
        }
        return INSTANCE;
    }

    private Delegate2DefaultBank()
    {
        super(BANKNAME, null, 0, 0);
        for (RvType rvType : RvType.values())
        {
            addInstrument(getDelegateInstrument(rvType));
        }
        addInstrument(getDelegateInstrumentUser());
    }

    /**
     * Get the delegate instrument to default instrument associated to rbType.
     *
     * @param rvType
     * @return
     */
    protected Delegate2DefaultInstrument getDelegateInstrument(RvType rvType)
    {
        return Delegate2DefaultInstrument.getInstance(rvType);
    }

    /**
     * Get the delegate instrument to default instrument User.
     *
     * @return
     */
    protected Delegate2DefaultInstrument getDelegateInstrumentUser()
    {
        return Delegate2DefaultInstrument.getInstanceUser();
    }

}

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

import org.jjazz.midi.MidiSynth;
import org.jjazz.rhythm.api.RvType;

/**
 * Contain the JJazz Utility Bank.
 */
public class JJazzSynth extends MidiSynth
{

    public static String NAME = "JJazz Synth";
    public static String MANUFACTURER = "JJazz";
    private static JJazzSynth INSTANCE;

    public static JJazzSynth getInstance()
    {
        synchronized (JJazzSynth.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new JJazzSynth();
            }
        }
        return INSTANCE;
    }

    private JJazzSynth()
    {
        super(NAME, MANUFACTURER);
        addBank(Delegate2DefaultBank.getInstance());
        addBank(NotSetBank.getInstance());
    }

    public NotSetBank<?> getNotSetBank()
    {
        return NotSetBank.getInstance();
    }

    public Delegate2DefaultBank<?> getDelegate2DefaultBank()
    {
        return Delegate2DefaultBank.getInstance();
    }

    /**
     * Convenience method to get the generic VoidInstrument.
     *
     * @return
     */
    static public VoidInstrument getVoidInstrument()
    {
        return getInstance().getNotSetBank().getVoidInstrument();
    }
    

    /**
     * Get the Delegate2DefaultInstrument to the default instrument for rvType.
     *
     * @param rvType
     * @return Can't be null.
     */
    static public Delegate2DefaultInstrument getDelegate2DefaultInstrument(RvType rvType)
    {
        if (rvType == null)
        {
            throw new NullPointerException("rvType");
        }
        return getInstance().getDelegate2DefaultBank().getDelegateInstrument(rvType);
    }

    /**
     * Get the Delegate2DefaultInstrument to the User default instrument.
     *
     * @return Can't be null
     */
    static public Delegate2DefaultInstrument getDelegate2DefaultInstrumentUser()
    {
        return getInstance().getDelegate2DefaultBank().getDelegateInstrumentUser();
    }

}

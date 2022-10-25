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
        setCompatibility(true, false, false, false);
    }

    public final GM1Bank getGM1Bank()
    {
        return GM1Bank.getInstance();
    }

    /**
     * A special "empty" GM1Instrument: when used, no Midi messages are sent (no bank select/program change).
     *
     * @return
     */
    public final VoidInstrument getVoidInstrument()
    {
        return NotSetBank.getInstance().getVoidInstrument();
    }


    /**
     * Get the Instrument from this MidiSynth which best matches the specified instrument.
     * <p>
     * Take into account ins' MidiSynth getGM1BankBaseMidiAddress(), if it is defined.
     * <p>
     * Note that if ins' MidiSynth is defined but not marked as GM-compatible, method returns null.
     *
     * @param ins
     * @return Null if no match
     */
    @Override
    public Instrument getMatchingInstrument(Instrument ins)
    {
        var insAddr = ins.getMidiAddress();
        var insBank = ins.getBank();
        var insSynth = insBank != null ? insBank.getMidiSynth() : null;


        if (insSynth == null)
        {
            // Can't do much without compatibility info
            return getGM1Bank().getInstrument(ins.getMidiAddress());    // Can be null
        }


        if (!insSynth.isGMcompatible())
        {
            return null;
        }

        // Return the GM instrument only if ins' MSB/LSB match its MidiSynth' GM bank                
        MidiAddress gmBankBaseAddr = insSynth.getGM1BankBaseMidiAddress();
        if (ins.getMidiAddress().getBankMSB() == gmBankBaseAddr.getBankMSB()
                && ins.getMidiAddress().getBankMSB() == gmBankBaseAddr.getBankLSB())
        {
            return getGM1Bank().getInstrument(ins.getMidiAddress().getProgramChange());
        } else
        {
            return null;
        }

    }

}

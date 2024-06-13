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

import com.google.common.base.Preconditions;
import java.util.logging.Logger;
import org.jjazz.midi.api.Instrument;
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
     * <p>
     * This special instrument is part of the special InstrumentBank NotSetBank.
     *
     * @return
     */
    public final VoidInstrument getVoidInstrument()
    {
        return NotSetBank.getInstance().getVoidInstrument();
    }

    /**
     * Convenience method to directly get the GM1Instrument with specified Program Change.
     *
     * @param progChange
     * @return
     */
    static public GM1Instrument getGM1Instrument(int progChange)
    {
        return getInstance().getGM1Bank().getInstrument(progChange);
    }

    /**
     * Convenience method to get the GM1Instrument from this synth corresponding to an instrument from a synth's GM bank.
     *
     * @param ins Should be an ins from the GM-bank of its synth
     * @return Null if ins is not an instrument of its synth's GM bank
     */
    static public GM1Instrument getGM1Instrument(Instrument ins)
    {
        Preconditions.checkNotNull(ins);
        if (ins.getBank() == null)
        {
            return null;
        }
        var insSynth = ins.getBank().getMidiSynth();
        var insAddr = ins.getMidiAddress();
        if (insSynth == null || !insSynth.isGMcompatible() || !insSynth.isGM1BankMidiAddress(insAddr))
        {
            return null;
        }
        return getGM1Instrument(insAddr.getProgramChange());
    }

}

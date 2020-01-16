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
package org.jjazz.midiconverters.api;

import org.jjazz.midi.DrumKit;
import org.jjazz.midiconverters.spi.KeyMapConverter;
import org.jjazz.midiconverters.spi.InstrumentConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;
import org.openide.util.Lookup;

/**
 * Manage the conversion between instruments (normal or drums/percussion) on different synths.
 * <p>
 */
public class ConvertersManager
{

    private static ConvertersManager INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(ConvertersManager.class.getSimpleName());

    public static ConvertersManager getInstance()
    {
        synchronized (ConvertersManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ConvertersManager();
            }
        }
        return INSTANCE;
    }

    private ConvertersManager()
    {

    }

    /**
     * Try to convert a drum key (a note) from srcKit into a key of destKit.
     * <p>
     * The method asks each KeyMapConverter found in the global lookup to do the conversion, until a conversion succeeds.
     *
     * @param srcKit
     * @param key     The pitch of the note
     * @param destKit
     * @return -1 if no conversion could be done.
     */
    public int convertDrumKey(DrumKit srcKit, int key, DrumKit destKit)
    {
        if (srcKit == null || destKit == null || srcKit.getKeyMap().getKeyName(key) == null)
        {
            throw new IllegalArgumentException("srcKit=" + srcKit + " key=" + key + " destKit=" + destKit);
        }
        int res = -1;
        for (KeyMapConverter dkm : getKeyMapConverters())
        {
            res = dkm.convertKey(srcKit, key, destKit);
            if (res != -1)
            {
                break;
            }
        }
        return res;
    }

    /**
     * Try to convert an Instrument into an instrument of destSynth.
     * <p>
     * The method asks each InstrumentConverter found in the global lookup to do the conversion, until a conversion succeeds.
     *
     * @param srcIns
     * @param destSynth
     * @param banks     Limit the search to these destSynth banks. If null use all banks of destSynth.
     * @return null if no conversion could be done.
     */
    public Instrument convertInstrument(Instrument srcIns, MidiSynth destSynth, List<InstrumentBank<?>> banks)
    {
        if (srcIns == null || destSynth == null)
        {
            throw new IllegalArgumentException("ins=" + srcIns + " destSynth=" + destSynth);
        }
        if (banks != null)
        {
            for (InstrumentBank<?> bank : banks)
            {
                if (bank.getMidiSynth() != destSynth)
                {
                    throw new IllegalArgumentException("srcIns=" + srcIns.toLongString() + " destSynth=" + destSynth + " banks=" + banks + " bank=" + bank);
                }
            }
        }
        // Special easy case: check if srcSynth is an instrument from destSynth and its bank is searched
        InstrumentBank<?> srcBank = srcIns.getBank();
        if (srcBank != null && destSynth.getBanks().contains(srcBank) && (banks == null || banks.contains(srcBank)))
        {
            return srcIns;
        }

        // Ask all InstrumentConverters from the Lookup
        Instrument res = null;
        for (InstrumentConverter ic : getInstrumentConverters())
        {
            res = ic.convertInstrument(srcIns, destSynth, banks);
            if (res != null)
            {
                break;
            }
        }
        return res;
    }

    // ======================================================================================
    // Private methods
    // ======================================================================================
    /**
     * Get all the KeyMapConverters present in the global lookup.
     *
     * @return Can be empty
     */
    private List<KeyMapConverter> getKeyMapConverters()
    {
        ArrayList<KeyMapConverter> res = new ArrayList<>();
        for (KeyMapConverter dmc : Lookup.getDefault().lookupAll(KeyMapConverter.class))
        {
            res.add(dmc);
        }
        return res;
    }

    /**
     * Get all the Instrument present in the global lookup.
     *
     * @return Can be empty
     */
    private List<InstrumentConverter> getInstrumentConverters()
    {
        ArrayList<InstrumentConverter> res = new ArrayList<>();
        for (InstrumentConverter ic : Lookup.getDefault().lookupAll(InstrumentConverter.class))
        {
            res.add(ic);
        }
        return res;
    }

}

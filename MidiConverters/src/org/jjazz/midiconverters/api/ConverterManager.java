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
public class ConverterManager
{

    private static ConverterManager INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(ConverterManager.class.getSimpleName());

    public static ConverterManager getInstance()
    {
        synchronized (ConverterManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ConverterManager();
            }
        }
        return INSTANCE;
    }

    private ConverterManager()
    {

    }

    /**
     * Search the first available KeyMapConverter which accepts to convert notes from the specified DrumKit.KeyMaps.
     *
     * @param srcMap
     * @param destMap
     * @return Can be null.
     */
    public KeyMapConverter getKeyMapConverter(DrumKit.KeyMap srcMap, DrumKit.KeyMap destMap)
    {
        StdKeyMapConverter stdConverter = StdKeyMapConverter.getInstance();
        if (stdConverter.accept(srcMap, destMap))
        {
            return stdConverter;
        }
        for (KeyMapConverter c : Lookup.getDefault().lookupAll(KeyMapConverter.class))
        {
            if (c.accept(srcMap, destMap))
            {
                return c;
            }
        }
        return null;
    }

    /**
     * Try to convert an Instrument into an instrument of destSynth.
     * <p>
     * Manage the trivial cases (srcIns already belongs to destSynth). Then the method asks each InstrumentConverter found in the
     * global lookup to do the conversion, until a conversion succeeds.
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
        for (InstrumentConverter ic : Lookup.getDefault().lookupAll(InstrumentConverter.class))
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
}

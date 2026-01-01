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
package org.jjazz.yamjjazz.rhythm.api;

import java.util.HashMap;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiAddress;

/**
 * Store the SInt section information: the instruments and settings associated to AccTypes.
 * <p>
 * Associate datad are original MidiAddress, inferred InstrumentMix using YamahaRefSynth, isUsingMegaVoice.
 * <p>
 * Note that there is NO guarantee that SInt contains data for ALL AccTypes defined in the CASM.
 */
public class SInt
{

    private final HashMap<AccType, InstrumentMix> mapAccTypeInsMix = new HashMap<>();
    private final HashMap<AccType, Boolean> mapAccTypeMegaVoice = new HashMap<>();
    private final HashMap<AccType, MidiAddress> mapAccTypeAddress = new HashMap<>();
    
    public void set(SInt sInt)
    {
        mapAccTypeInsMix.clear();
        mapAccTypeInsMix.putAll(sInt.mapAccTypeInsMix);
        mapAccTypeMegaVoice.clear();
        mapAccTypeMegaVoice.putAll(mapAccTypeMegaVoice);
        mapAccTypeAddress.clear();
        mapAccTypeAddress.putAll(mapAccTypeAddress);
    }

    /**
     * Associate an InstrumentMix to an AccType.
     * <p>
     * By default the specified AccType does not expect a MegaVoice.
     *
     * @param at
     * @param insMix
     */
    public void set(AccType at, InstrumentMix insMix)
    {
        mapAccTypeInsMix.put(at, insMix);
        mapAccTypeMegaVoice.put(at, false);
    }
    
    public InstrumentMix get(AccType at)
    {
        return mapAccTypeInsMix.get(at);
    }

    /**
     * Associate the original instrument MidiAddress to an AccType.
     * <p>
     *
     * @param at
     * @param adr
     */
    public void setOriginalMidiAddress(AccType at, MidiAddress adr)
    {
        mapAccTypeAddress.put(at, adr);
    }
    
    public MidiAddress getOriginalMidiAddress(AccType at)
    {
        return mapAccTypeAddress.get(at);
    }

    /**
     * Indicate if the Yamaha style file expects a Yamaha MegaVoice instrument for this AccType.
     *
     * @param at
     * @param b
     * @return
     */
    public boolean setExpectingMegaVoice(AccType at, boolean b)
    {
        return mapAccTypeMegaVoice.put(at, b);
    }

    /**
     * Return true if the Yamaha style file expects a Yamaha MegaVoice instrument for this AccType.
     *
     * @param at
     * @return
     */
    public boolean isExpectingMegaVoice(AccType at)
    {
        return mapAccTypeMegaVoice.get(at) != null ? mapAccTypeMegaVoice.get(at) : false;   // In some rare cases it can be null...
    }
    
    @Override
    public String toString()
    {
        return mapAccTypeInsMix.toString();
    }
}

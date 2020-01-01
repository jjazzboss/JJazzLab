/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midiconverters;

import org.jjazz.midi.Instrument;
import org.jjazz.midi.MidiAddress;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midiconverters.spi.InstrumentConverter;

/**
 * Conversion between GM/GS/GM2/XG sounds.
 */
public class StdInstrumentConverter implements InstrumentConverter
{
    @Override
    public String getConverterId()
    {
        return "Standard Instrument Converter";
    }

    @Override
    public Instrument convertInstrument(Instrument srcIns, MidiSynth destSynth)
    {
        MidiAddress adr = srcIns.getMidiAddress();                        
    }

    // =====================================================================================
    // Private methods
    // =====================================================================================    
}

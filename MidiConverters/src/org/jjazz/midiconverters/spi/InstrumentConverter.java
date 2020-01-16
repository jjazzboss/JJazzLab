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
package org.jjazz.midiconverters.spi;

import java.util.List;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;

/**
 * Converts a source Instrument to another instrument on a destination MidiSynth.
 */
public interface InstrumentConverter
{

    String getConverterId();

    /**
     * Try to find in destSynth an instrument corresponding to srcIns.
     *
     * @param srcIns    srcIns's MidiSynth is different from destSynth
     * @param destSynth
     * @param banks     Limit search to these banks. They must belong to destSynth. If null search all the destSynth banks.
     * @return Can be null if no matching instrument found.
     */
    Instrument convertInstrument(Instrument srcIns, MidiSynth destSynth, List<InstrumentBank<?>> banks);
}

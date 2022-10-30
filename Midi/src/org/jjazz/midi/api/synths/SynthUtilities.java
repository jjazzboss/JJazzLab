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

import java.util.List;
import org.jjazz.midi.api.MidiSynth;

/**
 * Convenience methods.
 */
public class SynthUtilities
{

    private static final List<MidiSynth> STD_SYNTHS = List.of(GMSynth.getInstance(), GM2Synth.getInstance(), XGSynth.getInstance(), GSSynth.getInstance());

    /**
     * Get all the "standard" MidiSynth instances: GM, GM2, XG, GS.
     *
     * @return
     */
    static public List<MidiSynth> getStandardSynths()
    {
        return STD_SYNTHS;
    }

    /**
     * Get a "standard" MidiSynth from its name.
     *
     * @param name
     * @return Can be null
     */
    static public MidiSynth getStandardSynth(String name)
    {
        return STD_SYNTHS.stream()
                .filter(ms -> ms.getName().equals(name))
                .findAny()
                .orElse(null);
    }
}

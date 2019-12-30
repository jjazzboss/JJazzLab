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
package org.jjazz.synthmanager.api;

import org.jjazz.midi.MidiSynth;

/**
 * This represents the synth actually connected via Midi to the output of JJazzLab.
 */
public class ConnectedSynth
{
    private MidiSynth midiSynth;

    public ConnectedSynth(MidiSynth synth)
    {
        if (synth == null)
        {
            throw new IllegalArgumentException("synth=" + synth);
        }
        midiSynth = synth;
    }

    public MidiSynth getMidiSynth()
    {
        return midiSynth;
    }
}

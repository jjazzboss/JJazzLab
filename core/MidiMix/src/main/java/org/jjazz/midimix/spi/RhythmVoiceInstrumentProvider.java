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
package org.jjazz.midimix.spi;

import org.jjazz.midi.api.Instrument;
import org.jjazz.rhythm.api.RhythmVoice;
import org.openide.util.Lookup;

/**
 * A service provider to find an appropriate Instrument to render a RhythmVoice in a MidiMix.
 */
public interface RhythmVoiceInstrumentProvider
{

    /**
     * Name reserved for the default implementation, see getProvider().
     */
    static public String DEFAULT_ID = "DefaultId";

    /**
     * Return a provider.
     * <p>
     * Search for implementations in the global lookup: return the first one with Id different from DEFAULT_ID, otherwise return
     * the one with DEFAULT_ID.
     *
     * @return Can't be null
     * @throws IllegalStateException If no provider found.
     */
    static public RhythmVoiceInstrumentProvider getProvider()
    {
        RhythmVoiceInstrumentProvider defaultProvider = null;
        RhythmVoiceInstrumentProvider otherProvider = null;
        for (RhythmVoiceInstrumentProvider p : Lookup.getDefault().lookupAll(RhythmVoiceInstrumentProvider.class))
        {
            if (p.getId().equals(DEFAULT_ID))
            {
                defaultProvider = p;
            } else
            {
                otherProvider = p;
            }
        }
        if (otherProvider != null)
        {
            return otherProvider;
        }
        if (defaultProvider != null)
        {
            return defaultProvider;
        }
        throw new IllegalStateException("No provider found");   
    }

    /**
     * The id of the provider.
     *
     * @return
     */
    public String getId();

    /**
     * Find the most appropriate Instrument on the system to render the specified RhythmVoice in a MidiMix.
     *
     * @param rv
     * @return Can't be null. It may be the VoidInstrument for drums/percussion.
     */
    public Instrument findInstrument(RhythmVoice rv);
}

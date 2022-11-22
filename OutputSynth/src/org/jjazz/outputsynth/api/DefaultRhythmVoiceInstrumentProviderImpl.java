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
package org.jjazz.outputsynth.api;

import org.jjazz.midi.api.Instrument;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.midimix.spi.RhythmVoiceInstrumentProvider;
import org.jjazz.rhythm.api.RhythmVoice;
import org.openide.util.lookup.ServiceProvider;

/**
 * Default implementation of this service provider
 */
@ServiceProvider(service = RhythmVoiceInstrumentProvider.class)
public class DefaultRhythmVoiceInstrumentProviderImpl implements RhythmVoiceInstrumentProvider
{

    @Override
    public String getId()
    {
        return RhythmVoiceInstrumentProvider.DEFAULT_ID;
    }

    @Override
    public Instrument findInstrument(RhythmVoice rv)
    {
        Instrument ins;
        var outSynth = OutputSynthManager.getInstance().getDefaultOutputSynth();
    
        if (!(rv instanceof UserRhythmVoice))
        {
            ins = outSynth.findInstrument(rv);

        } else
        {
            ins = outSynth.getUserSettings().getUserInstrument();
        }
        return ins;
    }

}

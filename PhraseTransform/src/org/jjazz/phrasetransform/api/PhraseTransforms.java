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
package org.jjazz.phrasetransform.api;

import static com.google.common.base.Preconditions.checkNotNull;
import org.jjazz.midi.api.Instrument;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.songcontext.api.SongPartContext;

/**
 * Various helper methods about PhraseTransforms.
 */
public class PhraseTransforms
{

    static public Instrument getInstrument(SizedPhrase sp, SongPartContext context)
    {
        checkNotNull(sp);
        checkNotNull(context);
        return context.getMidiMix().getInstrumentMixFromChannel(sp.getChannel()).getInstrument();
    }

    static public RhythmVoice getRhythmVoice(SizedPhrase sp, SongPartContext context)
    {
        checkNotNull(sp);
        checkNotNull(context);
        return context.getMidiMix().getRhythmVoice(sp.getChannel());
    }

}

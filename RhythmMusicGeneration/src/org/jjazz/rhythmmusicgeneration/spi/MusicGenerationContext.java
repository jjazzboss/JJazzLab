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
package org.jjazz.rhythmmusicgeneration.spi;

import org.jjazz.midimix.MidiMix;
import org.jjazz.song.api.Song;

/**
 * Information to be used by a Rhythm to generate music.
 */
public class MusicGenerationContext
{

    private Song song;
    private MidiMix mix;

    public MusicGenerationContext(Song s, MidiMix mix)
    {
        if (s == null || mix == null)
        {
            throw new IllegalArgumentException("s=" + s + " mix=" + mix);
        }
        song = s;
        this.mix = mix;
    }

    public Song getSong()
    {
        return song;
    }

    public MidiMix getMidiMix()
    {
        return mix;
    }

    @Override
    public String toString()
    {
        return "MusicGenerationContext[song=" + song.getName() + ", midiMix=" + mix + "]";
    }
}

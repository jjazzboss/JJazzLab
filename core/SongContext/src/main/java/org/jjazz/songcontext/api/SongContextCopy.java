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
package org.jjazz.songcontext.api;

import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.IntRange;


/**
 * Create a new SongContext by deep copying the Song and the MidiMix.
 * <p>
 * References to the source Song and MidiMix are kept. If you don't need these references, you should directly use SongContext.deepClone().
 */
public class SongContextCopy extends SongContext
{

    private final Song originalSong;
    private final MidiMix originalMidiMix;

    public SongContextCopy(Song s, MidiMix mm, boolean registerSongCopy)
    {
        this(s, mm, null, registerSongCopy);
    }

    public SongContextCopy(SongContext sgContext, IntRange newRange, boolean registerSongCopy)
    {
        this(sgContext.getSong(), sgContext.getMidiMix(), newRange, registerSongCopy);
    }

    public SongContextCopy(Song s, MidiMix mm, IntRange barRange, boolean registerSongCopy)
    {
        super(new SongContext(s, mm).deepClone(registerSongCopy), barRange);

        originalSong = s;
        originalMidiMix = mm;
    }

    /**
     * The original song from which we made this context copy.
     *
     * @return
     */
    public Song getOriginalSong()
    {
        return originalSong;
    }

    /**
     * The original MidiMix from which we made this context copy.
     *
     * @return
     */
    public MidiMix getOriginalMidiMix()
    {
        return originalMidiMix;
    }

}

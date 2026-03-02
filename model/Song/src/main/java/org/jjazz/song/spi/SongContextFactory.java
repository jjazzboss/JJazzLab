/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.song.spi;

import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongContext;
import org.jjazz.song.api.SongPartContext;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.Lookup;

/**
 * Create SongContext instances.
 * <p>
 */
public interface SongContextFactory
{

    public static SongContextFactory getDefault()
    {
        var res = Lookup.getDefault().lookup(SongContextFactory.class);
        if (res == null)
        {
            throw new IllegalStateException("No SongContextFactory implementation found");
        }
        return res;
    }

    /**
     * Create a SongContext for the whole song.
     * <p>
     *
     * @param song
     * @param midiMix
     * @return
     */
    SongContext of(Song song, MidiMix midiMix);

    /**
     * Create a SongContext for a whole or part of the song.
     *
     * @param s
     * @param mm
     * @param barRange
     * @return
     */
    SongContext of(Song s, MidiMix mm, IntRange barRange);

    /**
     * Create a copy using a part of the song.
     *
     * @param sgContext
     * @param newBarRange
     * @return
     */
    SongContext of(SongContext sgContext, IntRange newBarRange);

    /**
     * Create a SongPartContext for one SongPart.
     *
     * @param s
     * @param mm
     * @param spt
     * @return
     */
    SongPartContext of(Song s, MidiMix mm, SongPart spt);

    /**
     * Create a SongPartContext for a part of the song's unique SongPart.
     *
     * @param s        Must contain only one SongPart
     * @param mix
     * @param barRange
     * @return
     */
    SongPartContext ofSongPartContext(Song s, MidiMix mix, IntRange barRange);
}

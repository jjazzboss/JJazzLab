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
     * Loop restart bar is set to 0.
     *
     * @param song
     * @param midiMix
     * @return
     */
    SongContext of(Song song, MidiMix midiMix);

    /**
     * Create a SongContext for a whole or part of the song.
     * <p>
     * Loop restart bar is set to barRange.from.
     *
     * @param s
     * @param mm
     * @param barRange If null uses the whole song bar range. Must be within Song bar range.
     * @return
     */
    SongContext of(Song s, MidiMix mm, IntRange barRange);

    /**
     * Create a SongContext for a whole or part of the song, with the specified loop restart bar.
     * <p>
     *
     * @param s
     * @param mm
     * @param barRange       If null uses the whole song bar range. Must be within Song bar range.
     * @param loopRestartBar Must be within barRange
     * @return
     */
    SongContext of(Song s, MidiMix mm, IntRange barRange, int loopRestartBar);

    /**
     * Create a copy using a part of the song.
     * <p>
     * If sgContext.getLoopRestartBar() is outside of newBarRange, returned value will use newBarRange.from as LoopRestartBar.
     *
     * @param sgContext
     * @param newBarRange
     * @return
     */
    SongContext of(SongContext sgContext, IntRange newBarRange);

    /**
     * Create a SongPartContext for one SongPart.
     * <p>
     * Loop restart bar is set to spt.getBarRange().from.
     *
     * @param s
     * @param mm
     * @param spt
     * @return
     */
    SongPartContext of(Song s, MidiMix mm, SongPart spt);

    /**
     * Create a SongPartContext for a part of the SongPart.
     *
     * Loop restart bar is set to barRange.from.
     * 
     * @param s 
     * @param mix
     * @param spt
     * @param barRange Must be within spt barRange
     * @return
     */
    SongPartContext of(Song s, MidiMix mix, SongPart spt, IntRange barRange);
}

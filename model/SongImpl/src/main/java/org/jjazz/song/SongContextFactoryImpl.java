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
package org.jjazz.song;

import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongContext;
import org.jjazz.song.api.SongPartContext;
import org.jjazz.song.spi.SongContextFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.lookup.ServiceProvider;

/**
 * SongContextFactory implementation.
 */
@ServiceProvider(service = SongContextFactory.class)
public class SongContextFactoryImpl implements SongContextFactory
{

    @Override
    public SongContext of(Song s, MidiMix mm)
    {
        return new SongContextImpl(s, mm, null);
    }

    @Override
    public SongContext of(Song s, MidiMix mm, IntRange barRange)
    {
        return new SongContextImpl(s, mm, barRange);
    }

    @Override
    public SongContext of(SongContext sgContext, IntRange newBarRange)
    {
        return new SongContextImpl(sgContext, newBarRange);
    }

    @Override
    public SongPartContext of(Song s, MidiMix mm, SongPart spt)
    {
        return new SongPartContextImpl(s, mm, spt);
    }

    @Override
    public SongPartContext ofSongPartContext(Song s, MidiMix mix, IntRange barRange)
    {
        return new SongPartContextImpl(s, mix, barRange);
    }

}

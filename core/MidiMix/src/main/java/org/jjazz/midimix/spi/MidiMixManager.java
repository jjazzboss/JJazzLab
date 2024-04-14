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
package org.jjazz.midimix.spi;


import javax.sound.midi.MidiUnavailableException;
import org.jjazz.midimix.api.DefaultMidiMixManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;

/**
 * A service provider to obtain MidiMix instances for songs and rhythms.
 */
public interface MidiMixManager
{

    public static MidiMixManager getDefault()
    {
        var res = Lookup.getDefault().lookup(MidiMixManager.class);
        if (res == null)
        {
            res = DefaultMidiMixManager.getInstance();
        }
        return res;
    }

    /**
     * Get a MidiMix for the specified song in the following order.
     * <p>
     * 1. If the mix for song s already exists, just return it <br>
     * 2. Load mix from song mix file <br>
     * 3. Create a new mix for s using findMix(Rhythm) for each song's rhythm
     * <p>
     *
     * @param s
     * @return A valid MidiMix usable for the specified song.
     * @throws javax.sound.midi.MidiUnavailableException
     */
    MidiMix findMix(Song s) throws MidiUnavailableException;

    /**
     * Find a mix which must be existing.
     * <p>
     * If you're not sure if a MidiMix was already created for the specified song, use findMix(Song) instead.
     *
     * @param s
     * @return Can't be null
     * @throws IllegalStateException If no mix found
     */
    MidiMix findExistingMix(Song s);

    /**
     * Try to get a MidiMix for the specified Rhythm in the following order:
     * <p>
     * 1. Load mix from the default rhythm mix file <br>
     * 2. Create a new mix for r
     * <p>
     *
     * @param r
     * @return Can't be null
     */
    MidiMix findMix(Rhythm r);

    /**
     * Create a new MidiMix for the specified song.
     * <p>
     * Use the default rhythm mix for each song's rhythm. Create UserRhythmVoices key if song as some user phrases.
     *
     * @param sg
     * @return
     * @throws MidiUnavailableException If there is not enough available channels to accomodate song's rhythms, or other errors.
     */
    MidiMix createMix(Song sg) throws MidiUnavailableException;

    /**
     * Create a MidiMix for the specified rhythm.
     * <p>
     * Create one InstrumentMix per rhythm voice, using rhythm voice's preferred instrument and settings, and preferred channel (except if several voices share
     * the same preferred channel)
     * .<p>
     *
     * @param r If r is
     * @return A MidiMix associated to this rhythm. Rhythm voices are used as keys for InstrumentMixes.
     */
    MidiMix createMix(Rhythm r);

}

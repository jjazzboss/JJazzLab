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


import java.util.List;
import java.util.Map;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;

/**
 * A service provider which manages the MidiMix instances associated to Songs and Rhythms.
 * <p>
 */
public interface MidiMixManager
{

    public static MidiMixManager getDefault()
    {
        var res = Lookup.getDefault().lookup(MidiMixManager.class);
        if (res == null)
        {
            throw new IllegalStateException("No MidiMixManager implementation found");
        }
        return res;
    }

    /**
     * Find a mix previously registered.
     * <p>
     * If you're not sure use {@link #createMix(org.jjazz.song.api.Song)} instead.
     *
     * @param s
     * @return Can't be null
     * @throws IllegalStateException If no mix found
     */
    MidiMix findRegisteredMix(Song s);

    /**
     * Get a MidiMix for the specified song in the following order.
     * <p>
     * 1. If the mix for song s already exists, just return it <br>
     * 2. Load mix from song mix file <br>
     * 3. Create and register a new mix for s using {@link #createMix(org.jjazz.song.api.Song)} for each song's rhythm
     * <p>
     *
     * @param s
     * @return A valid MidiMix usable for the specified song.
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException If there is not enough available channels to accomodate all song rhythms
     */
    MidiMix findMix(Song s) throws UnsupportedEditException;


    /**
     * Try to get a MidiMix for the specified Rhythm in the following order:
     * <p>
     * 1. If the registered mix for rhythm r already exists, just return it <br>
     * 2. Load mix from the default rhythm mix file <br>
     * 3. Create and register a new mix for r using {@link #createMix(org.jjazz.rhythm.api.Rhythm) }
     * <p>
     *
     * @param r
     * @return Can't be null
     */
    MidiMix findMix(Rhythm r);

    /**
     * Create and register a new MidiMix for the specified song.
     * <p>
     * Use the default rhythm mix for each song's rhythm. Create UserRhythmVoices key if song as some user phrases.
     *
     * @param sg
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException If there is not enough available channels to accomodate all song rhythms
     */
    MidiMix createMix(Song sg) throws UnsupportedEditException;

    /**
     * Create and register a MidiMix for the specified rhythm.
     * <p>
     * Create one InstrumentMix per rhythm voice, using rhythm voice's preferred instrument and settings, and preferred channel (except if several voices share
     * the same preferred channel)
     * .<p>
     *
     * @param r If r is
     * @return A MidiMix associated to this rhythm. Rhythm voices are used as keys for InstrumentMixes.
     */
    MidiMix createMix(Rhythm r);

    /**
     * Get the midiMix channels which need drums rerouting.
     * <p>
     * A channel needs rerouting if all the following conditions are met:<br>
     * 0/ InsMix at MidiConst.CHANNEL_DRUMS has its instrument Midi message enabled <br>
     * 1/ channel != MidiConst.CHANNEL_DRUMS <br>
     * 2/ rv.isDrums() == true and rerouting is not already enabled <br>
     * 3/ instrument (or new instrument if one is provided in the mapChannelNewIns parameter) is the VoidInstrument<br>
     *
     * @param midiMix
     * @param mapChannelNewIns Optional channel instruments to be used for the exercise. Ignored if null. See OutputSynth.getNeedFixInstruments().
     * @return Can be empty
     */
    List<Integer> getChannelsNeedingDrumsRerouting(MidiMix midiMix, Map<Integer, Instrument> mapChannelNewIns);

    /**
     * Import InstrumentMixes from mmSrc to mmDest.
     * <p>
     * Import is first done on matching RhythmVoices from the same rhythm. Then import only when RvTypes match. For UserRhythmVoices import is done only if name
     * matches.<br>
     * Create new copy instances of Instruments Mixes with solo OFF.
     * <p>
     *
     * @param midiMixDest
     * @param midiMixSrc
     */
    void importInstrumentMixes(MidiMix midiMixDest, MidiMix midiMixSrc);

    /**
     * Build a rhythm MidiMix from a song MidiMix.
     *
     * @param midiMix
     * @param r
     * @return Can be empty if midiMix does not use r
     */
    MidiMix getRhythmMix(MidiMix midiMix, Rhythm r);

    /**
     * Get a deep copy of midiMix.
     * <p>
     *
     * @param midiMix
     * @param song    If not null, the returned instance is registered with this song (caller is responsible to provide a song consistent with midiMix).
     * @return
     */
    MidiMix getDeepCopy(MidiMix midiMix, Song song);

}

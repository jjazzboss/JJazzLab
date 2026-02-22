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
package org.jjazz.midimix.api;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.ExecutionManager;
import org.jjazz.song.Operation;
import org.jjazz.song.ThrowingWriteOperation;
import org.jjazz.song.WriteOperation;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.utilities.api.ResUtil;

/**
 * MidiMix helper methods.
 */

public class MidiMixUtils
{

    private static final Logger LOGGER = Logger.getLogger(MidiMixUtils.class.getSimpleName());

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
    static public List<Integer> getChannelsNeedingDrumsRerouting(MidiMix midiMix, HashMap<Integer, Instrument> mapChannelNewIns)
    {
        Objects.requireNonNull(midiMix);

        return ((MidiMixImpl)midiMix).performReadAPImethod(() -> 
        {
            List<Integer> res = new ArrayList<>();

            var channelDrumsInsMix = midiMix.getInstrumentMix(MidiConst.CHANNEL_DRUMS);
            if (channelDrumsInsMix == null || !channelDrumsInsMix.isInstrumentEnabled())
            {
                return res;
            }

            for (RhythmVoice rv : midiMix.getRhythmVoices())
            {
                int channel = midiMix.getChannel(rv);
                InstrumentMix insMix = midiMix.getInstrumentMix(rv);
                Instrument newIns = mapChannelNewIns == null ? null : mapChannelNewIns.get(channel);
                Instrument ins = (newIns != null) ? newIns : insMix.getInstrument();
                LOGGER.log(Level.FINE, "getChannelsNeedingDrumsRerouting() rv={0} channel={1} ins={2}", new Object[]
                {
                    rv, channel, ins
                });


                if (channel != MidiConst.CHANNEL_DRUMS
                        && rv.isDrums()
                        && !midiMix.getDrumsReroutedChannels().contains(channel)
                        && ins == GMSynth.getInstance().getVoidInstrument())
                {
                    res.add(channel);
                }

            }
            LOGGER.log(Level.FINE, "getChannelsNeedingDrumsRerouting() res={0}", res);

            return res;
        });
    }


    /**
     * Check if midiMix is consistent with song.
     * <p>
     * Check that all RhythmVoices of this MidiMix belong to song rhythms. Check user tracks consistency between midiMix and song.
     *
     * @param midiMix
     * @param song
     * @param fullCheck If true also check that all song RhythmVoices are used in this MidiMix.
     * @throws org.jjazz.song.api.SongCreationException If an inconsistency is detected
     */
    static public void checkConsistency(MidiMix midiMix, Song song, boolean fullCheck) throws SongCreationException
    {
        Objects.requireNonNull(midiMix);
        Objects.requireNonNull(song);

        ((MidiMixImpl)midiMix).performReadAPImethodThrowing(() -> 
        {
            List<RhythmVoice> sgRvs = song.getSongStructure().getUniqueRhythmVoices(true, false);
            for (Integer channel : midiMix.getUsedChannels())
            {
                RhythmVoice rv = midiMix.getRhythmVoice(channel);
                if (rv instanceof UserRhythmVoice)
                {
                    // Check that we have the corresponding phrase in the song
                    Phrase p = song.getUserPhrase(rv.getName());
                    if (p == null)
                    {
                        String msg = "missing user phrase with name=" + rv.getName();
                        LOGGER.log(Level.WARNING, "checkConsistency() song={0} inconsistency found: {1}", new Object[]
                        {
                            song.getName(), msg
                        });
                        throw new SongCreationException(msg);
                    }
                } else if (!sgRvs.contains(rv))
                {
                    String msg = "MidMix rv=" + rv + " not found in song";
                    LOGGER.log(Level.WARNING, "checkConsistency() song={0} inconsistency found: {1}", new Object[]
                    {
                        song.getName(), msg
                    });
                    throw new SongCreationException(msg);
                }
            }


            if (fullCheck)
            {
                for (RhythmVoice rv : sgRvs)
                {
                    if (midiMix.getChannel(rv) == -1)
                    {
                        String msg = "song rv=" + rv + " not found in MidiMix " + midiMix.toString();
                        LOGGER.log(Level.WARNING, "checkConsistency() song={0} inconsistency found: {1}", new Object[]
                        {
                            song.getName(), msg
                        });
                        throw new SongCreationException(msg);
                    }
                }

                var rvs = midiMix.getRhythmVoices();
                for (String userPhraseName : song.getUserPhraseNames())
                {
                    if (!rvs.stream().anyMatch(rv -> rv.getName().equals(userPhraseName)))
                    {
                        String msg = "missing RhythmVoice for song user phrase " + userPhraseName;
                        LOGGER.log(Level.WARNING, "checkConsistency() song={0} inconsistency found: {1}", new Object[]
                        {
                            song.getName(), msg
                        });
                        throw new SongCreationException(msg);
                    }
                }
            }
        });
    }


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
    static public void importInstrumentMixes(MidiMix midiMixDest, MidiMix midiMixSrc)
    {
        Objects.requireNonNull(midiMixDest);
        Objects.requireNonNull(midiMixSrc);

        LOGGER.log(Level.FINE, "importInstrumentMixes() -- midiMixDest={0} midiMixSrc={1}", new Object[]
        {
            midiMixDest, midiMixSrc
        });


        Map<RhythmVoice, RhythmVoice> mapSrcDestMatchingVoices = new HashMap<>();
        var rvsSrc = midiMixSrc.getRhythmVoices();
        var userRvsSrc = midiMixSrc.getUserRhythmVoices();


        ExecutionManager executionManager = ((SongImpl) midiMixDest.getSong()).getExecutionManager();

        
        List<WriteOperation> operations = ((MidiMixImpl)midiMixDest).performReadAPImethodThrowing(() -> 
        {
            // Find the matching voices except the user phrase channels
            List<RhythmVoice> rvsDest = midiMixDest.getRhythmVoices();
            for (RhythmVoice rvSrc : rvsSrc)
            {
                boolean matched = false;

                // Try first on matching RhythmVoices from same rhythm
                for (RhythmVoice rvDest : rvsDest.toArray(RhythmVoice[]::new))
                {
                    if (!(rvDest instanceof UserRhythmVoice) && rvSrc == rvDest)
                    {
                        mapSrcDestMatchingVoices.put(rvSrc, rvDest);
                        rvsDest.remove(rvDest);
                        matched = true;
                        break;
                    }
                }
                if (!matched)
                {
                    // If no match, try any channel with the same RvType
                    for (RhythmVoice rvDest : rvsDest.toArray(RhythmVoice[]::new))
                    {
                        if (!(rvDest instanceof UserRhythmVoice) && !(rvSrc instanceof UserRhythmVoice) && rvSrc.getType().equals(rvDest.getType()))
                        {
                            mapSrcDestMatchingVoices.put(rvSrc, rvDest);
                            rvsDest.remove(rvDest);
                            matched = true;
                            break;
                        }
                    }
                }
            }

            List<WriteOperation> res = new ArrayList<>();

            // Copy the InstrumentMixes
            for (RhythmVoice rvSrc : mapSrcDestMatchingVoices.keySet())
            {
                InstrumentMix insMixSrc = midiMixSrc.getInstrumentMix(rvSrc);
                RhythmVoice rvDest = mapSrcDestMatchingVoices.get(rvSrc);
                int channel = midiMixDest.getChannel(rvDest);
                res.add(((MidiMixImpl)midiMixDest).setInstrumentMixOperation(channel, rvDest, new InstrumentMix(insMixSrc)));
            }


            // For user phrase channels, import instrument mixes only when name match
            for (var userRvSrc : userRvsSrc)
            {
                var urvDest = midiMixDest.getUserRhythmVoice(userRvSrc.getName());
                if (urvDest != null)
                {
                    InstrumentMix insMixSrc = midiMixSrc.getInstrumentMix(userRvSrc);
                    res.add(((MidiMixImpl)midiMixDest).setInstrumentMixOperation(midiMixDest.getChannel(urvDest), urvDest, new InstrumentMix(insMixSrc)));
                }
            }

            return res;
        });

        executionManager.executeWriteOperations(operations);
    }

    /**
     * Add RhythmVoices and InstrumentMixes copies from midiMixSrc to midiMixDest.
     * <p>
     *
     * @param midiMixDest
     * @param midiMixSrc
     * @param r           If non null, copy midiMixSrc instrumentMixes only if they belong to rhythm r (if r is an AdaptedRhythm, use its source rhythm).
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException If not enough channels available to accommodate mm instruments.
     */
    static public void addInstrumentMixes(MidiMix midiMixDest, MidiMix midiMixSrc, Rhythm r) throws UnsupportedEditException
    {
        Objects.requireNonNull(midiMixDest);
        Objects.requireNonNull(midiMixSrc);

        LOGGER.log(Level.FINE, "addInstrumentMixes() -- midiMixDest={0} midiMixSrc={1} r={2}", new Object[]
        {
            midiMixDest, midiMixSrc, r
        });

        ExecutionManager executionManager = ((SongImpl) midiMixDest.getSong()).getExecutionManager();


        List<WriteOperation> operations = ((MidiMixImpl)midiMixDest).performReadAPImethodThrowing(() -> 
        {
            List<Integer> usedChannelsSrc = (r == null) ? midiMixSrc.getUsedChannels() : midiMixSrc.getUsedChannels(r);
            if (midiMixDest.getUnusedChannels().size() < usedChannelsSrc.size())
            {
                throw new UnsupportedEditException(ResUtil.getString(MidiMix.class, "ERR_NotEnoughChannels"));
            }


            List<WriteOperation> res = new ArrayList<>();
            for (Integer channelSrc : usedChannelsSrc)
            {
                RhythmVoice rvSrc = midiMixSrc.getRhythmVoice(channelSrc);
                if (!(rvSrc instanceof UserRhythmVoice))
                {
                    int channelDest = midiMixDest.getUsedChannels().contains(channelSrc) ? midiMixDest.findFreeChannel(rvSrc.isDrums()) : channelSrc;
                    assert channelDest != -1;
                    InstrumentMix insMixSrc = midiMixSrc.getInstrumentMix(channelSrc);
                    res.add(((MidiMixImpl)midiMixDest).setInstrumentMixOperation(channelDest, rvSrc, new InstrumentMix(insMixSrc)));
                }
            }
            return res;
        });


        executionManager.executeWriteOperations(operations);

        LOGGER.log(Level.FINE, "addInstrumentMixes()     exit : midiMixDest={0}", midiMixDest);
    }


}

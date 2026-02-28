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
package org.jjazz.midimix;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.midimix.spi.RhythmVoiceInstrumentProvider;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.ExecutionManager;
import org.jjazz.song.SongImpl;
import org.jjazz.song.WriteOperation;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.StatusDisplayer;
import org.openide.util.lookup.ServiceProvider;

/**
 * Default implementation.
 */
@ServiceProvider(service = MidiMixManager.class)
public class MidiMixManagerImpl implements MidiMixManager, PropertyChangeListener
{

    private final Map<Song, MidiMix> mapSongMix;
    private final Map<Rhythm, MidiMix> mapRhythmMix;

    private static final Logger LOGGER = Logger.getLogger(MidiMixManagerImpl.class.getSimpleName());


    public MidiMixManagerImpl()
    {
        LOGGER.info("DefaultMidiMixManager() Started");
        this.mapSongMix = new IdentityHashMap<>();
        this.mapRhythmMix = new IdentityHashMap<>();
    }


    @Override
    public MidiMix findMix(Song s) throws UnsupportedEditException
    {
        LOGGER.log(Level.FINE, "findMix() -- s={0}", s);

        // Try to get existing MidiMix in memory
        MidiMix mm = mapSongMix.get(s);
        if (mm == null)
        {
            // No MidiMix associated with the song, need to load or create it
            File mixFile = MidiMix.getSongMixFile(s.getFile());


            if (mixFile != null && mixFile.canRead())
            {
                // Mix file exists, read it
                try
                {
                    // Try to get it from the song mix file
                    mm = MidiMix.loadFromFile(mixFile);
                    StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "LoadedSongMix", mixFile.getAbsolutePath()));

                } catch (IOException ex)
                {
                    LOGGER.log(Level.WARNING, "findMix(Song) Problem reading mix file: {0} : {1}", new Object[]
                    {
                        mixFile.getAbsolutePath(),
                        ex.getMessage()
                    });
                }

                if (mm != null)
                {
                    try
                    {
                        // Robustness - file may have been corrupted
                        checkConsistency(mm, s, true);
                    } catch (UnsupportedEditException ex)
                    {
                        LOGGER.log(Level.WARNING, "findMix(Song) song mix file: {0} not consistent with song, ignored. ex={1}", new Object[]
                        {
                            mixFile.getAbsolutePath(),
                            ex.getMessage()
                        });
                        mm = null;
                    }
                }
            }


            if (mm == null)
            {
                // If mix could not be created from file, create one from scratch
                mm = createMix(s);          // throws UnsupportedEditException
            }

            registerSong(mm, s);
        }
        return mm;
    }

    @Override
    public MidiMix findExistingMix(Song s)
    {
        MidiMix mm = mapSongMix.get(s);
        if (mm == null)
        {
            throw new IllegalStateException("No MidiMix found for s=" + s);
        }
        return mm;
    }

    @Override
    public MidiMix findMix(Rhythm r)
    {
        Objects.requireNonNull(r);
        LOGGER.log(Level.FINE, "findMix() -- r={0}", r);

        MidiMix mm = mapRhythmMix.get(r);
        if (mm == null)
        {
            File mixFile = r instanceof AdaptedRhythm ? null : MidiMix.getRhythmMixFile(r.getName(), r.getFile());

            if (mixFile != null && mixFile.canRead())
            {
                try
                {
                    mm = MidiMix.loadFromFile(mixFile);
                    StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "LoadedRhythmMix", mixFile.getAbsolutePath()));
                } catch (IOException ex)
                {
                    LOGGER.log(Level.SEVERE, "findMix(rhythm) Problem reading mix file: {0} : {1}. Creating a new mix instead.", new Object[]
                    {
                        mixFile.getAbsolutePath(),
                        ex.getMessage()
                    });
                }

                if (mm != null)
                {
                    try
                    {
                        // Robustness - file may have been corrupted
                        checkConsistency(mm, r);
                    } catch (UnsupportedEditException ex)
                    {
                        LOGGER.log(Level.WARNING, "findMix(rhythm) file: {0} not consistent with rhythm, ignored. ex={1}", new Object[]
                        {
                            mixFile.getAbsolutePath(),
                            ex.getMessage()
                        });
                        mm = null;
                    }
                }


            }
            if (mm == null)
            {
                // No valid mixFile or problem loading rhythm mix file, create a new mix
                mm = createMix(r);
            }

            mapRhythmMix.put(r, mm);
        }

        return mm;
    }

    @Override
    public MidiMix createMix(Song sg) throws UnsupportedEditException
    {
        Preconditions.checkArgument(sg instanceof SongImpl);

        LOGGER.log(Level.FINE, "createMix() -- sg={0}", sg);

        MidiMixImpl mm = new MidiMixImpl();
        for (Rhythm r : sg.getSongStructure().getUniqueRhythms(true, false))
        {
            MidiMix rMm = findMix(r);
            addInstrumentMixes(mm, rMm, null);
        }

        for (String userPhraseName : sg.getUserPhraseNames())
        {
            mm.addUserChannel(userPhraseName, sg.getUserPhrase(userPhraseName).isDrums());
        }

        checkConsistency(mm, sg, true);
        mm.setSong((SongImpl) sg);
        registerSong(mm, sg);

        return mm;

    }

    @Override
    public MidiMix createMix(Rhythm r)
    {
        Objects.requireNonNull(r);
        LOGGER.log(Level.FINE, "createMix() -- r={0}", r);

        MidiMix mm = new MidiMixImpl();
        if (!(r instanceof AdaptedRhythm))
        {
            for (RhythmVoice rv : r.getRhythmVoices())
            {
                RhythmVoiceInstrumentProvider p = RhythmVoiceInstrumentProvider.getProvider();
                Instrument ins = p.findInstrument(rv);
                assert ins != null : "rv=" + rv;
                int channel = rv.getPreferredChannel();

                if (mm.getInstrumentMix(channel) != null)
                {
                    // If 2 rhythm voices have the same preferred channel (strange...)
                    int newChannel = mm.findFreeChannel(rv.isDrums());
                    LOGGER.log(Level.WARNING, "createMix() 2 RhythmVoices have the same preferredChannel. rv={0} mm={1} channel={2}  => using free channel {3}",
                            new Object[]
                            {
                                rv, mm, channel, newChannel
                            });
                    channel = newChannel;
                    if (channel == -1)
                    {
                        throw new IllegalStateException("No Midi channel available in MidiMix. r=" + r + " rhythmVoices=" + r.getRhythmVoices());
                    }
                }

                InstrumentMix im = new InstrumentMix(ins, rv.getPreferredInstrumentSettings());
                mm.setInstrumentMix(channel, rv, im);
                LOGGER.log(Level.FINE, "createMix()    created InstrumentMix for rv={0} ins={1}", new Object[]
                {
                    rv, ins
                });
            }
        }

        return mm;
    }


    @Override
    public List<Integer> getChannelsNeedingDrumsRerouting(MidiMix midiMix, Map<Integer, Instrument> mapChannelNewIns)
    {
        Objects.requireNonNull(midiMix);

        return ((MidiMixImpl) midiMix).performReadAPImethod(() -> 
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
    @Override
    public void importInstrumentMixes(MidiMix midiMixDest, MidiMix midiMixSrc)
    {
        Preconditions.checkArgument(midiMixDest instanceof MidiMixImpl, "midiMixDest=%s", midiMixDest);
        Objects.requireNonNull(midiMixSrc);

        LOGGER.log(Level.FINE, "importInstrumentMixes() -- midiMixDest={0} midiMixSrc={1}", new Object[]
        {
            midiMixDest, midiMixSrc
        });


        Map<RhythmVoice, RhythmVoice> mapSrcDestMatchingVoices = new HashMap<>();
        var rvsSrc = midiMixSrc.getRhythmVoices();
        var userRvsSrc = midiMixSrc.getUserRhythmVoices();


        ExecutionManager executionManager = ((MidiMixImpl) midiMixDest).getExecutionManager();


        List<WriteOperation> operations = ((MidiMixImpl) midiMixDest).performReadAPImethodThrowing(() -> 
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
                res.add(((MidiMixImpl) midiMixDest).setInstrumentMixOperation(channel, rvDest, new InstrumentMix(insMixSrc)));
            }


            // For user phrase channels, import instrument mixes only when name match
            for (var userRvSrc : userRvsSrc)
            {
                var urvDest = midiMixDest.getUserRhythmVoice(userRvSrc.getName());
                if (urvDest != null)
                {
                    InstrumentMix insMixSrc = midiMixSrc.getInstrumentMix(userRvSrc);
                    res.add(((MidiMixImpl) midiMixDest).setInstrumentMixOperation(midiMixDest.getChannel(urvDest), urvDest, new InstrumentMix(insMixSrc)));
                }
            }

            return res;
        });

        executionManager.executeWriteOperations(operations);
    }


    /**
     * Build a rhythm MidiMix from a song MidiMix.
     *
     * @param songMidiMix
     * @param r
     * @return
     */
    @Override
    public MidiMix getRhythmMix(MidiMix songMidiMix, Rhythm r)
    {
        Objects.requireNonNull(songMidiMix);
        Objects.requireNonNull(r);
        Preconditions.checkArgument(songMidiMix.getUniqueRhythms().contains(r), "r=%s songMidiMix=%s", r, songMidiMix);

        MidiMix mmRhythm = new MidiMixImpl();
        try
        {
            addInstrumentMixes(mmRhythm, songMidiMix, r);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen 
            throw new IllegalStateException("songMidiMix=" + songMidiMix + " r=" + r);
        }
        return mmRhythm;
    }


    // =================================================================================
    // PropertyChangeListener methods
    // =================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() instanceof Song song)
        {
            if (e.getPropertyName().equals(Song.PROP_CLOSED))
            {
                assert mapSongMix.containsKey(song) : "song=" + song + " mapSongMix=" + mapSongMix;
                unregisterSong(song);
            }
        }
    }


    // ==================================================================
    // Private functions
    // ==================================================================
    private void registerSong(MidiMix mm, Song sg)
    {
        if (!mapSongMix.containsKey(sg))
        {
            sg.addPropertyChangeListener(Song.PROP_CLOSED, this);
        }
        mapSongMix.put(sg, mm);
    }

    private void unregisterSong(Song song)
    {
        song.removePropertyChangeListener(Song.PROP_CLOSED, this);
        mapSongMix.remove(song);
    }

    /**
     * Add RhythmVoices and InstrumentMixes copies from midiMixSrc to midiMixDest.
     * <p>
     * Method is not thread-safe and should be used only for creating a new MidiMix instance.
     *
     * @param midiMixDest
     * @param midiMixSrc
     * @param r           If non null, copy midiMixSrc instrumentMixes only if they belong to rhythm r (if r is an AdaptedRhythm, use its source rhythm).
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException If not enough channels available to accommodate mm instruments.
     */
    private void addInstrumentMixes(MidiMix midiMixDest, MidiMix midiMixSrc, Rhythm r) throws UnsupportedEditException
    {
        Objects.requireNonNull(midiMixDest);
        Objects.requireNonNull(midiMixSrc);

        LOGGER.log(Level.FINE, "addInstrumentMixes() -- midiMixDest={0} midiMixSrc={1} r={2}", new Object[]
        {
            midiMixDest, midiMixSrc, r
        });

        List<Integer> usedChannelsSrc = (r == null) ? midiMixSrc.getUsedChannels() : midiMixSrc.getUsedChannels(r);
        if (midiMixDest.getUnusedChannels().size() < usedChannelsSrc.size())
        {
            throw new UnsupportedEditException(ResUtil.getString(MidiMix.class, "ERR_NotEnoughChannels"));
        }

        for (Integer channelSrc : usedChannelsSrc)
        {
            RhythmVoice rvSrc = midiMixSrc.getRhythmVoice(channelSrc);
            if (!(rvSrc instanceof UserRhythmVoice))
            {
                int channelDest = midiMixDest.getUsedChannels().contains(channelSrc) ? midiMixDest.findFreeChannel(rvSrc.isDrums()) : channelSrc;
                assert channelDest != -1;
                InstrumentMix insMixSrc = midiMixSrc.getInstrumentMix(channelSrc);
                midiMixDest.setInstrumentMix(channelDest, rvSrc, new InstrumentMix(insMixSrc));
            }
        }

        LOGGER.log(Level.FINE, "addInstrumentMixes()     exit : midiMixDest={0}", midiMixDest);
    }

    /**
     * Check if midiMix is consistent with song.
     * <p>
     * Check that all RhythmVoices of this MidiMix belong to song rhythms. Check user tracks consistency between midiMix and song.
     *
     * @param midiMix
     * @param song
     * @param fullCheck If true also check that all song RhythmVoices are used in this MidiMix.
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException If an inconsistency is detected
     */
    private void checkConsistency(MidiMix midiMix, Song song, boolean fullCheck) throws UnsupportedEditException
    {
        Objects.requireNonNull(midiMix);
        Objects.requireNonNull(song);

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
                    throw new UnsupportedEditException(msg);
                }
            } else if (!sgRvs.contains(rv))
            {
                String msg = "MidMix rv=" + rv + " not found in song";
                LOGGER.log(Level.WARNING, "checkConsistency() song={0} inconsistency found: {1}", new Object[]
                {
                    song.getName(), msg
                });
                throw new UnsupportedEditException(msg);
            }
        }


        if (fullCheck)
        {
            for (RhythmVoice rv : sgRvs)
            {
                if (midiMix.getChannel(rv) == -1)
                {
                    String msg = "song rv=" + rv + " not found in MidiMix " + midiMix;
                    LOGGER.log(Level.WARNING, "checkConsistency() song={0} inconsistency found: {1}", new Object[]
                    {
                        song.getName(), msg
                    });
                    throw new UnsupportedEditException(msg);
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
                    throw new UnsupportedEditException(msg);
                }
            }
        }

    }

    /**
     * Check if midiMix is consistent with rhythm.
     * <p>
     * Check that all RhythmVoices of this MidiMix belong to rhythm, and vice-versa.
     *
     * @param midiMix
     * @param r
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException If an inconsistency is detected
     */
    private void checkConsistency(MidiMix midiMix, Rhythm r) throws UnsupportedEditException
    {
        Objects.requireNonNull(midiMix);
        Objects.requireNonNull(r);

        var rRvs = r.getRhythmVoices();
        var mmRvs = midiMix.getRhythmVoices();

        for (RhythmVoice rv : rRvs)
        {
            if (!mmRvs.contains(rv))
            {
                String msg = "checkConsistency() Missing RhythmVoice " + rv + " in midiMix. mmRvs=" + mmRvs;
                throw new UnsupportedEditException(msg);
            }
        }
        for (RhythmVoice rv : mmRvs)
        {
            if (!rRvs.contains(rv))
            {
                String msg = "checkConsistency() RhythmVoice " + rv + " not part of rhythm=" + r.getName() + " found in midiMix. mmRvs=" + mmRvs;
                throw new UnsupportedEditException(msg);
            }
        }
    }
}

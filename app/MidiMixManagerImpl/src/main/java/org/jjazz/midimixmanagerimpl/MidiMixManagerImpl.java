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
package org.jjazz.midimixmanagerimpl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.midimix.spi.RhythmVoiceInstrumentProvider;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.StatusDisplayer;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = MidiMixManager.class)
public class MidiMixManagerImpl implements MidiMixManager, PropertyChangeListener
{

    /**
     * Need WeakReferences: we don't want to maintain a strong reference if song is no more used.
     */
    private WeakHashMap<Song, MidiMix> mapSongMix = new WeakHashMap<>();

    private static final Logger LOGGER = Logger.getLogger(MidiMixManagerImpl.class.getSimpleName());


    public MidiMixManagerImpl()
    {
        LOGGER.info("MidiMixManagerImpl() Started");
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
    @Override
    public MidiMix findMix(Song s) throws MidiUnavailableException
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
                        // Robustness : check that mm is still valid (files might have been changed independently)
                        mm.checkConsistency(s, true);
                    } catch (SongCreationException ex)
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
                mm = createMix(s);          // throws MidiUnavailableException
            }

            mm.setSong(s);
            registerSong(mm, s);
        }
        return mm;
    }

    /**
     * Find a mix which must be existing.
     * <p>
     * If you're not sure if a MidiMix was already created for the specified song, use findMix(Song) instead.
     *
     * @param s
     * @return Can't be null
     * @throws IllegalStateException If no mix found
     */
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
    @Override
    public MidiMix findMix(Rhythm r)
    {
        Objects.requireNonNull(r);
        LOGGER.log(Level.FINE, "findMix() -- r={0}", r);
        MidiMix mm = null;
        File mixFile = r instanceof AdaptedRhythm ? null : MidiMix.getRhythmMixFile(r.getName(), r.getFile(),
                FileDirectoryManager.getInstance().getUserRhythmDirectory());
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
        }
        if (mm == null)
        {
            // No valid mixFile or problem loading rhythm mix file, create a new mix
            mm = createMix(r);
        }
        return mm;
    }

    /**
     * Create a new MidiMix for the specified song.
     * <p>
     * Use the default rhythm mix for each song's rhythm. Create UserRhythmVoices key if song as some user phrases.
     *
     * @param sg
     * @return
     * @throws MidiUnavailableException If there is not enough available channels to accomodate song's rhythms, or other errors.
     */
    @Override
    public MidiMix createMix(Song sg) throws MidiUnavailableException
    {

        LOGGER.log(Level.FINE, "createMix() -- sg={0}", sg);
        MidiMix mm = new MidiMix(sg);


        for (Rhythm r : sg.getSongStructure().getUniqueRhythms(true, false))
        {
            MidiMix rMm = findMix(r);
            mm.addInstrumentMixes(rMm, r);
        }

        for (String userPhraseName : sg.getUserPhraseNames())
        {
            mm.addUserChannel(userPhraseName);
        }


        registerSong(mm, sg);
        return mm;

    }

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
    @Override
    public MidiMix createMix(Rhythm r)
    {
        Objects.requireNonNull(r);
        LOGGER.log(Level.FINE, "createMix() -- r={0}", r);

        MidiMix mm = new MidiMix();

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
                    LOGGER.log(Level.WARNING, "createMix() 2 rhythm voices have the same preferredChannel. r={0} mm={1} channel={2}", new Object[]
                    {
                        r.getName(), mm, channel
                    });
                    channel = mm.findFreeChannel(rv.isDrums());
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

// =================================================================================
// PropertyChangeListener methods
// =================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() instanceof Song song)
        {
            assert mapSongMix.get(song) != null : "song=" + song + " mapSongMix=" + mapSongMix;
            if (e.getPropertyName().equals(Song.PROP_CLOSED))
            {
                unregisterSong(song);
            }
        }
    }

    // ==================================================================
    // Private functions
    // ==================================================================
    private void registerSong(MidiMix mm, Song sg)
    {
        if (mapSongMix.get(sg) == null)
        {
            // Do not register twice !
            sg.addPropertyChangeListener(this);
        }
        mapSongMix.put(sg, mm);
    }

    private void unregisterSong(Song song)
    {
        song.removePropertyChangeListener(this);
        mapSongMix.remove(song);
    }

}

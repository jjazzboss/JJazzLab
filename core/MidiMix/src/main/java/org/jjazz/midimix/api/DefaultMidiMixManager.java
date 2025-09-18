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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.midimix.spi.RhythmVoiceInstrumentProvider;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.StatusDisplayer;

public class DefaultMidiMixManager implements MidiMixManager, PropertyChangeListener
{

    static private DefaultMidiMixManager INSTANCE;

    static public DefaultMidiMixManager getInstance()
    {
        synchronized (DefaultMidiMixManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DefaultMidiMixManager();
            }
        }
        return INSTANCE;
    }
    /**
     * Need WeakReferences: we don't want to maintain a strong reference if song is no more used.
     */
    private final WeakHashMap<Song, MidiMix> mapSongMix = new WeakHashMap<>();

    private static final Logger LOGGER = Logger.getLogger(DefaultMidiMixManager.class.getSimpleName());


    public DefaultMidiMixManager()
    {
        LOGGER.info("DefaultMidiMixManager() Started");
    }


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

        MidiMix mm = null;
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
        }
        if (mm == null)
        {
            // No valid mixFile or problem loading rhythm mix file, create a new mix
            mm = createMix(r);
        }
        return mm;
    }

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
                    int newChannel = mm.findFreeChannel(rv.isDrums());
                    LOGGER.log(Level.WARNING, "createMix() 2 RhythmVoices have the same preferredChannel. rv={0} mm={1} channel={2}  => using free channel {3}", new Object[]
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

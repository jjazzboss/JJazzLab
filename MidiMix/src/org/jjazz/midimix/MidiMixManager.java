/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.midimix;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midimix.spi.RhythmVoiceInstrumentProvider;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongStructure;
import org.openide.awt.StatusDisplayer;

/**
 * Global instance to obtain MidiMixes for songs and rhythms.
 */
public class MidiMixManager implements PropertyChangeListener
{

    private static MidiMixManager INSTANCE;

    /**
     * Need WeakReferences: we don't want to maintain a strong reference if song is no more used.
     */
    private WeakHashMap<Song, MidiMix> mapSongMix = new WeakHashMap<>();

    private static final Logger LOGGER = Logger.getLogger(MidiMixManager.class.getSimpleName());

    public static MidiMixManager getInstance()
    {
        synchronized (MidiMixManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new MidiMixManager();
            }
        }
        return INSTANCE;
    }

    private MidiMixManager()
    {
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
    public MidiMix findMix(Song s) throws MidiUnavailableException
    {
        LOGGER.fine("findMix() -- s=" + s);
        // Try to get existing MidiMix in memory
        MidiMix mm = mapSongMix.get(s);
        if (mm == null)
        {
            // No MidiMix associated with the song, need to load or create it
            File mixFile = FileDirectoryManager.getInstance().getSongMixFile(s.getFile());
            if (mixFile != null && mixFile.canRead())
            {
                try
                {
                    // Try to get it from the song mix file
                    mm = MidiMix.loadFromFile(mixFile);
                    StatusDisplayer.getDefault().setStatusText("Loaded song mix file: " + mixFile.getAbsolutePath());
                } catch (IOException ex)
                {
                    LOGGER.warning("findMix(Song) Problem reading mix file: " + mixFile.getAbsolutePath() + " : " + ex.getLocalizedMessage());
                }
                if (mm == null)
                {
                    // Problem loading song mix file, create a new mix
                    mm = createMix(s);          // throw MidiUnavailableException
                }
            } else
            {
                // Song has never been saved, need to create from scratch
                mm = createMix(s);          // throw MidiUnavailableException
            }
            mm.setSong(s);
            registerSong(mm, s);
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
    public MidiMix findMix(Rhythm r)
    {
        LOGGER.fine("findMix() -- r=" + r);
        MidiMix mm = null;
        File mixFile = FileDirectoryManager.getInstance().getRhythmMixFile(r);
        if (mixFile != null && mixFile.canRead())
        {
            try
            {
                mm = MidiMix.loadFromFile(mixFile);
                StatusDisplayer.getDefault().setStatusText("Loaded rhythm mix file: " + mixFile.getAbsolutePath());
            } catch (IOException ex)
            {
                LOGGER.severe("findMix(rhythm) Problem reading mix file: " + mixFile.getAbsolutePath() + " : " + ex.getLocalizedMessage() + ". Creating a new mix instead.");
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
     * Use the default rhythm mix for each song's rhythm.
     *
     * @param sg
     * @return
     * @throws MidiUnavailableException If there is not enough available channels to accomodate song's rhythms, or other errors.
     */
    public MidiMix createMix(Song sg) throws MidiUnavailableException
    {
        LOGGER.fine("createMix() -- sg=" + sg);
        MidiMix mm = new MidiMix(sg);
        for (Rhythm r : SongStructure.Util.getUniqueRhythms(sg.getSongStructure()))
        {
            MidiMix rMm = findMix(r);
            mm.addInstrumentMixes(rMm, r);
        }
        registerSong(mm, sg);
        return mm;
    }

    /**
     * Create a MidiMix for the specified rhythm.
     * <p>
     * Create one InstrumentMix per rhythm voice, using rhythm voice's preferred instrument and settings, and preferred channel
     * (except if several voices share the same preferred channel).
     *
     * @param r
     * @return A MidiMix associated to this rhythm. Rhythm voices are used as keys for InstrumentMixes.
     */
    public MidiMix createMix(Rhythm r)
    {
        LOGGER.fine("createMix() -- r=" + r);
        MidiMix mm = new MidiMix();
        for (RhythmVoice rv : r.getRhythmVoices())
        {
            RhythmVoiceInstrumentProvider p = RhythmVoiceInstrumentProvider.Util.getProvider();
            Instrument ins = p.findInstrument(rv);
            assert ins != null : "rv=" + rv;
            int channel = rv.getPreferredChannel();
            if (mm.getInstrumentMixFromChannel(channel) != null)
            {
                // If 2 rhythm voices have the same preferred channel (strange...)
                LOGGER.warning("createMix() 2 rhythm voices have the same preferredChannel! mm=" + mm + " channel=" + channel);
                channel = mm.findFreeChannel(rv.isDrums());
                if (channel == -1)
                {
                    throw new IllegalStateException("No Midi channel available in MidiMix. r=" + r + " rhythmVoices=" + r.getRhythmVoices());
                }
            }
            InstrumentMix im = new InstrumentMix(ins, rv.getPreferredInstrumentSettings());
            mm.setInstrumentMix(channel, rv, im);
            LOGGER.fine("createMix()    created InstrumentMix for rv=" + rv + " ins=" + ins);
        }
        return mm;
    }

    // =================================================================================
    // PropertyChangeListener methods
    // =================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() instanceof Song)
        {
            Song song = (Song) e.getSource();
            assert mapSongMix.get(song) != null : "song=" + song + " mapSongMix=" + mapSongMix;
            if (e.getPropertyName() == Song.PROP_CLOSED)
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
        if (mapSongMix.get(sg) != mm)
        {
            mapSongMix.put(sg, mm);
            sg.addPropertyChangeListener(this);
        }
    }

    private void unregisterSong(Song song)
    {
        song.removePropertyChangeListener(this);
        mapSongMix.remove(song);
    }
}

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
package org.jjazz.song.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructureFactory;
import org.openide.util.Exceptions;

/**
 * Manage the creation and the registration of the songs.
 * <p>
 * All songs created by this factory are automatically registered. Registered songs are unregistered when song is closed.
 */
public class SongFactory implements PropertyChangeListener
{

    static private SongFactory INSTANCE;
    // Use WeakReference to avoid a memory leak if for some reason a closed song was not unregistered. Integer value is not used. 
    private final WeakHashMap<Song, Integer> songs;
    /**
     * Used to make sure we don't have the same name twice.
     */
    private static int counter = 1;

    private static final Logger LOGGER = Logger.getLogger(SongFactory.class.getSimpleName());

    static public SongFactory getInstance()
    {
        synchronized (SongFactory.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SongFactory();
            }
        }
        return INSTANCE;
    }

    private SongFactory()
    {
        songs = new WeakHashMap<>();
    }

    /**
     *
     * @return An unmodifiable set of the songs registered by this factory.
     */
    public Set<Song> getRegisteredSongs()
    {
        return Collections.unmodifiableSet(songs.keySet());
    }

    /**
     * Register a song if it was not created by this SongFactory.
     *
     * @param sg
     */
    public void registerSong(Song sg)
    {
        Objects.requireNonNull(sg);
        if (songs.put(sg, 0) == null)
        {
            sg.addPropertyChangeListener(this);
        }
    }

    /**
     * Provide a new song name which is not used by any currently opened song.
     *
     * @param baseName Can't be blank
     * @return
     */
    public String getNewSongName(String baseName)
    {
        Objects.requireNonNull(baseName);
        Preconditions.checkArgument(!baseName.isBlank(), "baseName=%s", baseName);
        String name = baseName + counter;
        while (!isSongNameUsed(name))
        {
            counter++;
            name = baseName + counter;
        }
        return name;
    }

    /**
     * Remove a song from the list returned by getRegisteredSong().
     *
     * @param song
     */
    public void unregisterSong(Song song)
    {
        Objects.requireNonNull(song);
        songs.remove(song);
        song.removePropertyChangeListener(this);
    }

    /**
     * Find in the created song the first one which uses the specified SongStructure.
     *
     * @param sgs
     * @return
     */
    public Song findSong(SongStructure sgs)
    {
        Song res = null;
        for (Song song : songs.keySet())
        {
            if (song.getSongStructure() == sgs)
            {
                res = song;
                break;
            }
        }
        return res;
    }

    /**
     * Find in the created song the first one which uses the specified ChordLeadSheet.
     *
     * @param cls
     * @return
     */
    public Song findSong(ChordLeadSheet cls)
    {
        Song res = null;
        for (Song song : songs.keySet())
        {
            if (song.getChordLeadSheet() == cls)
            {
                res = song;
                break;
            }
        }
        return res;
    }

    /**
     * Create a Song from the specified chordleadsheet.
     *
     * @param name
     * @param cls
     * @return
     * @throws UnsupportedEditException Can happen if too many timesignature changes resulting in not enough Midi channels for the various rhythms.
     */
    public Song createSong(String name, ChordLeadSheet cls) throws UnsupportedEditException
    {
        if (name == null || name.isEmpty() || cls == null)
        {
            throw new IllegalArgumentException("name=" + name + " cls=" + cls);
        }
        Song song = new Song(name, cls);
        registerSong(song);
        return song;
    }

    /**
     * Create a Song from a SongStructure and its parent ChordLeadSheet.
     *
     * @param name
     * @param sgs  sgs.getParentChordLeadSheet() must be non null
     * @return
     * @throws UnsupportedEditException Can happen if too many timesignature changes resulting in not enough Midi channels for the various rhythms.
     */
    public Song createSong(String name, SongStructure sgs) throws UnsupportedEditException
    {
        return createSong(name, sgs, false);
    }

    /**
     * Create a Song from a SongStructure and its parent ChordLeadSheet, possibly unlinked.
     *
     * @param name
     * @param sgs          sgs.getParentChordLeadSheet() must be non null
     * @param noClsSgsLink If true, there will be no automatic update between the ChordLeadSheet and the SongStructure. To be used with care only for special purposes (e.g. unit tests), as
     *                     the Song might be in an inconsistent state.
     * @return
     * @throws UnsupportedEditException Can happen if too many timesignature changes resulting in not enough Midi channels for the various rhythms.
     */
    public Song createSong(String name, SongStructure sgs, boolean noClsSgsLink) throws UnsupportedEditException
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(sgs);
        if (name.isEmpty() || sgs.getParentChordLeadSheet() == null)
        {
            throw new IllegalArgumentException("name=" + name + " sgs=" + sgs + " sgs.getParentChordLeadSheet()=" + sgs.getParentChordLeadSheet());
        }
        Song song = new Song(name, sgs, noClsSgsLink);
        registerSong(song);
        return song;
    }

    /**
     * Create a 8-bar empty song with only the 4/4 initial Section named "A" and its corresponding SongPart.
     *
     * @param songName
     * @return
     */
    public Song createEmptySong(String songName)
    {
        return createEmptySong(songName, 8, "A", TimeSignature.FOUR_FOUR, null);
    }

    /**
     * Create an empty song with the specified parameters.
     * <p>
     *
     * @param songName        The name of the song
     * @param nbBars
     * @param initSectionName The name of the initial section
     * @param ts              The time signature of the initial section
     * @param initialChord    eg "Cm7". A string describing an initial chord to be put at the start of the song. If null no chord is inserted.
     * @return
     */
    public Song createEmptySong(String songName, int nbBars, String initSectionName, TimeSignature ts, String initialChord)
    {
        Objects.requireNonNull(songName);
        Objects.requireNonNull(initSectionName);
        Objects.requireNonNull(ts);
        Preconditions.checkArgument(nbBars > 0, "nbBars=%s", nbBars);

        ChordLeadSheetFactory clsf = ChordLeadSheetFactory.getDefault();
        ChordLeadSheet cls = clsf.createEmptyLeadSheet(initSectionName, ts, nbBars, initialChord);
        Song song = null;
        try
        {
            song = new Song(songName, cls);
        } catch (UnsupportedEditException ex)
        {
            // We should not be here
            throw new IllegalStateException("Unexpected 'UnsupportedEditException'.", ex);
        }
        int tempo = song.getSongStructure().getSongPart(0).getRhythm().getPreferredTempo();
        song.setTempo(tempo);
        song.setSaveNeeded(false);
        registerSong(song);
        return song;
    }

    public boolean isSongNameUsed(String name)
    {
        boolean b = true;
        for (Song sg : getRegisteredSongs())
        {
            if (sg.getName().equals(name))
            {
                b = false;
                break;
            }
        }
        return b;
    }

    /**
     * Return a deep copy of the specified song.
     * <p>
     * Listeners or file are NOT copied. Returned song is not closed, even if the original song was.
     *
     * @param song
     * @param noClsSgsLink If true, updating the chord leadsheet will not update the song structure
     * @param register If true register the created song
     * @return
     */
    @SuppressWarnings(
            {
                "unchecked"
            })
    public Song getCopy(Song song, boolean noClsSgsLink, boolean register)
    {
       Song res = song.getDeepCopy(noClsSgsLink);
        if (register)
        {
            registerSong(res);
        }
        return res;
    }

    // =================================================================================
    // PropertyChangeListener methods
    // =================================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() instanceof Song sg)
        {
            assert songs.keySet().contains(sg) : "song=" + sg + " songs=" + songs.keySet();
            if (e.getPropertyName().equals(Song.PROP_CLOSED))
            {
                unregisterSong(sg);
            }
        }
    }

    // =================================================================================
    // Private methods
    // =================================================================================
}

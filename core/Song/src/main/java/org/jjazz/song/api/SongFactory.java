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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.songstructure.SongStructureImpl;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;

/**
 * Manage the creation of Song and related components.
 * <p>
 */
public class SongFactory
{

    static private SongFactory INSTANCE;

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
    }


    /**
     * Create a Song from the specified ChordLeadSheet.
     * <p>
     * The SongStructure is created from the ChordLeadSheet.
     *
     * @param name
     * @param cls
     * @return
     * @throws UnsupportedEditException Can happen if too many timesignature changes resulting in not enough Midi channels for the various rhythms.
     */
    public Song createSong(String name, ChordLeadSheet cls) throws UnsupportedEditException
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(cls);
        Preconditions.checkArgument(!name.isBlank());
        var sgs = createSongStructure(cls);
        Song song = new Song(name, sgs, false);
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
     * Create a Song from a SongStructure and its parent ChordLeadSheet.
     *
     * @param name
     * @param sgs                        sgs.getParentChordLeadSheet() must be non null
     * @param disableSongInternalUpdates If true the returned instance will have internal consistency updates disabled, e.g. changing a section in the
     *                                   ChordLeadSheet won't impact the SongStructure. For special purposes only, this can lead to inconsistent Song states..
     * @return
     * @throws UnsupportedEditException Can happen if too many timesignature changes resulting in not enough Midi channels for the various rhythms.
     */
    public Song createSong(String name, SongStructure sgs, boolean disableSongInternalUpdates) throws UnsupportedEditException
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(sgs);
        Preconditions.checkArgument(!name.isBlank());
        Song song = new Song(name, sgs, disableSongInternalUpdates);
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


        Song song = null;
        try
        {
            ChordLeadSheet cls = createEmptyChordLeadSheet(initSectionName, ts, nbBars, initialChord);
            var sgs = createSongStructure(cls);
            song = new Song(songName, sgs, false);
        } catch (UnsupportedEditException ex)
        {
            // We should not be here
            throw new IllegalStateException("Unexpected 'UnsupportedEditException'.", ex);
        }
        int tempo = song.getSongStructure().getSongPart(0).getRhythm().getPreferredTempo();
        song.setTempo(tempo);
        song.setSaveNeeded(false);
        return song;
    }


    /**
     * Create an empty chord leadsheet with the specified parameters.
     *
     * @param sectionName
     * @param ts
     * @param size
     * @param initialChord e.g. "C7". If null no initial chord is added.
     * @return
     */
    public ChordLeadSheet createEmptyChordLeadSheet(String sectionName, TimeSignature ts, int size, String initialChord)
    {
        ChordLeadSheet cls = new ChordLeadSheetImpl(sectionName, ts, size);
        CLI_Factory clif = CLI_Factory.getDefault();
        if (initialChord != null)
        {
            try
            {
                var ecs = ExtChordSymbol.get(initialChord);
                cls.addItem(clif.createChordSymbol(ecs, new Position(0)));
            } catch (ParseException ex)
            {
                LOGGER.log(Level.WARNING, "createEmptyChordLeadSheet() Invalid initialChord={0}, ignored", initialChord);
            }

        }
        return cls;
    }

    public ChordLeadSheet createSampleChordLeadSheet12bars(String sectionName, int size)
    {
        if (size < 12)
        {
            throw new IllegalArgumentException("size=" + size);
        }
        ChordLeadSheet cls = new ChordLeadSheetImpl(sectionName, TimeSignature.FOUR_FOUR, size);
        CLI_Factory clif = CLI_Factory.getDefault();
        try
        {
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Dm7"), 0, 0));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("C-7"), 0, 2f));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("F#7"), 1, 0));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Bbmaj7#5"), 1, 2));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("A"), 1, 3f));
            cls.addSection(clif.createSection("Chorus", TimeSignature.THREE_FOUR, 2, null));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("D7b9b5"), 2, 1f));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("FM7#11"), 4, 3f));
            cls.addSection(clif.createSection("Bridge", TimeSignature.FOUR_FOUR, 5, null));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Eb7b9#5"), 5, 0.75f));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Ab7#11"), 6, 0));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("A#"), 6, 4f));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("F7alt"), 6, 1.5f));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("G7#9#5"), 7, 2f));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("G#7dim"), 8, 0f));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Dbmaj7"), 8, 2));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("Gbmaj7"), 9, 0));
            cls.addItem(clif.createChordSymbol(ExtChordSymbol.get("G#maj7"), 11, 3f));
        } catch (ParseException | UnsupportedEditException ex)
        {
            String msg = "Error creating sample leadsheet.\n" + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, "createSampleChordLeadSheet12bars() {0}", msg);
            throw new IllegalStateException(msg);
        }
        return cls;
    }

    public ChordLeadSheet createRamdomChordLeadSheet(String sectionName, TimeSignature ts, int size)
    {
        int sectionId = 0;
        if (size < 1)
        {
            throw new IllegalArgumentException("size=" + size);
        }
        CLI_Factory clif = CLI_Factory.getDefault();
        ChordLeadSheet cls = new ChordLeadSheetImpl(sectionName, ts, size);
        for (int i = 0; i < size; i++)
        {
            ExtChordSymbol cs = ExtChordSymbol.createRandomChordSymbol();
            CLI_ChordSymbol cli = clif.createChordSymbol(cs, i, 0);
            cls.addItem(cli);
            if (Math.random() > .8f)
            {
                cs = ExtChordSymbol.createRandomChordSymbol();
                cli = clif.createChordSymbol(cs, i, 2);
                cls.addItem(cli);
            }
            if (i > 0 && Math.random() > .9f)
            {
                TimeSignature ts2 = TimeSignature.FOUR_FOUR;
                if (Math.random() > 0.8f)
                {
                    ts2 = TimeSignature.THREE_FOUR;
                }
                CLI_Section section = clif.createSection(sectionName + sectionId++, ts2, i, null);
                try
                {
                    cls.addSection(section);
                } catch (UnsupportedEditException ex)
                {
                    // Do nothing, section is not added but it's not a problem as it's a random thing
                }
            }
        }
        return cls;
    }


    /**
     * Create a SongStructure with cls as parentChordLeadSheet.
     * <p>
     * One SongPart is created for each cls section. <br>
     *
     * @param cls
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    public SongStructure createSongStructure(ChordLeadSheet cls) throws UnsupportedEditException
    {
        Objects.requireNonNull(cls);

        SongStructureImpl sgs = new SongStructureImpl(cls);

        var rdb = RhythmDatabase.getDefault();

        var newSpts = new ArrayList<SongPart>();
        for (var section : cls.getItems(CLI_Section.class))
        {
            int sptBarIndex = section.getPosition().getBar();


            Rhythm r = null;
            RhythmInfo ri = null;
            try
            {
                ri = rdb.getDefaultRhythm(section.getData().getTimeSignature());
                r = rdb.getRhythmInstance(ri);
            } catch (UnavailableRhythmException ex)
            {
                // Might happen if file deleted
                LOGGER.log(Level.WARNING, "createSgs() Can''t get rhythm instance for {0}. Using stub rhythm instead. ex={1}", new Object[]
                {
                    ri.name(),
                    ex.getMessage()
                });
                r = rdb.getDefaultStubRhythmInstance(section.getData().getTimeSignature());  // non null
            }

            SongPart spt = sgs.createSongPart(
                    r,
                    section.getData().getName(),
                    sptBarIndex,
                    section,
                    false);
            newSpts.add(spt);
        }

        // Add new song parts in one shot to avoid issue if an AdaptedRhythm is used      
        sgs.addSongParts(newSpts);      // throws UnsupportedEditException


        return sgs;
    }


    // =================================================================================
    // Private methods
    // =================================================================================
}

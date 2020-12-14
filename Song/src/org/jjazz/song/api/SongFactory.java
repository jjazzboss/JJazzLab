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
package org.jjazz.song.api;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructureFactory;
import org.jjazz.util.Utilities;
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
    private WeakHashMap<Song, Integer> songs;
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
     * All songs created by this object are automatically registered.
     *
     *
     * @return A list of the songs registered by this object.
     */
    public List<Song> getRegisteredSongs()
    {
        return new ArrayList<>(songs.keySet());
    }

    /**
     * Register a song if it was not created by the SongManager.
     *
     * @param sg
     */
    public void registerSong(Song sg)
    {
        if (!songs.keySet().contains(sg))
        {
            songs.put(sg, 0);
            sg.addPropertyChangeListener(this);
        }
    }

    /**
     * Provide a new song name which is not used by any currently opened song.
     *
     * @return
     */
    public String getNewSongName()
    {
        String name = "NewSong" + counter;
        while (!isSongNameUsed(name))
        {
            counter++;
            name = "NewSong" + counter;
        }
        return name;
    }

    /**
     * Get a Song object from a file.
     * <p>
     * Song's getFile() will return f. <br>
     * Song's getName() will return f.getName(). <br>
     *
     * @param f
     * @return
     * @throws org.jjazz.song.api.SongCreationException
     */
    public Song createFromFile(File f) throws SongCreationException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);   //NOI18N
        }
        Song song = null;


        XStream xstream = Utilities.getSecuredXStreamInstance();


        // Read file
        try (var fis = new FileInputStream(f))
        {
            Reader r = new BufferedReader(new InputStreamReader(fis, "UTF-8"));        // Needed to support special/accented chars
            song = (Song) xstream.fromXML(r);
        } catch (XStreamException | IOException e)
        {
            throw new SongCreationException(e);
        }

        // Update song
        song.setFile(f);
        song.setName(Song.removeSongExtension(f.getName()));
        song.resetNeedSave();
        registerSong(song);


        return song;
    }

    /**
     * Remove a song from the list returned by getRegisteredSong().
     *
     * @param song
     */
    public void unregisterSong(Song song)
    {
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
     * @throws UnsupportedEditException Can happen if too many timesignature changes resulting in not enough Midi channels for the
     * various rhythms.
     */
    public Song createSong(String name, ChordLeadSheet cls) throws UnsupportedEditException
    {
        if (name == null || name.isEmpty() || cls == null)
        {
            throw new IllegalArgumentException("name=" + name + " cls=" + cls);   //NOI18N
        }
        Song song = new Song(name, cls);
        registerSong(song);
        return song;
    }

    /**
     * Create a 8-bar empty song.
     *
     * @param name
     * @return
     */
    public Song createEmptySong(String name)
    {
        return createEmptySong(name, 8);
    }

    /**
     * Create an empty song of specified length.
     * <p>
     * Initial section is "A" with a C starting chord symbol.
     *
     * @param name The name of the song
     * @param clsSize The number of bars of the song.
     * @return
     */
    public Song createEmptySong(String name, int clsSize)
    {
        if (name == null || name.isEmpty() || clsSize < 1)
        {
            throw new IllegalArgumentException("name=" + name + " clsSize=" + clsSize);   //NOI18N
        }
        ChordLeadSheetFactory clsf = ChordLeadSheetFactory.getDefault();
        ChordLeadSheet cls = clsf.createEmptyLeadSheet("A", TimeSignature.FOUR_FOUR, clsSize);
        Song song = null;
        try
        {
            song = new Song(name, cls);
        } catch (UnsupportedEditException ex)
        {
            // We should not be here
            throw new IllegalStateException("Unexpected 'UnsupportedEditException'.", ex);   //NOI18N
        }
        int tempo = song.getSongStructure().getSongPart(0).getRhythm().getPreferredTempo();
        song.setTempo(tempo);
        song.resetNeedSave();
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
     * Copy only the following variables: chordleadsheet, songStructure, name, tempo, comments, tags<br>
     * Listeners or file are NOT copied. Created song is registered.
     *
     * @param song
     * @return
     */
    @SuppressWarnings(
            {
                "unchecked"
            })
    public Song getCopy(Song song)
    {
        if (song == null)
        {
            throw new IllegalArgumentException("song");   //NOI18N
        }
        ChordLeadSheetFactory clsf = ChordLeadSheetFactory.getDefault();
        ChordLeadSheet newCls = clsf.getCopy(song.getChordLeadSheet());

        Song s = null;
        try
        {
            s = new Song(song.getName(), newCls);       // SongStructure and ChordLeadsheet will be linked
        } catch (UnsupportedEditException ex)
        {
            // Should not occur since it's a clone, ie already accepted edits
            throw new IllegalArgumentException("clone() failed. Song's name=" + song.getName(), ex);   //NOI18N
        }
        s.setComments(song.getComments());
        s.setTempo(song.getTempo());
        s.setTags(song.getTags());

        // Clean the default songStructure
        SongStructure newSgs = s.getSongStructure();
        try
        {
            newSgs.removeSongParts(newSgs.getSongParts());
        } catch (UnsupportedEditException ex)
        {
            // Should not happen since it's a copy
            Exceptions.printStackTrace(ex);
        }

        // Recreate each SongPart copy
        var newSpts = new ArrayList<SongPart>();
        for (SongPart spt : song.getSongStructure().getSongParts())
        {
            CLI_Section newParentSection = newCls.getSection(spt.getParentSection().getData().getName());
            assert newParentSection != null : "spt=" + spt;   //NOI18N
            SongPart sptCopy = spt.clone(spt.getRhythm(), spt.getStartBarIndex(), spt.getNbBars(), newParentSection);
            newSpts.add(sptCopy);
        }
        // Add new song parts in one shot to avoid issue if an AdaptedRhythm is used
        try
        {
            newSgs.addSongParts(newSpts);            // Can raise UnsupportedEditException
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            throw new IllegalArgumentException("getCopy() failed. Song's name=" + song.getName() + " newSgs=" + newSgs + " newSpts=" + newSpts, ex);   //NOI18N
        }


        s.resetNeedSave();
        registerSong(s);
        return s;
    }

    /**
     * Return a copy of the song where the SongStructure does NOT listen to the ChordLeadsheet changes.
     * <p>
     * WARNING: Because SongStructure and ChordLeadsheet are not linked, changing them might result in inconsistent states. This
     * should be used only in special cases.<p>
     * Copy the following variables: chordleadsheet, songStructure, name, tempo, comments, tags. Listeners or file are NOT copied.
     * Created song is registered.
     *
     * @param song
     * @return
     */
    @SuppressWarnings(
            {
                "unchecked"
            })
    public Song getCopyUnlinked(Song song)
    {
        if (song == null)
        {
            throw new IllegalArgumentException("song");   //NOI18N
        }
        ChordLeadSheet cls = ChordLeadSheetFactory.getDefault().getCopy(song.getChordLeadSheet());
        SongStructure ss = null;
        try
        {
            ss = SongStructureFactory.getDefault().createSgs(cls, false);     // Don't link sgs to cls.  Can raise UnsupportedEditException
            ss.removeSongParts(ss.getSongParts());


            // Get a copy for each SongPart
            var newSpts = new ArrayList<SongPart>();
            for (SongPart spt : song.getSongStructure().getSongParts())
            {
                String parentSectionName = spt.getParentSection().getData().getName();
                CLI_Section parentSectionCopy = cls.getSection(parentSectionName);
                SongPart sptCopy = spt.clone(spt.getRhythm(), spt.getStartBarIndex(), spt.getNbBars(), parentSectionCopy);
                newSpts.add(sptCopy);
            }


            // Add new song parts in one shot to avoid issue if an AdaptedRhythm is used      
            ss.addSongParts(newSpts);            // Can raise UnsupportedEditException     
        } catch (UnsupportedEditException ex)
        {
            throw new IllegalArgumentException("getCopyUnlinked() failed. Song's name=" + song.getName() + " ss=" + ss, ex);   //NOI18N
        }

        // Now create the song copy
        Song s = new Song(song.getName(), cls, ss);
        s.setComments(song.getComments());
        s.setTempo(song.getTempo());
        s.setTags(song.getTags());

        s.resetNeedSave();
        registerSong(s);
        return s;
    }

    /**
     * Get a new song with the lead sheet developped/unrolled according to the song structure.
     * <p>
     * Return song where each SongPart corresponds to one Section in a linear order. Created song is registered.
     *
     * @param song
     * @return
     */
    public Song getDeveloppedLeadSheet(Song song)
    {
        if (song == null)
        {
            throw new IllegalArgumentException("song");   //NOI18N
        }


        var cls = song.getChordLeadSheet();
        var ss = song.getSongStructure();
        if (ss.getSongParts().isEmpty())
        {
            // Special case
            return getCopy(song);
        }


        // Create an empty song with the right size
        var resSong = createEmptySong(song.getName(), ss.getSizeInBars());
        var resCls = resSong.getChordLeadSheet();
        for (var cliCs : resCls.getItems(CLI_ChordSymbol.class))
        {
            resCls.removeItem(cliCs);
        }
        var resSs = resSong.getSongStructure();
        try
        {
            resSs.removeSongParts(resSs.getSongParts());
        } catch (UnsupportedEditException ex)
        {
            // Should never happen as we remove everything
            Exceptions.printStackTrace(ex);
        }


        // The created song parts
        List<SongPart> newSpts = new ArrayList<>();


        // Fill it from the original song data
        for (SongPart spt : ss.getSongParts())
        {
            var parentCliSection = spt.getParentSection();
            int barIndex = spt.getStartBarIndex();


            CLI_Section resCliSection;


            // Update the initial section or create the corresponding parent section
            if (barIndex == 0)
            {
                resCliSection = resCls.getSection(0);
                resCls.setSectionName(resCliSection, parentCliSection.getData().getName());
                try
                {
                    resCls.setSectionTimeSignature(resCliSection, parentCliSection.getData().getTimeSignature());
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen since we copy a valid song
                    Exceptions.printStackTrace(ex);
                }
            } else
            {
                // Create it
                String name = CLI_Section.Util.createSectionName(parentCliSection.getData().getName(), resCls);
                resCliSection = CLI_Factory.getDefault().createSection(resCls, name, parentCliSection.getData().getTimeSignature(), barIndex);
                try
                {
                    resCls.addSection(resCliSection);
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen since we copy a valid song
                    Exceptions.printStackTrace(ex);
                }
            }


            // Fill the corresponding section with chord symbols copies
            for (CLI_ChordSymbol cliCs : cls.getItems(parentCliSection, CLI_ChordSymbol.class))
            {
                var pos = cliCs.getPosition();
                int resBar = barIndex + pos.getBar() - parentCliSection.getPosition().getBar();
                var cliCsCopy = cliCs.getCopy(resCls, new Position(resBar, pos.getBeat()));
                resCls.addItem(cliCsCopy);
            }


            // Create the corresponding SongPart
            SongPart resSpt = spt.clone(null, barIndex, spt.getNbBars(), resCliSection);
            newSpts.add(resSpt);

        }


        // Add all SongParts in one shot to avoid problem with AdaptedRhythms
        try
        {
            resSs.addSongParts(newSpts);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen since copy of existing song
            Exceptions.printStackTrace(ex);
        }


        registerSong(resSong);
        return resSong;
    }

    /**
     * Get a new song with a simplified lead sheet.
     * <p>
     * <p>
     * Created song is registered.
     *
     * @param song
     * @return
     * @see ChordLeadSheetFactory#getSimplified(ChordLeadSheet)
     */
    public Song getSimplifiedLeadSheet(Song song)
    {
        if (song == null)
        {
            throw new IllegalArgumentException("song");   //NOI18N
        }

        // Create a full copy to preserve links between SongParts and Sections
        Song resSong = getCopy(song);
        ChordLeadSheet resCls = resSong.getChordLeadSheet();


        // Get a working simplified copy and use it to update the new leadsheet
        ChordLeadSheet simplifiedCls = ChordLeadSheetFactory.getDefault().getSimplified(song.getChordLeadSheet());


        // Remove all chord symbols 
        for (var item : resCls.getItems(CLI_ChordSymbol.class))
        {
            resCls.removeItem(item);
        }

        // Copy chord symboles from the simplified cls
        for (var item : simplifiedCls.getItems(CLI_ChordSymbol.class))
        {
            resCls.addItem(item);
        }


        simplifiedCls.cleanup();

        registerSong(resSong);

        return resSong;
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
            assert songs.keySet().contains(song) : "song=" + song + " songs=" + songs.keySet();   //NOI18N
            if (e.getPropertyName() == Song.PROP_CLOSED)
            {
                unregisterSong(song);
            }
        }
    }

    // =================================================================================
    // Private methods
    // =================================================================================
}

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
package org.jjazz.song.spi;

import java.io.File;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.songstructure.api.SongStructure;
import org.openide.util.Lookup;

/**
 * Manage the creation of Song and related components.
 * <p>
 */
public interface SongFactory
{

    public static SongFactory getDefault()
    {
        var res = Lookup.getDefault().lookup(SongFactory.class);
        if (res == null)
        {
            throw new IllegalStateException("No SongFactory implementation found");
        }
        return res;
    }

    /**
     * Load a Song from a file.
     * <p>
     * Song's getFile() will return f. <br>
     * Song's getName() will return f.getName(). <br>
     * <p>
     *
     * @param f
     * @return
     * @throws org.jjazz.song.api.SongCreationException
     */
    Song loadFromFile(File f) throws SongCreationException;

    /**
     * Create an empty chord leadsheet with the specified parameters.
     *
     * @param sectionName
     * @param ts
     * @param size
     * @param initialChord e.g. "C7". If null no initial chord is added.
     * @return
     */
    ChordLeadSheet createEmptyChordLeadSheet(String sectionName, TimeSignature ts, int size, String initialChord);

    /**
     * Create a 8-bar empty song with only the 4/4 initial Section named "A" and its corresponding SongPart.
     *
     * @param songName
     * @return
     */
    Song createEmptySong(String songName);

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
    Song createEmptySong(String songName, int nbBars, String initSectionName, TimeSignature ts, String initialChord);

    /**
     * Create a ChordLeadSheet with random chord symbols and sections.
     *
     * @param sectionName
     * @param ts
     * @param size
     * @return
     */
    ChordLeadSheet createRamdomChordLeadSheet(String sectionName, TimeSignature ts, int size);

    /**
     * Create a ChordLeadSheet with a fixed set of sections and chord symbols in the first 12 bars.
     *
     * @param sectionName
     * @param size        Must be &gt;= 12
     * @return
     */
    ChordLeadSheet createSampleChordLeadSheet(String sectionName, int size);

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
    Song createSong(String name, ChordLeadSheet cls) throws UnsupportedEditException;

    /**
     * Create a Song from a SongStructure and its parent ChordLeadSheet.
     *
     * @param name
     * @param sgs  sgs.getParentChordLeadSheet() must be non null
     * @return
     * @throws UnsupportedEditException Can happen if too many timesignature changes resulting in not enough Midi channels for the various rhythms.
     */
    Song createSong(String name, SongStructure sgs) throws UnsupportedEditException;

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
    Song createSong(String name, SongStructure sgs, boolean disableSongInternalUpdates) throws UnsupportedEditException;

    /**
     * Create a SongStructure with cls as parentChordLeadSheet.
     * <p>
     * One SongPart is created for each cls section. <br>
     *
     * @param cls
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    SongStructure createSongStructure(ChordLeadSheet cls) throws UnsupportedEditException;

}

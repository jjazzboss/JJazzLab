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
package org.jjazz.importers;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Logger;
import nu.xom.ParsingException;
import org.jjazz.harmony.ChordSymbol;
import org.jjazz.harmony.ChordType;
import org.jjazz.harmony.ChordTypeDatabase;
import org.jjazz.harmony.Note;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.importers.jfugue.MusicXmlParser;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;

/**
 * MusicXML leadsheet file reader.
 */
public class MusicXMLFileReader
{

    private File file;
    private static final HashMap<String, String> mapChordTypeConversion = new HashMap<>();  // Map special improvisor chordtypes to JJazz ChordTypes
    private static final Logger LOGGER = Logger.getLogger(MusicXMLFileReader.class.getSimpleName());

    public MusicXMLFileReader(File f)
    {
        if (f == null)
        {
            throw new NullPointerException("f");
        }
        this.initMap();
        this.file = f;
    }

    /**
     * Get the song from the current file.
     * <p>
     * Construct a basic Song from the elements available in the file.
     *
     * @return
     * @throws IOException
     */
    public Song readSong() throws IOException
    {
        MusicXmlParser parser = new MusicXmlParser();
        parser.addParserListener(new MusicXMLParserListener());
        try
        {
            parser.parse(file);
        } catch (ParsingException ex)
        {
            throw new IOException(ex);
        }
        if (true)
        {
            throw new IOException("YEAAH============");
        }
        String title = "No Title";
        String composer = null;
        int tempo = 120;
        TimeSignature ts = TimeSignature.FOUR_FOUR;
        ChordLeadSheet cls = null;

        // Create the song object from the collected data
        SongFactory sf = SongFactory.getInstance();
        Song song = null;
        try
        {
            song = sf.createSong(title, cls);  // Throw exception        
        } catch (UnsupportedEditException ex)
        {
            throw new IOException(ex);
        }
        song.setTempo(tempo);
        if (composer != null && composer.isEmpty())
        {
            song.setComments("Composer: " + composer);
        }
        return song;
    }

    // ==========================================================================================
    // Private methods
    // ==========================================================================================
    /**
     * Convert a Improvisor chord symbol string into a JJazzLab chord symbol.
     *
     * @param token
     * @return Null if conversion failed.
     */
    private ChordSymbol getChordSymbol(String token)
    {
        if (mapChordTypeConversion.isEmpty())
        {
            initMap();
        }
        ChordSymbol cs = null;
        try
        {
            Note bassNote = null;
            String str = token;

            // Remove rare but possible poly chord like "E\FM"
            int backslashIndex = token.indexOf(String.valueOf('\\'));
            if (backslashIndex != -1)
            {
                str = str.substring(backslashIndex + 1, str.length());
            }

            // Process possible bass note            
            int slashIndex = token.indexOf("/");
            if (slashIndex != -1)
            {
                String strBassNote = token.substring(slashIndex + 1, token.length());
                bassNote = new Note(strBassNote);
                str = token.substring(0, slashIndex);
            }

            // Bass note has been stripped from token
            String strRootNote;
            String strChordType;
            boolean alteration = str.length() > 1 && (str.charAt(1) == 'b' || str.charAt(1) == '#');
            if (alteration)
            {
                strRootNote = str.substring(0, 2);
                strChordType = str.substring(2, str.length());
            } else
            {
                strRootNote = str.substring(0, 1);
                strChordType = str.substring(1, str.length());
            }

            // Build root note
            Note rootNote = new Note(strRootNote);

            // Build chordtype : should we replace the Improvisor chordtype string by a JJazzLab compatible one?             
            String strJJazzCt = mapChordTypeConversion.get(strChordType);  // yes by default
            if (strJJazzCt == null)
            {
                // No, we can directly use the original chord type string
                strJJazzCt = strChordType;
            }
            ChordType ct = ChordTypeDatabase.getInstance().getChordType(strJJazzCt);
            if (ct == null)
            {
                throw new ParseException("Chord type '" + strJJazzCt + "' not recognized ", 0);
            }

            // Create the chord symbol eventually
            cs = new ChordSymbol(rootNote, bassNote, ct);

        } catch (ParseException ex)
        {
            LOGGER.warning("getChordSymbol() can't convert token=" + token + ". ex=" + ex.getLocalizedMessage());
        }

        return cs;
    }

    private void initMap()
    {
        if (!mapChordTypeConversion.isEmpty())
        {
            return;
        }
        // Put in the map only Improvisor chord symbols for which we don't want to add an ChordType alias 
        // because chordtype is too exotic, eg '7b9b13sus4' !!!!
        mapChordTypeConversion.put("phryg", "m7");
        mapChordTypeConversion.put("m11#5", "m11");
        mapChordTypeConversion.put("m7#5", "m7");
        mapChordTypeConversion.put("m9#5", "m9");
        mapChordTypeConversion.put("6b5", "13#11");
        mapChordTypeConversion.put("mM7b6", "m7M");
        mapChordTypeConversion.put("mb6", "m");
        mapChordTypeConversion.put("mb6M7", "m7M");
        mapChordTypeConversion.put("mb6b9", "m");
        mapChordTypeConversion.put("M#5add9", "M7#5");
        mapChordTypeConversion.put("M7+", "7M");
        mapChordTypeConversion.put("M9#5", "M7#5");
        mapChordTypeConversion.put("M69#11", "M9#11");
        mapChordTypeConversion.put("M7#9#11", "M9#11");
        mapChordTypeConversion.put("6#11", "M7#11");
        mapChordTypeConversion.put("+add9", "+");
        mapChordTypeConversion.put("add9no3", "2");
        mapChordTypeConversion.put("addb9", "7b9b5");
        mapChordTypeConversion.put("7b13", "7");
        mapChordTypeConversion.put("7b5#9", "7alt");
        mapChordTypeConversion.put("7b5b13", "7b5");
        mapChordTypeConversion.put("7b5b9b13", "7b9b5");
        mapChordTypeConversion.put("7b6", "7#5");
        mapChordTypeConversion.put("+add#9", "7#9#5");
        mapChordTypeConversion.put("7b9#11b13", "7b9b5");
        mapChordTypeConversion.put("7b9b13#11", "7b9b5");
        mapChordTypeConversion.put("7b9b13", "7b9");
        mapChordTypeConversion.put("7no5", "7");
        mapChordTypeConversion.put("7#11b13", "7#11");
        mapChordTypeConversion.put("7#5b9#11", "7#5b9");
        mapChordTypeConversion.put("7#9#11b13", "7#9#11");
        mapChordTypeConversion.put("7#9b13", "7#9");
        mapChordTypeConversion.put("9#11b13", "9#11");
        mapChordTypeConversion.put("9#5#11", "9#11");
        mapChordTypeConversion.put("9b13", "9#11");
        mapChordTypeConversion.put("9b5b13", "9b5");
        mapChordTypeConversion.put("9no5", "9");
        mapChordTypeConversion.put("13#9#11", "13#11");
        mapChordTypeConversion.put("Msus2", "sus");
        mapChordTypeConversion.put("Msus4", "sus");
        mapChordTypeConversion.put("sus2", "sus");
        mapChordTypeConversion.put("sus24", "sus");
        mapChordTypeConversion.put("susb9", "7susb9");
        mapChordTypeConversion.put("7b9b13sus4", "7susb9");
        mapChordTypeConversion.put("7sus4b9b13", "7susb9");
    }

}

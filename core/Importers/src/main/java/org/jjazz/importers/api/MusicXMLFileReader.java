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
package org.jjazz.importers.api;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import nu.xom.ParsingException;
import org.jjazz.importers.musicxml.MusicXmlParser;
import org.jjazz.importers.musicxml.SongBuilder;
import org.jjazz.song.api.Song;
import org.xml.sax.SAXException;

/**
 * MusicXML leadsheet file reader.
 */
public class MusicXMLFileReader
{

    private File file;
    private String musicalStyle;
    private int tempo;
    private static final Logger LOGGER = Logger.getLogger(MusicXMLFileReader.class.getSimpleName());

    /**
     *
     * @param f The input file
     */
    public MusicXMLFileReader(File f)
    {
        if (f == null)
        {
            throw new NullPointerException("f");
        }
        this.file = f;
    }

    /**
     * An optional String indicating musical style.
     *
     * @return Can be empty if no musical style defined
     */
    public String getMusicalStyle()
    {
        return musicalStyle == null ? "" : musicalStyle;
    }

    /**
     * -1 if tempo was not set in the musicXML file.
     *
     * @return
     */
    public int getTempo()
    {
        return tempo;
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
        var songBuilder = new SongBuilder();
        parser.addParserListener(songBuilder);


        try
        {
            parser.parse(file);
        } catch (ParsingException | ParserConfigurationException | SAXException ex)
        {
            throw new IOException(ex);
        }


        Song song = songBuilder.getSong();
        if (song == null)
        {
            throw new IOException("Error creating imported song. Please check log file.");
        }
        musicalStyle = songBuilder.getMusicalStyle();
        tempo = songBuilder.getTempo();


        // Set name
        int dotIndex = file.getName().lastIndexOf('.');
        String name = dotIndex >= 0 ? file.getName().substring(0, dotIndex) : file.getName();
        song.setName(name);


        return song;
    }

}

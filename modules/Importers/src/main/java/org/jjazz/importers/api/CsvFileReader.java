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

import com.google.common.base.Preconditions;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.chordleadsheet.api.item.Position;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;

/**
 * Read chord symbols from a .csv file.
 * <p>
 * There are 2 possible formats to specify a chord symbol, BEAT-BASED or TIME-BASED.
 * <p>
 * BEAT-BASED="bar, beat, chord_symbol" <br>
 * bar and beat are 0-based by default.
 * <p>
 * Example:<br>
 * 0, 0, C <br>
 * 1, 0, F7<br>
 * 1, 1.5, Eb7<br>
 * 2, 0, D7<br>
 * .
 * ..<p>
 * TIME-BASED="pos_in_seconds, chord_symbol"   <br>
 * The time signature and tempo must be set first so that pos_in_seconds can be converted into bar/beat. If not set, the default values are
 * 4/4 and 120bpm.
 * <p>
 * Example:<br>
 * timeSignature=3/4<br>
 * tempoBPM=60<br>
 * 0, C<br>
 * 3, F7<br>
 * 4.5, Eb7<br>
 * 6, D7<br>
 * .
 * ..<p>
 * OTHER FORMAT INFORMATION:<br>
 * // Put comment after //
 * "title=My song name" : if specified the created song will use this title as name.<br>
 * "useBase1" : if specified the bar/beat positions start at 1.<br>
 * Accepted delimiter characters are ',', ';' or space or tab
 */
public class CsvFileReader
{

    private final File file;
    private static final Logger LOGGER = Logger.getLogger(CsvFileReader.class.getSimpleName());

    public CsvFileReader(File f)
    {
        Preconditions.checkNotNull(f);
        this.file = f;
    }

    /**
     * Get the song from the current file.
     * <p>
     * Construct a Song from the elements available in the file.
     *
     * @return
     * @throws IOException
     */
    public Song readSong() throws IOException
    {
        String title = SongFactory.getInstance().getNewSongName("NewSongCSV");
        int tempoBPM = 120;
        TimeSignature timeSignature = TimeSignature.FOUR_FOUR;
        List<CLI_ChordSymbol> cliChordSymbols = new ArrayList<>();
        CLI_Factory clif = CLI_Factory.getDefault();
        int beatBarBase = 0;
        int barSize = 4;

        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            int lineCount = 0;
            String line;
            Pattern pTitle = Pattern.compile("^\\s*title\\s*=\\s*(\\S.+)", Pattern.CASE_INSENSITIVE);
            Pattern pTimeSignature = Pattern.compile("^\\s*timeSignature\\s*=\\s*(\\d+)/(\\d+)", Pattern.CASE_INSENSITIVE);
            Pattern pTempo = Pattern.compile("^\\s*tempoBPM\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Pattern pUseBase1 = Pattern.compile("^\\s*useBase1\\s*$", Pattern.CASE_INSENSITIVE);
            Pattern pBeatBasedChord = Pattern.compile("^\\s*(\\d+)\\s*[,;\\s]\\s*([0-9.]+)\\s*[,;\s]\\s*(\\S+)\\s*$");
            Pattern pTimeBasedChord = Pattern.compile("^\\s*([0-9.]+)\\s*[,;\\s]\\s*(\\S+)\\s*$");


            while ((line = reader.readLine()) != null)
            {
                lineCount++;

                // Remove comments
                line = line.replaceFirst("//.*", "");
                if (line.trim().isBlank())
                {
                    continue;
                }


                Matcher mTitle = pTitle.matcher(line);
                Matcher mTimeSignature = pTimeSignature.matcher(line);
                Matcher mTempo = pTempo.matcher(line);
                Matcher mUseBase1 = pUseBase1.matcher(line);
                Matcher mBeatBasedChord = pBeatBasedChord.matcher(line);
                Matcher mTimeBasedChord = pTimeBasedChord.matcher(line);

                if (mTitle.find())
                {
                    title = mTitle.group(1);

                } else if (mTimeSignature.find())
                {
                    int upper = readUIntFromString(mTimeSignature.group(1), 4, lineCount);
                    int lower = readUIntFromString(mTimeSignature.group(2), 4, lineCount);
                    timeSignature = TimeSignature.get(upper, lower);
                    if (timeSignature == null)
                    {
                        throw new IOException("Invalid time signature " + lineCount + ": " + line);
                    }

                } else if (mTempo.find())
                {
                    int tmp = readUIntFromString(mTempo.group(1), 120, lineCount);
                    if (tmp >= 20 && tmp <= 400)
                    {
                        tempoBPM = tmp;
                    }

                } else if (mUseBase1.find())
                {
                    beatBarBase = 1;
                } else if (mBeatBasedChord.find())
                {
                    int bar = 0;
                    float beat = 0;
                    ExtChordSymbol ecs = null;
                    try
                    {
                        bar = Integer.parseUnsignedInt(mBeatBasedChord.group(1));
                        beat = Float.parseFloat(mBeatBasedChord.group(2));
                        if (bar < beatBarBase || beat < beatBarBase)
                        {
                            throw new NumberFormatException("bar and beat must be >= " + beatBarBase);
                        }
                        ecs = ExtChordSymbol.get(mBeatBasedChord.group(3));
                    } catch (NumberFormatException | ParseException ex)
                    {
                        LOGGER.log(Level.WARNING, "readSong()() file={0}, invalid line[{1}]={2}, exception={3}", new Object[]
                        {
                            file.getName(), lineCount,
                            line, ex.getMessage()
                        });
                    }
                    if (ecs != null)
                    {
                        bar -= beatBarBase;
                        beat -= beatBarBase;
                        var cliCs = clif.createChordSymbol(null, ecs, new Position(bar, beat));
                        cliChordSymbols.add(cliCs);
                        if (bar >= barSize)
                        {
                            barSize = ((bar / 4) + 1) * 4;
                        }
                    }
                } else if (mTimeBasedChord.find())
                {
                    float posInSeconds = 0;
                    ExtChordSymbol ecs = null;
                    try
                    {
                        posInSeconds = Float.parseFloat(mTimeBasedChord.group(1));
                        if (posInSeconds < 0)
                        {
                            throw new NumberFormatException("pos_in_seconds must ve >= 0");
                        }
                        ecs =ExtChordSymbol.get(mTimeBasedChord.group(2));
                    } catch (NumberFormatException | ParseException ex)
                    {
                        LOGGER.log(Level.WARNING, "readSong()() file={0}, invalid line[{1}]={2}, exception={3}", new Object[]
                        {
                            file.getName(), lineCount,
                            line, ex.getMessage()
                        });
                    }
                    if (ecs != null)
                    {
                        float oneBeatDurInSeconds = tempoBPM / 60f;
                        float oneBarDurInSeconds = oneBeatDurInSeconds * timeSignature.getNbNaturalBeats();
                        int bar = (int) (posInSeconds / oneBarDurInSeconds);
                        float barPosInSeconds = bar * oneBarDurInSeconds;
                        float beat = (posInSeconds - barPosInSeconds) / oneBeatDurInSeconds;
                        var cliCs = clif.createChordSymbol(null, ecs, new Position(bar, beat));
                        cliChordSymbols.add(cliCs);
                        if (bar >= barSize)
                        {
                            barSize = ((bar / 4) + 1) * 4;
                        }
                    }
                }
            }
        }


        // Create the song object from the collected data
        ChordLeadSheet cls = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet("A", timeSignature, barSize, false);
        SongFactory sf = SongFactory.getInstance();
        Song song = null;
        try
        {
            song = sf.createSong(title, cls);  // Throw exception        
        } catch (UnsupportedEditException ex)
        {
            throw new IOException(ex);
        }
        song.setTempo(tempoBPM);


        // Add the chord symbols
        cliChordSymbols.forEach(cliCs -> cls.addItem(cliCs));


        return song;
    }

    private int readUIntFromString(String strUInt, int defaultValue, int lineId)
    {
        int res = defaultValue;
        try
        {
            res = Integer.parseUnsignedInt(strUInt);
        } catch (NumberFormatException ex)
        {
            LOGGER.log(Level.WARNING, "readUIntFromString() file={0}, line {1}, ex={2}", new Object[]
            {
                file.getName(), lineId,
                ex.getMessage()
            });
        }
        return res;
    }


    // ==========================================================================================
    // Private methods
    // ==========================================================================================
}

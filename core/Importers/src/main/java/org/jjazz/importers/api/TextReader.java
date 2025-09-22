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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;

/**
 * Read chord symbols from a multi-line text.
 * <p>
 * There are 3 possible formats to specify chords, GRID-BASED, BEAT-BASED or TIME-BASED.
 * <p>
 * GRID-BASED="|4/4 chord1 chord2 | chord3 | chord4|" <br>
 * Use % in a bar to repeat the same chords from the previous bar. Bars can start with an optional time signature. <br>
 * Example:<br>
 * | C | F7 Eb7 | % | Db7 | <br>
 * | C | &nbsp;&nbsp; |3/4 Fm7 | Bb7 |<br>
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
 * The time signature and tempo must be set first so that pos_in_seconds can be converted into bar/beat. If not set, the default values are 4/4 and 120bpm.
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
 * <p>
 * OTHER FORMAT INFORMATION:<br>
 * // Put comment after // <br>
 * "title=My song name" : if specified the created song will use this title as name.<br>
 * "useBase1" : if specified the bar/beat positions start at 1.<br>
 * Accepted delimiter characters are ',', ';' or space or tab '|' can be replaced by '!' when importing text ("!C7 !D6 ! Gm ! !" is valid).
 */
public class TextReader
{

    static private final String TEXT_DATASOURCE = "TextBuffer";
    private final List<String> lines = new ArrayList<>();
    private final String dataSource;  // used for error messages
    private final Pattern pTitle = Pattern.compile("^\\s*title\\s*=\\s*(\\S.+)", Pattern.CASE_INSENSITIVE);
    private final Pattern pTimeSignature = Pattern.compile("^\\s*timeSignature\\s*=\\s*(\\d+)/(\\d+)", Pattern.CASE_INSENSITIVE);
    private final Pattern pTempo = Pattern.compile("^\\s*tempo(BPM)?\\s*[=:]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private final Pattern pUseBase1 = Pattern.compile("^\\s*useBase1\\s*$", Pattern.CASE_INSENSITIVE);
    private final Pattern pBeatBasedChord = Pattern.compile("^\\s*(\\d+)\\s*[,;\\s]\\s*([0-9.]+)\\s*[,;\s]\\s*(\\S+)\\s*$");
    private final Pattern pTimeBasedChord = Pattern.compile("^\\s*([0-9.]+)\\s*[,;\\s]\\s*(\\S+)\\s*$");
    private final Pattern pGridBasedLine = Pattern.compile("^\\s*[|!]");
    private static final Logger LOGGER = Logger.getLogger(TextReader.class.getSimpleName());

    public TextReader(File f) throws IOException
    {
        Preconditions.checkNotNull(f);
        this.dataSource = f.getName();
        lines.addAll(Files.readAllLines(f.toPath()));
    }

    public TextReader(String text)
    {
        Preconditions.checkNotNull(text);
        this.dataSource = TEXT_DATASOURCE;
        for (var line : text.split("\\R+"))
        {
            lines.add(line);
        }
    }

    /**
     * Get the song from the current text or file.
     * <p>
     * Construct a Song from the textual elements.
     *
     * @return Can be null if no valid input was found
     */
    public Song readSong()
    {
        String title = SongFactory.getInstance().getNewSongName("NewSongText");
        int tempoBPM = 120;
        TimeSignature ts0 = TimeSignature.FOUR_FOUR;
        TimeSignature timeSignature = ts0;
        List<CLI_ChordSymbol> cliChordSymbols = new ArrayList<>();
        List<CLI_Section> extraSections = new ArrayList<>();
        int beatBarBase = 0;
        List<CLI_ChordSymbol> lastBarChords = new ArrayList<>();
        int barIndex = 0;
        int lineCount = 0;
        boolean isDataValid = false;


        for (var line : lines)
        {
            lineCount++;        // base1 because used for user error messages


            // Clean up
            line = line.replaceFirst("//.*", "").trim();
            if (line.isBlank())
            {
                continue;
            }


            Matcher mTitle = pTitle.matcher(line);
            Matcher mTimeSignature = pTimeSignature.matcher(line);
            Matcher mTempo = pTempo.matcher(line);
            Matcher mUseBase1 = pUseBase1.matcher(line);
            Matcher mBeatBasedChord = pBeatBasedChord.matcher(line);
            Matcher mTimeBasedChord = pTimeBasedChord.matcher(line);
            Matcher mGridBasedLine = pGridBasedLine.matcher(line);


            if (mTitle.find())
            {
                title = mTitle.group(1);


            } else if (mTimeSignature.find())
            {
                int upper = readUIntFromString(mTimeSignature.group(1), 4, lineCount);
                int lower = readUIntFromString(mTimeSignature.group(2), 4, lineCount);
                ts0 = TimeSignature.get(upper, lower);
                if (ts0 == null)
                {
                    ts0 = TimeSignature.FIVE_FOUR;
                    LOGGER.log(Level.WARNING, "readSong()() source={0}, invalid line[{1}]={2}, invalid time signature, using 4/4 instead", new Object[]
                    {
                        dataSource, lineCount, line
                    });
                }


            } else if (mTempo.find())
            {
                int tmp = readUIntFromString(mTempo.group(2), 120, lineCount);
                if (tmp >= 20 && tmp <= 400)
                {
                    tempoBPM = tmp;
                }


            } else if (mUseBase1.find())
            {
                beatBarBase = 1;


            } else if (mBeatBasedChord.find())
            {
                isDataValid = true;
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
                    LOGGER.log(Level.WARNING, "readSong()() source={0}, invalid line[{1}]={2}, exception={3}", new Object[]
                    {
                        dataSource, lineCount,
                        line, ex.getMessage()
                    });
                }
                if (ecs != null)
                {
                    bar -= beatBarBase;
                    beat -= beatBarBase;
                    var cliCs = CLI_Factory.getDefault().createChordSymbol(ecs, bar, beat);
                    cliChordSymbols.add(cliCs);
                }


            } else if (mTimeBasedChord.find())
            {
                isDataValid = true;
                float posInSeconds = 0;
                ExtChordSymbol ecs = null;
                try
                {
                    posInSeconds = Float.parseFloat(mTimeBasedChord.group(1));
                    if (posInSeconds < 0)
                    {
                        throw new NumberFormatException("pos_in_seconds must ve >= 0");
                    }
                    ecs = ExtChordSymbol.get(mTimeBasedChord.group(2));
                } catch (NumberFormatException | ParseException ex)
                {
                    LOGGER.log(Level.WARNING, "readSong()() source={0}, invalid line[{1}]={2}, exception={3}", new Object[]
                    {
                        dataSource, lineCount,
                        line, ex.getMessage()
                    });
                }
                if (ecs != null)
                {
                    float oneBeatDurInSeconds = tempoBPM / 60f;
                    float oneBarDurInSeconds = oneBeatDurInSeconds * ts0.getNbNaturalBeats();
                    int bar = (int) (posInSeconds / oneBarDurInSeconds);
                    float barPosInSeconds = bar * oneBarDurInSeconds;
                    float beat = (posInSeconds - barPosInSeconds) / oneBeatDurInSeconds;
                    var cliCs = CLI_Factory.getDefault().createChordSymbol(ecs, bar, beat);
                    cliChordSymbols.add(cliCs);
                }


            } else if (mGridBasedLine.find())
            {
                isDataValid = true;


                // To be compatible with ChordPulse text export, remove all "." and isolated "/" (indicate a beat position)
                String lineGrid = line.replaceAll("\\.", "");
                lineGrid = lineGrid.replaceAll("/ ", "");

                // for split to create the right nb of bars, remove first '|' and possibly last '|'
                lineGrid = lineGrid.replaceFirst("^\\s*[|!]", "");
                lineGrid = lineGrid.replaceFirst("[|!]\\s*$", "");
                String[] strBars = lineGrid.split("[|!]", -1);


                List<CLI_ChordSymbol> curBarChords;

                for (String strBar : strBars)
                {
                    strBar = strBar.trim();

                    if (strBar.isBlank())
                    {
                        // Nothing


                    } else if (strBar.equals("%"))
                    {
                        // Special case %, reuse previous chords
                        curBarChords = new ArrayList<>();
                        for (var cliCs : lastBarChords)
                        {
                            var newCliCs = (CLI_ChordSymbol) cliCs.getCopy(null, cliCs.getPosition().getMoved(1, 0));
                            curBarChords.add(newCliCs);
                            cliChordSymbols.add(newCliCs);
                        }
                        lastBarChords = curBarChords;


                    } else
                    {
                        // Standard case, there is some text in this bar

                        // Check if the bar starts with a time signature
                        String strTs = strBar.split("\\s+")[0];      // array should not be empty
                        try
                        {
                            TimeSignature ts = TimeSignature.parse(strTs);      // throws exception
                            if (!ts.equals(timeSignature))
                            {
                                if (barIndex == 0)
                                {
                                    ts0 = ts;
                                } else
                                {
                                    String name = "T" + (extraSections.size() + 1);
                                    var cliSection = CLI_Factory.getDefault().createSection(name, ts, barIndex, null);
                                    extraSections.add(cliSection);
                                }
                                timeSignature = ts;
                            }

                            // remove time signature string for chord parsing
                            strBar = strBar.substring(strTs.length());

                        } catch (ParseException ex)
                        {
                            // Do nothing, it might be just a chord
                        }


                        // Parse chords
                        try
                        {
                            curBarChords = CLI_ChordSymbol.toCLI_ChordSymbolsNoPosition(strBar, timeSignature, null, barIndex, false);
                            cliChordSymbols.addAll(curBarChords);
                            lastBarChords = curBarChords;
                        } catch (ParseException ex)
                        {
                            lastBarChords = new ArrayList<>();
                            LOGGER.log(Level.WARNING, "readSong()() source={0}, invalid line[{1}]={2}, exception={3}", new Object[]
                            {
                                dataSource, lineCount,
                                line, ex.getMessage()
                            });
                        }
                    }


                    barIndex++;
                }

            } else
            {
                LOGGER.log(Level.INFO, "readSong() Ignored line: {0}", line);
            }
        }

        if (!isDataValid)
        {
            return null;
        }


        // Create the song object from the collected data
        String sName = "A";
        ChordLeadSheet cls = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet(sName, ts0, barIndex, null);
        SongFactory sf = SongFactory.getInstance();
        Song song = null;
        try
        {
            song = sf.createSong(title, cls);
        } catch (UnsupportedEditException ex)
        {
            throw new IllegalStateException(ex.getMessage());   // Should never happen
        }
        song.setTempo(tempoBPM);


        // Add the extra sections if any
        extraSections.forEach(cliSection -> 
        {
            try
            {
                cls.addSection(cliSection);
            } catch (UnsupportedEditException ex)
            {
                LOGGER.log(Level.WARNING, "readSong()() source={0}, impossible to add section {1}: {2}", new Object[]
                {
                    dataSource, cliSection, ex.getMessage()
                });
            }
        });


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
            LOGGER.log(Level.WARNING, "readUIntFromString() source={0}, line {1}, ex={2}", new Object[]
            {
                dataSource, lineId,
                ex.getMessage()
            });
        }
        return res;
    }


    /**
     * Get a (possibly multiline) string representing the chord leadsheet e.g. "|4/4 C7 | Dm6 G7 | Ab7M | G7#5|".
     *
     * @param cls
     * @param nbBarsPerLine
     * @param showInitialTimeSignature
     * @return A string which could be read by a TextReader.
     */
    public static String toText(ChordLeadSheet cls, int nbBarsPerLine, boolean showInitialTimeSignature)
    {
        Preconditions.checkNotNull(cls);
        Preconditions.checkArgument(nbBarsPerLine > 0);
        StringBuilder sb = new StringBuilder();
        TimeSignature ts = showInitialTimeSignature ? null: cls.getSection(0).getData().getTimeSignature();

        for (int bar = 0; bar < cls.getSizeInBars(); bar++)
        {
            sb.append("|");

            // Add time signature only when changed
            var barTs = cls.getSection(bar).getData().getTimeSignature();
            if (!barTs.equals(ts))
            {
                ts = barTs;
                sb.append(ts);
            }
            sb.append(" ");

            // Add chords
            boolean noChords = true;
            for (var cliCs : cls.getItems(bar, bar, CLI_ChordSymbol.class))
            {
                sb.append(cliCs.getData().getOriginalName()).append(" ");
                noChords = false;
            }
            if (noChords)
            {
                sb.append("  ");
            }

            if (bar == cls.getSizeInBars() - 1)
            {
                // Last bar
                sb.append("|");
            } else if ((bar % nbBarsPerLine) == nbBarsPerLine - 1)
            {
                sb.append("|\n");
            }
        }


        return sb.toString();
    }


    // ==========================================================================================
    // Private methods
    // ==========================================================================================
}

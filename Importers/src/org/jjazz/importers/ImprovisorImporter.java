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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.harmony.ChordSymbol;
import org.jjazz.harmony.ChordType;
import org.jjazz.harmony.ChordTypeDatabase;
import org.jjazz.harmony.Note;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongManager;
import org.jjazz.song.spi.SongImporter;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Import from Improvisor leadsheet files.
 */
@ServiceProvider(service = SongImporter.class)
public class ImprovisorImporter implements SongImporter
{

    public static final String ID = "Impro-visor leadsheet (.ls) importer";
    // Parameters for the SongImporter.PostProcessor interface
    public static final String PARAM_STYLE = "Style";
    public static final String PARAM_TEMPO = "Tempo";
    public static final String PARAM_COMPOSER = "Composer";
    private final FileNameExtensionFilter FILTER = new FileNameExtensionFilter("Impro-visor files (.ls)", "ls");
    private final ChordSymbol REPEAT_CHORD_SYMBOL = new ChordSymbol();            // Used to represent the '/' or 'NC' symbol
    private final HashMap<String, String> mapChordTypeConversion = new HashMap<>(); // Map special improvisor chordtypes to JJazz ChordTypes
    private static final Logger LOGGER = Logger.getLogger(ImprovisorImporter.class.getSimpleName());

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public List<FileNameExtensionFilter> getSupportedFileTypes()
    {
        return Arrays.asList(FILTER);
    }

    @Override
    public Song importFromFile(File f) throws IOException
    {
        if (f == null)
        {
            throw new NullPointerException("f");
        }
        String title = "No Title";
        String composer = null;
        String style = null;
        int tempo = 120;
        TimeSignature ts = TimeSignature.FOUR_FOUR;
        ChordLeadSheet cls = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(f)))
        {
            int lineCount = 0;
            int barIndex = 0;
            char sectionIndex = 'A';
            String line;
            Pattern pTitle = Pattern.compile("^\\(title\\s+([^)]+)");
            Pattern pComposer = Pattern.compile("^\\(composer\\s+([^)]+)");
            Pattern pTempo = Pattern.compile("^\\(tempo\\s+(\\d+)");
            Pattern pMeter = Pattern.compile("^\\(meter\\s+(\\d+)\\s+(\\d+)");
            Pattern pSection = Pattern.compile("^\\(section");
            Pattern pChord = Pattern.compile("^\\s*[A-GN].*");      // N for NC
            Pattern pStyle = Pattern.compile("style\\s+([a-ZA-Z0-9-]+)");

            while ((line = reader.readLine()) != null)
            {
                lineCount++;
                if (line.trim().isEmpty())
                {
                    continue;
                }
                Matcher mTitle = pTitle.matcher(line);
                Matcher mComposer = pComposer.matcher(line);
                Matcher mTempo = pTempo.matcher(line);
                Matcher mMeter = pMeter.matcher(line);
                Matcher mSection = pSection.matcher(line);
                Matcher mChord = pChord.matcher(line);
                Matcher mStyle = pStyle.matcher(line);
                // if (mTitle.matches())
                if (mTitle.find())
                {
                    title = mTitle.group(1);
                } else if (mComposer.find())
                {
                    composer = mComposer.group(1);
                } else if (mTempo.find())
                {
                    try
                    {
                        int tmp = Integer.parseUnsignedInt(mTempo.group(1));
                        if (tmp >= 20 && tmp <= 400)
                        {
                            tempo = tmp;
                        }
                    } catch (NumberFormatException ex)
                    {
                        LOGGER.warning("importFromFile() file=" + f.getName() + ", line " + lineCount + ", ex=" + ex.getLocalizedMessage());
                    }
                } else if (mMeter.find())
                {
                    int upper = 4;
                    int lower = 4;
                    try
                    {
                        upper = Integer.parseUnsignedInt(mMeter.group(1));
                    } catch (NumberFormatException ex)
                    {
                        LOGGER.warning("importFromFile() file=" + f.getName() + ", line " + lineCount + ", ex=" + ex.getLocalizedMessage());
                    }
                    try
                    {
                        lower = Integer.parseUnsignedInt(mMeter.group(2));
                    } catch (NumberFormatException ex)
                    {
                        LOGGER.warning("importFromFile() file=" + f.getName() + ", line " + lineCount + ", ex=" + ex.getLocalizedMessage());
                    }
                    ts = TimeSignature.get(upper, lower);
                    if (ts == null)
                    {
                        throw new IOException("Time signature not supported : " + upper + "/" + lower);
                    }
                } else if (mSection.find())
                {
                    if (cls == null)
                    {
                        // Ignore the first section before the first chord
                    } else
                    {
                        // It's a new section within the leadsheet       
                        sectionIndex++;
                        CLI_Factory clif = CLI_Factory.getDefault();
                        CLI_Section section = clif.createSection(cls, String.valueOf(sectionIndex), ts, barIndex);
                        try
                        {
                            cls.setSize(barIndex + 1);
                            cls.addSection(section);
                        } catch (UnsupportedEditException ex)
                        {
                            // We should not get here
                            Logger.getLogger(ImprovisorImporter.class.getName()).log(Level.SEVERE, null, ex);   // user notif. + log
                        }
                    }
                } else if (mChord.find())
                {
                    if (cls == null)
                    {
                        // Create an empty leadsheet, 1 bar size
                        ChordLeadSheetFactory clsf = ChordLeadSheetFactory.getDefault();
                        cls = clsf.createEmptyLeadSheet(String.valueOf(sectionIndex), ts, 1);
                        cls.removeItem(cls.getItems(CLI_ChordSymbol.class).get(0));
                    }
                    barIndex = fillInChordLeadSheet(cls, ts, barIndex, line, f, lineCount);
                    // LOGGER.severe("importFromFile() chord line=" + line);
                } else if (mStyle.find())
                {
                    style = mStyle.group(1);
                }
            } // End while
        } // End try

        // Create the song object from the collected data
        SongManager sf = SongManager.getInstance();
        Song song = null;
        try
        {
            song = sf.createSong(title, cls);  // Throw exception        
        } catch (UnsupportedEditException ex)
        {
            LOGGER.log(Level.WARNING, null, ex);        // Log + user notification
        }

        assert song != null;

        // Post process
        SongImporter.PostProcessor postProcessor = Lookup.getDefault().lookup(SongImporter.PostProcessor.class);
        if (postProcessor != null)
        {
            HashMap<String, Object> params = new HashMap<>();
            params.put(PARAM_STYLE, style);
            params.put(PARAM_TEMPO, tempo);
            params.put(PARAM_COMPOSER, composer);
            postProcessor.postProcessImportedSong(this, song, params);
        } else
        {
            song.setTempo(tempo);
            if (composer != null && composer.isEmpty())
            {
                song.setComments("Composer: " + composer);
            }
        }

        return song;

    }

    // ==========================================================================================
    // Private methods
    // ==========================================================================================
    /**
     *
     * @param cls
     * @param barIndex
     * @param chordLine
     * @param f
     * @param lineIndex
     * @return The barIndex right after the end of the chordleadsheet.
     */
    private int fillInChordLeadSheet(ChordLeadSheet cls, TimeSignature ts, int barIndex, String chordLine, File f, int lineIndex)
    {
        Scanner scanner = new Scanner(chordLine);       // white space delimiter by default
        List<ChordSymbol> currentBarCsBuffer = new ArrayList<>();
        while (scanner.hasNext())
        {
            cls.setSize(barIndex + 1);// Make sure chordleadsheet is big enough            
            String token = scanner.next();
            char firstChar = token.charAt(0);
            if (firstChar >= 'A' && firstChar <= 'G')
            {
                // Chord symbol
                // There is sometimes a trailing '_' ! don't know why, remove it
                if (token.endsWith("_"))
                {
                    token = token.substring(0, token.length() - 1);
                }
                ChordSymbol cs = getChordSymbol(token);
                if (cs == null)
                {
                    LOGGER.warning("fillInChordLeadSheet() chord symbol not recognized: '" + token + "', file=" + f.getName() + ", line " + lineIndex);
                } else
                {
                    currentBarCsBuffer.add(cs);
                }

            } else if (token.equals("|"))
            {
                // Bar change
                if (!currentBarCsBuffer.isEmpty())
                {
                    flush(cls, ts, currentBarCsBuffer, barIndex);
                }
                barIndex++;
            } else if (token.equals("/"))
            {
                // Same chord as before
                currentBarCsBuffer.add(REPEAT_CHORD_SYMBOL);

            } else if (token.equals("NC"))
            {
                // No Chord
                currentBarCsBuffer.add(REPEAT_CHORD_SYMBOL);
            } else
            {
                // It can be a melody note (starts with [a-g]), or something else not recognized
                // IGNORE
            }
        }

        return barIndex;
    }

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

    /**
     * Create the CLI_ChordSymbols in cls from the buffer.
     * <p>
     * Clear the contents of barCsBuffer when cls has been filled.
     *
     * @param cls
     * @param barCsBuffer The ChordSymbols at BarIndex
     * @param barIndex
     */
    private void flush(ChordLeadSheet cls, TimeSignature ts, List<ChordSymbol> barCsBuffer, int barIndex)
    {
        assert !barCsBuffer.isEmpty() : "barCsBuffer empty ! barIndex=" + barIndex;
        CLI_Factory clif = CLI_Factory.getDefault();
        float beatStep = (float) ts.getNbNaturalBeats() / barCsBuffer.size();
        int i = 0;
        for (ChordSymbol cs : barCsBuffer)
        {
            if (cs == REPEAT_CHORD_SYMBOL)
            {
                i++;
                continue;
            }
            Position pos = new Position(barIndex, i * beatStep);
            ExtChordSymbol ecs = new ExtChordSymbol(cs, new ChordRenderingInfo(), null, null);
            CLI_ChordSymbol cliCs = clif.createChordSymbol(cls, ecs, pos);
            cls.addItem(cliCs);
            i++;
        }
        barCsBuffer.clear();
    }

    private void initMap()
    {
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
        mapChordTypeConversion.put("+add9", "+");
        mapChordTypeConversion.put("add9no3", "2");
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

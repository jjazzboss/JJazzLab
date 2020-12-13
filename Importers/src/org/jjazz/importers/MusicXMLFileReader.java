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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.ParsingException;
import org.jjazz.harmony.Note;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.importers.musicxml.MusicXmlParser;
import org.jjazz.importers.musicxml.MusicXmlParserListener;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.util.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 * MusicXML leadsheet file reader.
 */
public class MusicXMLFileReader
{

    private File file;
    private boolean noUserPrompt;
    private static final Logger LOGGER = Logger.getLogger(MusicXMLFileReader.class.getSimpleName());

    /**
     *
     * @param f
     * @param noUserPrompt If true assume a Yes answer to possible user prompt
     */
    public MusicXMLFileReader(File f, boolean noUserPrompt)
    {
        if (f == null)
        {
            throw new NullPointerException("f");
        }
        this.noUserPrompt = noUserPrompt;
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
        var myListener = new MyXmlParserListener();
        parser.addParserListener(myListener);


        try
        {
            parser.parse(file);
        } catch (ParsingException ex)
        {
            throw new IOException(ex);
        }


        // Result
        Song song = myListener.song;


        // Propose to remove useless bars (BIAB seems to systematically insert 2 bars at the beginning)                            
        Position firstPos = myListener.firstChordPos;
        if (firstPos != null && firstPos.isFirstBarBeat() && firstPos.getBar() > 0)
        {
            boolean doIt = true;

            if (!noUserPrompt)
            {
                String msg = ResUtil.getString(getClass(), "MusicXMLFileReader.UserPrompt", file.getName(), (firstPos.getBar() + 1));
                NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
                doIt = NotifyDescriptor.YES_OPTION == DialogDisplayer.getDefault().notify(d);
            }


            if (doIt)
            {
                try
                {
                    song.getChordLeadSheet().deleteBars(0, firstPos.getBar() - 1);
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
            }
        }


        int dotIndex = file.getName().lastIndexOf('.');
        String name = dotIndex >= 0 ? file.getName().substring(0, dotIndex) : file.getName();
        song.setName(name);


        return song;
    }

    // ==========================================================================================
    // Private methods
    // ==========================================================================================
    // ============================================================================
    // Private classes
    // ============================================================================
    private class MyXmlParserListener implements MusicXmlParserListener
    {

        Song song;
        ChordLeadSheet cls;
        int songSizeInBars = 0;
        char sectionChar = 'A';
        Position firstChordPos = null;

        MyXmlParserListener()
        {
            // Create a 4/4 empty song, C chord, "A" section
            song = SongFactory.getInstance().createEmptySong("musicXML import", ChordLeadSheet.MAX_SIZE);  // Make room !
            cls = song.getChordLeadSheet();
        }

        @Override
        public void beforeParsingStarts()
        {
            // Nothing
        }

        @Override
        public void afterParsingFinished()
        {
            // Update size
            if (songSizeInBars == 0)
            {
                songSizeInBars = 4;
            }
            try
            {
                song.getChordLeadSheet().setSize(songSizeInBars);
            } catch (UnsupportedEditException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
            }
            if (firstChordPos == null)
            {
                LOGGER.warning("afterParsingFinished() No chord symbols found, importing an empty song.");
            }
        }

        @Override
        public void onTempoChanged(int tempoBPM, int barIndex)
        {
            if (barIndex == 0)
            {
                if (tempoBPM < TempoRange.TEMPO_MIN)
                {
                    LOGGER.log(Level.WARNING, "onTempoChanged() Invalid tempo={0}, using {1} instead", new Object[]
                    {
                        tempoBPM, TempoRange.TEMPO_MIN
                    });
                    tempoBPM = TempoRange.TEMPO_MIN;
                }
                if (tempoBPM > TempoRange.TEMPO_MAX)
                {
                    LOGGER.log(Level.WARNING, "onTempoChanged() Invalid tempo={0}, using {1} instead", new Object[]
                    {
                        tempoBPM, TempoRange.TEMPO_MAX
                    });
                    tempoBPM = TempoRange.TEMPO_MAX;
                }
                song.setTempo(tempoBPM);
            } else
            {
                LOGGER.log(Level.WARNING, "onTempoChanged() Tempo changed to {0} at barIndex={1}: ignored", new Object[]
                {
                    tempoBPM, barIndex
                });
            }
        }

        @Override
        public void onTimeSignatureParsed(TimeSignature ts, int barIndex)
        {
            CLI_Section cliSection = cls.getSection(barIndex);
            Section section = cliSection.getData();
            if (barIndex == 0)
            {
                // Special case
                try
                {
                    cls.setSectionTimeSignature(cliSection, ts);
                } catch (UnsupportedEditException ex)
                {
                    LOGGER.warning("onTimeSignatureParsed() Can't change time signature to " + ts + " at bar " + barIndex + " because: " + ex);
                    return;
                }
            } else if (!section.getTimeSignature().equals(ts))
            {
                // Need to introduce a new section
                sectionChar++;
                cliSection = CLI_Factory.getDefault().createSection(cls, String.valueOf(sectionChar), ts, barIndex);
                try
                {
                    cls.addSection(cliSection);
                } catch (UnsupportedEditException ex)
                {
                    LOGGER.warning("onTimeSignatureParsed() Can't change time signature to " + ts + " at bar " + barIndex + " because: " + ex);
                }
            }
        }

        @Override
        public void onBarLineParsed(String id, int barIndex)
        {
            songSizeInBars = barIndex + 1;
        }

        @Override
        public void onLyricParsed(String lyric, Position pos)
        {
            // Nothing
        }

        @Override
        public void onNoteParsed(Note note, Position pos)
        {
            // Nothing
        }

        @Override
        public void onChordSymbolParsed(String strChord, Position pos)
        {
            if (pos.getBar() == 0 && pos.isFirstBarBeat())
            {
                // Special case, remove first the initial chord since it will be replaced
                List<? extends CLI_ChordSymbol> clis = cls.getItems(0, 0, CLI_ChordSymbol.class);
                if (!clis.isEmpty())
                {
                    cls.removeItem(clis.get(0));
                }
            }
            ExtChordSymbol ecs;
            try
            {
                ecs = new ExtChordSymbol(strChord);
            } catch (ParseException ex)
            {
                LOGGER.warning("onChordSymbolParsed() Invalid chord string=" + strChord + "(" + ex.getLocalizedMessage() + "), can't insert chord at pos=" + pos);
                return;
            }
            CLI_ChordSymbol cliCs = CLI_Factory.getDefault().createChordSymbol(cls, ecs, pos);
            cls.addItem(cliCs);
            if (firstChordPos == null)
            {
                firstChordPos = pos;
            }
        }

    }
}

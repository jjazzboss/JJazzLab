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
package org.jjazz.importers.musicxml;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import static org.jjazz.importers.musicxml.NavigationMark.CODA;
import static org.jjazz.importers.musicxml.NavigationMark.DACAPO;
import static org.jjazz.importers.musicxml.NavigationMark.DACAPO_ALCODA;
import static org.jjazz.importers.musicxml.NavigationMark.DACAPO_ALFINE;
import static org.jjazz.importers.musicxml.NavigationMark.DALSEGNO;
import static org.jjazz.importers.musicxml.NavigationMark.DALSEGNO_ALCODA;
import static org.jjazz.importers.musicxml.NavigationMark.DALSEGNO_ALFINE;
import static org.jjazz.importers.musicxml.NavigationMark.FINE;
import static org.jjazz.importers.musicxml.NavigationMark.SEGNO;
import static org.jjazz.importers.musicxml.NavigationMark.TOCODA;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.Exceptions;

/**
 * Perform a conversion from musicXml file to Song.
 */
public class SongBuilder implements MusicXmlParserListener
{

    private int nbMeasures = 0;
    private TimeSignature timeSignature = TimeSignature.FOUR_FOUR;
    private Position firstChordPos = null;
    private int tempo = -1;
    private final ChordLeadSheet clsWork;
    private Song song = null;
    private String musicalStyle;
    private static final Logger LOGGER = Logger.getLogger(SongBuilder.class.getSimpleName());

    public SongBuilder()
    {
        // clsWork is just used to store all parsed items, actual song creation will be done after
        clsWork = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet("A", TimeSignature.FOUR_FOUR, ChordLeadSheet.MAX_SIZE, null);
    }

    /**
     * The String found in other-play/groove.
     *
     * @return Can be null
     */
    public String getMusicalStyle()
    {
        return musicalStyle;
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
     * Get the resulting song.
     *
     * @return Might be null if parsing is not started yet, or if problem occured.
     */
    public Song getSong()
    {
        return song;
    }

    @Override
    public void beforeParsingStarts()
    {
        // Nothing
    }

    @Override
    public void afterParsingFinished()
    {
        song = buildSong();
    }

    @Override
    public void onTempoChanged(int tempoBPM, int barIndex)
    {
        LOGGER.log(Level.FINE, "onTempoChanged() tempoBPM={0} barIndex={1}", new Object[]
        {
            tempoBPM, barIndex
        });
        if (barIndex == 0)
        {
            tempo = Math.max(TempoRange.TEMPO_MIN, tempoBPM);
            tempo = Math.min(TempoRange.TEMPO_MAX, tempo);
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
        LOGGER.log(Level.FINE, "onTimeSignatureParsed() ts={0} barIndex={1}", new Object[]
        {
            ts, barIndex
        });
        timeSignature = ts;
        CLI_Section cliSection = clsWork.getSection(barIndex);
        Section section = cliSection.getData();
        if (!section.getTimeSignature().equals(ts))
        {
            // Introduce a new section (or change current)
            String name = CLI_Section.createSectionName(cliSection.getData().getName(), clsWork);
            cliSection = CLI_Factory.getDefault().createSection(name, ts, barIndex, null);
            try
            {
                clsWork.addSection(cliSection);
            } catch (UnsupportedEditException ex)
            {
                LOGGER.log(Level.WARNING, "onTimeSignatureParsed() Can''t change time signature to {0} at bar {1} because: {2}",
                        new Object[]
                        {
                            ts, barIndex, ex
                        });
            }
        }
    }

    @Override
    public void onBarLineParsed(String id, int barIndex)
    {
        nbMeasures = barIndex + 1;
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
        LOGGER.log(Level.FINE, "onChordSymbolParsed() strChord={0} pos={1}", new Object[]
        {
            strChord, pos
        });

        ExtChordSymbol ecs;
        try
        {
            ecs = ExtChordSymbol.get(strChord);
        } catch (ParseException ex)
        {
            LOGGER.log(Level.WARNING, "onChordSymbolParsed() Invalid chord string={0}({1}), can''t insert chord at pos={2}",
                    new Object[]
                    {
                        strChord,
                        ex.getMessage(), pos
                    });
            return;
        }
        CLI_ChordSymbol cliCs = CLI_Factory.getDefault().createChordSymbol(ecs, pos);
        clsWork.addItem(cliCs);
        if (firstChordPos == null)
        {
            firstChordPos = pos;
        }
    }

    @Override
    public void onRepeatParsed(int barIndex, boolean startOrEnd, int times)
    {
        LOGGER.log(Level.FINE, "onRepeatParsed() barIndex={0} repeatStart={1} times={2}", new Object[]
        {
            barIndex, startOrEnd, times
        });
        Repeat data = new Repeat(startOrEnd, times);
        float beat = startOrEnd ? 0 : getLastPossibleBeat();
        CLI_Repeat cliItem = new CLI_Repeat(new Position(barIndex, beat), data);
        clsWork.addItem(cliItem);
    }

    @Override
    public void onRehearsalParsed(int barIndex, String value)
    {
        LOGGER.log(Level.FINE, "onRehearsalParsed() barIndex={0} value={1}", new Object[]
        {
            barIndex, value
        });
        CLI_Section curSection = clsWork.getSection(barIndex);
        if (curSection.getPosition().getBar() == barIndex && curSection.getData().getName().equals(value))
        {
            // Special case, nothing to do
            return;
        }

        // Add a section 
        var newSection = CLI_Factory.getDefault().createSection(value, timeSignature, barIndex, clsWork);
        try
        {
            clsWork.addSection(newSection);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);     // Should never happen, we don't change the time signature
        }
    }


    @Override
    public void onOtherPlayParsed(int barIndex, String value, String type)
    {
        LOGGER.log(Level.FINE, "onOtherPlayParsed() barIndex={0} value={1} type={2}", new Object[]
        {
            barIndex, value, type
        });
        if (type.equals("groove"))
        {
            musicalStyle = value;
        }
    }

    @Override
    public void onEndingParsed(int barIndex, List<Integer> numbers, int type)
    {
        LOGGER.log(Level.FINE, "onEndingParsed() barIndex={0} numbers={1} type={2}", new Object[]
        {
            barIndex, numbers, type
        });

        final float beat;
        EndingType endType;
        switch (type)
        {
            case 0 ->
            {
                endType = EndingType.START;
                beat = 0;
            }
            case 1 ->
            {
                endType = EndingType.STOP;
                beat = getLastPossibleBeat();
            }
            case 2 ->
            {
                endType = EndingType.DISCONTINUE;
                beat = getLastPossibleBeat();
            }
            default -> throw new IllegalStateException("type=" + type);
        }

        Ending data = new Ending(endType, numbers);
        CLI_Ending cliItem = new CLI_Ending(new Position(barIndex, beat), data);
        clsWork.addItem(cliItem);
    }

    @Override
    public void onNavigationMarkParsed(int barIndex, NavigationMark mark, String value, List<Integer> timeOnly)
    {
        LOGGER.log(Level.FINE, "onStructureMarkerParsed() barIndex={0} marker={1} value={2}, timeOnly={3}", new Object[]
        {
            barIndex, mark, value, timeOnly
        });
        NavItem data = new NavItem(mark, value, timeOnly);
        float beat = switch (mark)
        {
            case TOCODA, DACAPO, DACAPO_ALCODA, DACAPO_ALFINE, DALSEGNO, DALSEGNO_ALCODA, DALSEGNO_ALFINE, FINE ->
                getLastPossibleBeat();
            case CODA, SEGNO ->
                0;
            default ->
                throw new IllegalStateException("mark=" + mark);
        };

        CLI_NavigationItem cliItem = new CLI_NavigationItem(new Position(barIndex, beat), data);
        clsWork.addItem(cliItem);
    }

    // ==========================================================================================
    // Private methods
    // ==========================================================================================

    /**
     * Build the final song from parsed data collected into clsWork.
     *
     * @return
     */
    private Song buildSong()
    {
        if (nbMeasures == 0)
        {
            nbMeasures = 4;
        }
        try
        {
            clsWork.setSizeInBars(nbMeasures);
        } catch (UnsupportedEditException ex)
        {
            // Should never occur
            Exceptions.printStackTrace(ex);
        }

        LOGGER.log(Level.FINE, "buildSong() musicalStyle={0}", musicalStyle);
        LOGGER.log(Level.FINE, "buildSong() clsWork={0}", Utilities.toMultilineString(clsWork.getItems(), "  "));
        LOGGER.log(Level.FINE, "buildSong() ====");


        // Add section if required for coda and repeat-start
        addMissingSections();


        // Fix missing repeat-start error (common in some files)
        fixMissingRepeatStartError();


        // Retrieve the barlists and related sections
        List<CLI_Section> sectionsOrdered = new ArrayList<>();
        List<List<Integer>> barListsOrdered = new ArrayList<>();
        try
        {
            computeBarListsWithSections(barListsOrdered, sectionsOrdered);
        } catch (ParseException ex)
        {
            LOGGER.warning(ex.getMessage());
            return null;
        }


        LOGGER.log(Level.FINE, "buildSong() ====");
        for (int i = 0; i < sectionsOrdered.size(); i++)
        {
            LOGGER.log(Level.FINE, "{0} : {1}", new Object[]
            {
                sectionsOrdered.get(i), barListsOrdered.get(i)
            });
        }
        assert sectionsOrdered.size() == barListsOrdered.size();
        LOGGER.log(Level.FINE, "buildSong() ====");


        // Now we can create the song
        var sg = createSong(barListsOrdered, sectionsOrdered);


        if (firstChordPos == null)
        {
            LOGGER.warning("afterParsingFinished() No chord symbols found, importing an empty song.");
        }

        return sg;
    }

    private void fixMissingRepeatStartError()
    {
        var repeatEnd = clsWork.getFirstItemAfter(new Position(0), false, CLI_Repeat.class, cli -> !cli.getData().startOrEnd());
        if (repeatEnd != null)
        {
            var repeatStart = clsWork.getLastItemBefore(repeatEnd, CLI_Repeat.class, cli -> cli.getData().startOrEnd());
            if (repeatStart == null)
            {
                // Probably an error, assume repeat-start bar is on first bar
                repeatStart = new CLI_Repeat(new Position(0), new Repeat(true, 2));     // times is ignored for repeat-start
                clsWork.addItem(repeatStart);
            }
        }
    }

    private Song createSong(List<List<Integer>> barListsOrdered, List<CLI_Section> sectionsOrdered)
    {
        var ts0 = sectionsOrdered.get(0).getData().getTimeSignature();
        var sg = SongFactory.getInstance().createEmptySong("MusicXML-import", 4, "A", ts0, null);
        var cls = sg.getChordLeadSheet();
        try
        {
            cls.setSectionTimeSignature(cls.getSection(0), ts0);
        } catch (UnsupportedEditException ex)
        {

            Exceptions.printStackTrace(ex);         // Should not happen, would have broken before
        }
        var sgs = sg.getSongStructure();


        // Add each barList to the chordleadsheet, unless it was already done and we can just add a songpart
        Map<List<Integer>, CLI_Section> mapBarListSection = new HashMap<>();
        int songBarIndex = 0;
        int clsBarIndex = 0;
        for (var barList : barListsOrdered)
        {
            var cliSection = mapBarListSection.get(barList);
            LOGGER.log(java.util.logging.Level.FINE, "buildSong() barList={0} cliSection={1} clsBarIndex={2} songBarIndex={3}", new Object[]
            {
                barList,
                cliSection, clsBarIndex, songBarIndex
            });

            // Is this bar list already associated to a section ?             
            if (cliSection == null)
            {
                try
                {
                    // No, create the section with the implied SongPart
                    cls.setSizeInBars(clsBarIndex + barList.size());
                } catch (UnsupportedEditException ex)
                {
                    Exceptions.printStackTrace(ex);  // Should never occur
                }
                CLI_Section clsWorkSection = sectionsOrdered.get(barListsOrdered.indexOf(barList));
                var name = clsWorkSection.getData().getName();
                if (clsBarIndex > 0)
                {
                    name = CLI_Section.createSectionName(name, cls);
                }
                TimeSignature ts = clsWorkSection.getData().getTimeSignature();
                CLI_Section newSection = CLI_Factory.getDefault().createSection(name, ts, clsBarIndex, cls);
                try
                {
                    newSection = cls.addSection(newSection);
                    LOGGER.log(java.util.logging.Level.FINE, "buildSong() Adding new section {0}", newSection);
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen, would have broke before
                    Exceptions.printStackTrace(ex);
                }


                // Adding the section has created a SongPart at default location (see SongStructure/SgsUpdater). We need to check if it's the right location.
                var sTmp = newSection;  // for lambda next line
                var spt = sgs.getSongParts(spti -> spti.getParentSection() == sTmp).get(0);
                if (spt.getStartBarIndex() != songBarIndex)
                {
                    // Need to move it
                    var newSpt = spt.getCopy(null, songBarIndex, spt.getNbBars(), newSection);
                    try
                    {
                        sgs.removeSongParts(List.of(spt));
                        sgs.addSongParts(List.of(newSpt));
                    } catch (UnsupportedEditException ex)
                    {
                        Exceptions.printStackTrace(ex); // Should never happen, would have broke before
                    }
                }


                // Copy the rest of the bars
                for (int bar : barList)
                {
                    var items = clsWork.getItems(bar, bar, CLI_ChordSymbol.class, cli -> true);
                    for (var item : items)
                    {
                        Position pos = item.getPosition();
                        var newPos = new Position(clsBarIndex, pos.getBeat());
                        cls.addItem(item.getCopy(null, newPos));
                    }
                    clsBarIndex++;
                }

                // Save the new associated section
                mapBarListSection.putIfAbsent(barList, newSection);

            } else
            {
                // Yes, just add the corresponding song part                
                var spts = sgs.getSongParts();
                var curSpt = spts.stream()
                        .filter(spt -> spt.getParentSection() == cliSection)
                        .findAny()
                        .orElseThrow();
                var spt = sgs.createSongPart(curSpt.getRhythm(), cliSection.getData().getName(), songBarIndex, barList.size(), cliSection, true);
                LOGGER.log(java.util.logging.Level.FINE, "buildSong() Adding SongPart={0}", spt);
                try
                {
                    sgs.addSongParts(List.of(spt));
                } catch (UnsupportedEditException ex)
                {
                    Exceptions.printStackTrace(ex);     // Should never happen, would have broke before
                }
            }

            songBarIndex += barList.size();
        }

        return sg;
    }

    private void computeBarListsWithSections(List<List<Integer>> barListsOrdered, List<CLI_Section> sectionsOrdered) throws ParseException
    {
        List<Integer> curBarList = new ArrayList<>();
        var it = new BarNavigationIterator(clsWork);
        int robustness = 1000;
        while (it.hasNext())
        {
            var bar = it.next();
            var curSection = clsWork.getSection(bar);
            if (curSection.getPosition().getBar() == bar)
            {
                // Save barList of previous section
                if (!curBarList.isEmpty())
                {
                    barListsOrdered.add(curBarList);
                }

                // Reset barList
                curBarList = new ArrayList<>();
                sectionsOrdered.add(curSection);
            }
            curBarList.add(bar);


            var barSection = curSection.getPosition().getBar() != bar ? null : curSection;
            String barSectionName = barSection != null ? barSection.getData().getName() : "";
            LOGGER.log(Level.FINE, "computeBarListsWithSections()  bar= {0}    {1} ", new Object[]
            {
                bar, barSectionName
            });

            if (--robustness <= 0)
            {
                throw new ParseException("computeBarListsWithSections() error, endless loop detected (in general caused by unusual repeat bars), aborting.", 0);
            }

        }
        // Add the last curBarList
        barListsOrdered.add(curBarList);
    }

    /**
     * Make sure there is a section for each repeat start or coda.
     */
    private void addMissingSections()
    {
        for (var cli : clsWork.getItems(ChordLeadSheetItem.class, cli -> (cli instanceof CLI_Repeat) || (cli instanceof CLI_NavigationItem)))
        {
            int bar = cli.getPosition().getBar();
            var curSection = clsWork.getSection(bar);

            if (curSection.getPosition().getBar() == bar)
            {
                // There is already a section
                continue;
            }
            String name = null;
            if (cli instanceof CLI_Repeat cliRepeat && cliRepeat.getData().startOrEnd())
            {
                name = curSection.getData().getName();
            } else if (cli instanceof CLI_NavigationItem cliNavItem && cliNavItem.getData().mark().equals(NavigationMark.CODA))
            {
                name = "CODA";
            }

            if (name != null)
            {
                var sectionName = CLI_Section.createSectionName(name, clsWork);
                var newSection = CLI_Factory.getDefault().createSection(sectionName, curSection.getData().getTimeSignature(), bar, clsWork);
                try
                {
                    clsWork.addSection(newSection);
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen, don't add new timeSignature
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    private float getLastPossibleBeat()
    {
        return timeSignature.getNbNaturalBeats() - 0.001f;
    }

    // ===================================================================================================
    // Private classes
    // ===================================================================================================

}

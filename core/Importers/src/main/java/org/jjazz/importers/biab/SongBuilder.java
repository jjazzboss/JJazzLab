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
package org.jjazz.importers.biab;

import org.jjazz.importers.api.BiabStyleFeatures;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.importers.api.BiabFileReader;
import org.jjazz.rhythm.api.Division;
import org.jjazz.rhythm.api.Genre;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.openide.util.Exceptions;

/**
 * Build the song from the BIAB file data.
 */
public class SongBuilder
{

    private BiabFileReader reader;

    private enum ChorusType
    {
        FIRST, MIDDLE, LAST, OUT
    };
    private static final Logger LOGGER = Logger.getLogger(SongBuilder.class.getSimpleName());

    public SongBuilder(BiabFileReader r)
    {
        reader = r;
    }

    /**
     * Build a song from the collected BIAB file data.
     * <p>
     * Also update some BiabFileReader data: timeSignature, styleFeatures, mapClsBarMarker
     *
     * @return
     */
    public Song buildSong()
    {
        if (reader.timeSignature == null)
        {
            reader.timeSignature = guessTimeSignature();
        }
        if (reader.styleFeatures == null)
        {
            Genre g = Genre.UNKNOWN;
            if (reader.styleFileName != null)
            {
                g = BiabStyleFeatures.guessGenre(reader.styleFileName);
            }
            if (g == Genre.UNKNOWN)
            {
                g = BiabStyleFeatures.guessGenre(reader.file.getName());
            }
            reader.styleFeatures = new BiabStyleFeatures(g, Division.UNKNOWN,reader.timeSignature);
        }

        // Compute cls size
        int introSize = reader.chorusStart - 1;
        int oneChorusSize = reader.chorusEnd - reader.chorusStart + 1;
        int lastChorusSize = oneChorusSize;
        int tagDestSize = 0;
        if (reader.useTagJump && reader.tagAfterBar <= reader.chorusEnd)    // In some situations you can have a useless tag (tagBeginBar is not right afterreader.chorusEnd)
        {
            lastChorusSize = reader.tagAfterBar - reader.chorusStart + 1;
            tagDestSize = reader.tagEndBar - reader.tagBeginBar + 1;
        }
        int endSize = reader.generate2barsEnding ? 2 : 0;
        LOGGER.log(Level.FINE, "buildSong() introSize={0}, oneChorusSize={1}, lastChorusSize={2}, tagDestSize={3}, endSize={4}", new Object[]
        {
            introSize,
            oneChorusSize, lastChorusSize, tagDestSize, endSize
        });
        int clsSize = introSize;
        if (reader.chorusNbRepeats == 1)
        {
            clsSize += lastChorusSize;
        } else if (lastChorusSize < oneChorusSize)
        {
            clsSize += oneChorusSize + lastChorusSize;
        } else
        {
            clsSize += oneChorusSize;
        }
        clsSize += tagDestSize + endSize;


        // Create the initial song
        reader.title = reader.title.isBlank() ? "Title" : reader.title;
        Song song = SongFactory.getInstance().createEmptySong(reader.title, clsSize, "A", TimeSignature.FOUR_FOUR, "C");
        song.setTempo(reader.tempo);
        song.setComments("Imported from file " + reader.file.getName());
        ChordLeadSheet cls = song.getChordLeadSheet();
        cls.removeItem(cls.getItems(CLI_ChordSymbol.class).get(0));   // remove the initial C chord symbol
        setSectionTimeSignature(cls, cls.getSection(0), reader.timeSignature);
        SongStructure ss = song.getSongStructure();
        Rhythm r = ss.getSongPart(0).getRhythm();
        CLI_Factory clif = CLI_Factory.getDefault();
        CLI_Section section;


        // Update ChordLeadSheet
        // Intro section
        int clsCurrentBar = 0;
        if (introSize > 0)
        {
            // Update section name and add the chord symbols
            cls.setSectionName(cls.getSection(0), "Intro-A");
            for (CLI_ChordSymbol cliCs : getChordSymbols(cls, reader.timeSignature, 0, introSize - 1, clsCurrentBar, ChorusType.OUT))
            {
                cls.addItem(cliCs);
            }
            ss.setSongPartsName(ss.getSongParts(), "Intro");    // There will be only 1 songpart
            if (reader.mapBiabBarMarker.get(0) != null)
            {
                reader.mapClsBarMarker.put(0, reader.mapBiabBarMarker.get(0));
            }

            // Add sections for additional markers
            List<Integer> barIndexes = getMarkerBars(1, introSize - 1);
            char c = 'B';
            for (Integer barIndex : barIndexes)
            {
                section = clif.createSection("Intro-" + c, reader.timeSignature, barIndex, null);
                addSection(song, section, "Intro");
                if (reader.mapBiabBarMarker.get(barIndex) != null)
                {
                    reader.mapClsBarMarker.put(section.getPosition().getBar(), reader.mapBiabBarMarker.get(barIndex));
                }
                c++;
                if (c == 91)
                {
                    c = 97;
                }
            }
            clsCurrentBar += introSize;
        }


        // Chorus section
        if (introSize > 0)
        {
            section = clif.createSection("Chorus-A", reader.timeSignature, clsCurrentBar, null);
            addSection(song, section, "Chorus1");
        } else
        {
            section = cls.getSection(0);
            cls.setSectionName(section, "Chorus-A");
            ss.setSongPartsName(ss.getSongParts(), "Chorus1");    // There will be only 1 songpart            
        }
        if (reader.mapBiabBarMarker.get(reader.chorusStart - 1) != null)
        {
            reader.mapClsBarMarker.put(section.getPosition().getBar(), reader.mapBiabBarMarker.get(reader.chorusStart - 1));
        }
        int lastChorusEnd = reader.useTagJump ? reader.tagAfterBar : reader.chorusEnd;       // lastChorusEnd is zeroBased 1
        int chorusLastBar = (reader.chorusNbRepeats == 1 ? lastChorusEnd : reader.chorusEnd);  // chorsuLastBar is zeroBased 1
        for (CLI_ChordSymbol cliCs : getChordSymbols(cls, reader.timeSignature, reader.chorusStart - 1, chorusLastBar - 1, clsCurrentBar, ChorusType.FIRST))
        {
            cls.addItem(cliCs);
        }
        addInitChord(cls, section);

        // Add sections for additional markers
        List<Integer> barIndexes = getMarkerBars(reader.chorusStart, chorusLastBar - 1);
        char c = 'B';
        for (Integer barIndex : barIndexes)
        {
            section = clif.createSection("Chorus-" + c, reader.timeSignature, barIndex, null);
            addSection(song, section, "Chorus1");
            addInitChord(cls, section);
            if (reader.mapBiabBarMarker.get(barIndex) != null)
            {
                reader.mapClsBarMarker.put(section.getPosition().getBar(), reader.mapBiabBarMarker.get(barIndex));
            }
            c++;
            if (c == 91)
            {
                c = 97;
            }
        }


        clsCurrentBar += chorusLastBar - reader.chorusStart + 1;
        int repeatChorusStartBar = clsCurrentBar;


        // Optional last chorus section, shorter because of the tag jump
        if (reader.chorusNbRepeats > 1 && lastChorusSize < oneChorusSize)
        {
            section = clif.createSection("Last Chorus-A", reader.timeSignature, clsCurrentBar, null);
            addSection(song, section, "Last Chorus");
            for (CLI_ChordSymbol cliCs : getChordSymbols(cls, reader.timeSignature, reader.chorusStart - 1, lastChorusEnd - 1, clsCurrentBar, ChorusType.LAST))
            {
                cls.addItem(cliCs);
            }
            addInitChord(cls, section);
            if (reader.mapBiabBarMarker.get(reader.chorusStart - 1) != null)
            {
                reader.mapClsBarMarker.put(section.getPosition().getBar(), reader.mapBiabBarMarker.get(reader.chorusStart - 1));
            }


            // Add sections for additional markers
            barIndexes = getMarkerBars(reader.chorusStart, lastChorusEnd - 1);
            c = 'B';
            for (Integer barIndex : barIndexes)
            {
                section = clif.createSection("Last Chorus-" + c, reader.timeSignature, oneChorusSize + barIndex, null);
                addSection(song, section, "Last Chorus");
                addInitChord(cls, section);
                if (reader.mapBiabBarMarker.get(barIndex) != null)
                {
                    reader.mapClsBarMarker.put(section.getPosition().getBar(), reader.mapBiabBarMarker.get(barIndex));
                }
                c++;
                if (c == 91)
                {
                    c = 97;
                }
            }
            clsCurrentBar += lastChorusEnd - reader.chorusStart + 1;
        }


        // Tag destination
        if (tagDestSize > 0)
        {
            section = clif.createSection("Coda-A", reader.timeSignature, clsCurrentBar, null);
            addSection(song, section, "Coda");
            for (CLI_ChordSymbol cliCs : getChordSymbols(cls, reader.timeSignature, reader.tagBeginBar - 1, reader.tagEndBar - 1, clsCurrentBar, ChorusType.OUT))
            {
                cls.addItem(cliCs);
            }
            addInitChord(cls, section);
            if (reader.mapBiabBarMarker.get(reader.tagBeginBar - 1) != null)
            {
                reader.mapClsBarMarker.put(section.getPosition().getBar(), reader.mapBiabBarMarker.get(reader.tagBeginBar - 1));
            }

            // Add sections for additional markers
            barIndexes = getMarkerBars(reader.tagBeginBar, reader.tagEndBar - 1);
            c = 'B';
            for (Integer barIndex : barIndexes)
            {
                section = clif.createSection("Coda-" + c, reader.timeSignature, clsCurrentBar + 1 + barIndex - reader.tagBeginBar, null);
                addSection(song, section, "Coda");
                addInitChord(cls, section);
                if (reader.mapBiabBarMarker.get(barIndex) != null)
                {
                    reader.mapClsBarMarker.put(section.getPosition().getBar(), reader.mapBiabBarMarker.get(barIndex));
                }
                c++;
                if (c == 91)
                {
                    c = 97;
                }
            }
            clsCurrentBar += tagDestSize;
        }


        // End 
        if (endSize > 0)
        {
            section = clif.createSection("End-A", reader.timeSignature, clsCurrentBar, null);
            addSection(song, section, "End");
            int endBiabStartBar = (tagDestSize > 0 ? reader.tagEndBar : reader.chorusEnd) + 1; // zeroBased 1
            int endBiabLastBar = endBiabStartBar + endSize - 1;     // zeroBased 1
            for (CLI_ChordSymbol cliCs : getChordSymbols(cls, reader.timeSignature, endBiabStartBar - 1, endBiabLastBar - 1, clsCurrentBar, ChorusType.OUT))
            {
                cls.addItem(cliCs);
            }
            addInitChord(cls, section);
            if (reader.mapBiabBarMarker.get(endBiabStartBar - 1) != null)
            {
                reader.mapClsBarMarker.put(section.getPosition().getBar(), reader.mapBiabBarMarker.get(endBiabStartBar - 1));
            }

            // Add sections for additional markers
            barIndexes = getMarkerBars(endBiabStartBar, endBiabLastBar - 1);
            c = 'B';
            for (Integer barIndex : barIndexes)
            {
                section = clif.createSection("End-" + c, reader.timeSignature, clsCurrentBar + 1 + barIndex - endBiabStartBar, null);
                addSection(song, section, "End");
                addInitChord(cls, section);
                if (reader.mapBiabBarMarker.get(barIndex) != null)
                {
                    reader.mapClsBarMarker.put(section.getPosition().getBar(), reader.mapBiabBarMarker.get(barIndex));
                }
                c++;
                if (c == 91)
                {
                    c = 97;
                }
            }
        }


        // Add song parts for middle choruses
        if (reader.chorusNbRepeats > 1 && !(tagDestSize > 0 && reader.chorusNbRepeats == 2))
        {
            int startBar = repeatChorusStartBar;
            List<SongPart> chorusSongParts = ss.getSongParts(spt -> spt.getName().startsWith("Chorus"));
            for (int i = 0; i < reader.chorusNbRepeats - 1; i++)
            {
                for (SongPart spt : chorusSongParts)
                {
                    section = spt.getParentSection();
                    SongPart newSpt = ss.createSongPart(r, section.getData().getName(), startBar, spt.getNbBars(), section, false);
                    try
                    {
                        ss.addSongParts(Arrays.asList(newSpt));
                    } catch (UnsupportedEditException ex)
                    {
                        // No rhythm change, should never happen
                        Exceptions.printStackTrace(ex);
                    }
                    ss.setSongPartsName(Arrays.asList(newSpt), "Chorus" + (i + 2));
                    startBar += spt.getNbBars();
                }
            }
        }


        return song;

    }

    /**
     * Get the JJazzLab chord symbols from the BIAB chords for a bar range.
     *
     * @param cls
     * @param ts
     * @param barFrom      zeroBased 0
     * @param barTo        zeroBased 0
     * @param destBarIndex zeroBased 0
     * @param cType
     * @return
     */
    private List<CLI_ChordSymbol> getChordSymbols(ChordLeadSheet cls, TimeSignature ts, int barFrom, int barTo, int destBarIndex, ChorusType cType)
    {
        List<CLI_ChordSymbol> res = new ArrayList<>();
        for (int i = barFrom; i <= barTo; i++)
        {
            res.addAll(getChordSymbols(cls, ts, i, destBarIndex + i - barFrom, cType));
        }
        return res;
    }

    /**
     * Get the JJazzLab chord symbols from the BIAB chords for a specific bar.
     *
     * @param cls
     * @param ts
     * @param barIndex     zeroBased 0
     * @param destBarIndex zeroBased 0
     * @param cType
     * @return
     */
    private List<CLI_ChordSymbol> getChordSymbols(ChordLeadSheet cls, TimeSignature ts, int barIndex, int destBarIndex, ChorusType cType)
    {
        CLI_Factory clif = CLI_Factory.getDefault();
        List<CLI_ChordSymbol> res = new ArrayList<>();

        // LOGGER.fine("getChordSymbols() -- barIndex=" + barIndex + " destBarIndex=" + destBarIndex);

        BiabChord[] bcs = getBiabChords(barIndex);
        // LOGGER.fine("getChordSymbols()  bcs=" + Arrays.asList(bcs));

        for (int beatIndex = 0; beatIndex < 4; beatIndex++)
        {
            if (beatIndex == 3 && ts.equals(TimeSignature.THREE_FOUR))
            {
                // Special case : don't insert any chord
                continue;
            }

            if (bcs[beatIndex] != null)
            {
                BiabChord bc = bcs[beatIndex];
                // LOGGER.fine("getChordSymbols()  chordIndex=" + beatIndex + " bc=" + bc);
                // Compute position
                float beat = beatIndex;
                int bar = destBarIndex;
                if (!(destBarIndex == 0 && beatIndex == 0) && (cType != ChorusType.MIDDLE || reader.allowPushInMiddleChorus))
                {
                    // Handle push
                    switch (bc.push)
                    {
                        case PUSH8 -> beat -= !reader.swing ? 0.5f : 0.33f;
                        case PUSH16 -> beat -= !reader.swing ? 0.25f : 0.33f;
                    }
                    if (beat < 0)
                    {
                        beat += reader.timeSignature.getNbNaturalBeats();
                        bar--;
                    }
                }
                Position pos = new Position(bar, beat);


                // Handle rest     
                ChordRenderingInfo cri = new ChordRenderingInfo();
                if (cType == ChorusType.OUT
                        || (cType == ChorusType.FIRST && reader.allowRestInFirstChorus)
                        || (cType == ChorusType.MIDDLE && reader.allowRestInMiddleChorus)
                        || (cType == ChorusType.LAST && reader.allowRestInLastChorus))
                {
                    switch (bc.rest)
                    {
                        case REST:
                        case SHOT:
                            //cri = new ChordRenderingInfo(ChordRenderingInfo.PlayStyle.ACCENT_SHOT);
                            cri = new ChordRenderingInfo(EnumSet.of(Feature.ACCENT, Feature.SHOT, Feature.EXTENDED_HOLD_SHOT), null);
                            break;
                        case HOLD:
                            cri = new ChordRenderingInfo(EnumSet.of(Feature.ACCENT, Feature.HOLD, Feature.EXTENDED_HOLD_SHOT), null);
                            break;
                        default:
                        // Nothing
                    }
                }
                ExtChordSymbol ecs = new ExtChordSymbol(bc.rootNote, bc.bassNote, bc.chordType, cri, null, null);


                // Create the chord symbol
                CLI_ChordSymbol cliCs = clif.createChordSymbol(ecs, pos);
                res.add(cliCs);
                // LOGGER.fine("getChordSymbols()      cliCs=" + cliCs);
            }
        }
        return res;
    }

    /**
     * Get the Biab chords at the specified barIndex.
     *
     * @param barIndex zeroBased 0
     * @return An size=4 array
     */
    public BiabChord[] getBiabChords(int barIndex)
    {
        if (barIndex < 0 || barIndex > 255)
        {
            throw new IllegalArgumentException("barIndex=" + barIndex);   //NOI18N
        }
        BiabChord[] res = new BiabChord[4];
        int startIndex = barIndex * 4;
        for (int i = startIndex; i < startIndex + 4; i++)
        {
            res[i - startIndex] = reader.chords.get(i);
        }
        return res;
    }

    /**
     * Guess based on style name.
     *
     * @return 4/4 by default
     */
    private TimeSignature guessTimeSignature()
    {
        String s = reader.title.toLowerCase();
        if (s.contains("waltz") || s.contains("wlz") || s.contains("walz"))
        {
            return TimeSignature.THREE_FOUR;
        } else
        {
            return TimeSignature.FOUR_FOUR;
        }
    }


    /**
     * Add the section and add a starting chord if not already present.
     *
     * @param s
     * @param section
     * @param sptName If not null use this name for the associated created SongPart
     */
    private void addSection(Song s, CLI_Section section, String sptName)
    {
        try
        {
            section = s.getChordLeadSheet().addSection(section);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
        if (sptName != null)
        {
            SongStructure ss = s.getSongStructure();
            SongPart spt = getSongPart(ss, section);
            assert spt != null : "s=" + s + " section=" + section + " sptName=" + sptName;   //NOI18N
            ss.setSongPartsName(Arrays.asList(spt), sptName);
        }
    }

    /**
     * Make sure there is a starting chord for the section.
     *
     * @param cls
     * @param section
     */
    private void addInitChord(ChordLeadSheet cls, CLI_Section section)
    {
        Position pos = section.getPosition();
        var chordSymbols = cls.getItems(section, CLI_ChordSymbol.class);
        if (chordSymbols.isEmpty() || !chordSymbols.get(0).getPosition().equals(pos))
        {
            // Need to add an initial chord symbol
            // Use the last previous one with a clean ChordRenderingInfo
            chordSymbols = cls.getItems(0, section.getPosition().getBar() - 1, CLI_ChordSymbol.class);
            assert !chordSymbols.isEmpty() : "section=" + section + ", cls=" + cls;   //NOI18N
            ExtChordSymbol lastEcs = chordSymbols.get(chordSymbols.size() - 1).getData();
            ExtChordSymbol cs = lastEcs.getCopy(null, new ChordRenderingInfo(), null, null);
            CLI_ChordSymbol newCs = CLI_Factory.getDefault().createChordSymbol(cs, pos);
            cls.addItem(newCs);
        }
    }

    private void setSectionTimeSignature(ChordLeadSheet cls, CLI_Section section, TimeSignature ts)
    {
        try
        {
            cls.setSectionTimeSignature(section, ts);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Get the first songpart whose parent section if section.
     *
     * @param ss
     * @param section
     * @return Can be null
     */
    private SongPart getSongPart(SongStructure ss, CLI_Section section)
    {
        return ss.getSongParts().stream().filter(spt -> spt.getParentSection() == section).findAny().orElse(null);
    }

    /**
     * Return the bar indexes for bar which have a marker defined.
     *
     * @param fromBar
     * @param toBar
     * @return
     */
    private List<Integer> getMarkerBars(int fromBar, int toBar)
    {
        List<Integer> res = reader.mapBiabBarMarker.navigableKeySet().stream().filter(i -> i >= fromBar && i <= toBar).toList();
        return res;
    }

}

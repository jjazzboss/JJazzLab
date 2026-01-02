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
package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A ChordSequence built for a song, or a part of a song.
 * <p>
 * When constructed a SongChordSequence always has a starting chord symbol.
 */
public class SongChordSequence extends ChordSequence
{

    private final Song song;
    private static final Logger LOGGER = Logger.getLogger(SongChordSequence.class.getSimpleName());

    /**
     * Build a ChordSequence for the specified song, or part of the song.
     * <p>
     * The constructor relies on fillChordSequence() to add the chord symbols, then it makes sure that the created object has a ChordSymbol at beginning.<br>
     *
     * @param song
     * @param barRange If null, use the whole song.
     * @throws IllegalArgumentException     If barRange is not contained in the song.
     * @throws UserErrorGenerationException If no chord found to be the 1st chord of the ChordSequence
     * @see #fillChordSequence(org.jjazz.rhythmmusicgeneration.api.ChordSequence, org.jjazz.song.api.Song, org.jjazz.util.api.IntRange)
     */
    public SongChordSequence(Song song, IntRange barRange) throws UserErrorGenerationException
    {
        super(barRange == null ? song.getSongStructure().getBarRange() : barRange);
        if (barRange == null)
        {
            barRange = song.getSongStructure().getBarRange();
        } else
        {
            checkArgument(song.getSongStructure().getBarRange().contains(barRange), "song=%s barRange=%s", song, barRange);
        }
        this.song = song;
        fillChordSequence(this, song, barRange);


        if (!hasChordAtBeginning())
        {
            // OK if the range starts in the middle of the first section                
            SongPart spt0 = getSongParts().get(0);
            IntRange clsRange = toClsBarRange(spt0);
            if (clsRange.from == 0)
            {
                throw new UserErrorGenerationException("Missing chord symbol at the start of song " + song.getName());
            }
            var prevCliCs = song.getChordLeadSheet().getLastItemBefore(new Position(clsRange.from),
                    false, CLI_ChordSymbol.class,
                    item -> true);
            CLI_ChordSymbol newCs = getInitCopy(prevCliCs);
            add(newCs);
            LOGGER.log(Level.FINE, "fixChordSequence()   lacking a starting chord. Add a copy of previous chord={0}", newCs);
        }
    }

    public Song getSong()
    {
        return song;
    }


    /**
     * Get the SongParts (whole or partially) included in this SongChordSequence.
     *
     * @return
     */
    public List<SongPart> getSongParts()
    {
        return song.getSongStructure().getSongParts().stream()
                .filter(spt -> !getSptBarRange(spt).equals(IntRange.EMPTY_RANGE))
                .toList();
    }

    /**
     * Get the BeatRange of this SongChordSequence.
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        return song.getSongStructure().toBeatRange(getBarRange());
    }

    /**
     * Get the intersection between the specified SongPart and the bar range of this ChordSequence.
     *
     * @param spt
     * @return
     */
    public IntRange getSptBarRange(SongPart spt)
    {
        return spt.getBarRange().getIntersection(getBarRange());
    }

    /**
     * Return the duration in natural beats of the specified chord.
     * <p>
     * This is the duration until next chord or the end of the SongChordSequence.
     *
     * @param cliCs
     * @param ts    The TimeSignature of the section where chordIndex belongs to (a chord symbol can not span on 2 sections).
     * @return
     */
    public float getChordDuration(CLI_ChordSymbol cliCs, TimeSignature ts)
    {
        Preconditions.checkNotNull(cliCs);
        Preconditions.checkNotNull(ts);

        Position pos = cliCs.getPosition();
        Position nextPos = cliCs == last() ? new Position(getBarRange().to + 1) : higher(cliCs).getPosition();
        float duration = pos.getDuration(nextPos, ts);

        return duration;
    }


    /**
     * Build a SimpleChordSequence for each sequence of contiguous SongParts which match the predicate.
     * <p>
     * Returned SimpleChordSequences are limited to the bar range of this SongChordSequence.
     *
     * @param sptTester All the accepted SongParts must have the same TimeSignature
     * @return An ordered list by bar. Can be empty.
     * @throws IllegalArgumentException If all the accepted SongParts don't have the same TimeSignature
     */
    public List<SimpleChordSequence> buildSimpleChordSequences(Predicate<SongPart> sptTester)
    {
        Objects.requireNonNull(sptTester);
        List<SimpleChordSequence> res = new ArrayList<>();

        // the SongParts which intersect this SongChordSequence range
        var rangeSpts = getSongParts().stream()
                .filter(spt -> sptTester.test(spt))
                .toList();
        if (rangeSpts.isEmpty())
        {
            return res;
        }

        var ts = rangeSpts.getFirst().getRhythm().getTimeSignature();
        if (rangeSpts.stream()
                .anyMatch(spt -> !spt.getRhythm().getTimeSignature().equals(ts)))
        {
            throw new IllegalArgumentException("All accepted SongParts should have the same TimeSignature. spts=" + rangeSpts + " this=" + this);
        }


        List<SongPart> tmpList = new ArrayList<>();
        SongPart prevSpt = null;

        for (var spt : rangeSpts)
        {
            if (prevSpt == null || prevSpt.getStartBarIndex() + prevSpt.getNbBars() == spt.getStartBarIndex())
            {
                tmpList.add(spt);
            } else if (!tmpList.isEmpty())
            {
                // Create a SimpleChordSequence for these contiguous SongParts
                var brSptFirst = tmpList.getFirst().getBarRange().getIntersection(getBarRange());
                var brSptLast = tmpList.getLast().getBarRange().getIntersection(getBarRange());
                var barRange = new IntRange(brSptFirst.from, brSptLast.to);
                var beatStart = song.getSongStructure().toBeatRange(brSptFirst).from;
                var scs = new SimpleChordSequence(subSequence(barRange, false), beatStart, ts);
                res.add(scs);
                tmpList.clear();
            }
            prevSpt = spt;
        }

        if (!tmpList.isEmpty())
        {
            var brSptFirst = tmpList.getFirst().getBarRange().getIntersection(getBarRange());
            var brSptLast = tmpList.getLast().getBarRange().getIntersection(getBarRange());
            var barRange = new IntRange(brSptFirst.from, brSptLast.to);
            var beatStart = song.getSongStructure().toBeatRange(brSptFirst).from;
            var scs = new SimpleChordSequence(subSequence(barRange, false), beatStart, ts);
            res.add(scs);
        }

        return res;
    }

    /**
     * Fill a ChordSequence with the chord symbols of the specified song (or part of the song).
     * <p>
     * Use the song's SongStructure and ChordLeadSheet, limited to the specified bar range, to fill the specified ChordSequence. Process the alternate chord
     * symbols when relevant.<br>
     * Example: <br>
     * - ChordLeadSheet: Section B1: bar0=Cm7, bar1=empty Section B2: bar2=Bb bar3=empty<br>
     * - SongStructure: B1 B2 B1 <br>
     * - Range: [bar1; bar5] Method returns: Cm7(bar1), Bb(bar2), Cm7(bar4), empty(bar5)
     *
     * @param cSeq
     * @param song
     * @param barRange If null, use the whole song.
     * @throws IllegalArgumentException If barRange is not contained in the song.
     */
    static public void fillChordSequence(ChordSequence cSeq, Song song, IntRange barRange)
    {
        Preconditions.checkNotNull(cSeq);
        Preconditions.checkNotNull(song);
        final IntRange br = barRange == null ? song.getSongStructure().getBarRange() : barRange;
        checkArgument(song.getSongStructure().getBarRange().contains(br), "song=%s br=%s", song, br);

        ChordLeadSheet cls = song.getChordLeadSheet();
        CLI_Factory clif = CLI_Factory.getDefault();

        // The song parts impacted by the bar range
        var spts = song.getSongStructure().getSongParts().stream()
                .filter(spt -> !spt.getBarRange().getIntersection(br).equals(IntRange.EMPTY_RANGE))
                .toList();

        // Process all SongParts in the range
        for (SongPart spt : spts)
        {
            // The song part range impacted by barRange
            IntRange sptSubRange = spt.getBarRange().getIntersection(br);

            // The corresponding range in the chord leadsheet
            CLI_Section section = spt.getParentSection();
            int sectionStartBar = section.getPosition().getBar();
            IntRange clsSubRange = new IntRange(sectionStartBar + sptSubRange.from - spt.getStartBarIndex(),
                    sectionStartBar + sptSubRange.to - spt.getStartBarIndex());


            // Prepare Marker data
            RP_SYS_Marker rpMarker = RP_SYS_Marker.getMarkerRp(spt.getRhythm());
            String sptMarker = (rpMarker == null) ? null : spt.getRPValue(rpMarker);


            // Process each chord symbol
            for (var cliCs : cls.getItems(clsSubRange.from, clsSubRange.to, CLI_ChordSymbol.class))
            {
                Position pos = cliCs.getPosition();
                ExtChordSymbol ecs = cliCs.getData();
                int absoluteBar = spt.getStartBarIndex() + pos.getBar() - spt.getParentSection().getPosition().getBar();

                // Prepare the ChordSymbol copy to be added
                Position newPos = new Position(absoluteBar, pos.getBeat());
                ExtChordSymbol newEcs = ecs.getChordSymbol(sptMarker);      // Use alternate chord symbol if relevant      

                // Don't allow Void chordsymbol if it's the init chord symbol
                if (newEcs == VoidAltExtChordSymbol.getInstance() && newPos.equals(new Position(0)))
                {
                    LOGGER.log(Level.INFO, "fillChordSequence() Can''t use the void alternate chord symbol of {0} at initial position.",
                            ecs.getName());
                    newEcs = ecs;
                }

                // Add the ChordSymbol to this ChordSequence
                if (newEcs != VoidAltExtChordSymbol.getInstance())
                {
                    CLI_ChordSymbol newCliCs = clif.createChordSymbol(newEcs, newPos);
                    cSeq.add(newCliCs);
                }
            }
        }
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================
    /**
     * Get the chord leadsheet bar range corresponding to the intersection of the specified song part with this ChordSequence bar range.
     *
     * @param spt
     * @return
     */
    private IntRange toClsBarRange(SongPart spt)
    {
        IntRange sptRange = getSptBarRange(spt);
        CLI_Section section = spt.getParentSection();
        int sectionStartBar = section.getPosition().getBar();
        IntRange r = new IntRange(sectionStartBar + sptRange.from - spt.getStartBarIndex(),
                sectionStartBar + sptRange.to - spt.getStartBarIndex());
        return r;
    }

}

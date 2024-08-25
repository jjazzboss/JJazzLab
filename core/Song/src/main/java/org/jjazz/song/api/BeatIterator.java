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
package org.jjazz.song.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.api.Position;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.IntRange;

/**
 * A beat Iterator within a Song.
 * <p>
 */
public class BeatIterator implements Iterator<Position>
{

    private final Song song;
    private final SongStructure songStructure;
    private Position position;
    private int lastBar;

    /**
     * Create a beat iterator for the specified song.
     * <p>
     * NOTE: modifying the song while using this iterator will produce unknown results.
     *
     * @param song
     */
    public BeatIterator(Song song)
    {
        this(song, new Position(0));
    }

    /**
     * Construct an iterator for the specified song, starting at specified position.
     * <p>
     * NOTE: modifying the song while using this iterator will produce unknown results.
     *
     * @param song
     * @param pos Must be a position within the song.
     */
    public BeatIterator(Song song, Position pos)
    {
        this(song, song.getSongStructure().getBarRange(), pos);
    }

    /**
     * Construct an iterator for the specified bar range within a song.
     * <p>
     * NOTE: modifying the song while using this iterator will produce unknown results.
     *
     * @param song
     * @param barRange A bar range within the song.
     */
    public BeatIterator(Song song, IntRange barRange)
    {
        this(song,
                barRange,
                song.getSongStructure().toPosition(song.getSongStructure().toPositionInNaturalBeats(barRange.from)));
    }

    /**
     * Construct an iterator for the specified bar range within a song, starting at pos.
     * <p>
     * NOTE: modifying the song while using this iterator will produce unknown results.
     *
     * @param song
     * @param pos must be within the bar range.
     * @param barRange A bar range within the song.
     */
    public BeatIterator(Song song, IntRange barRange, Position pos)
    {
        checkNotNull(song);
        checkNotNull(barRange);
        checkNotNull(pos);
        checkArgument(barRange.contains(pos.getBar()), "song=%s pos=%s barRange=%s", song, pos, barRange);
        checkArgument(song.getSongStructure().getBarRange().contains(barRange), "song=%s pos=%s barRange=%s", song, pos, barRange);

        this.song = song;
        this.songStructure = song.getSongStructure();
        this.position = pos;
        this.lastBar = songStructure.getSizeInBars() - 1;
    }

    public Song getSong()
    {
        return song;
    }

    /**
     * Get the next position without advancing the iterator.
     * <p>
     * NOTE: Position might be out of the song.
     *
     * @return
     */
    public Position peek()
    {
        return new Position(position);
    }

    /**
     * True if next integer beat position is not in the song.
     *
     * @return
     */
    @Override
    public boolean hasNext()
    {
        boolean b = position.getBar() < lastBar
                || (position.getBar() == lastBar && position.getBeat() <= getTimeSignature().getNbNaturalBeats() - 1);
        return b;
    }

    /**
     * Advance the song position to the next integer beat.
     *
     * @return A position with an integer beat (e.g.bar=2 beat=3.0)
     * @throws NoSuchElementException
     */
    @Override
    public Position next() throws NoSuchElementException
    {
        if (!hasNext())
        {
            throw new NoSuchElementException("position=" + position + " songStructure=" + songStructure);
        }
        var ts = getTimeSignature();


        // If current position is not an int beat, advance position
        Position res = new Position(position);
        int intbeat = (int) position.getBeat();
        if (position.getBeat() - intbeat > 0)
        {
            int bar = position.getBar();
            intbeat++;
            if (intbeat >= ts.getNbNaturalBeats())
            {
                intbeat = 0;
                bar++;
            }
            position = new Position(bar, intbeat);
            res = new Position(position);
        }


        // Set position to next int beat
        int bar = position.getBar();
        int beat = (int) position.getBeat() + 1;
        if (beat >= ts.getNbNaturalBeats())
        {
            beat = 0;
            bar++;
        }
        position = new Position(bar, beat);

        return res;
    }

    /**
     * True if there is a next bar in the song.
     *
     * @return
     */
    public boolean hasNextBar()
    {
        boolean b = position.getBar() < lastBar
                || (position.getBar() == lastBar && position.isFirstBarBeat());
        return b;
    }

    /**
     * Advance the song position to the next bar (with beat 0).
     *
     * @return A position with beat=0
     * @throws NoSuchElementException
     */
    public Position nextBar() throws NoSuchElementException
    {
        if (!hasNextBar())
        {
            throw new NoSuchElementException("position=" + position + " songStructure=" + songStructure);
        }

        // If position is not on first beat, advance position
        Position res = new Position(position);
        if (!position.isFirstBarBeat())
        {
            position = new Position(position.getBar() + 1);
            res = new Position(position);
        }


        // Set position to next bar
        position = new Position(position.getBar() + 1);

        return res;
    }

    /**
     * True if there is a next half bar in the song.
     *
     * @param swing
     * @return
     */
    public boolean hasNextHalfBar(boolean swing)
    {
        boolean b = position.getBar() < lastBar
                || (position.getBar() == lastBar && position.getBeat() <= getTimeSignature().getHalfBarBeat(swing));
        return b;
    }

    /**
     * Advance the song position to the next half bar (beat=0 or half-bar).
     *
     * @param swing
     * @return
     * @throws NoSuchElementException
     */
    public Position nextHalfBar(boolean swing) throws NoSuchElementException
    {
        if (!hasNextHalfBar(swing))
        {
            throw new NoSuchElementException("position=" + position + " songStructure=" + songStructure);
        }
        float halfBeat = getTimeSignature().getHalfBarBeat(swing);


        // Advance if not on a half-bar
        Position res;
        if (position.isFirstBarBeat() || position.getBeat() == halfBeat)
        {
            res = new Position(position);
        } else if (position.getBeat() < halfBeat)
        {
            position = new Position(position.getBar(), halfBeat);
            res = new Position(position);
        } else
        {
            position = new Position(position.getBar() + 1);
            res = new Position(position);
        }


        // Set position to the next half-bar         
        position = position.isFirstBarBeat() ? new Position(position.getBar(), halfBeat) : new Position(position.getBar() + 1);


        return res;
    }


    /**
     * The time signature corresponding to the current position.
     *
     * @return Can be null if outside the song
     */
    public TimeSignature getTimeSignature()
    {
        TimeSignature res = null;
        if (position.getBar() < songStructure.getSizeInBars())
        {
            res = songStructure.getSongPart(position.getBar()).getRhythm().getTimeSignature();
        }
        return res;
    }


    // =========================================================================================
    // Private methods
    // =========================================================================================
}

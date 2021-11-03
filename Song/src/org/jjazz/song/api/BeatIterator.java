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
package org.jjazz.song.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.songstructure.api.SongStructure;

/**
 * A beat Iterator within a Song.
 * <p>
 * TODO: create an Iterable for Song.
 */
public class BeatIterator implements Iterator<Position>
{

    private final Song song;
    private final SongStructure songStructure;
    private Position position;

    /**
     * Create a beat iterator for the specified song.
     * <p>
     * NOTE: modifying the song while using this iterator will produce unknown results.
     *
     * @param song
     */
    public BeatIterator(Song song)
    {
        this(song, new Position(0, 0));
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
        checkNotNull(song);
        checkNotNull(pos);

        this.song = song;
        this.songStructure = song.getSongStructure();
        checkArgument(songStructure.getBeatRange(null).contains(songStructure.getPositionInNaturalBeats(pos), true), "song=%s pos=%s", song, pos);
        this.position = pos;
    }

    public Song getSong()
    {
        return song;
    }

    public Position getPosition()
    {
        return new Position(position);
    }

    /**
     * True if there is a next (integer) beat in the song.
     *
     * @return
     */
    @Override
    public boolean hasNext()
    {
        boolean b = true;

        if (position.getBar() == songStructure.getSizeInBars() - 1)
        {
            var ts = getTimeSignature();
            int iBeat = (int) position.getBeat();
            if (iBeat >= ts.getNbNaturalBeats() - 1)
            {
                b = false;
            }
        } else if (position.getBar() > songStructure.getSizeInBars() - 1)
        {
            b = false;
        }

        return b;
    }

    /**
     * Get the song position corresponding to the next integer beat.
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
        int bar = position.getBar();
        int beat = (int) position.getBeat() + 1;
        if (beat >= ts.getNbNaturalBeats())
        {
            beat = 0;
            bar++;
        }

        position = new Position(bar, beat);

        return new Position(position);
    }

    /**
     * True if there is a next bar in the song.
     *
     * @return
     */
    public boolean hasNextBar()
    {
        boolean b = position.getBar() < songStructure.getSizeInBars() - 1;
        return b;
    }

    /**
     * Get the song position corresponding to the next bar.
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

        position = new Position(position.getBar() + 1, 0);

        return new Position(position);
    }

    /**
     * True if there is a next half bar in the song.
     *
     * @param swing
     * @return
     */
    public boolean hasNextHalfBar(boolean swing)
    {
        boolean b = true;

        if (position.getBar() == songStructure.getSizeInBars() - 1)
        {
            var ts = getTimeSignature();
            float halfBeat = ts.getHalfBarBeat(swing);
            if (position.getBeat() >= halfBeat)
            {
                b = false;
            }
        } else if (position.getBar() > songStructure.getSizeInBars() - 1)
        {
            b = false;
        }

        return b;
    }

    /**
     * Get the song position corresponding to the next half bar.
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

        var ts = getTimeSignature();
        float halfBeat = ts.getHalfBarBeat(swing);
        int bar = position.getBar();
        float beat = halfBeat;
        if (position.getBeat() >= halfBeat)
        {
            bar++;
            beat = 0;
        }

        position = new Position(bar, beat);

        return new Position(position);
    }


    /**
     * True if position beat is 0 (strict), or &lt; 1 (not strict).
     *
     * @param strict
     * @return
     */
    public boolean isFirstBeat(boolean strict)
    {
        return position.isFirstBarBeat();
    }

    /**
     * True is position beat if on the last beat of the bar (strict), or &gt;= the last beat of the bar (not strict).
     *
     * @return
     */
    public boolean isLastBeat(boolean strict)
    {
        return position.isLastBarBeat(getTimeSignature());
    }

    /**
     * True if position beat is on the half-bar position.
     *
     * @param swing
     * @return
     */
    public boolean isHalfBeat(boolean swing)
    {
        return position.isHalfBarBeat(getTimeSignature(), swing);
    }


    /**
     * The time signature corresponding to the current position.
     *
     * @return
     */
    public TimeSignature getTimeSignature()
    {
        return songStructure.getSongPart(position.getBar()).getRhythm().getTimeSignature();
    }


    // =========================================================================================
    // Private methods
    // =========================================================================================
}

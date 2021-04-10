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
package org.jjazz.rhythmmusicgeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.MidiConst;
import org.jjazz.midimix.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.util.FloatRange;
import org.jjazz.util.IntRange;
import org.jjazz.util.LongRange;

/**
 * Information to be used by a Rhythm to generate music.
 * <p>
 * The class also provides convenient methods to extract song data (e.g. song parts) relevant to the context.
 */
public class MusicGenerationContext
{

    private Song song;
    private MidiMix mix;
    private IntRange range;

    /**
     * Create a MusicGenerationContext object for the whole song.
     *
     * @param s
     * @param mix
     */
    public MusicGenerationContext(Song s, MidiMix mix)
    {
        this(s, mix, null);
    }

    /**
     * Create a MusicGenerationContext object for a whole or a part of the song.
     *
     * @param s
     * @param mix
     * @param r If null, the range will represent the whole song from first to last bar.
     */
    public MusicGenerationContext(Song s, MidiMix mix, IntRange r)
    {
        if (s == null || mix == null)
        {
            throw new IllegalArgumentException("s=" + s + " mix=" + mix + "r=" + r);   //NOI18N
        }
        song = s;
        this.mix = mix;
        int sizeInBars = s.getSongStructure().getSizeInBars();
        if (sizeInBars == 0)
        {
            this.range = IntRange.EMPTY_RANGE;
        } else if (r == null)
        {
            this.range = new IntRange(0, sizeInBars - 1);
        } else if (r.from > sizeInBars - 1 || r.to > sizeInBars - 1)
        {
            throw new IllegalArgumentException("s=" + s + " mix=" + mix + "r=" + r);   //NOI18N
        } else
        {
            this.range = r;
        }
    }

    /**
     * Create a MusicGenerationContext which reuse mgc's Song and MidiMix, but with the specified range.
     *
     * @param mgc
     * @param newRange
     */
    public MusicGenerationContext(MusicGenerationContext mgc, IntRange newRange)
    {
        this(mgc.getSong(), mgc.getMidiMix(), newRange);
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.song);
        hash = 97 * hash + Objects.hashCode(this.mix);
        hash = 97 * hash + Objects.hashCode(this.range);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final MusicGenerationContext other = (MusicGenerationContext) obj;
        if (!Objects.equals(this.song, other.song))
        {
            return false;
        }
        if (!Objects.equals(this.mix, other.mix))
        {
            return false;
        }
        if (!Objects.equals(this.range, other.range))
        {
            return false;
        }
        return true;
    }

    /**
     * Music should be produced for this song.
     *
     * @return
     */
    public Song getSong()
    {
        return song;
    }

    /**
     * Music should be produced for this MidiMix.
     *
     * @return
     */
    public MidiMix getMidiMix()
    {
        return mix;
    }

    /**
     * Music should be produced only for this range of bars.
     * <p>
     * The range can start/end anywhere in the song (including in the middle of a song part).
     *
     * @return
     */
    public IntRange getBarRange()
    {
        return range;
    }

    /**
     * Music should be produced only for this range of natural beats.
     * <p>
     * The range can start/end anywhere in the song (including in the middle of a song part). If getBarRange().from &gt; 0 then
     * getBeatRange().from is also &gt; 0.
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        SongStructure ss = song.getSongStructure();
        return ss.getBeatRange(range);
    }

    /**
     * The tick range corresponding to getBarRange() or getBeatRange().
     * <p>
     * The range can start/end anywhere in the song (including in the middle of a song part). If getBeatRange().from &gt; 0 then
     * getTickRange().from is also &gt; 0.
     *
     * @return
     */
    public LongRange getTickRange()
    {
        FloatRange fr = getBeatRange();
        LongRange lr = new LongRange((long) (fr.from * MidiConst.PPQ_RESOLUTION), (long) (fr.to * MidiConst.PPQ_RESOLUTION));
        return lr;
    }

    /**
     * Get all the song parts which are contained in this context.
     * <p>
     * See the contains() method.
     *
     * @return Can be empty.
     */
    public List<SongPart> getSongParts()
    {
        ArrayList<SongPart> res = new ArrayList<>();
        for (SongPart spt : song.getSongStructure().getSongParts())
        {
            if (contains(spt))
            {
                res.add(spt);
            }
        }
        return res;
    }

    /**
     * Get the Range of bars of spt belonging to this context.
     *
     * @param spt
     * @return Can be the EMPTY_RANGE if spt is not part of this context.
     */
    public IntRange getSptBarRange(SongPart spt)
    {
        return spt.getBarRange().getIntersectRange(range);
    }

    /**
     * Get the range of natural beats of spt belonging to this context.
     *
     * @param spt
     * @return
     */
    public FloatRange getSptBeatRange(SongPart spt)
    {
        FloatRange sptRange = song.getSongStructure().getBeatRange(spt.getBarRange());
        return sptRange.getIntersectRange(getBeatRange());
    }

    /**
     * Get the range of ticks of spt belonging to this context.
     *
     * @param spt
     * @return
     */
    public LongRange getSptTickRange(SongPart spt)
    {
        FloatRange sptRange = getSptBeatRange(spt);
        LongRange lr = new LongRange((long) (sptRange.from * MidiConst.PPQ_RESOLUTION), (long) (sptRange.to * MidiConst.PPQ_RESOLUTION));
        return lr;
    }

    /**
     * Check is the specified spt has at least one bar in the range of this context.
     *
     * @param spt
     * @return
     */
    public boolean contains(SongPart spt)
    {
        return spt.getBarRange().contains(range);
    }

    /**
     * Get the list of unique rhythms used in this context.
     *
     * @return
     */
    public List<Rhythm> getUniqueRhythms()
    {
        ArrayList<Rhythm> res = new ArrayList<>();
        for (SongPart spt : song.getSongStructure().getSongParts())
        {
            if (contains(spt) && !res.contains(spt.getRhythm()))
            {
                res.add(spt.getRhythm());
            }
        }
        return res;
    }

    /**
     * Get the list of unique rhythm voices used in this context.
     *
     * @return
     */
    public List<RhythmVoice> getUniqueRhythmVoices()
    {
        ArrayList<RhythmVoice> rvs = new ArrayList<>();
        for (Rhythm r : getUniqueRhythms())
        {
            rvs.addAll(r.getRhythmVoices());
        }
        return rvs;
    }

    /**
     * Convert a tick position relative to this context into an absolute SongStructure Position.
     *
     * @param relativeTick 0 for the start of this context bar range.
     * @return Null if tick is out of the bounds of this context.
     */
    public Position getPosition(long relativeTick)
    {
        FloatRange br = getBeatRange();
        float absPosInBeats = br.from + (float) relativeTick / MidiConst.PPQ_RESOLUTION;
        if (!br.contains(absPosInBeats, true))
        {
            return null;
        }
        return song.getSongStructure().getPosition(absPosInBeats);
    }

    /**
     * Convert a tick position relative to this context into a ChordLeadSheet Position.
     *
     * @param relativeTick
     * @return Null if tick is out of the bounds of this context.
     */
    public Position getClsPosition(long relativeTick)
    {
        Position ssPos = getPosition(relativeTick);
        if (ssPos == null)
        {
            return null;
        }
        SongPart spt = song.getSongStructure().getSongPart(ssPos.getBar());
        int sectionBar = spt.getParentSection().getPosition().getBar();
        return new Position(ssPos.getBar() - spt.getStartBarIndex() + sectionBar, ssPos.getBeat());
    }

    /**
     * Compute the tick relative to this context for the given absolute position expressed in bar/beat.
     * <p>
     *
     * @param pos
     * @return -1 if pos is outside this context range. Returns 0 for the first bar/beat of the context range.
     */
    public long getRelativeTick(Position pos)
    {
        if (pos == null)
        {
            throw new NullPointerException("pos");   //NOI18N
        }
        long tick = -1;
        int bar = pos.getBar();
        float beat = pos.getBeat();
        if (range.contains(bar))
        {
            SongPart spt = song.getSongStructure().getSongPart(bar);
            LongRange sptTickRange = getSptTickRange(spt);
            IntRange sptBarRange = getSptBarRange(spt);
            int relativeBar = bar - sptBarRange.from;
            float relativeNbBeats = (relativeBar * spt.getRhythm().getTimeSignature().getNbNaturalBeats()) + beat;
            tick = sptTickRange.from + (long) (relativeNbBeats * MidiConst.PPQ_RESOLUTION);
            tick -= getTickRange().from;
        }
        return tick;
    }

    @Override
    public String toString()
    {
        return "MusicGenerationContext[song=" + song.getName() + ", midiMix=" + mix + ", range=" + range + "]";
    }
}

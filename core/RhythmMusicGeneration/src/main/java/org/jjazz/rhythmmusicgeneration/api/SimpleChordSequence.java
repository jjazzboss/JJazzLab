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
import static java.lang.Math.round;
import java.text.DecimalFormat;
import java.util.Objects;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A ChordSequence which has only one TimeSignature.
 * <p>
 * User is responsible to ensure CLI_ChordSymbols are added in the right position order, in the startBar/nbBars range, and are compatible with the
 * TimeSignature.
 */
public class SimpleChordSequence extends ChordSequence
{

    private TimeSignature timeSignature;

    public SimpleChordSequence(IntRange barRange, TimeSignature ts)
    {
        super(barRange);
        this.timeSignature = ts;
    }

    /**
     * Construct a SimpleChordSequence from a standard ChordSequence.
     *
     * @param cSeq All ChordSymbols are checked to be compatible with the specified TimeSignature.
     * @param ts
     */
    public SimpleChordSequence(ChordSequence cSeq, TimeSignature ts)
    {
        this(cSeq.getBarRange(), ts);

        // Add the ChordSymbols
        for (var cliCs : cSeq)
        {
            checkArgument(ts.checkBeat(cliCs.getPosition().getBeat()), "cliCs=%s", cliCs);
            add(cliCs);
        }
    }

    @Override
    public int hashCode()
    {
        int hash = super.hashCode();
        hash = 37 * hash + Objects.hashCode(this.timeSignature);
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
        final SimpleChordSequence other = (SimpleChordSequence) obj;
        if (this.timeSignature != other.timeSignature)
        {
            return false;
        }
        return super.equals(other);
    }


    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }


    /**
     * The beat range of this SimpleChordSequence provided it starts at startBarPosInBeats.
     *
     * @param startBarPosInBeats
     * @return
     */
    public FloatRange getBeatRange(float startBarPosInBeats)
    {
        if (startBarPosInBeats < 0)
        {
            throw new IllegalArgumentException("startBarPosInBeats=" + startBarPosInBeats);
        }
        return new FloatRange(startBarPosInBeats, startBarPosInBeats + getBarRange().size() * timeSignature.getNbNaturalBeats());
    }

    /**
     * The beat range of a ChordSymbol, provided ChordSequence starts at startBarPosInBeats.
     *
     * @param cliCs
     * @param startBarPosInBeats
     * @return
     */
    public FloatRange getBeatRange(CLI_ChordSymbol cliCs, float startBarPosInBeats)
    {
        Preconditions.checkArgument(startBarPosInBeats >= 0, "%s", startBarPosInBeats);
        Preconditions.checkArgument(contains(cliCs), "cliCs=%s  this=%s", cliCs, this);

        var beatPos = toPositionInBeats(cliCs.getPosition(), startBarPosInBeats);
        FloatRange br = new FloatRange(beatPos, beatPos + getChordDuration(cliCs));
        
        return br;
    }

    /**
     * Return the specified chord duration in natural beats.
     * <p>
     * This is the duration until next chord or the end of the chordsequence.
     *
     * @param cliCs
     * @return
     */
    public float getChordDuration(CLI_ChordSymbol cliCs)
    {
        Preconditions.checkNotNull(cliCs);

        Position pos = cliCs.getPosition();
        Position nextPos = cliCs == last() ? new Position(getBarRange().to + 1) : higher(cliCs).getPosition();
        float duration = pos.getDuration(nextPos, timeSignature);

        return duration;
    }


    /**
     * Convert the specified position into an absolute position in natural beats.
     *
     * @param pos
     * @param startBarPosInBeats The start position of this chord sequence.
     * @return If pos is beyond the end of this ChordSequence, then returned value will also be beyond this ChordSequence.
     */
    public float toPositionInBeats(Position pos, float startBarPosInBeats)
    {
        Objects.requireNonNull(pos);
        Preconditions.checkArgument(startBarPosInBeats >= 0, "startBarPosInBeats=%s", startBarPosInBeats);

        float relPosInBeats = (pos.getBar() - getBarRange().from) * timeSignature.getNbNaturalBeats() + pos.getBeat();
        return startBarPosInBeats + relPosInBeats;
    }

    /**
     * Convert the specified position in beats into a Position.
     *
     * @param posInBeats
     * @param startBarPosInBeats The start position of this chord sequence.
     * @return If posInBeats is beyond the end of this ChordSequence, then returned value will also be beyond this ChordSequence.
     */
    public Position toPosition(float posInBeats, float startBarPosInBeats)
    {
        Preconditions.checkArgument(posInBeats >= 0 && posInBeats >= startBarPosInBeats, "posInBeats=%s startBarPosInBeats=%s", posInBeats, startBarPosInBeats);
        float relPosInBeats = posInBeats - startBarPosInBeats;
        int relBars = (int) Math.floor(relPosInBeats / getTimeSignature().getNbNaturalBeats());
        float beat = relPosInBeats - relBars * getTimeSignature().getNbNaturalBeats();
        Position pos = new Position(getBarRange().from + relBars, beat);
        return pos;
    }

    /**
     * Remove successive identical chord symbols.
     *
     * @return True if sequence was modified
     */
    public boolean removeRedundantChords()
    {
        boolean changed = false;
        var it = iterator();
        ExtChordSymbol lastEcs = null;
        while (it.hasNext())
        {
            var ecs = it.next().getData();
            if (Objects.equals(lastEcs, ecs))
            {
                it.remove();
                changed = true;
            } else
            {
                lastEcs = ecs;
            }
        }
        return changed;
    }

    /**
     * Overridden to return a SimpleChordSequence.
     *
     * @param subRange
     * @param addInitChordSymbol
     * @return
     */
    @Override
    public SimpleChordSequence subSequence(IntRange subRange, boolean addInitChordSymbol)
    {
        ChordSequence cSeq = super.subSequence(subRange, addInitChordSymbol);
        return new SimpleChordSequence(cSeq, timeSignature);
    }

    /**
     * A String which combines the chord sequence size, the relative chord root ascending intervals+durations, to allow a quick comparison between 2
     * SimpleChordSequences.
     * <p>
     * If 2 root profiles of 2 SimpleChordSequences are equal, it means that the 2 ChordSequences have the same size, same number of ChordSymbols at the same
     * position, and that the root relative root ascending intervals are equals, like for e.g. |Dm|G7|C7M|%| and |E7|Am|Dm|%|.
     * <p>
     * Example: |Dm|G7|Ab7M|%| will produce "s4n3f0:a4i5:a4i1"  <br>
     * "s4" = size is 4 bars<br>
     * "n3" = there is 3 chord symbols<br>
     * "f0" = first chord symbol position is on beat 0 of the chord sequence<br>
     * "a4i5" means advance 4 beats to next chord with a root ascending relative interval of 5 semi-tons.<br>
     * <p>
     * Example: if chord sequence is empty, return a string like "s4n0".
     * <p>
     * Example: if chord sequence has only 1 chord symbol, return a string like "s4n1f0".
     *
     * @return A String like "s4n3f0:a4i5:a4i1"
     */
    public String getRootProfile()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("s").append(getBarRange().size());
        sb.append("n").append(size());

        if (isEmpty())
        {
            return sb.toString();
        }


        float firstPos = toPositionInBeats(first().getPosition(), 0);
        var df = new DecimalFormat("#.##");             // Avoid trailing 0 if round value
        sb.append("f").append(df.format(firstPos));


        for (var cliCs : this)
        {
            if (cliCs == last())
            {
                break;
            }
            int interval = cliCs.getData().getRootNote().getRelativeAscInterval(higher(cliCs).getData().getRootNote());
            float dur = getChordDuration(cliCs);
            sb.append(":").append("a").append(df.format(dur)).append("i").append(interval);
        }

        return sb.toString();
    }

    /**
     * Generate a root profile for a chord progression with 1 chord per bar and the specified root ascending intervals.
     * <p>
     * Example: if rootAscendingIntervals=[8;4] and ts is 4/4, return "s3n3f0:a4i8:a4i4" (3 chord symbols, one per bar).
     *
     * @param ts                     The TimeSignature to use
     * @param rootAscendingIntervals Number of values is (number of chords - 1). Can't be empty.
     * @return
     */
    static public String getRootProfileOneChordPerBar(TimeSignature ts, int... rootAscendingIntervals)
    {
        checkArgument(rootAscendingIntervals.length > 0);

        StringBuilder sb = new StringBuilder();
        int nbChords = rootAscendingIntervals.length + 1;
        sb.append("s").append(nbChords);
        sb.append("n").append(nbChords);
        sb.append("f0");

        int a = round(ts.getNbNaturalBeats());
        for (int i : rootAscendingIntervals)
        {
            sb.append(":a").append(a).append("i").append(i);
        }

        return sb.toString();
    }


    /**
     * Merge this SimpleChordSequence with scs into a new instance.
     * <p>
     * You might want to use removeRedundantChords() on the result.
     *
     * @param scs must have the same TimeSignature that this object.
     * @return A new SimpleChordSequence instance
     * @see #removeRedundantChords()
     */
    public SimpleChordSequence getMerged(SimpleChordSequence scs)
    {
        Preconditions.checkArgument(scs.getTimeSignature() == timeSignature);
        IntRange newRange = scs.getBarRange().getUnion(getBarRange());
        SimpleChordSequence res = new SimpleChordSequence(newRange, timeSignature);
        res.addAll(this);
        res.addAll(scs);
        return res;
    }

}

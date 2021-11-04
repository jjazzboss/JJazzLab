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
package org.jjazz.improvisionsupport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.song.api.BeatIterator;
import org.jjazz.song.api.Song;

/**
 * A play/rest scenario for a given song.
 * <p>
 * A change event is fired each time generatePlayRestValues() is called.
 */
public class PlayRestScenario
{

    public enum Level
    {
        LEVEL1, LEVEL2
    };

    public enum PlayRestValue
    {
        PLAY, REST;
    }

    public enum DenseSparseValue
    {
        DENSE, SPARSE, DENSE_SPARSE, SPARSE_DENSE;
    }


    private enum SeqPlayRestValue
    {
        PH(0.5f), P1(1f), P1H(1.5f), P2(2f), P2H(2.5f), P3(3f), P4(4f),
        RH(0.5f), R1(1f), R1H(1.5f), R2(2f), R2H(2.5f), R3(3f), R4(4f);

        private final float barDuration;


        /**
         *
         * @param barDuration A float value x.0 or x.5
         */
        SeqPlayRestValue(float barDuration)
        {
            checkArgument(barDuration > 0, "barDuration=%s", barDuration);
            this.barDuration = barDuration;
            float f = barDuration - (int) barDuration;
            checkArgument(f == 0 || f == 0.5f, "barDuration=%s", barDuration);
        }

        /**
         * The duration in bars.
         *
         * @param tempo
         * @return Can be 0.5, 1, 1.5, etc.
         */
        public float getDurationInBars(int tempo)
        {
            float res = barDuration;
            if (tempo > 200)
            {
                res = barDuration < 2 ? barDuration * 2 : barDuration + 1.5f;
            }
            return res;
        }

        /**
         * The duration in half-bars.
         *
         * @param tempo
         * @return
         */
        public int getDurationInHalfBars(int tempo)
        {
            return (int) (getDurationInBars(tempo) * 2);
        }

        public PlayRestValue toValue()
        {
            return name().startsWith("P") ? PlayRestValue.PLAY : PlayRestValue.REST;
        }

        /**
         * Get one PlayRestValue per half-bar.
         *
         * @return
         */
        public List<PlayRestValue> toHalfBarValues(int tempo)
        {
            var res = new ArrayList<PlayRestValue>();
            PlayRestValue value = toValue();
            for (int i = 0; i < getDurationInHalfBars(tempo); i++)
            {
                res.add(value);
            }
            return res;
        }

    }

    private static final SeqPlayRestValue[] SEQ_LEVEL1 = new SeqPlayRestValue[]
    {
        SeqPlayRestValue.R1, SeqPlayRestValue.P1, SeqPlayRestValue.R1, SeqPlayRestValue.P1,
        SeqPlayRestValue.R2, SeqPlayRestValue.P2,
        SeqPlayRestValue.R1, SeqPlayRestValue.P1, SeqPlayRestValue.R1, SeqPlayRestValue.P1,
        SeqPlayRestValue.R2, SeqPlayRestValue.P2,
        SeqPlayRestValue.P3, SeqPlayRestValue.R1,
        SeqPlayRestValue.R1, SeqPlayRestValue.P3
    };

    /**
     * The possible start indexes for SEQ_EASY (start on 4-bar boundaries)
     */
    private static final int[] SEQ_LEVEL1_START_INDEXES = new int[]
    {
        0, 4, 6, 10, 12, 14
    };


    private static final SeqPlayRestValue[] SEQ_LEVEL2 = new SeqPlayRestValue[]
    {
        SeqPlayRestValue.P2, SeqPlayRestValue.R3, SeqPlayRestValue.P1, SeqPlayRestValue.R2,
        SeqPlayRestValue.P3, SeqPlayRestValue.R2, SeqPlayRestValue.P2, SeqPlayRestValue.R1H,
        SeqPlayRestValue.P3, SeqPlayRestValue.R2, SeqPlayRestValue.P1H, SeqPlayRestValue.R2,
        SeqPlayRestValue.P1H, SeqPlayRestValue.R2, SeqPlayRestValue.P2, SeqPlayRestValue.R2H,
        SeqPlayRestValue.P1, SeqPlayRestValue.R1H, SeqPlayRestValue.P1H, SeqPlayRestValue.R3,
        SeqPlayRestValue.P2H, SeqPlayRestValue.R1, SeqPlayRestValue.P1, SeqPlayRestValue.R2H,
        SeqPlayRestValue.P2H, SeqPlayRestValue.R1, SeqPlayRestValue.P2, SeqPlayRestValue.R2,
        SeqPlayRestValue.P1H, SeqPlayRestValue.R1, SeqPlayRestValue.P2, SeqPlayRestValue.R1,
        SeqPlayRestValue.P2, SeqPlayRestValue.R1, SeqPlayRestValue.P1H, SeqPlayRestValue.R2,
    };

    private final Level level;
    private final Song songOriginal;
    private List<PlayRestValue> playRestValues;
    private List<DenseSparseValue> denseSparseValues;
    private static final Logger LOGGER = Logger.getLogger(PlayRestScenario.class.getName());


    public PlayRestScenario(Level level, Song song)
    {
        checkNotNull(song);
        checkNotNull(level);
        this.songOriginal = song;
        this.level = level;
    }


    /**
     * Generate a Play/Rest values for the current context.
     * <p>
     *
     * @return The list of PlayRestValues, one for each half-bar of the song.
     * @see #getPlayRestValues(int)
     * @see #getPlayRestValues()
     */
    public List<PlayRestValue> generatePlayRestValues()
    {
        playRestValues = new ArrayList<>();


        int seqIndex;
        SeqPlayRestValue[] seqValues;
        if (level.equals(Level.LEVEL1))
        {
            seqIndex = SEQ_LEVEL1_START_INDEXES[(int) Math.round((SEQ_LEVEL1_START_INDEXES.length - 1) * Math.random())];
            seqValues = SEQ_LEVEL1;
        } else
        {
            seqIndex = (int) Math.round((SEQ_LEVEL2.length - 1) * Math.random());
            seqValues = SEQ_LEVEL2;
        }


        BeatIterator it = new BeatIterator(songOriginal);
        LinkedList<PlayRestValue> remainingValues = new LinkedList<>();


        // Loop on every half-bar
        while (it.hasNextHalfBar(false))
        {
            if (remainingValues.isEmpty())
            {
                // Fetch a new SeqValue and the resulting values for each half-bar
                var seqValue = seqValues[seqIndex % seqValues.length];
                seqIndex++;
                remainingValues.addAll(seqValue.toHalfBarValues(songOriginal.getTempo()));
            }

            playRestValues.add(remainingValues.pop());

            it.next();

        }
        return playRestValues;
    }

    /**
     * Generate Play/Rest values and Dense/Sparse values for the current context.
     * <p>
     *
     * @return The list of DenseSparseValues, one per PlayRestValue.PLAY value.
     * @see #getDenseSparseValues()
     */
    public List<DenseSparseValue> generateDenseSparseValues(List<PlayRestValue> prValues)
    {
        checkArgument(prValues != null && !prValues.isEmpty(), "prValues=%s", prValues);
        denseSparseValues = new ArrayList<>();

        for (var prValue : prValues)
        {
            if (prValue.equals(PlayRestValue.PLAY))
            {
                int i = (int) Math.round(Math.random() * (DenseSparseValue.values().length - 1));
                denseSparseValues.add(DenseSparseValue.values()[i]);
            }
        }

        return denseSparseValues;

    }

    /**
     * Provide the 2 half-bar values for the specified bar.
     * <p>
     * Require that generatePlayRestValues() was called before, if it's not the case return an empty list.
     *
     * @param barIndex
     * @return A list with 2 values.
     * @see #generatePlayRestValues()
     */
    public List<PlayRestValue> getPlayRestValues(int barIndex)
    {
        checkArgument(barIndex >= 0, "barIndex=%s", barIndex);
        var res = new ArrayList<PlayRestValue>();
        int halfBarIndex = 2 * barIndex;
        if (playRestValues == null || halfBarIndex + 1 >= playRestValues.size())
        {
            return res;
        }
        res.addAll(playRestValues.subList(halfBarIndex, halfBarIndex + 2));
        return res;
    }

    /**
     * The the play/rest values, one per half-bar.
     *
     * @return
     * @see #generatePlayRestValues()
     */
    public List<PlayRestValue> getPlayRestValues()
    {
        return new ArrayList<>(playRestValues);
    }

    /**
     * The dense/sparse values, one per PlayRestValue.PLAY value.
     *
     * @return
     * @see #generateDenseSparseValues()
     */
    public List<DenseSparseValue> getDenseSparseValues()
    {
        return new ArrayList<>(denseSparseValues);
    }

    public Level getLevel()
    {
        return level;
    }

    public Song getSongOriginal()
    {
        return songOriginal;
    }


    // =======================================================================
    // Private methods
    // =======================================================================    
}

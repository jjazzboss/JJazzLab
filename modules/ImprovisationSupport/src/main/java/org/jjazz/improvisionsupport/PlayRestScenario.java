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
package org.jjazz.improvisionsupport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.song.api.BeatIterator;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.IntRange;

/**
 * A play/rest scenario for a given song.
 * <p>
 * A change event is fired each time generatePlayRestValues() is called.
 */
public class PlayRestScenario
{

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
    private final IntRange barRange;
    private List<PlayRestValue> playRestValues;
    private List<DenseSparseValue> denseSparseValues;
    private static final Logger LOGGER = Logger.getLogger(PlayRestScenario.class.getName());


    public PlayRestScenario(Level level, Song song)
    {
        this(level, song, song.getSongStructure().getBarRange());
    }

    public PlayRestScenario(Level level, Song song, IntRange barRange)
    {
        checkNotNull(song);
        checkNotNull(level);
        checkNotNull(barRange);
        checkArgument(song.getSongStructure().getBarRange().contains(barRange), "song=%s barRange=%s", song, barRange);

        this.songOriginal = song;
        this.level = level;
        this.barRange = barRange;
        playRestValues = new ArrayList<>();
        denseSparseValues = new ArrayList<>();
    }

    /**
     * Copy values from the specified scenario into this scenario, for matching song bar indexes.
     *
     * @param scenario
     */
    public void importValues(PlayRestScenario scenario)
    {
        IntRange br = barRange.getIntersection(scenario.getBarRange());
        if (!br.isEmpty())
        {
            for (int barIndex = br.from; barIndex < br.size(); barIndex++)
            {
                if (scenario.isPlayRestDataAvailable(barIndex) && isPlayRestDataAvailable(barIndex))
                {
                    var prValues = scenario.getPlayRestValues(barIndex);
                    int index = (barIndex - barRange.from) * 2;
                    playRestValues.set(index, prValues.get(0));
                    playRestValues.set(index + 1, prValues.get(1));
                }

                if (scenario.isDenseSparseDataAvailable(barIndex) && isDenseSparseDataAvailable(barIndex))
                {
                    var prValues = scenario.getDenseSparseValues(barIndex);
                    int index = (barIndex - barRange.from) * 2;
                    denseSparseValues.set(index, prValues.get(0));
                    denseSparseValues.set(index + 1, prValues.get(1));
                }
            }
        }
    }

    /**
     * Tell if some PlayRest data is available for the specified bar.
     *
     * @param barIndex
     * @return
     */
    public boolean isPlayRestDataAvailable(int barIndex)
    {
        return barRange.contains(barIndex) && (2 * (barIndex - barRange.from) + 1) < playRestValues.size();
    }

    /**
     * Tell if some DenseSparse data is available for the specified bar.
     *
     * @param barIndex
     * @return
     */
    public boolean isDenseSparseDataAvailable(int barIndex)
    {
        return barRange.contains(barIndex) && (2 * (barIndex - barRange.from) + 1) < denseSparseValues.size();
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


        BeatIterator it = new BeatIterator(songOriginal, barRange);
        LinkedList<PlayRestValue> remainingValues = new LinkedList<>();


        // Loop on every half-bar
        while (it.hasNextHalfBar(false))
        {
            it.nextHalfBar(false);

            if (remainingValues.isEmpty())
            {
                // Fetch a new SeqValue and the resulting values for each half-bar
                var seqValue = seqValues[seqIndex % seqValues.length];
                seqIndex++;
                remainingValues.addAll(seqValue.toHalfBarValues(songOriginal.getTempo()));
            }

            playRestValues.add(remainingValues.pop());
        }
        return playRestValues;
    }

    /**
     * Generate Dense/Sparse values for the specified PlayRestValue.PLAY values.
     * <p>
     *
     * @return The list of DenseSparseValues, one per PlayRestValue/half-bar. If PlayRestValue==REST, the corresponding
     * DenseSparseValue is null.
     * @see #getDenseSparseValues()
     */
    public List<DenseSparseValue> generateDenseSparseValues(List<PlayRestValue> prValues)
    {
        checkArgument(prValues != null, "prValues=%s", prValues);
        denseSparseValues = new ArrayList<>();

        int playCount = 0;

        for (int i = 0; i < prValues.size(); i++)
        {
            var prValue = prValues.get(i);
            boolean last = i == prValues.size() - 1;


            if (!last && prValue.equals(PlayRestValue.PLAY))
            {
                playCount++;

            } else
            {
                if (last && prValue.equals(PlayRestValue.PLAY))
                {
                    playCount++;
                }

                if (playCount > 0)
                {
                    // There was a series of Plays
                    if (playCount < getDualRhythmDensityNbHalfBarMin() || Math.random() < 0.3)
                    {
                        // The play series is too short for 2 values, or it's not too short but random says use only 1 value
                        DenseSparseValue dsv = Math.random() > 0.5 ? DenseSparseValue.DENSE : DenseSparseValue.SPARSE;
                        while (playCount > 0)
                        {
                            denseSparseValues.add(dsv);
                            playCount--;
                        }
                    } else
                    {
                        // 2 values
                        var dsv0 = Math.random() > 0.5 ? DenseSparseValue.DENSE : DenseSparseValue.SPARSE;
                        var dsv1 = dsv0.getOther();
                        int half = playCount / 2;
                        while (playCount > 0)
                        {
                            denseSparseValues.add(playCount > half ? dsv0 : dsv1);
                            playCount--;
                        }
                    }
                }

                // For the current rest 
                if (prValue.equals(PlayRestValue.REST))
                {
                    denseSparseValues.add(null);
                }
            }
        }


        return denseSparseValues;

    }

    /**
     * Provide the 2 half-bar values for the specified bar.
     * <p>
     * Require that generatePlayRestValues() was called before.
     *
     * @param barIndex
     * @return A list with 2 values.
     * @see #generatePlayRestValues()
     * @see #isPlayRestDataAvailable(int) 
     */
    public List<PlayRestValue> getPlayRestValues(int barIndex)
    {
        checkArgument(barRange.contains(barIndex), "barRange=%s barIndex=%s", barRange, barIndex);

        var res = new ArrayList<PlayRestValue>();
        int halfBarIndex = 2 * (barIndex - barRange.from);
        if (halfBarIndex + 1 >= playRestValues.size())
        {
            throw new IllegalArgumentException("barIndex=" + barIndex + " halfBarIndex+1=" + (halfBarIndex + 1) + " playRestValues=" + playRestValues);
        }
        res.addAll(playRestValues.subList(halfBarIndex, halfBarIndex + 2));
        return res;
    }

    /**
     * Provide the 2 half-bar values for the specified bar.
     * <p>
     * Require that generateDenseSparseValues() was called before, if it's not the case return an empty list.
     *
     * @param barIndex
     * @return A list with 2 values.
     * @see #getDenseSparseValues()
     * @see #isDenseSparseDataAvailable(int) 
     */
    public List<DenseSparseValue> getDenseSparseValues(int barIndex)
    {
        checkArgument(barRange.contains(barIndex), "barRange=%s barIndex=%s", barRange, barIndex);

        var res = new ArrayList<DenseSparseValue>();
        int halfBarIndex = 2 * (barIndex - barRange.from);
        if (halfBarIndex + 1 >= denseSparseValues.size())
        {
            return res;
        }
        res.addAll(denseSparseValues.subList(halfBarIndex, halfBarIndex + 2));
        return res;
    }

    /**
     * The the play/rest values, one per half-bar.
     *
     * @return Empty list if not generated yet
     * @see #generatePlayRestValues()
     */
    public List<PlayRestValue> getPlayRestValues()
    {
        return new ArrayList<>(playRestValues);
    }

    /**
     * The dense/sparse values, one per PlayRestValue.PLAY value.
     *
     * @return Empty list if not generated yet
     * @see #generateDenseSparseValues()
     */
    public List<DenseSparseValue> getDenseSparseValues()
    {
        return new ArrayList<>(denseSparseValues);
    }

    public IntRange getBarRange()
    {
        return barRange;
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
    /**
     * Minimum bar length to accomodate 2 DenseSparseValues.
     *
     * @return
     */
    private int getDualRhythmDensityNbHalfBarMin()
    {
        int res;
        int tempo = songOriginal.getTempo();
        if (tempo < 95)
        {
            // Slow
            res = 3;
        } else if (tempo < 180)
        {
            // medium
            res = 4;
        } else
        {
            // Fast
            res = 6;
        }
        return res;
    }


    // =======================================================================
    // Inner classes
    // =======================================================================    
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
        DENSE, SPARSE;

        public DenseSparseValue getOther()
        {
            return this.equals(DENSE) ? SPARSE : DENSE;
        }
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

}

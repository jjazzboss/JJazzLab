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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
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
        EASY, MEDIUM
    };

    public enum Value
    {
        PLAY, REST;

//        int lengthSlow, lenghtMedium, lengthFast;
//
//
//        Value(int lengthSlow, int lenghtMedium, int lengthFast)
//        {
//            this.lengthSlow = lengthSlow;
//            this.lenghtMedium = lenghtMedium;
//            this.lengthFast = lengthFast;
//        }
//
//        public int getLengthSlow()
//        {
//            return lengthSlow;
//        }
//
//        public int getLenghtMedium()
//        {
//            return lenghtMedium;
//        }
//
//        public int getLengthFast()
//        {
//            return lengthFast;
//        }
    }


    private enum SeqValue
    {
        PH(0.5f), P1(1f), P1H(1.5f), P2(2f), P2H(2.5f), P3(3f), P4(4f),
        RH(0.5f), R1(1f), R1H(1.5f), R2(2f), R2H(2.5f), R3(3f), R4(4f);

        private final float barDuration;


        /**
         *
         * @param barDuration A float value x.0 or x.5
         */
        SeqValue(float barDuration)
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

        public Value toValue()
        {
            return name().startsWith("P") ? Value.PLAY : Value.REST;
        }

        /**
         * Get one Value per half-bar.
         *
         * @return
         */
        public List<Value> toHalfBarValues(int tempo)
        {
            var res = new ArrayList<Value>();
            Value value = toValue();
            for (int i = 0; i < getDurationInHalfBars(tempo); i++)
            {
                res.add(value);
            }
            return res;
        }

    }

    private static final SeqValue[] SEQ_EASY = new SeqValue[]
    {
        SeqValue.R1, SeqValue.P1, SeqValue.R1, SeqValue.P1,
        SeqValue.R2, SeqValue.P2,
        SeqValue.R1, SeqValue.P1, SeqValue.R1, SeqValue.P1,
        SeqValue.R2, SeqValue.P2,
        SeqValue.P3, SeqValue.R1,
        SeqValue.R1, SeqValue.P3
    };
    
    /**
     * The possible start indexes for SEQ_EASY (start on 4-bar boundaries)
     */
    private static final int[] SEQ_EASY_START_INDEXES = new int[]
    {
      0, 4, 6, 10, 12, 14  
    };


    private static final SeqValue[] SEQ_MEDIUM = new SeqValue[]
    {
        SeqValue.P2, SeqValue.R3, SeqValue.P1, SeqValue.R2,
        SeqValue.P3, SeqValue.R2, SeqValue.P2, SeqValue.R1H,
        SeqValue.P3, SeqValue.R2, SeqValue.P1H, SeqValue.R2,
        SeqValue.P1H, SeqValue.R2, SeqValue.P2, SeqValue.R2H,
        SeqValue.P1, SeqValue.R1H, SeqValue.P1H, SeqValue.R3,
        SeqValue.P2H, SeqValue.R1, SeqValue.P1, SeqValue.R2H,
        SeqValue.P2H, SeqValue.R1, SeqValue.P2, SeqValue.R2,
        SeqValue.P1H, SeqValue.R1, SeqValue.P2, SeqValue.R1,
        SeqValue.P2, SeqValue.R1, SeqValue.P1H, SeqValue.R2,
    };

    private final Level level;
    private final Song songOriginal;
    private List<Value> values;
    private static final Logger LOGGER = Logger.getLogger(PlayRestScenario.class.getName());


    public PlayRestScenario(Level level, Song song)
    {
        checkNotNull(song);
        checkNotNull(level);
        this.songOriginal = song;
        this.level = level;
    }

    /**
     * Regenerate the scenario for the current context.
     * <p>
     * Fire a change event.
     *
     * @return The list of Values, one for each half-bar of the song.
     * @see #getPlayRestValues(int)
     * @see #getPlayRestValues()
     */
    public List<Value> generatePlayRestValues()
    {
        values = new ArrayList<>();


        int seqIndex;
        SeqValue[] seqValues;
        if (level.equals(Level.EASY))
        {
            seqIndex = SEQ_EASY_START_INDEXES[(int) Math.round((SEQ_EASY_START_INDEXES.length - 1) * Math.random())];
            seqValues = SEQ_EASY;
        } else
        {
            seqIndex = (int) Math.round((SEQ_MEDIUM.length - 1) * Math.random());
            seqValues = SEQ_MEDIUM;
        }


        BeatIterator it = new BeatIterator(songOriginal);
        LinkedList<Value> remainingValues = new LinkedList<>();


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

            values.add(remainingValues.pop());

            it.next();

        }
        return values;
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
    public List<Value> getPlayRestValues(int barIndex)
    {
        checkArgument(barIndex >= 0, "barIndex=%s", barIndex);
        var res = new ArrayList<Value>();
        int halfBarIndex = 2 * barIndex;
        if (values == null || halfBarIndex + 1 >= values.size())
        {
            return res;
        }
        res.addAll(values.subList(halfBarIndex, halfBarIndex + 2));
        return res;
    }

    /**
     * The the play/rest values, one per half-bar.
     *
     * @return
     * @see #generatePlayRestValues()
     */
    public List<Value> getPlayRestValues()
    {
        return new ArrayList<>(values);
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

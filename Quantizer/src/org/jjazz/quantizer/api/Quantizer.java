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
package org.jjazz.quantizer.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import static org.jjazz.quantizer.api.Quantization.BEAT;
import static org.jjazz.quantizer.api.Quantization.HALF_BAR;
import static org.jjazz.quantizer.api.Quantization.HALF_BEAT;
import static org.jjazz.quantizer.api.Quantization.OFF;
import static org.jjazz.quantizer.api.Quantization.ONE_QUARTER_BEAT;
import static org.jjazz.quantizer.api.Quantization.ONE_THIRD_BEAT;

/**
 * Provide quantize related methods and properties.
 */
public class Quantizer
{

    private static Quantizer INSTANCE;
    private Quantization qValue;

    public static Quantizer getInstance()
    {
        synchronized (Quantizer.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new Quantizer();
            }
        }
        return INSTANCE;
    }

    private Quantizer()
    {
        qValue = Quantization.ONE_QUARTER_BEAT;
    }

    /**
     * Return the closest quantized position using the Quantizer's quantization value.
     *
     * @param pos         The original position.
     * @param ts          The TimeSignature for the original position.
     * @param maxBarIndex The quantized position can not exceed this maximum bar index.
     * @return
     * @see #getQuantized(org.jjazz.quantizer.api.Quantization, org.jjazz.leadsheet.chordleadsheet.api.item.Position,
     * org.jjazz.harmony.api.TimeSignature, int)
     */
    public Position getQuantized(Position pos, TimeSignature ts, int maxBarIndex)
    {
        return Quantizer.getQuantized(qValue, pos, ts, maxBarIndex);
    }

    /**
     * Return the closest quantized position using the specified quantization setting.
     *
     * @param q           The quantization setting
     * @param pos         The original position.
     * @param ts          The TimeSignature for the original position.
     * @param maxBarIndex The quantized position can not exceed this maximum bar index.
     * @return
     */
    static public Position getQuantized(Quantization q, Position pos, TimeSignature ts, int maxBarIndex)
    {
        if (q == null || !ts.checkBeat(pos.getBeat()) || pos.getBar() > maxBarIndex)
        {
            throw new IllegalArgumentException("q=" + q + " pos=" + pos + " ts=" + ts + " maxBarIndex=" + maxBarIndex);   //NOI18N
        }
        Position newPos;
        switch (q)
        {
            case OFF:
                newPos = new Position(pos);
                break;
            case HALF_BAR:
                newPos = quantizeHalfBar(pos, ts, maxBarIndex, false);      // Half-bar straight
                break;
            case BEAT:
            case HALF_BEAT:
            case ONE_THIRD_BEAT:
            case ONE_QUARTER_BEAT:
                newPos = quantizeStandard(pos, ts, maxBarIndex, q.getBeats());
                break;

            default:
                throw new IllegalStateException("quantization=" + q);   //NOI18N
        }

        return newPos;
    }

    /**
     * Quantize beatPos using the specified quantization setting.
     *
     * @param q
     * @param beatPos
     * @return
     */
    static public float getQuantized(Quantization q, float beatPos)
    {
        checkNotNull(q);
        checkArgument(beatPos >= 0, "q=%s beatPos=%s", q, beatPos);
        float res;
        switch (q)
        {
            case OFF:
            case HALF_BAR:
                res = beatPos;      // We don't know the TimeSignature...
                break;
            case BEAT:
            case HALF_BEAT:
            case ONE_THIRD_BEAT:
            case ONE_QUARTER_BEAT:
                res = quantizeStandard(beatPos, q.getBeats());
                break;
            default:
                throw new IllegalStateException("quantization=" + q);   //NOI18N
        }

        return res;
    }

    /**
     * Get the next quantized position after beatPos.
     * <p>
     * If beatPos is already a quantized position, return, beatPos.
     *
     * @param q
     * @param beatPos
     * @return
     */
    static public float getQuantizedNext(Quantization q, float beatPos)
    {
        checkNotNull(q);
        checkArgument(beatPos >= 0, "q=%s beatPos=%s", q, beatPos);
        float res;
        switch (q)
        {
            case OFF:
            case HALF_BAR:
                res = beatPos;      // We don't know the TimeSignature...
                break;
            case BEAT:
            case HALF_BEAT:
            case ONE_THIRD_BEAT:
            case ONE_QUARTER_BEAT:
                float beatInt = (float) Math.floor(beatPos);
                float beatDecimal = beatPos - beatInt;
                Float nextBeatDecimal = q.getBeatsAsTreeSet().ceiling(beatDecimal);
                assert nextBeatDecimal != null : "beatPos=" + beatPos + " q=" + q;
                res = beatInt + nextBeatDecimal;
                break;
            default:
                throw new IllegalStateException("quantization=" + q);   //NOI18N
        }

        return res;
    }

    /**
     * Get the previous quantized position before beatPos.
     * <p>
     * If beatPos is already a quantized position, return beatPos.
     *
     * @param q
     * @param beatPos
     * @return
     */
    static public float getQuantizedPrevious(Quantization q, float beatPos)
    {
        checkNotNull(q);
        checkArgument(beatPos >= 0, "q=%s beatPos=%s", q, beatPos);
        float res;
        switch (q)
        {
            case OFF:
            case HALF_BAR:
                res = beatPos;      // We don't know the TimeSignature...
                break;
            case BEAT:
            case HALF_BEAT:
            case ONE_THIRD_BEAT:
            case ONE_QUARTER_BEAT:
                float beatInt = (float) Math.floor(beatPos);
                float beatDecimal = beatPos - beatInt;
                Float previousBeatDecimal = q.getBeatsAsTreeSet().floor(beatDecimal);
                assert previousBeatDecimal != null : "beatPos=" + beatPos + " q=" + q;
                res = beatInt + previousBeatDecimal;
                break;
            default:
                throw new IllegalStateException("quantization=" + q);   //NOI18N
        }

        return res;
    }

    public void setQuantizationValue(Quantization q)
    {
        qValue = q;
    }

    public Quantization getQuantizationValue()
    {
        return qValue;
    }

    public Quantization getDefaultQuantizationValue(TimeSignature ts)
    {
        Quantization q;
        switch (ts)
        {
            case THREE_FOUR:
            case FOUR_FOUR:
                q = Quantization.HALF_BEAT;
                break;
            case FIVE_FOUR:
            case SIX_FOUR:
            case SEVEN_FOUR:
            case SIX_EIGHT:
            case TWELVE_EIGHT:
            default:
                q = Quantization.BEAT;
        }
        return q;
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================    
    static private Position quantizeHalfBar(Position pos, TimeSignature ts, int maxBarIndex, boolean swing)
    {
        int bar = pos.getBar();
        float beat = pos.getBeat();
        float halfBeat = ts.getHalfBarBeat(swing);
        if (beat < (halfBeat / 2))
        {
            beat = 0;
        } else if (beat < (3 * halfBeat / 2))
        {
            beat = halfBeat;
        } else if (bar < maxBarIndex)
        {
            // Next bar OK
            bar++;
            beat = 0;
        } else
        {
            // Can't go next bar, stay on last half-beat
            beat = halfBeat;
        }
        return new Position(bar, beat);
    }

    static private Position quantizeStandard(Position pos, TimeSignature ts, int maxBarIndex, float[] qPoints)
    {
        float beatInt = (float) Math.floor(pos.getBeat());
        float beatDecimal = pos.getBeat() - beatInt;
        int bar = pos.getBar();
        int nbPoints = qPoints.length;
        for (int i = 0; i < (nbPoints - 1); i++)
        {
            if (beatDecimal == qPoints[i] || beatDecimal == qPoints[i + 1])
            {
                // Already quantized
                break;
            }
            if (beatDecimal < qPoints[i + 1])
            {
                // beat is in the range of these 2 values
                float lower = qPoints[i];
                float upper = qPoints[i + 1];
                beatDecimal = (beatDecimal < ((lower + upper) / 2)) ? lower : upper;
                break;
            }
        }

        // Check if we reached the next bar
        Position newPos;
        if (ts.checkBeat(beatInt + beatDecimal))
        {
            newPos = new Position(bar, beatInt + beatDecimal);
        } else if ((bar + 1) <= maxBarIndex)
        {
            // Go to next bar
            newPos = new Position(bar + 1, 0);
        } else
        {
            // Can't go to next bar, stick to last possible position
            newPos = new Position(bar, beatInt + qPoints[nbPoints - 2]);
        }

        return newPos;
    }

    private static float quantizeStandard(float beatPos, float[] qPoints)
    {
        float res = beatPos;
        float beatInt = (float) Math.floor(beatPos);
        float beatDecimal = beatPos - beatInt;
        int nbPoints = qPoints.length;

        for (int i = 0; i < (nbPoints - 1); i++)
        {
            if (beatDecimal == qPoints[i] || beatDecimal == qPoints[i + 1])
            {
                // Already quantized
                break;
            }
            if (beatDecimal < qPoints[i + 1])
            {
                // beat is in the range of these 2 values
                float lower = qPoints[i];
                float upper = qPoints[i + 1];
                beatDecimal = (beatDecimal < ((lower + upper) / 2)) ? lower : upper;
                res = beatInt + beatDecimal;
                break;
            }
        }

        return res;
    }

}

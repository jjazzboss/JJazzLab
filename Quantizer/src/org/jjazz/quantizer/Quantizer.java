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
package org.jjazz.quantizer;

import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.openide.util.NbPreferences;

/**
 * Provide quantize related methods and properties.
 */
public class Quantizer
{

    private static Quantizer INSTANCE;
    private Quantization qValue;
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static Preferences prefs = NbPreferences.forModule(Quantizer.class);

    /**
     * All possible valid positions within one beat.
     */
    private static float[] QPOINTS_ONE_QUARTER_BEAT = new float[]
    {
        0f, .25f, .5f, .75f, 1f
    };
    private static float[] QPOINTS_ONE_THIRD_BEAT = new float[]
    {
        0f, .3333333f, .6666666f, 1f
    };
    private static float[] QPOINTS_HALF_BEAT = new float[]
    {
        0f, .5f, 1f
    };
    private static float[] QPOINTS_BEAT = new float[]
    {
        0f, 1f
    };

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
     * Return a quantized position using the Quantizer's quantization value.
     *
     * @param pos The original position.
     * @param ts The TimeSignature for the original position.
     * @param maxBarIndex The quantized position can not exceed this maximum bar index.
     * @return
     */
    public Position quantize(Position pos, TimeSignature ts, int maxBarIndex)
    {
        if (!ts.checkBeat(pos.getBeat()) || pos.getBar() > maxBarIndex)
        {
            throw new IllegalArgumentException("pos=" + pos + " ts=" + ts);
        }
        Position newPos;
        switch (qValue)
        {
            case OFF:
                newPos = new Position(pos);
                break;
            case HALF_BAR:
                newPos = quantizeHalfBar(pos, ts, maxBarIndex);
                break;
            case BEAT:
                newPos = quantizeStandard(pos, ts, maxBarIndex, QPOINTS_BEAT);
                break;
            case HALF_BEAT:
                newPos = quantizeStandard(pos, ts, maxBarIndex, QPOINTS_HALF_BEAT);
                break;
            case ONE_THIRD_BEAT:
                newPos = quantizeStandard(pos, ts, maxBarIndex, QPOINTS_ONE_THIRD_BEAT);
                break;
            case ONE_QUARTER_BEAT:
                newPos = quantizeStandard(pos, ts, maxBarIndex, QPOINTS_ONE_QUARTER_BEAT);
                break;
            default:
                throw new IllegalStateException("quantization=" + qValue);
        }

        return newPos;
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

    public void addPropertyListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================    
    private Position quantizeHalfBar(Position pos, TimeSignature ts, int maxBarIndex)
    {
        int bar = pos.getBar();
        float beat = pos.getBeat();
        float halfBeat = ts.getHalfBarBeat();
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

    private Position quantizeStandard(Position pos, TimeSignature ts, int maxBarIndex, float[] qPoints)
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

}

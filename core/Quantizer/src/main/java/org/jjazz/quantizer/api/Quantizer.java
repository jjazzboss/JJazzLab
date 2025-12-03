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
package org.jjazz.quantizer.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.Preferences;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.api.Position;
import static org.jjazz.quantizer.api.Quantization.BEAT;
import static org.jjazz.quantizer.api.Quantization.HALF_BAR;
import static org.jjazz.quantizer.api.Quantization.HALF_BEAT;
import static org.jjazz.quantizer.api.Quantization.OFF;
import static org.jjazz.quantizer.api.Quantization.ONE_QUARTER_BEAT;
import static org.jjazz.quantizer.api.Quantization.ONE_THIRD_BEAT;
import org.openide.util.NbPreferences;

/**
 * Provide quantize related methods and properties.
 */
public class Quantizer
{

    public static final String PROP_ITERATIVE_QUANTIZE_STRENGTH = "IterativeQuantizeStrength";
    public static final String PROP_ITERATIVE_ENABLED = "IterativeEnabled";
    private static final float DEFAULT_ITERATIVE_QUANTIZE_STRENGTH = 0.15f;
    private static final float ROUND_BEAT_WINDOW = 0.01f;
    private static Quantizer INSTANCE;
    private Quantization qValue;
    private float iterativeQuantizeStrength;
    private boolean iterativeEnabled;
    private static Preferences prefs = NbPreferences.forModule(Quantizer.class);
    private transient final PropertyChangeSupport pcs = new PropertyChangeSupport(this);


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
        iterativeQuantizeStrength = prefs.getFloat(PROP_ITERATIVE_QUANTIZE_STRENGTH, DEFAULT_ITERATIVE_QUANTIZE_STRENGTH);
        iterativeEnabled = prefs.getBoolean(PROP_ITERATIVE_ENABLED, true);
    }

    /**
     * Convenience method to get the current quantization strength.
     *
     * @return 1f if isIterativeQuantizedEnabled() is false, otherwise return getIterativeQuantizeStrength().
     */
    public float getQuantizeStrength()
    {
        return iterativeEnabled ? iterativeQuantizeStrength : 1f;
    }

    /**
     *
     * @return @see #setIterativeQuantizeStrength(float)
     */
    public float getIterativeQuantizeStrength()
    {
        return iterativeQuantizeStrength;
    }

    /**
     * Set the iterative quantize strength.
     * <p>
     * 1 means hard quantize. 0.2f means note is moved 0.2f * half_quantize_distance.
     *
     * @param strength A value in the ]0;1] range.
     */
    public void setIterativeQuantizeStrength(float strength)
    {
        Preconditions.checkArgument(strength > 0 && strength <= 1f, "strength=%s", strength);
        float old = this.iterativeQuantizeStrength;
        this.iterativeQuantizeStrength = strength;
        prefs.putFloat(PROP_ITERATIVE_QUANTIZE_STRENGTH, strength);
        pcs.firePropertyChange(PROP_ITERATIVE_QUANTIZE_STRENGTH, old, strength);
    }


    /**
     * Check if iterative quantize is enabled.
     * <p>
     * This is enabled by default.
     *
     * @return
     */
    public boolean isIterativeQuantizeEnabled()
    {
        return iterativeEnabled;
    }

    /**
     * Set the value of iterativeEnabled
     *
     * @param b new value of iterativeEnabled
     */
    public void setIterativeQuantizeEnabled(boolean b)
    {
        boolean old = this.iterativeEnabled;
        this.iterativeEnabled = b;
        prefs.putBoolean(PROP_ITERATIVE_ENABLED, b);
        pcs.firePropertyChange(PROP_ITERATIVE_ENABLED, old, b);
    }

    /**
     * Return the closest quantized position using the Quantizer's global settings.
     *
     * @param pos         The original position.
     * @param ts          The TimeSignature for the original position.
     * @param maxBarIndex The quantized position can not exceed this maximum bar index.
     * @return
     * @see #getQuantized(org.jjazz.harmony.api.Position, org.jjazz.harmony.api.TimeSignature, int) 
     * @see #isIterativeQuantizeEnabled()
     * @see #getIterativeQuantizeStrength()
     */
    public Position getQuantized(Position pos, TimeSignature ts, int maxBarIndex)
    {
        return Quantizer.getQuantized(qValue, pos, ts, isIterativeQuantizeEnabled() ? getIterativeQuantizeStrength() : 1f, maxBarIndex);
    }

    /**
     * Return the closest quantized position using the specified quantization settings.
     *
     * @param q           The quantization setting
     * @param pos         The original position.
     * @param ts          The TimeSignature for the original position.
     * @param qStrength   A value in the ]0;1] range. 1 means hard quantize. 0.2f means note is moved half_quantize_distance*0.2f.
     * @param maxBarIndex The quantized position can not exceed this maximum bar index.
     * @return
     */
    static public Position getQuantized(Quantization q, Position pos, TimeSignature ts, float qStrength, int maxBarIndex)
    {
        Preconditions.checkNotNull(q);
        Preconditions.checkNotNull(pos);
        Preconditions.checkNotNull(ts);
        Preconditions.checkArgument(ts.checkBeat(pos.getBeat()), "ts=%s pos=%s", ts, pos);
        Preconditions.checkArgument(pos.getBar() <= maxBarIndex, "pos=%s maxBarIndex=%s", pos, maxBarIndex);
        Preconditions.checkArgument(qStrength > 0 && qStrength <= 1, "qStrength=%s", qStrength);

        Position newPos = switch (q)
        {
            case OFF ->
                new Position(pos);
            case HALF_BAR ->
                quantizeHalfBar(pos, ts, maxBarIndex, false);      // Half-bar straight
            default ->
                quantizeImpl(pos, ts, maxBarIndex, qStrength, q.getBeats());
        };

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

        float res = switch (q)
        {
            case OFF, HALF_BAR ->
                beatPos;      // We don't know the TimeSignature...
            default ->
                quantizeImpl(beatPos, q.getBeats());
        };

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
            case OFF, HALF_BAR ->
                res = beatPos;      // We don't know the TimeSignature...
            case BEAT, HALF_BEAT, ONE_THIRD_BEAT, ONE_QUARTER_BEAT, ONE_SIXTH_BEAT ->
            {
                float beatInt = (float) Math.floor(beatPos);
                float beatDecimal = beatPos - beatInt;
                Float nextBeatDecimal = q.getBeatsAsTreeSet().ceiling(beatDecimal);
                assert nextBeatDecimal != null : "beatPos=" + beatPos + " q=" + q;
                return beatInt + nextBeatDecimal;
            }
            default ->
                throw new IllegalStateException("quantization=" + q);
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
            case OFF, HALF_BAR ->
                res = beatPos;      // We don't know the TimeSignature...
            case BEAT, HALF_BEAT, ONE_THIRD_BEAT, ONE_QUARTER_BEAT, ONE_SIXTH_BEAT ->
            {
                float beatInt = (float) Math.floor(beatPos);
                float beatDecimal = beatPos - beatInt;
                Float previousBeatDecimal = q.getBeatsAsTreeSet().floor(beatDecimal);
                assert previousBeatDecimal != null : "beatPos=" + beatPos + " q=" + q;
                res = beatInt + previousBeatDecimal;
            }
            default ->
                throw new IllegalStateException("quantization=" + q);
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

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }


    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
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

    /**
     * Perform the quantize.
     *
     * @param pos
     * @param ts
     * @param maxBarIndex
     * @param qStrength   A value in the [0;1] range. 1 means a hard quantize.
     * @param qPoints
     * @return
     */
    static private Position quantizeImpl(Position pos, TimeSignature ts, int maxBarIndex, float qStrength, float[] qPoints)
    {
        float beatInt = (float) Math.floor(pos.getBeat());
        float beatFractionalPart = pos.getBeatFractionalPart();
        int bar = pos.getBar();
        int nbPoints = qPoints.length;
        for (int i = 0; i < (nbPoints - 1); i++)
        {
            if (beatFractionalPart == qPoints[i] || beatFractionalPart == qPoints[i + 1])
            {
                // Already quantized
                break;
            }
            if (beatFractionalPart < qPoints[i + 1])
            {
                // beat is in the range of these 2 values
                float lower = qPoints[i];
                float upper = qPoints[i + 1];
                float toUpper = upper - beatFractionalPart;
                float toLower = beatFractionalPart - lower;
                float step = ((upper - lower) / 2) * qStrength;
                if (toLower < toUpper)
                {
                    // Go to lower
                    beatFractionalPart -= step;
                    if (beatFractionalPart - lower <= ROUND_BEAT_WINDOW)
                    {
                        beatFractionalPart = lower;
                    }
                } else
                {
                    // Go to upper
                    beatFractionalPart += step;
                    if (upper - beatFractionalPart <= ROUND_BEAT_WINDOW)
                    {
                        beatFractionalPart = upper;
                    }
                }
                break;
            }
        }

        // Check if we reached the next bar
        Position newPos;
        if (ts.checkBeat(beatInt + beatFractionalPart))
        {
            newPos = new Position(bar, beatInt + beatFractionalPart);
        } else if ((bar + 1) <= maxBarIndex)
        {
            // Go to next bar
            newPos = new Position(bar + 1);
        } else
        {
            // Can't go to next bar, stick to last possible position
            newPos = new Position(bar, beatInt + qPoints[nbPoints - 2]);
        }

        return newPos;
    }

    private static float quantizeImpl(float beatPos, float[] qPoints)
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

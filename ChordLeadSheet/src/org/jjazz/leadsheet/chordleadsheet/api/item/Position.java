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
package org.jjazz.leadsheet.chordleadsheet.api.item;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import static org.jjazz.leadsheet.chordleadsheet.api.item.Bundle.CTL_InvalidValue;
import static org.jjazz.leadsheet.chordleadsheet.api.item.Bundle.CTL_MissingEnclosingChars;
import static org.jjazz.leadsheet.chordleadsheet.api.item.Bundle.CTL_NegativeValue;
import org.openide.util.NbBundle.Messages;

/**
 * A position in a leadsheet.
 */
public final class Position implements Comparable<Position>, Serializable
{

    public static final String PROP_BAR = "PropBar";
    public static final String PROP_BEAT = "PropBeat";
    /**
     * Separator char, e.g ':' in "[8:2.5])".
     */
    public static final char SEPARATOR_CHAR = ':';
    public static final char START_CHAR = '[';
    public static final char END_CHAR = ']';
    /**
     * The index of the bar
     */
    private int bar;
    /**
     * The beat within the bar
     */
    private float beat;
    private static final Logger LOGGER = Logger.getLogger(Position.class.getSimpleName());
    private transient final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private String string;

    /**
     * Get the value of string
     *
     * @return the value of string
     */
    public String getString()
    {
        return string;
    }

    /**
     * Set the value of string
     *
     * @param string new value of string
     */
    public void setString(String string)
    {
        this.string = string;
    }

    /**
     * Equivalent to Position(0,0)
     */
    public Position()
    {
        this(0, 0f);
    }

    public Position(Position pos)
    {
        this(pos.getBar(), pos.getBeat());
    }

    /**
     * @param bar The index of the bar (&gt;=0).
     * @param beat The beat within this bar.
     */
    public Position(int bar, float beat)
    {
        if ((beat < 0) || (bar < 0))
        {
            throw new IllegalArgumentException("b=" + beat + " bar=" + bar);
        }
        this.bar = bar;
        this.beat = beat;
    }

    public void setBeat(float beat)
    {
        if (beat < 0)
        {
            throw new IllegalArgumentException("beat=" + beat);
        }
        float old = this.beat;
        this.beat = beat;
        pcs.firePropertyChange(PROP_BEAT, old, this.beat);

    }

    public void setBar(int bar)
    {
        if (bar < 0)
        {
            throw new IllegalArgumentException("bar=" + bar);
        }
        int old = this.bar;
        this.bar = bar;
        pcs.firePropertyChange(PROP_BAR, old, this.bar);
    }

    /**
     * Set the position from another position.
     * <p>
     * @param p A Position.
     */
    public void set(Position p)
    {
        setBeat(p.getBeat());
        setBar(p.getBar());
    }

    /**
     * Change position to be on first beat.
     */
    public void setFirstBarBeat()
    {
        setBeat(0);
    }

    /**
     * Change position to be on last beat of the bar which has the specified TimeSignature.
     *
     * @param ts The TimeSignature of the bar.
     */
    public void setLastBarBeat(TimeSignature ts)
    {
        setBeat(ts.getNbNaturalBeats() - 1);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Position)
        {
            Position p = (Position) o;

            return (bar == p.bar) && (beat == p.beat);
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 71 * hash + this.bar;
        hash = 71 * hash + Float.floatToIntBits(this.beat);
        return hash;
    }

    @Override
    public int compareTo(Position p)
    {
        if (p.bar < bar)
        {
            return 1;
        } else if (p.bar > bar)
        {
            return -1;
        } else if (p.beat < beat)
        {
            return 1;
        } else if (p.beat > beat)
        {
            return -1;
        } else
        {
            return 0;
        }
    }

    /**
     * @return True if position is beat=0.
     */
    public boolean isFirstBarBeat()
    {
        return beat == 0f;
    }

    /**
     *
     * @return True if beat's fractional part is != 0, eg true if beat=1.2
     */
    public boolean isOffBeat()
    {
        return beat - Math.floor(beat) > 0;
    }

    /**
     * True if position is equals or after the last logical beat of a bar.
     * <p>
     * Ex: ts=4/4 =&gt; return true if beat=3 or 3.3, false if beat &lt; 3.
     *
     * @param ts The TimeSignature of the bar.
     *
     * @return
     */
    public boolean isLastBarBeat(TimeSignature ts)
    {
        return beat >= (ts.getNbNaturalBeats() - 1);
    }

    /**
     * Get the adjusted position adapted to fit the specified time signature.
     *
     * @param ts
     * @return
     */
    public Position getAdjustedPosition(TimeSignature ts)
    {
        Position newPos = new Position(this);
        int lastBeat = ts.getNbNaturalBeats() - 1;
        if ((newPos.getBeat() - lastBeat) >= 1)
        {
            newPos.setBeat(lastBeat);
        }
        return newPos;
    }

    /**
     * The duration in natural beats between this position and the specified position.
     *
     * @param pos Can be before or after this position.
     * @param ts
     * @return A positive value.
     */
    public float getDuration(Position pos, TimeSignature ts)
    {
        Position posMin = this;
        Position posMax = pos;
        if (compareTo(pos) > 0)
        {
            posMax = this;
            posMin = pos;
        }
        int maxBar = posMax.getBar();
        float maxBeat = posMax.getBeat();
        int minBar = posMin.getBar();
        float minBeat = posMin.getBeat();
        float duration;
        if (maxBar == minBar)
        {
            duration = maxBeat - minBeat;
        } else if (maxBar == minBar + 1)
        {
            duration = maxBeat + (ts.getNbNaturalBeats() - minBeat);
        } else
        {
            duration = maxBeat + (ts.getNbNaturalBeats() - minBeat) + (maxBar - minBar - 1) * ts.getNbNaturalBeats();
        }
        return duration;
    }

    public int getBar()
    {
        return bar;
    }

    public float getBeat()
    {
        return beat;
    }

    /**
     * The fractional part of the beat.
     *
     * @return eg .2 if beat=3.2
     */
    public float getBeatFractionalPart()
    {
        return beat - (float) Math.floor(beat);
    }

    /**
     * @return "[3:2.5]" means bar=3, beat=2.5. Note that bar and beat start at 0.
     */
    @Override
    public String toString()
    {
        String s = String.format("%.2f", beat);
        int index = s.length() - 1;
        while (index >= 0 && (s.charAt(index) == '0' || s.charAt(index) == ',' || s.charAt(index) == '.'))
        {
            // Remove trailing 0
            index--;
        }
        String str = index >= 0 ? s.substring(0, index + 1) : "0";
        String res = String.valueOf(START_CHAR) + bar + String.valueOf(SEPARATOR_CHAR) + str + String.valueOf(END_CHAR);
        return res;
    }

    /**
     * Same as toString() except bar and beat start at 1 instead of 0
     *
     * @return
     */
    public String toUserString()
    {
        return String.valueOf(START_CHAR) + (bar + 1) + String.valueOf(SEPARATOR_CHAR) + getBeatAsUserString() + String.valueOf(END_CHAR);
    }

    /**
     * The beat as a string (beat start at 1), e.g 2 if beat=1, 2.5 if beat=1.5
     *
     * @return
     */
    public String getBeatAsUserString()
    {
        String s = String.format("%.2f", (beat + 1));
        int index = s.length() - 1;
        while (s.charAt(index) == '0' || s.charAt(index) == ',' || s.charAt(index) == '.')
        {
            // Remove trailing 0
            index--;
        }

        return s.substring(0, index + 1);
    }

    /**
     * Return the position as a double value = bar + beat/10
     * <p>
     * @return double
     */
    public double toDouble()
    {
        return bar + beat / 10d;
    }

    /**
     * Set the position from a string as the one returned by toString() eg "[2:3.5]" or "[3]"
     * <p>
     * Ex: "[2:3.5]" will set bar=2 and beat=3.5<br>
     * Ex: "[3.5]" set bar=defaultBar and beat=3.5
     *
     * @param userString The string as returned by toString()
     * @param defaultBar If bar is not specified, defaultBar is used.
     *
     * @throws ParseException If syntax error in string.
     */
    @Messages(
            {
                "CTL_InvalidValue=Invalid position string",
                "CTL_MissingEnclosingChars=Missing enclosing chars",
                "CTL_NegativeValue=Negative value not allowed"
            })
    public void valueOf(String userString, int defaultBar) throws ParseException
    {
        int newBar = bar;
        float newBeat = beat;
        if ((userString == null) || (defaultBar < 0))
        {
            throw new IllegalArgumentException("str=" + userString + " defaultBar=" + defaultBar);
        }

        // Remove brackets
        String s1 = userString.trim();
        if (s1.indexOf(START_CHAR) != 0 || s1.indexOf(END_CHAR) != s1.length() - 1)
        {
            throw new ParseException(userString + " : " + CTL_MissingEnclosingChars() + " " + START_CHAR + END_CHAR, 0);
        }
        String s = s1.substring(1, s1.length() - 1);
        int indSep = s.indexOf(SEPARATOR_CHAR);
        if (indSep == -1)
        {
            // No newBar specified so use default newBar
            newBar = defaultBar + 1;

            String strBeat = s.replace(',', '.');
            try
            {
                newBeat = Float.parseFloat(strBeat);
            } catch (NumberFormatException e)
            {
                throw new ParseException(userString + " : " + CTL_InvalidValue() + " " + e.getLocalizedMessage(), 0);
            }

            if (newBeat < 0)
            {
                throw new ParseException(userString + " : " + CTL_NegativeValue(), 0);
            }
        } else
        {
            // Bar and newBeat specified
            try
            {
                newBar = Integer.parseInt(s.substring(0, indSep));
            } catch (NumberFormatException e)
            {
                throw new ParseException(userString + " : " + CTL_InvalidValue() + " " + e.getLocalizedMessage(), 0);
            }

            if (newBar < 0)
            {
                throw new ParseException(userString + " : " + CTL_NegativeValue(), 0);
            }
            String strBeat = s.substring(indSep + 1).replace(',', '.');
            try
            {
                newBeat = Float.parseFloat(strBeat);
            } catch (NumberFormatException e)
            {
                throw new ParseException(userString + " : " + CTL_InvalidValue() + " " + e.getLocalizedMessage(), indSep + 1);
            }

            if (newBeat < 0)
            {
                throw new ParseException(userString + " : " + CTL_NegativeValue(), indSep + 1);
            }
        }

        setBar(newBar);
        setBeat(newBeat);
    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    /* --------------------------------------------------------------------- Serialization
    * --------------------------------------------------------------------- */
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 7987126309001277L;
        private final int spVERSION = 1;
        private final String spPos;

        private SerializationProxy(Position pos)
        {
            spPos = pos.toString();
        }

        private Object readResolve()
                throws ObjectStreamException
        {
            Position pos = new Position();
            try
            {
                pos.valueOf(spPos, 0);
            } catch (ParseException ex)
            {
                LOGGER.log(Level.SEVERE, "Can't read position " + spPos + ", using position(0,0) instead", ex);
                pos = new Position(0, 0);
            }
            return pos;
        }
    }

}

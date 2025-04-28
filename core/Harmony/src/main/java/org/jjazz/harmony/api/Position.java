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
package org.jjazz.harmony.api;

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * A bar/beat position.
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
     * A position on first beat (0) of the specified bar.
     *
     * @param bar
     */
    public Position(int bar)
    {
        this(bar, 0);
    }

    /**
     * @param bar  The index of the bar (&gt;=0).
     * @param beat The beat within this bar.
     */
    public Position(int bar, float beat)
    {
        Preconditions.checkArgument(bar >= 0 && beat >= 0, "bar=%s beat=%s", (Object) bar, beat);
        this.bar = bar;
        this.beat = beat;
    }

    /**
     * Set the beat parameter.
     *
     * @param beat
     * @return This instance
     */
    public Position setBeat(float beat)
    {
        Preconditions.checkArgument(beat >= 0, "beat=%s", beat);
        float old = this.beat;
        this.beat = beat;
        pcs.firePropertyChange(PROP_BEAT, old, this.beat);
        return this;
    }

    /**
     * Set the bar parameter.
     *
     * @param bar
     * @return This instance
     */
    public Position setBar(int bar)
    {
        Preconditions.checkArgument(bar >= 0, "bar=%", bar);
        int old = this.bar;
        this.bar = bar;
        pcs.firePropertyChange(PROP_BAR, old, this.bar);
        return this;
    }

    /**
     * Set bar and beat to 0.
     *
     * @return This instance
     */
    public Position reset()
    {
        setBar(0);
        setBeat(0);
        return this;
    }

    /**
     * Set the position from another position.
     * <p>
     * @param p A Position.
     * @return This instance
     */
    public Position set(Position p)
    {
        setBeat(p.getBeat());
        setBar(p.getBar());
        return this;
    }


    /**
     * Change position to be on first beat.
     *
     * @return This instance
     */
    public Position setFirstBarBeat()
    {
        setBeat(0);
        return this;
    }

    /**
     * Change position to be on last beat of the bar which has the specified TimeSignature.
     * <p>
     * For example set beat=3 for a 4/4 bar.
     *
     * @param ts The TimeSignature of the bar.
     * @return This instance
     */
    public Position setLastBarBeat(TimeSignature ts)
    {
        setBeat(ts.getNbNaturalBeats() - 1);
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o != null && o.getClass() == this.getClass())
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
     * True if position is at the half of the bar for the specified TimeSignature.
     * <p>
     * Ex: beat 2 for ts=4/4, beat 1.5 for 3/4 (if not swing), or beat 3 for 5/4.
     *
     * @param ts    The TimeSignature of the bar.
     * @param swing If true for example half beat for a 3/4 waltz is 5/3=1.666...
     *
     * @return
     */
    public boolean isHalfBarBeat(TimeSignature ts, boolean swing)
    {
        return beat == ts.getHalfBarBeat(swing);
    }

    /**
     * Get a new adjusted position which is guaranteed to fit the specified time signature.
     *
     * @param ts
     * @return
     */
    public Position getAdjusted(TimeSignature ts)
    {
        Position newPos = new Position(this);
        float lastBeat = ts.getNbNaturalBeats() - 1;
        if ((newPos.getBeat() - lastBeat) >= 1)
        {
            newPos.setBeat(lastBeat);
        }
        return newPos;
    }

    /**
     * Convert the current position in tsFrom context, to a new position in tsTo context.
     *
     * @param tsFrom
     * @param tsTo
     * @return
     */
    public Position getConverted(TimeSignature tsFrom, TimeSignature tsTo)
    {
        if (tsFrom == null || tsTo == null || beat >= tsFrom.getNbNaturalBeats())
        {
            throw new IllegalArgumentException("this=" + this + " tsFrom=" + tsFrom + " tsTo=" + tsTo);
        }


        Position newPos = new Position(this);
        float lastBeat = tsTo.getNbNaturalBeats() - 1;


        if (beat == tsFrom.getHalfBarBeat(false))
        {
            newPos.setBeat(tsTo.getHalfBarBeat(false));

        } else if (beat == tsFrom.getHalfBarBeat(true))
        {
            newPos.setBeat(tsTo.getHalfBarBeat(true));
        }

        if ((newPos.getBeat() - lastBeat) >= 1)
        {
            newPos.setBeat(lastBeat);
        }

        return newPos;
    }

    /**
     * Get a new position where bar and beat are moved by the offset parameters.
     *
     * @param barOffset
     * @param beatOffset
     * @return
     * @throws IllegalArgumentException If resulting bar or beat is a negative value.
     */
    public Position getMoved(int barOffset, float beatOffset)
    {
        int barNew = this.bar + barOffset;
        float beatNew = this.beat + beatOffset;
        if (barNew < 0 || beatNew < 0)
        {
            throw new IllegalArgumentException("barOffset=" + barOffset + " beatOffset=" + beatOffset + " this=" + this);
        }
        return new Position(barNew, beatNew);
    }

    /**
     * Get the position corresponding to bar+1 and beat=0.
     *
     * @return
     */
    public Position getNextBarStart()
    {
        return new Position(bar + 1);
    }

    /**
     * Get the next integer beat position in the specified TimeSignature context.
     *
     * @param ts
     * @return
     */
    public Position getNext(TimeSignature ts)
    {
        int nextBar = bar;
        float nextBeat = (float) (Math.floor(beat) + 1);
        if (nextBeat >= ts.getNbNaturalBeats())
        {
            nextBar++;
            nextBeat = 0;
        }
        return new Position(nextBar, nextBeat);
    }

    /**
     * Get the previous integer beat position in the specified TimeSignature context.
     *
     * @param ts
     * @return
     * @throws IllegalArgumentException If this position is already bar=0 beat=0.
     */
    public Position getPrevious(TimeSignature ts)
    {
        if (bar == 0 && beat == 0)
        {
            throw new IllegalArgumentException("this=" + this + " ts=" + ts);
        }
        int previousBar = bar;
        float previousBeat = (int) (Math.ceil(beat) - 1);
        if (previousBeat < 0)
        {
            previousBar--;
            previousBeat = ts.getNbNaturalBeats() - 1;
        }
        return new Position(previousBar, previousBeat);
    }

    /**
     * The position in natural beats if all bars use the specified TimeSignature.
     *
     * @param ts
     * @return
     */
    public float getPositionInBeats(TimeSignature ts)
    {
        return bar * ts.getNbNaturalBeats() + beat;
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
     * @param posString  The string as returned by toString() or toUserString()
     * @param defaultBar If bar is not specified, defaultBar is used.
     * @param oneBased   If true bar/beat in userString are considered 1-based instead of 0-based
     * @return This instance
     *
     * @throws ParseException If syntax error in string.
     */
    public Position setFromString(String posString, int defaultBar, boolean oneBased) throws ParseException
    {

        if ((posString == null) || (defaultBar < 0))
        {
            throw new IllegalArgumentException("str=" + posString + " defaultBar=" + defaultBar);
        }

        // Remove brackets
        String s1 = posString.trim();
        if (s1.indexOf(START_CHAR) != 0 || s1.indexOf(END_CHAR) != s1.length() - 1)
        {
            throw new ParseException(posString + " : " + ResUtil.getString(getClass(), "CTL_MissingEnclosingChars", START_CHAR + END_CHAR), 0);
        }
        String errInvalidValue = ResUtil.getString(getClass(), "CTL_InvalidValue");
        String errNegativeValue = ResUtil.getString(getClass(), "CTL_NegativeValue");


        int newBar = bar;
        float newBeat = beat;
        String s = s1.substring(1, s1.length() - 1);
        int indSep = s.indexOf(SEPARATOR_CHAR);
        if (indSep == -1)
        {
            // No newBar specified so use default newBar
            newBar = defaultBar;

            String strBeat = s.replace(',', '.');
            try
            {
                newBeat = Float.parseFloat(strBeat);
                newBeat -= oneBased ? 1 : 0;
            } catch (NumberFormatException e)
            {
                throw new ParseException(posString + " : " + errInvalidValue + " " + e.getLocalizedMessage(), 0);
            }
            if (newBeat < 0)
            {
                throw new ParseException(posString + " : " + errNegativeValue, 0);
            }
        } else
        {
            // Bar and newBeat specified
            try
            {
                newBar = Integer.parseInt(s.substring(0, indSep));
                newBar -= oneBased ? 1 : 0;
            } catch (NumberFormatException e)
            {
                throw new ParseException(posString + " : " + errInvalidValue + " " + e.getLocalizedMessage(), 0);
            }
            if (newBar < 0)
            {
                throw new ParseException(posString + " : " + errNegativeValue, 0);
            }
            String strBeat = s.substring(indSep + 1).replace(',', '.');
            try
            {
                newBeat = Float.parseFloat(strBeat);
                newBeat -= oneBased ? 1 : 0;
            } catch (NumberFormatException e)
            {
                throw new ParseException(posString + " : " + errInvalidValue + " " + e.getLocalizedMessage(), indSep + 1);
            }
            if (newBeat < 0)
            {
                throw new ParseException(posString + " : " + errNegativeValue, indSep + 1);
            }
        }

        setBar(newBar);
        setBeat(newBeat);

        return this;
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

    // --------------------------------------------------------------------- 
    // Inner classes
    // ---------------------------------------------------------------------

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    if (instanceId.equals(SONG_LOAD))
                    {
                        // From 4.1.0 Position was moved from ChordLeadSheet module to Harmony module
                        xstream.alias("org.jjazz.chordleadsheet.api.item.Position$SerializationProxy", SerializationProxy.class);
                        // At some point the "leadsheet" part was dropped in the package name
                        xstream.alias("org.jjazz.leadsheet.chordleadsheet.api.item.Position$SerializationProxy", SerializationProxy.class);
                    }


                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files          
                    xstream.alias("Position", Position.class);
                    xstream.alias("PositionSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spPos");
                }
                case MIDIMIX_LOAD ->
                {
                    // Nothing
                }
                case MIDIMIX_SAVE ->
                {
                    // Nothing
                }
                default -> throw new AssertionError(instanceId.name());
            }
        }
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // ---------------------------------------------------------------------
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Serialization proxy
     * <p>
     * spVERSION 2 (JJazzLab 4.1.0) introduces aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction).<br>
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 7987126309001277L;
        private int spVERSION = 2;      // Do not make final!
        private String spPos;

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
                pos.setFromString(spPos, 0, false);
            } catch (ParseException ex)
            {
                LOGGER.log(Level.WARNING, "Can''t read position " + spPos + ", using position(0,0) instead", ex);
                pos = new Position(0);
            }
            return pos;
        }
    }

}

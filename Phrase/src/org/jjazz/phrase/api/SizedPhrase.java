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
package org.jjazz.phrase.api;

import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.util.api.FloatRange;

/**
 * A Phrase which has a size (beat range) and a time signature.
 * <p>
 * Added NoteEvents must be fully contained in the beat range, otherwise an IllegalArgumentException is thrown.
 */
public class SizedPhrase extends Phrase
{

    private final FloatRange beatRange;
    private final TimeSignature timeSignature;
    private static final Logger LOGGER = Logger.getLogger(SizedPhrase.class.getSimpleName());

    /**
     * Create an empty sized phrase.
     *
     * @param channel
     * @param beatRange
     * @param ts
     */
    public SizedPhrase(int channel, FloatRange beatRange, TimeSignature ts)
    {
        super(channel);
        checkNotNull(ts);
        checkNotNull(beatRange);
        this.beatRange = beatRange;
        this.timeSignature = ts;
    }

    public SizedPhrase(SizedPhrase sp)
    {
        super(sp.getChannel());
        this.beatRange = sp.getBeatRange();
        this.timeSignature = sp.getTimeSignature();
        add(sp);
    }


    /**
     * Overridden to check NoteEvent position.
     * <p>
     * @param ne
     */
    @Override
    public boolean add(NoteEvent ne)
    {
        checkNoteEvent(ne);
        return super.add(ne);
    }

    /**
     * Overridden to check NoteEvent position.
     * <p>
     * @param ne
     */
    @Override
    public void addFirst(NoteEvent ne)
    {
        checkNoteEvent(ne);
        super.addFirst(ne);
    }

    /**
     * Overridden to check NoteEvent position.
     * <p>
     * @param ne
     */
    @Override
    public void addLast(NoteEvent ne)
    {
        checkNoteEvent(ne);
        super.addLast(ne);
    }

    /**
     * Overridden to check NoteEvent position.
     * <p>
     * @param index
     * @param ne
     */
    @Override
    public void add(int index, NoteEvent ne)
    {
        checkNoteEvent(ne);
        super.add(index, ne);
    }

    /**
     * Overridden to check NoteEvent positions.
     *
     * @param index
     * @param nes
     * @return
     */
    @Override
    public boolean addAll(int index, Collection<? extends NoteEvent> nes)
    {
        for (var ne : nes)
        {
            checkNoteEvent(ne);
        }
        return super.addAll(index, nes);
    }

    @Override
    public SizedPhrase clone()
    {
        var sp = new SizedPhrase(getChannel(), beatRange, timeSignature);
        sp.add(this);
        return sp;
    }

    /**
     * Get the beat range corresponding to this phrase.
     * <p>
     * Overrides Phrase.getBeatRange() because the beat range is fixed for a SizedPhrase.
     *
     * @return
     */
    @Override
    public FloatRange getBeatRange()
    {
        return beatRange;
    }

    /**
     * @return The timeSignature used in this phrase.
     */
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }


    /**
     * Save the specified SizedPhrase as a string.
     * <p>
     * Example "[8|12.0|16.0|4/4|NoteEventStr0|NoteEventStr1]" means a Phrase for channel 8, beatRange=12-16, in 4/4, with 2
     * NoteEvents.
     *
     * @param sp
     * @return
     * @see loadAsString(String)
     */
    static public String saveAsString(SizedPhrase sp)
    {
        StringJoiner joiner = new StringJoiner("|", "[", "]");
        joiner.add(String.valueOf(sp.getChannel()));
        joiner.add(String.valueOf(sp.getBeatRange().from));
        joiner.add(String.valueOf(sp.getBeatRange().to));
        joiner.add(String.valueOf(sp.getTimeSignature()));
        sp.forEach(ne -> joiner.add(NoteEvent.saveAsString(ne)));
        return joiner.toString();
    }

    /**
     * Create a SizedPhrase from the specified string.
     * <p>
     *
     * @param s
     * @return
     * @throws ParseException If s is not a valid string.
     * @see saveAsString(SizedPhrase)
     */
    static public SizedPhrase loadAsString(String s) throws ParseException
    {
        SizedPhrase sp = null;
        if (s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']')
        {
            String[] strs = s.substring(1, s.length() - 1).split("\\|");    // "[8|12.0|16.0|4/4|NoteEventStr0|NoteEventStr1]"
            if (strs.length >= 4)
            {
                try
                {
                    int channel = Integer.parseInt(strs[0]);
                    float from = Float.parseFloat(strs[1]);
                    float to = Float.parseFloat(strs[2]);
                    TimeSignature ts = TimeSignature.parse(strs[3]);
                    sp = new SizedPhrase(channel, new FloatRange(from, to), ts);
                    for (int i = 4; i < strs.length; i++)
                    {
                        NoteEvent ne = NoteEvent.loadAsString(strs[i]);
                        sp.addOrdered(ne);
                    }
                } catch (IllegalArgumentException | ParseException ex)
                {
                    // Nothing
                    LOGGER.warning("loadAsString() Invalid string s=" + s + ", ex=" + ex.getMessage());
                }
            }
        }

        if (sp == null)
        {
            throw new ParseException("SizedPhrase.loadAsString() Invalid SizedPhrase string s=" + s, 0);
        }
        return sp;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SizedPhrase[ch=").append(getChannel())
                .append(", beatRange=")
                .append(beatRange)
                .append(", ts=")
                .append(timeSignature)
                .append("] size=").append(size())
                .append(" notes=").append(super.toString());
        return sb.toString();
    }

    // ==============================================================================
    // Private methods
    // ==============================================================================

    private void checkNoteEvent(NoteEvent ne)
    {
        if (!beatRange.contains(ne.getBeatRange(), false))
        {
            throw new IllegalArgumentException("ne=" + ne + " beatRange=" + beatRange);
        }
    }


}

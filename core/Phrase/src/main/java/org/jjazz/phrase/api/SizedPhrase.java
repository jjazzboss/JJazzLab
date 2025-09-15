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
package org.jjazz.phrase.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.utilities.api.FloatRange;

/**
 * A Phrase which has a size (beat range) and a time signature.
 * <p>
 * Added NoteEvents must be fully contained in the beat range, otherwise an IllegalArgumentException is thrown.
 */
public class SizedPhrase extends Phrase
{

    private FloatRange beatRange;
    private final TimeSignature timeSignature;
    private static final Logger LOGGER = Logger.getLogger(SizedPhrase.class.getSimpleName());

    /**
     * Create a sized phrase with no notes.
     *
     * @param channel
     * @param beatRange
     * @param ts
     * @param isDrums
     */
    public SizedPhrase(int channel, FloatRange beatRange, TimeSignature ts, boolean isDrums)
    {
        super(channel, isDrums);
        Objects.requireNonNull(ts);
        Objects.requireNonNull(beatRange);
        this.beatRange = beatRange;
        this.timeSignature = ts;
    }

    public SizedPhrase(SizedPhrase sp)
    {
        super(sp.getChannel(), sp.isDrums());
        this.beatRange = sp.getNotesBeatRange();
        this.timeSignature = sp.getTimeSignature();
        add(sp);
    }

    /**
     * Overridden to check NoteEvent is within the beat range (including the upper bound).
     * <p>
     *
     * @param ne
     * @return
     */
    @Override
    public boolean canAddNote(NoteEvent ne)
    {
        return beatRange.contains(ne.getBeatRange(), false);
    }


    @Override
    public SizedPhrase clone()
    {
        var sp = new SizedPhrase(getChannel(), beatRange, timeSignature, isDrums());
        sp.add(this);
        return sp;
    }

    /**
     * Get the beat range corresponding to this phrase.
     * <p>
Overrides Phrase.getNotesBeatRange() because the beat range is fixed for a SizedPhrase.
     *
     * @return
     */
    @Override
    public FloatRange getNotesBeatRange()
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


    public int getSizeInBars()
    {
        return Math.round(beatRange.size() / timeSignature.getNbNaturalBeats());
    }

    /**
     * Shift the associated BeatRange and all events.
     *
     * @param shiftInBeats
     */
    @Override
    public void shiftAllEvents(float shiftInBeats, boolean handleNegativePositions)
    {
        beatRange = beatRange.getTransformed(shiftInBeats);
        super.shiftAllEvents(shiftInBeats, handleNegativePositions);
    }

    /**
     * Save the specified SizedPhrase as a string.
     * <p>
     * Example "[8|12.0|16.0|4/4|NoteEventStr0|NoteEventStr1]" means a melodic Phrase for channel 8, beatRange=12-16, in 4/4, with 2 NoteEvents.<br>
     * Example "[drums_9|12.0|16.0|4/4|NoteEventStr0|NoteEventStr1]" means a drums Phrase for channel 9.
     *
     * @param sp
     * @return
     * @see loadAsString(String)
     */
    static public String saveAsString(SizedPhrase sp)
    {
        StringJoiner joiner = new StringJoiner("|", "[", "]");
        String drums = sp.isDrums() ? "drums_" : "";
        joiner.add(drums + String.valueOf(sp.getChannel()));
        joiner.add(String.valueOf(sp.getNotesBeatRange().from));
        joiner.add(String.valueOf(sp.getNotesBeatRange().to));
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
                    boolean drums = false;
                    if (strs[0].startsWith("drums_"))
                    {
                        drums = true;
                        strs[0] = strs[0].substring(6);
                    }
                    int channel = Integer.parseInt(strs[0]);
                    float from = Float.parseFloat(strs[1]);
                    float to = Float.parseFloat(strs[2]);
                    TimeSignature ts = TimeSignature.parse(strs[3]);
                    sp = new SizedPhrase(channel, new FloatRange(from, to), ts, drums);
                    for (int i = 4; i < strs.length; i++)
                    {
                        NoteEvent ne = NoteEvent.loadAsString(strs[i]);
                        sp.add(ne);
                    }
                } catch (IllegalArgumentException | ParseException ex)
                {
                    // Nothing
                    LOGGER.log(Level.WARNING, "loadAsString() Invalid string s={0}, ex={1}", new Object[]
                    {
                        s, ex.getMessage()
                    });
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
                .append(", br=")
                .append(beatRange)
                .append(", ts=")
                .append(timeSignature)
                .append("] size=").append(size())
                .append(" notes=").append(getNotes().toString());
        return sb.toString();
    }

    // ==============================================================================
    // Private methods
    // ==============================================================================

}

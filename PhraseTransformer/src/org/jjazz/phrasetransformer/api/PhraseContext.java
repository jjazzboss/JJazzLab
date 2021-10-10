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
package org.jjazz.phrasetransformer.api;

import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.util.api.IntRange;

/**
 * The context associated to a phrase to be transformed.
 */
public class PhraseContext
{
    private TimeSignature timeSignature;
    private IntRange barRange;
    private RhythmVoice rhythmVoice;
    private Song song;

    public PhraseContext(TimeSignature timeSignature, Song song, IntRange barRange, RhythmVoice rhythmVoice)
    {
        this.timeSignature = timeSignature;
        this.barRange = barRange;
        this.rhythmVoice = rhythmVoice;
        this.song = song;
    }

    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    public IntRange getBarRange()
    {
        return barRange;
    }

    public RhythmVoice getRhythmVoice()
    {
        return rhythmVoice;
    }

    public Song getSong()
    {
        return song;
    }
}

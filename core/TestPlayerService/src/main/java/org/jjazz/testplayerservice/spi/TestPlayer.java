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
package org.jjazz.testplayerservice.spi;

import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.openide.util.Lookup;

/**
 * A service to hear a few test notes.
 */
public interface TestPlayer
{

    /**
     * Get the first TestPlayer instance available in the global lookup.
     *
     * @return Can't be null
     */
    static public TestPlayer getDefault()
    {
        var tp = Lookup.getDefault().lookup(TestPlayer.class);
        assert tp != null;
        return tp;
    }

    /**
     * Play a few test notes on channel 0, blocking until sequence is complete.
     * <p>
     * Note that MusicController is stopped before playing the test notes.
     *
     * @throws MusicGenerationException
     */
    void playTestNotes() throws MusicGenerationException;

    /**
     * Send a short sequence of Midi notes on specified channel.
     * <p>
     * If fixPitch &lt; 0 then fixPitch is ignored: play a series of notes starting at 60+transpose. If fixPitch&gt;=0 then play a series of notes with same
     * pitch=fixPitch.
     * <p>
     * Note that MusicController is stopped before playing the test notes.
     *
     * @param channel
     * @param fixPitch  -1 means not used.
     * @param transpose Transposition value in semi-tons to be added to test notes. Ignored if fixPitch&gt;=0.
     * @param endAction Called when playback is over. Can be null.
     * @throws org.jjazz.rhythm.api.MusicGenerationException If a problem occurred.
     */
    void playTestNotes(int channel, int fixPitch, int transpose, final Runnable endAction) throws MusicGenerationException;

    /**
     * Play the test notes from specified phrase.
     * <p>
     * Note that MusicController is stopped before playing the test notes.
     *
     * @param phrase
     * @param endAction Called when sequence is over. Can be null.
     * @throws org.jjazz.rhythm.api.MusicGenerationException If a problem occurred.
     */
    void playTestNotes(Phrase phrase, final Runnable endAction) throws MusicGenerationException;
}

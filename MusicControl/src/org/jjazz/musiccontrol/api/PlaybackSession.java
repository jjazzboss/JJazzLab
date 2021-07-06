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
package org.jjazz.musiccontrol.api;

import java.util.HashMap;
import java.util.List;
import javax.sound.midi.Sequence;
import javax.swing.event.ChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * Contains a sequence and all the related data needed by MusicController to manage a playback session.
 * <p>
 * Implementations are expected to be immutable, expected for the isOutdated() method.
 */
public interface PlaybackSession
{

    /**
     * Get the Midi sequence.
     *
     * @return Can't be null.
     */
    Sequence getSequence();

    /**
     * The tick position of the start of the song.
     * <p>
     * Take into account the possible precount clicks.
     *
     * @return
     */
    long getTickStart();

    /**
     * The tick position of the end of the song/loop point.
     * <p>
     * Take into account the possible precount clicks.
     *
     * @return
     */
    long getTickEnd();

    /**
     * The click track index.
     * <p>
     * If provided, used by the MusicController to enabled/disable the click track depending on the Click UI.
     *
     * @return -1 if not used
     */
    default int getClickTrackId()
    {
        return -1;
    }

    /**
     * The positions in natural beat of all jjazz beat change controller events.
     * <p>
     * If provided, used by the MusicController to notify the current beat position to the framework.
     *
     * @return Null if not used
     */
    default List<Position> getNaturalBeatPositions()
    {
        return null;
    }   


    /**
     * Check if this session data is no more in sync with the underlying data (e.g. a Song).
     * <p>
     * Use addChangeListener() to be notified when isOutdated() return value changes. Once isOutdated() returns true, value can
     * never change back to false.
     *
     * @return If true a new session should be generated.
     */
    boolean isOutdated();

    /**
     * Add a listener to the isOutdated() state.
     *
     * @param listener
     */
    void addChangeListener(ChangeListener listener);

    /**
     * Remove a listener to the isOutdated() state.
     *
     * @param listener
     */
    void removeChangeListener(ChangeListener listener);

    /**
     * Must be called before disposing this session.
     */
    void cleanup();   

}

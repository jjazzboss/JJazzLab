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

import java.util.List;
import javax.sound.midi.Sequence;
import javax.swing.event.ChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.rhythm.api.MusicGenerationException;

/**
 * Contains a sequence and the minimum related data needed by MusicController to manage a playback session.
 * <p>
 * The session is responsible for maintaining its State and notifying listeners when State has changed.
 */
public interface PlaybackSession
{

    public enum State
    {
        /**
         * State of the session upon creation, sequence and related data are not generated yet. Sequence and related data values
         * are undefined in this state.
         */
        NEW,
        /**
         * Sequence and related data have been generated and are up to date with the underlying data.
         */
        GENERATED,
        /**
         * Sequence and related data were generated but are now out of date compared to the underlying data.
         */
        OUTDATED
    }

    /**
     * The state of the session.
     *
     * @return
     */
    State getState();

    /**
     * Create the sequence and the related data.
     * <p>
     * The method changes the state to GENERATED.
     *
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     * @throws IllegalStateException If State is not NEW.
     */
    void generate() throws MusicGenerationException;

    /**
     * Create a new identical session in the NEW state.
     *
     * @return
     */
    PlaybackSession getCopyInNewState();

    /**
     * Get the Midi sequence.
     *
     * @return
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
     * Add a listener to the state changes.
     *
     * @param listener
     */
    void addChangeListener(ChangeListener listener);

    void removeChangeListener(ChangeListener listener);

    /**
     * Must be called before disposing this session.
     */
    void cleanup();

}

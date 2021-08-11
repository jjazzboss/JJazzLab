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
package org.jjazz.musiccontrol.api.playbacksession;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.util.api.IntRange;

/**
 * A PlaybackSession contains the data needed by the MusicController to play music and provide related services (firing beat or
 * chord symbol events, managing tempo changes, ...).
 * <p>
 * Some session data values are meaningful only when state==GENERATED. A property change event is fired for mutable value changes.
 * <p>
 * Some implementations might not support the OUTDATED_UPDATABLE state (i.e. transition from GENERATED to OUTDATED).
 * Implementations may also implement additional session capabilities such as SongContextProvider, ChordSymbolProvider, etc.
 */
public interface PlaybackSession
{

    public enum State
    {
        /**
         * State of the session upon creation, sequence and related data are not generated yet.
         * <p>
         * Sequence and related data values are undefined in this state.
         */
        NEW,
        /**
         * Sequence and related data have been generated and are up to date with the underlying data.
         */
        GENERATED,
        /**
         * Sequence and related data were generated but are now out of date compared to the underlying data (and this can't be
         * fixed)
         */
        OUTDATED,
        /**
         * The session is closed (e.g. song is no more available) and any playback should be stopped.
         */
        CLOSED
    }


    /**
     * The session State has changed.
     */
    public static final String PROP_STATE = "State";
    /**
     * The playback reference tempo has changed.
     */
    public static final String PROP_TEMPO = "Tempo";
    /**
     * One or more tracks muted status have changed.
     *
     * @see getTracksMuteStatus()
     */
    public static final String PROP_MUTED_TRACKS = "MutedTracks";
    /**
     * The loop count has changed.
     */
    public static final String PROP_LOOP_COUNT = "LoopCount";

    /**
     * Create the sequence and the related data.
     * <p>
     * If generation is successful the method changes the state from NEW to GENERATED.
     *
     * @param silent If false a modal progress bar is shown why generating the music.
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     * @throws IllegalStateException If State is not NEW.
     */
    void generate(boolean silent) throws MusicGenerationException;

    /**
     * The Midi sequence.
     *
     * @return Can be null if no meaningful value can be returned.
     */
    Sequence getSequence();

    /**
     * Get the state of this session.
     *
     * @return
     * @see PROP_STATE
     */
    State getState();

    /**
     * Get the tempo in BPM.
     *
     * @return -1 if no meaningful value can be returned.
     * @see PROP_TEMPO
     */
    int getTempo();

    /**
     * Get the mute status of each track id.
     * <p>
     * Note that track 0 (e.g. mandatory track for sequence name, tempo and time signature changes, etc.) is not included.
     *
     * @return Key=track id, Value=true if track is muted. Can be null if no meaningful value can be returned.
     * @see PROP_MUTED_TRACKS
     */
    HashMap<Integer, Boolean> getTracksMuteStatus();

    /**
     * The tick position of the end of the loop.
     * <p>
     *
     * @return -1 if no meaningful value can be returned (loop will be at the end of sequence)
     */
    long getLoopEndTick();

    /**
     * The tick position of the start of the loop.
     * <p>
     *
     * @return -1 if no meaningful value can be returned.
     */
    long getLoopStartTick();

    /**
     * The sequencer loop count (see Java Sequencer setLoopCount()).
     *
     * @return 0 means sequence is played only once, 1 means sequence is played twice, etc. -1 means loop forever.
     */
    int getLoopCount();

    /**
     * The bar range corresponding to the sequence.
     *
     * @return Null if no meaningful value can be returned.
     */
    IntRange getBarRange();

    /**
     * Get the start tick position corresponding to the specified bar.
     *
     * @param barIndex
     * @return -1 if no meaningful value can be returned.
     */
    long getTick(int barIndex);

    /**
     * Must be called before disposing this session.
     */
    void cleanup();

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}

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
package org.jjazz.musiccontrol.api.playbacksession;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import javax.sound.midi.Sequence;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.utilities.api.IntRange;

/**
 * A PlaybackSession contains the data needed by the MusicController to play music and provide related services (firing beat or chord symbol events, managing
 * tempo changes, ...).
 * <p>
 * Implementations may also implement additional session capabilities such as SongContextProvider or ChordSymbolProvider.
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
         * Sequence and related data have been generated.
         */
        GENERATED,
        /**
         * The session is closed (e.g. song is no more available) and any playback should be stopped.
         */
        CLOSED
    }


    /**
     * A property change event is fired when the state has changed.
     */
    public static final String PROP_STATE = "PropState";
    /**
     * A property change event is fired when session is no more up-to-date with its underlying data.
     */
    public static final String PROP_DIRTY = "PropDirty";
    /**
     * A property change event is fired when the playback reference tempo has changed.
     */
    public static final String PROP_TEMPO = "PropTempo";
    /**
     * A property change event is fired when one or more tracks muted status have changed.
     *
     * @see getTracksMuteStatus()
     */
    public static final String PROP_MUTED_TRACKS = "PropMutedTracks";
    /**
     * A property change event is fired when the loop count has changed.
     */
    public static final String PROP_LOOP_COUNT = "PropLoopCount";

    /**
     * Provide the context in which the PlaybackSession is used.
     * 
     * Used by e.g. PlaybackListeners which want to be active only in a given context.
     * <p>
     */
    public enum Context
    {
        /**
         * Used when playing a song (or part of). This is the standard case.
         */
        SONG,
        /**
         * Used when playing in arranger mode.
         */
        ARRANGER, 
        /**
         * Used e.g. when hearing test notes. 
         */
        AUDITIONING, 
        /**
         * Used when "preview hearing" a rhythm.
         */
        RHYTHM_PREVIEW, 
        /**
         * Used when "preview hearing" 
         */
        RP_VALUE_PREVIEW, 
        /**
         * For other cases
         */
        OTHER
    };


    /**
     * The context of this PlaybackSession.
     * <p>
     *
     * @return Can not be null
     */
    public Context getContext();

    /**
     * Create the sequence and the related data.
     * <p>
     * If generation is successful the method changes the state from NEW to GENERATED. Method blocks until music generation is complete.
     *
     * @param silent If false a modal progress bar is shown while generating the music.
     * @throws org.jjazz.rhythm.api.MusicGenerationException
     * @throws IllegalStateException                         If State is not NEW.
     */
    void generate(boolean silent) throws MusicGenerationException;

    /**
     * Get a copy of this session in the NEW state for the specified context.
     *
     * @param sgContext If null use a copy of this session'SongContext
     * @return
     */
    PlaybackSession getFreshCopy(SongContext sgContext);

    /**
     * The Midi sequence.
     *
     * @return Can be null if no meaningful value can be returned.
     */
    Sequence getSequence();

    /**
     * Return true if the sequence is not up-to-date with its underlying data.
     * <p>
     * If playback is stopped a new session should be created.
     *
     * @return
     * @see PROP_DIRTY
     */
    boolean isDirty();

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
     * Put session in the CLOSED state and release used resources, so that session can be disposed.
     *
     * @see PROP_STATE
     */
    void close();

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}

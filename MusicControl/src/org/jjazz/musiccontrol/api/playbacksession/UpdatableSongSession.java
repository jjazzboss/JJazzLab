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

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.ContextChordSequence;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.util.api.IntRange;

/**
 * A PlaybackSession which is a wrapper for a BaseSongSession to enable on-the-fly updates of the playing sequence using
 * {@link updateSequence(Map&lt;RhythmVoice,Phrase&gt)}.
 * <p>
 * Authorized udpates are add/remove notes which do not change the Sequence size. The class uses buffer tracks and mute/unmute
 * tracks to enable on-the-fly sequence changes.
 * <p>
 * If the BaseSongSession is an instance of UpdateProvider, listen to update availability and automatically apply the update.
 */
public class UpdatableSongSession implements PropertyChangeListener, PlaybackSession, ControlTrackProvider, SongContextProvider, EndOfPlaybackActionProvider
{

    /**
     * A SongContextSession capability: can provide an update after sequence was generated (in the GENERATED state).
     * <p>
     * The session must fire a PROP_UPDATE_AVAILABLE property change event when an update is ready.
     */
    public interface UpdateProvider
    {

        public static String PROP_UPDATE_AVAILABLE = "PropUpdateAvailable";

        /**
         * Get the last available update after the PROP_UPDATE_AVAILABLE property change event was fired.
         *
         * @return Can be null if PROP_UPDATE_AVAILABLE event was never fired.
         */
        Map<RhythmVoice, Phrase> getUpdate();
    }


    private long originalTrackTickSize;
    private int nbPlayingTracks;
    private Map<RhythmVoice, Phrase> currentMapRvPhrase;
    private TrackSet trackSet;         // Exclude track 0 
    private final BaseSongSession songSession;
    private Sequence sequence;
    private final HashMap<Integer, Boolean> mapTrackIdMuted = new HashMap<>();
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(UpdatableSongSession.class.getSimpleName());  //NOI18N

    /**
     * Create an UpdatableSongSession to enable updates of the specified BaseSongSession.
     *
     * @param session If session is an UpdateProvider instance automatically apply the update.
     */
    public UpdatableSongSession(BaseSongSession session)
    {
        if (session == null)
        {
            throw new IllegalArgumentException("session=" + session);
        }


        songSession = session;
        songSession.addPropertyChangeListener(this);


        if (songSession.getState().equals(State.GENERATED))
        {
            prepareData();
        }
    }


    @Override
    public void generate(boolean silent) throws MusicGenerationException
    {
        songSession.generate(silent);

        prepareData();
    }


    /**
     * Get the sequence which contains the original song tracks plus additional empty tracks to allow "double buffering
     * modification" via muting/unmuting tracks.
     * <p>
     * The total number of tracks is 2 * getNbPlayingTracks().
     *
     * @return
     */
    @Override
    public Sequence getSequence()
    {
        return sequence;
    }


    /**
     * The number of playing tracks (excluding the additional double buffering extra tracks).
     *
     * @return
     */
    public int getNbPlayingTracks()
    {
        return nbPlayingTracks;
    }

    /**
     * The size in ticks of the original generated sequence.
     *
     * @return
     */
    public long getOriginalSequenceSize()
    {
        return originalTrackTickSize;
    }


    /**
     * Update the sequence by replacing the contents of one or more RhythmVoice tracks.
     * <p>
     * Update tracks for which there is an actual change. Changes are first applied to muted "buffer tracks", then we switch the
     * mute status between the buffer and the playing tracks. The transition might be noticeable if notes were still ringing when
     * tracks mute state is switched.
     *
     * @param newMapRvPhrase
     * @throws IllegalArgumentException If a MidiEvent tick position is beyond getOriginalSequenceSize(), or if session is not in
     * the GENERATED state.
     */
    public void updateSequence(Map<RhythmVoice, Phrase> newMapRvPhrase)
    {
        LOGGER.info("updateSequence() ---- newMapRvPhrase.keySet()=" + newMapRvPhrase.keySet());

        if (!getState().equals(PlaybackSession.State.GENERATED))
        {
            throw new IllegalStateException("newMapRvPhrase=" + newMapRvPhrase + " getState()=" + getState());
        }


        // Update sequence with a phrase if it's modified compared to current one
        for (RhythmVoice rv : newMapRvPhrase.keySet())
        {
            var newPhrase = newMapRvPhrase.get(rv);
            var oldPhrase = currentMapRvPhrase.get(rv);
//            LOGGER.info("   rv="+rv);
//            LOGGER.info("     oldPhrase="+oldPhrase);
//            LOGGER.info("     newPhrase="+newPhrase);


            if (oldPhrase.equals(newPhrase))
            {
                // No change do nothing
                continue;
            } else
            {
                // Replace the current events
                LOGGER.info("updateSequence()     changes detected for rv=" + rv + ", updating");
                currentMapRvPhrase.put(rv, newPhrase);
            }


            // Pre-calculate the precount shift
            long precountShift = 0;
            if (songSession.getPrecountTrackId() != -1)
            {
                TimeSignature ts = songSession.getSongContext().getSongParts().get(0).getRhythm().getTimeSignature();
                int nbPrecountBars = PlaybackSettings.getInstance().getClickPrecountNbBars(ts, songSession.getSongContext().getSong().getTempo());
                precountShift = (long) Math.ceil(nbPrecountBars * ts.getNbNaturalBeats() * MidiConst.PPQ_RESOLUTION);
            }

            // Clear and update the buffer track with the passed events
            int trackId = getOriginalRvTrackIdMap().get(rv);
            Track bufferTrack = trackSet.getBufferTrack(trackId);
            MidiUtilities.clearTrack(bufferTrack);


            for (MidiEvent me : newPhrase.toMidiEvents())
            {
                // Adjust position if precount bars are used
                me.setTick(me.getTick() + precountShift);

                if (me.getTick() > originalTrackTickSize)
                {
                    throw new IllegalArgumentException("me=" + MidiUtilities.toString(me.getMessage(), me.getTick()) + " originalTrackTickSize=" + originalTrackTickSize);
                }
                bufferTrack.add(me);
            }


            // Make sure size is not changed
            MidiUtilities.setEndOfTrackPosition(bufferTrack, originalTrackTickSize);


            // Update the track mute state : apply mute status of the active track to the buffer track, then mute the active track
            boolean activeTrackMuteState = mapTrackIdMuted.get(trackSet.getActiveTrackId(trackId));
            mapTrackIdMuted.put(trackSet.getBufferTrackId(trackId), activeTrackMuteState);
            mapTrackIdMuted.put(trackSet.getActiveTrackId(trackId), true);


            // Finally exchange the active and buffer tracks
            trackSet.swapBufferAndActiveTracks(trackId);
        }

//        LOGGER.info("updateSequence() AFTER: mapTrackIdMuted=" + mapTrackIdMuted);

        // Notify our listeners that tracks mute status has changed
        pcs.firePropertyChange(PlaybackSession.PROP_MUTED_TRACKS, null, mapTrackIdMuted);
    }

    /**
     * A map providing the original track id corresponding to each used RhythmVoice in the given context.
     * <p>
     * If a song uses rhythms R1 and R2 and context is only on R2 bars, then the map only contains R2 rhythm voices and track id.
     * <p>
     *
     * @return @see getOriginalTrackEvents()
     */
    public Map<RhythmVoice, Integer> getOriginalRvTrackIdMap()
    {
        return songSession.getRvTrackIdMap();
    }

    /**
     * A map giving the original resulting Phrase for each RhythmVoice, in the current context.
     * <p>
     *
     * @return
     */
    public Map<RhythmVoice, Phrase> getOriginalRvPhraseMap()
    {
        return songSession.getRvPhraseMap();
    }

    /**
     * Get the current Phrase for each RhythmVoice track.
     *
     * @return
     */
    public Map<RhythmVoice, Phrase> getCurrentRvPhraseMap()
    {
        return currentMapRvPhrase;
    }

    @Override
    public State getState()
    {
        return songSession.getState();
    }

    @Override
    public boolean isDirty()
    {
        return songSession.isDirty();
    }

    @Override
    public int getTempo()
    {
        return songSession.getTempo();
    }

    @Override
    public HashMap<Integer, Boolean> getTracksMuteStatus()
    {
        // Our version
        return mapTrackIdMuted;
    }

    @Override
    public long getLoopEndTick()
    {
        return songSession.getLoopEndTick();
    }

    @Override
    public long getLoopStartTick()
    {
        return songSession.getLoopStartTick();
    }

    @Override
    public int getLoopCount()
    {
        return songSession.getLoopCount();
    }

    @Override
    public IntRange getBarRange()
    {
        return songSession.getBarRange();
    }

    @Override
    public long getTick(int barIndex)
    {
        return songSession.getTick(barIndex);

    }

    @Override
    public void cleanup()
    {
        songSession.removePropertyChangeListener(this);
        songSession.cleanup();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }


    /**
     * Overridden to do the equals only on the underlying BaseSongSession.
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final UpdatableSongSession other = (UpdatableSongSession) obj;
        if (!Objects.equals(this.songSession, other.songSession))
        {
            return false;
        }
        return true;
    }

    /**
     * Overridden to do the hash only on the underlying BaseSongSession.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.songSession);
        return hash;
    }

    // ==========================================================================================================
    // SongContextProvider implementation
    // ==========================================================================================================    
    @Override
    public SongContext getSongContext()
    {
        return songSession.getSongContext();
    }

    // ==========================================================================================================
    // ControlTrackProvider implementation
    // ==========================================================================================================    
    @Override
    public List<Position> getSongPositions()
    {
        return songSession.getSongPositions();
    }


    @Override
    public ContextChordSequence getContextChordGetSequence()
    {
        return songSession.getContextChordGetSequence();
    }

    // ==========================================================================================================
    // EndOfPlaybackActionProvider implementation
    // ==========================================================================================================   
    @Override
    public ActionListener getEndOfPlaybackAction()
    {
        return songSession.getEndOfPlaybackAction();
    }

    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {

//        LOGGER.fine("propertyChange() e=" + e);

        if (e.getSource() == songSession)
        {

            PropertyChangeEvent newEvent = null;

            if (e.getPropertyName().equals(UpdateProvider.PROP_UPDATE_AVAILABLE)
                    && (songSession instanceof UpdateProvider)
                    && getState().equals(State.GENERATED))
            {
                var update = ((UpdateProvider) (songSession)).getUpdate();
                updateSequence(update);

            } else if (e.getPropertyName().equals(PlaybackSession.PROP_MUTED_TRACKS))
            {
                // Need to map the new mute status to the currently active tracks                
                var originalMapTrackIdMuted = songSession.getTracksMuteStatus();
                for (int trackId : originalMapTrackIdMuted.keySet())
                {
                    boolean muted = originalMapTrackIdMuted.get(trackId);
                    mapTrackIdMuted.put(trackSet.getActiveTrackId(trackId), muted);
                }
                newEvent = new PropertyChangeEvent(this, e.getPropertyName(), e.getOldValue(), mapTrackIdMuted);

            } else
            {
                newEvent = new PropertyChangeEvent(this, e.getPropertyName(), e.getOldValue(), e.getNewValue());

            }

            if (newEvent != null)
            {
                // Forward the event and make this object the event source
                newEvent.setPropagationId(e.getPropagationId());
                pcs.firePropertyChange(newEvent);
            }
        }


    }

    @Override
    public String toString()
    {
        return "UpdatableSongSession=[" + songSession + "]";
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================

    /**
     * Save data from the original session and prepare the trackSet for future updates.
     */
    private void prepareData()
    {

        sequence = songSession.getSequence();
        originalTrackTickSize = sequence.getTickLength();       // Possibly include precount leading bars
        nbPlayingTracks = sequence.getTracks().length;
        currentMapRvPhrase = songSession.getRvPhraseMap();


        // Create the trackset to manage double-buffering at track level
        var originalMapIdMuted = songSession.getTracksMuteStatus(); // Track 0 is not included, but may contain click/precount/control tracks
        trackSet = new TrackSet(sequence);
        originalMapIdMuted.keySet().forEach(trackId -> trackSet.addTrack(trackId));


        // Initialize our own tracks mute state
        for (int trackId : originalMapIdMuted.keySet())
        {
            mapTrackIdMuted.put(trackId, originalMapIdMuted.get(trackId));
            mapTrackIdMuted.put(trackSet.getBufferTrackId(trackId), true);         // buffer track is muted
        }


//        LOGGER.info("generate() mapTrackIdMuted=" + mapTrackIdMuted);
//        LOGGER.info("generate() mapRvTrackId=" + songContextSession.getRvTrackIdMap());
//        LOGGER.info("generate() trackSet=" + trackSet.toString());
//        LOGGER.info(" Original sequence=" + MidiUtilities.toString(sequence));
    }

    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================    
    /**
     * Manage a set of tracks of a sequence: N active tracks and N buffer tracks.
     */
    static private class TrackSet
    {

        private final Sequence trackSetSequence;
        private final Map<Integer, Integer> mapOriginalIdActiveId = new HashMap<>();
        private final Map<Integer, Integer> mapOriginalIdBufferId = new HashMap<>();

        public TrackSet(Sequence seq)
        {
            this.trackSetSequence = seq;
        }

        /**
         * Add a track to the TrackSet and create the corresponding buffer track.
         *
         * @param originalTrackId A value between 0 and getNbPlayingTracks()-1.
         */
        public void addTrack(int originalTrackId)
        {
            mapOriginalIdActiveId.put(originalTrackId, originalTrackId);

            // Create the corresponding buffer track
            Track bufferTrack = trackSetSequence.createTrack();
            int bufferTrackId = Arrays.asList(trackSetSequence.getTracks()).indexOf(bufferTrack);
            mapOriginalIdBufferId.put(originalTrackId, bufferTrackId);
        }

        /**
         * The buffer track for originalTrackId becomes the active track, and vice-versa.
         *
         * @param originalTrackId
         */
        public void swapBufferAndActiveTracks(int originalTrackId)
        {
            int tmp = getActiveTrackId(originalTrackId);
            mapOriginalIdActiveId.put(originalTrackId, getBufferTrackId(originalTrackId));
            mapOriginalIdBufferId.put(originalTrackId, tmp);
        }

        /**
         * The active track corresponding to the original track id.
         *
         * @param originalTrackId A value &lt; getNbPlayingTracks()
         * @return
         */
        public Track getActiveTrack(int originalTrackId)
        {
            return trackSetSequence.getTracks()[getActiveTrackId(originalTrackId)];
        }

        /**
         * The buffer track corresponding to the original track id.
         *
         * @param originalTrackId A value &lt; getNbPlayingTracks()
         * @return
         */
        public Track getBufferTrack(int originalTrackId)
        {
            return trackSetSequence.getTracks()[getBufferTrackId(originalTrackId)];
        }

        /**
         * The active track id corresponding to the original track id .
         *
         * @param originalTrackId A value &lt; getNbPlayingTracks()
         * @return
         */
        public int getActiveTrackId(int originalTrackId)
        {
            return mapOriginalIdActiveId.get(originalTrackId);
        }

        /**
         * The buffer track id corresponding to the original track id .
         *
         * @param originalTrackId A value &lt; getNbPlayingTracks()
         * @return
         */
        public int getBufferTrackId(int originalTrackId)
        {
            return mapOriginalIdBufferId.get(originalTrackId);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Trackset:\n");
            sb.append("   mapOriginalIdActiveId=" + mapOriginalIdActiveId + "\n");
            sb.append("   mapOriginalIdBufferId=" + mapOriginalIdBufferId + "\n");
            return sb.toString();
        }

    }

}

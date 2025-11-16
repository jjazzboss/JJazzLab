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

import com.google.common.base.Preconditions;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.musiccontrol.api.ControlTrack;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.utilities.api.IntRange;

/**
 * An UpdatableSongSession is a BaseSongSession wrapper which enables on-the-fly updates of the playing sequence using {@link updateSequence(Update)}.
 * <p>
 * Authorized udpates are notes+control track changes which do not change the Sequence size. Only user phrase modification or removal is supported.
 * <p>
 * The class uses buffer tracks and mute/unmute tracks to enable on-the-fly sequence changes.
 * <p>
 * If the BaseSongSession is an instance of UpdateProvider, the UpdatableSongSession listens to updates availability and automatically apply the updates.
 */
public class UpdatableSongSession implements PropertyChangeListener, PlaybackSession, ControlTrackProvider, SongContextProvider, EndOfPlaybackActionProvider
{

    public static final String PROP_ENABLED = "PropEnabled";
    /**
     * newValue = UpdatableSongSession.Update received.
     */
    public static final String PROP_UPDATE_RECEIVED = "PropUpdateReceived";

    /**
     * A song update produced by an UpdateProvider and processed by the UpdatableSongSession.
     */
    static public class Update
    {

        private final Map<RhythmVoice, Phrase> mapRvPhrases;
        private final ControlTrack controlTrack;

        public Update(Map<RhythmVoice, Phrase> mapRvPhrases, ControlTrack controlTrack)
        {
            Preconditions.checkArgument(mapRvPhrases != null || controlTrack != null);
            this.mapRvPhrases = mapRvPhrases;
            this.controlTrack = controlTrack;
        }

        /**
         * The updated phrases for one or more RhythmVoices.
         * <p>
         * Can be null (no update) if controlTrack is not null.
         *
         * @return
         */
        public Map<RhythmVoice, Phrase> getMapRvPhrases()
        {
            return mapRvPhrases;
        }

        /**
         * The updated control track.
         * <p>
         * Can be null (no update) if mapRvPhrases is not null.
         *
         * @return
         */
        public ControlTrack getControlTrack()
        {
            return controlTrack;
        }

        @Override
        public String toString()
        {
            return "<mapRvPhrases.keySet=" + getMapRvPhrases().keySet() + ", controlTrack=" + getControlTrack() + ">";
        }
    }

    /**
     * A SongContextSession capability: can provide updates after sequence was generated (i.e. while session is in the GENERATED state).
     * <p>
     * Implementation must fire the relevant property change events.
     */
    public interface UpdateProvider
    {

        /**
         * Property change event to be fired when a new update is available.
         * <p>
         * Note: event can be fired out of the Swing EDT.
         */
        public static String PROP_UPDATE_AVAILABLE = "PropUpdateAvailable";
        /**
         * Property change event to be fired when updates can't be provided anymore.
         */
        public static final String PROP_UPDATE_PROVISION_ENABLED = "PropUpdateProvisionEnabled";


        /**
         * Check if this UpdateProvider can still provide updates.
         *
         *
         * @return True upon this object's creation, but might become false if UpdateProvider is "too" dirty and not able anymore to provide updates.
         * @see PlaybackSession#isDirty()
         */
        boolean isUpdateProvisionEnabled();


        /**
         * Get the last available update after the PROP_UPDATE_AVAILABLE property change event was fired.
         *
         * @return Can be null if PROP_UPDATE_AVAILABLE change event was never fired.
         */
        Update getLastUpdate();
    }


    private long originalTrackTickSize;
    private int nbPlayingTracks;
    private Map<RhythmVoice, Phrase> currentMapRvPhrase;
    private ControlTrack currentControlTrack;
    private TrackSet trackSet;         // Exclude track 0 
    private BaseSongSession baseSongSession;
    private Sequence sequence;
    private boolean enabled;
    private final HashMap<Integer, Boolean> mapTrackIdMuted = new HashMap<>();
    private static final List<UpdatableSongSession> sessions = new ArrayList<>();

    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(UpdatableSongSession.class.getSimpleName());


    /**
     * Create or reuse a session for the specified parameters.
     * <p>
     * Sessions are cached: if an existing session already exists for the same parameters then return it, otherwise a new session is created.
     * <p>
     * @param session Must be in the NEW or GENERATED state. If it is an UpdateProvider instance automatically apply the updates when available.
     * @return
     */
    static public UpdatableSongSession getSession(BaseSongSession session)
    {
        if (session == null)
        {
            throw new IllegalArgumentException("session=" + session);
        }
        if (session.getState() == State.CLOSED)
        {
            throw new IllegalStateException("session=" + session);
        }

        UpdatableSongSession updatableSession = findSession(session);
        if (updatableSession == null)
        {
            UpdatableSongSession newSession = new UpdatableSongSession(session);
            sessions.add(newSession);
            LOGGER.fine("getSession() create new session");
            return newSession;
        } else
        {
            LOGGER.fine("getSession() reusing existing session!");
            return updatableSession;
        }
    }

    /**
     * Create an UpdatableSongSession to enable updates of the specified BaseSongSession.
     *
     * @param session If session is an UpdateProvider instance automatically apply the update.
     */
    protected UpdatableSongSession(BaseSongSession session)
    {
        if (session == null)
        {
            throw new IllegalArgumentException("session=" + session);
        }

        enabled = true;

        baseSongSession = session;
        baseSongSession.addPropertyChangeListener(this);


        if (baseSongSession.getState() == State.GENERATED)
        {
            prepareData();
        }
    }

    public BaseSongSession getBaseSession()
    {
        return baseSongSession;
    }

    @Override
    public Context getContext()
    {
        return baseSongSession.getContext();
    }

    @Override
    public UpdatableSongSession getFreshCopy(SongContext sgContext)
    {
        var newBaseSession = baseSongSession.getFreshCopy(sgContext);
        UpdatableSongSession newSession = new UpdatableSongSession(newBaseSession);
        sessions.add(newSession);
        return newSession;
    }


    @Override
    public void generate(boolean silent) throws MusicGenerationException
    {
        baseSongSession.generate(silent);

        prepareData();
    }


    /**
     * Get the sequence which contains the original song tracks plus additional empty tracks to allow "double buffering modification" via muting/unmuting
     * tracks.
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
     * Check is this session is enabled.
     * <p>
     * The session is enabled by default upon creation. It might be automatically disabled when our base song session is an UpdateProvider which can not provide
     * updates anymore.
     *
     * @return
     */
    public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * Disable the session: session will ignore received updates.
     *
     * @param b
     * @see #PROP_ENABLED
     */
    public void setEnabled(boolean b)
    {
        if (b != enabled)
        {
            enabled = b;
            pcs.firePropertyChange(PROP_ENABLED, !enabled, enabled);
        }
    }

    /**
     * Update the sequence with the specified parameter.
     * <p>
     * Update RhythmVoice tracks for which there is an actual change. Changes are first applied to muted "buffer tracks", then we switch the mute status between
     * the buffer and the playing tracks. The transition might be noticeable if notes were still ringing when tracks mute state is switched, but usually it's
     * unnoticeable.
     * <p>
     * Fire a PROP_UPDATED_RECEIVED change event.
     * <p>
     * The method does nothing if session is disabled.
     *
     * @param update
     * @throws IllegalArgumentException If a MidiEvent tick position is beyond getOriginalSequenceSize(), or if session is not in the GENERATED state.
     */
    public void updateSequence(Update update)
    {
        LOGGER.log(Level.FINE, "updateSequence() ---- update={0} nanoTime()={1}", new Object[]
        {
            update, System.nanoTime()
        });

        if (!enabled)
        {
            return;
        }

        if (!getState().equals(PlaybackSession.State.GENERATED))
        {
            throw new IllegalStateException("getState()=" + getState() + " update=" + update);
        }


        // Pre-calculate the optional precount shift
        long precountShift = 0;
        if (baseSongSession.getPrecountTrackId() != -1)
        {
            TimeSignature ts = baseSongSession.getSongContext().getSongParts().get(0).getRhythm().getTimeSignature();
            int nbPrecountBars = PlaybackSettings.getInstance().getClickPrecountNbBars(ts,
                    baseSongSession.getSongContext().getSong().getTempo());
            precountShift = (long) Math.ceil(nbPrecountBars * ts.getNbNaturalBeats() * MidiConst.PPQ_RESOLUTION);
        }


        // We might have potentially modified phrases, new user phrases or deleted user phrases
        Set<RhythmVoice> updatedRvs = update.getMapRvPhrases().keySet();
        Set<RhythmVoice> currentRvs = currentMapRvPhrase.keySet();
        var modifiedPhraseRvs = new HashSet<>(updatedRvs);
        modifiedPhraseRvs.retainAll(currentRvs);
        var newUserPhraseRvs = new HashSet<>(updatedRvs);
        newUserPhraseRvs.removeAll(currentRvs);
        var removedUserPhraseRvs = new HashSet<>(currentRvs);
        removedUserPhraseRvs.removeAll(updatedRvs);


        // It's an error if we have a new user phrase passed in an update: we can only work with a constant number of RhythmVoices/tracks
        if (!newUserPhraseRvs.isEmpty())
        {
            throw new IllegalStateException(
                    "updatedRvs=" + updatedRvs + " currentRvs=" + currentRvs + " => newUserPhraseRvs=" + newUserPhraseRvs);
        }


        // Update sequence for each modified phrase 
        for (RhythmVoice rv : modifiedPhraseRvs)
        {
            var updatedPhrase = update.getMapRvPhrases().get(rv);
            var currentPhrase = currentMapRvPhrase.get(rv);
            LOGGER.log(Level.FINE, "   rv={0}", rv);
            LOGGER.log(Level.FINE, "     currentPhrase={0}", currentPhrase);
            LOGGER.log(Level.FINE, "     updatedPhrase={0}", updatedPhrase);


            if (currentPhrase.equalsAsNoteNearPosition(updatedPhrase, 0))
            {
                // No change do nothing
                continue;
            } else
            {
                // Replace the current events
                LOGGER.log(Level.FINE, "updateSequence()     changes detected for rv={0}, updating", rv);
                currentMapRvPhrase.put(rv, updatedPhrase);
            }


            // Update the track
            int trackId = getOriginalRvTrackIdMap().get(rv);
            updateTrack(trackId, Phrases.toMidiEvents(updatedPhrase), precountShift);

        }


        // Update sequence for each removed user phrase : set an empty phrase
        for (RhythmVoice urv : removedUserPhraseRvs)
        {
            LOGGER.log(Level.FINE, "    Clearing user phrase for urv={0}", urv.getName());
            int channel = currentMapRvPhrase.get(urv).getChannel();
            Phrase emptyPhrase = new Phrase(channel, urv.isDrums());
            currentMapRvPhrase.put(urv, emptyPhrase);
            int trackId = getOriginalRvTrackIdMap().get(urv);
            updateTrack(trackId, Phrases.toMidiEvents(emptyPhrase), precountShift);
        }


        // Update control track if changed
        if (update.getControlTrack() != null)
        {
            currentControlTrack = update.getControlTrack();
            int trackId = update.getControlTrack().getTrackId();
            updateTrack(trackId, currentControlTrack.getMidiEvents(), precountShift);

        }


        LOGGER.log(Level.FINE, "updateSequence() AFTER: mapTrackIdMuted={0}", mapTrackIdMuted);


        // Notify our listeners that tracks mute status has changed
        pcs.firePropertyChange(PlaybackSession.PROP_MUTED_TRACKS, null, mapTrackIdMuted);


        // General notification about the update reception
        pcs.firePropertyChange(PROP_UPDATE_RECEIVED, null, update);
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
        return baseSongSession.getRvTrackIdMap();
    }

    /**
     * A map giving the original resulting Phrase for each RhythmVoice, in the current context.
     * <p>
     *
     * @return
     */
    public Map<RhythmVoice, Phrase> getOriginalRvPhraseMap()
    {
        return baseSongSession.getRvPhraseMap();
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
        return baseSongSession.getState();
    }

    @Override
    public boolean isDirty()
    {
        return baseSongSession.isDirty();
    }

    @Override
    public int getTempo()
    {
        return baseSongSession.getTempo();
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
        return baseSongSession.getLoopEndTick();
    }

    @Override
    public long getLoopStartTick()
    {
        return baseSongSession.getLoopStartTick();
    }

    @Override
    public int getLoopCount()
    {
        return baseSongSession.getLoopCount();
    }

    @Override
    public IntRange getBarRange()
    {
        return baseSongSession.getBarRange();
    }

    @Override
    public long getTick(int barIndex)
    {
        return baseSongSession.getTick(barIndex);
    }

    @Override
    public void close()
    {
        sessions.remove(this);
        baseSongSession.removePropertyChangeListener(this);
        baseSongSession.close();
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
        if (!Objects.equals(this.baseSongSession, other.baseSongSession))
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
        hash = 59 * hash + Objects.hashCode(this.baseSongSession);
        return hash;
    }

    // ==========================================================================================================
    // SongContextProvider implementation
    // ==========================================================================================================    
    @Override
    public SongContext getSongContext()
    {
        return baseSongSession.getSongContext();
    }

    // ==========================================================================================================
    // ControlTrackProvider implementation
    // ==========================================================================================================    
    @Override
    public ControlTrack getControlTrack()
    {
        return currentControlTrack;
    }

    // ==========================================================================================================
    // EndOfPlaybackActionProvider implementation
    // ==========================================================================================================   
    @Override
    public ActionListener getEndOfPlaybackAction()
    {
        return baseSongSession.getEndOfPlaybackAction();
    }

    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {

//        LOGGER.fine("propertyChange() e=" + e);

        if (e.getSource() == baseSongSession)
        {
            PropertyChangeEvent newEvent = null;

            if (e.getPropertyName().equals(PlaybackSession.PROP_MUTED_TRACKS))
            {
                // Need to map the new mute status to the currently active tracks                
                var originalMapTrackIdMuted = baseSongSession.getTracksMuteStatus();
                for (int trackId : originalMapTrackIdMuted.keySet())
                {
                    boolean muted = originalMapTrackIdMuted.get(trackId);
                    mapTrackIdMuted.put(trackSet.getActiveTrackId(trackId), muted);
                }

                // We still need to propagate the event (eg used by MusicController to effectively mute/unmute Sequencer tracks
                newEvent = new PropertyChangeEvent(this, e.getPropertyName(), e.getOldValue(), mapTrackIdMuted);


            } else if (getState() == State.GENERATED
                    && (baseSongSession instanceof UpdateProvider up)
                    && e.getPropertyName().equals(UpdateProvider.PROP_UPDATE_AVAILABLE))
            {
                // Got an update
                var update = up.getLastUpdate();
                updateSequence(update);

            } else if (getState() == State.GENERATED
                    && (baseSongSession instanceof UpdateProvider up)
                    && e.getPropertyName().equals(UpdateProvider.PROP_UPDATE_PROVISION_ENABLED))
            {
                // We won't receive updates anymore from our baseSongSession: disable ourselves
                assert !up.isUpdateProvisionEnabled() && baseSongSession.isDirty() : "baseSongSession=" + baseSongSession;
                setEnabled(false);

            } else
            {
                // Not handled here, just propagate the event
                newEvent = new PropertyChangeEvent(this, e.getPropertyName(), e.getOldValue(), e.getNewValue());
            }


            if (newEvent != null)
            {
                // Forward the event and make this object the event source (eg PROP_STATE, PROP_DIRTY, PROP_LOOP_COUNT, ...)
                newEvent.setPropagationId(e.getPropagationId());
                pcs.firePropertyChange(newEvent);
            }
        }
    }


    @Override
    public String toString()
    {
        return "UpdatableSongSession=[" + baseSongSession + "]";
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================

    /**
     * Save data from the original session and prepare the trackSet for future updates.
     */
    private void prepareData()
    {

        sequence = baseSongSession.getSequence();
        originalTrackTickSize = sequence.getTickLength();       // Possibly include precount leading bars
        nbPlayingTracks = sequence.getTracks().length;
        currentMapRvPhrase = baseSongSession.getRvPhraseMap();
        currentControlTrack = baseSongSession.getControlTrack();


        // Create the trackset to manage double-buffering at track level
        var originalMapIdMuted = baseSongSession.getTracksMuteStatus(); // Track 0 is not included, but may contain click/precount/control tracks
        trackSet = new TrackSet(sequence);
        originalMapIdMuted.keySet().forEach(trackId -> trackSet.addTrack(trackId));


        // Initialize our own tracks mute state
        for (int trackId : originalMapIdMuted.keySet())
        {
            mapTrackIdMuted.put(trackId, originalMapIdMuted.get(trackId));
            mapTrackIdMuted.put(trackSet.getBufferTrackId(trackId), true);         // buffer track is muted
        }


        LOGGER.log(Level.FINE, "prepareData() mapTrackIdMuted={0}", mapTrackIdMuted);
        LOGGER.log(Level.FINE, "prepareData() mapRvTrackId={0}", baseSongSession.getRvTrackIdMap());
        LOGGER.log(Level.FINE, "prepareData() trackSet={0}", trackSet);
    }


    /**
     * Update one track.
     *
     * @param trackId
     * @param newEvents          IMPORTANT events positions will be modified!
     * @param precountTickOffset
     * @throws IllegalArgumentException
     */
    private void updateTrack(int trackId, List<MidiEvent> newEvents, long precountTickOffset) throws IllegalArgumentException
    {
        Track bufferTrack = trackSet.getBufferTrack(trackId);
        MidiUtilities.clearTrack(bufferTrack);


        for (MidiEvent me : newEvents)
        {
            // Adjust position if precount bars are used
            me.setTick(me.getTick() + precountTickOffset);

            if (me.getTick() > originalTrackTickSize)
            {
                throw new IllegalArgumentException(
                        "me=" + MidiUtilities.toString(me.getMessage(), me.getTick()) + " originalTrackTickSize=" + originalTrackTickSize);
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


    /**
     * Find an identical existing session in state NEW or GENERATED and not dirty.
     *
     * @param session
     * @return Null if not found
     */
    static private UpdatableSongSession findSession(BaseSongSession session)
    {
        for (var updatableSession : sessions)
        {
            if (EnumSet.of(PlaybackSession.State.GENERATED, PlaybackSession.State.NEW).contains(session.getState())
                    && !updatableSession.isDirty()
                    && session == updatableSession.baseSongSession)
            {
                return updatableSession;
            }
        }
        return null;

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

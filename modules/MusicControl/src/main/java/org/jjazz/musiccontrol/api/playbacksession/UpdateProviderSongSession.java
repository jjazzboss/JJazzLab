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

import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.musiccontrol.api.ControlTrack;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.SongMusicGenerationListener;
import static org.jjazz.musiccontrol.api.playbacksession.BaseSongSession.PLAYBACK_SETTINGS_LOOP_COUNT;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession.Update;
import static org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession.UpdateProvider.PROP_UPDATE_PROVISION_ENABLED;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.openide.awt.StatusDisplayer;
import org.openide.util.*;

/**
 * A session to be used as a BaseSongSession for an UpdatableSongSession.
 * <p>
 * The session provides on-the-fly UpdatableSongSession.Updates for:<br>
 * - chord symbol changes (add/remove/change/moveAll)<br>
 * - rhythm parameter value changes<br>
 * - existing user phrase content changes (but not for add/remove user phrase events)<br>
 * - PlaybackSettings playback transposition changes<br>
 * - MidiMix instrument transposition/velocity changes, plus drum keymap and drum rerouting changes<br>
 * <p>
 * If change can't be handled as an update (eg a song part tempo factor change or a click setting), session is marked dirty (ie needs
 * regeneration). If session is dirty, editors can still show the playback point using the control track but the "dirty" changes are not
 * heard.
 * <p>
 * A more serious change like a structural change of the song context will make the session dirty, plus it disables the control track and
 * any future update. So editors should stop showing the playback point, and any further change will not be heard (until a new session is
 * generated).
 */
public class UpdateProviderSongSession extends BaseSongSession implements UpdatableSongSession.UpdateProvider
{

    /**
     * @see MusicGenerationQueue
     */
    public static final int DEFAULT_PRE_UPDATE_BUFFER_TIME_MS = 300;
    /**
     * @see MusicGenerationQueue
     */
    public static final int DEFAULT_POST_UPDATE_SLEEP_TIME_MS = 700;
    private int preUpdateBufferTimeMs = DEFAULT_PRE_UPDATE_BUFFER_TIME_MS;
    private int postUpdateSleepTimeMs = DEFAULT_POST_UPDATE_SLEEP_TIME_MS;
    private Update update;
    private boolean isUpdateProvisionEnabled = true;
    private boolean isControlTrackEnabled = true;
    private final boolean isUpdateControlEnabled;
    private SongMusicGenerationListener songMusicGenerationListener;
    private MusicGenerationQueue musicGenerationQueue;
    private Consumer<UserErrorGenerationException> userErrorExceptionHandler;
    private static final List<UpdateProviderSongSession> sessions = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(UpdateProviderSongSession.class.getSimpleName());


    /**
     * Create or reuse a session for the specified parameters.
     * <p>
     * <p>
     * Sessions are cached: if a non-dirty session in the NEW or GENERATED state already exists for the same parameters then return it,
     * otherwise a new session is created.
     * <p>
     *
     * @param sgContext
     * @param enablePlaybackTransposition If true apply the playback transposition
     * @param includeClickTrack           If true add the click track, and its muted/unmuted state will depend on the PlaybackSettings
     * @param includePrecountTrack        If true add the precount track, and loopStartTick will depend on the PlaybackSettings
     * @param includeControlTrack         if true add a control track (beat positions + chord symbol markers)
     * @param enableUpdateControl         If true updates are authorized depending on the PlaybackSettings AutoUpdateEnabled value.
     * @param loopCount                   See Sequencer.setLoopCount(). Use PLAYBACK_SETTINGS_LOOP_COUNT to rely on the PlaybackSettings
     *                                    instance value.
     * @param endOfPlaybackAction         Action executed when playback is stopped. Can be null.
     * @return A session in the NEW or GENERATED state.
     */
    static public UpdateProviderSongSession getSession(SongContext sgContext,
            boolean enablePlaybackTransposition, boolean includeClickTrack, boolean includePrecountTrack, boolean includeControlTrack,
            boolean enableUpdateControl,
            int loopCount,
            ActionListener endOfPlaybackAction)
    {
        if (sgContext == null)
        {
            throw new IllegalArgumentException("sgContext=" + sgContext);
        }
        UpdateProviderSongSession session = findSession(sgContext,
                enablePlaybackTransposition, includeClickTrack, includePrecountTrack, includeControlTrack,
                enableUpdateControl,
                loopCount,
                endOfPlaybackAction);
        if (session == null)
        {
            final UpdateProviderSongSession newSession = new UpdateProviderSongSession(sgContext,
                    enablePlaybackTransposition, includeClickTrack, includePrecountTrack, includeControlTrack,
                    enableUpdateControl,
                    loopCount,
                    endOfPlaybackAction);

            sessions.add(newSession);
            LOGGER.fine("getSession() create new session");
            return newSession;
        } else
        {
            LOGGER.fine("getSession() reusing existing session");
            return session;
        }
    }

    /**
     * Same as getSession(sgContext, true, true, true, true, true, PLAYBACK_SETTINGS_LOOP_COUNT, null);
     * <p>
     *
     * @param sgContext
     * @return A targetSession in the NEW or GENERATED state.
     */
    static public UpdateProviderSongSession getSession(SongContext sgContext)
    {
        return getSession(sgContext, true, true, true, true, true, PLAYBACK_SETTINGS_LOOP_COUNT, null);
    }


    private UpdateProviderSongSession(SongContext sgContext,
            boolean enablePlaybackTransposition,
            boolean includeClickTrack, boolean includePrecountTrack, boolean includeControlTrack,
            boolean enableUpdateControl,
            int loopCount, ActionListener endOfPlaybackAction)
    {
        super(sgContext,
                enablePlaybackTransposition,
                includeClickTrack, includePrecountTrack, includeControlTrack,
                loopCount, endOfPlaybackAction);

        isUpdateControlEnabled = enableUpdateControl;
        userErrorExceptionHandler = e -> StatusDisplayer.getDefault().setStatusText(e.getLocalizedMessage());
    }

    @Override
    public UpdateProviderSongSession getFreshCopy(SongContext sgContext)
    {
        var newContext = sgContext == null ? getSongContext().clone() : sgContext;
        UpdateProviderSongSession newSession = new UpdateProviderSongSession(newContext,
                isPlaybackTranspositionEnabled(),
                isClickTrackIncluded(),
                isPrecountTrackIncluded(),
                isControlTrackIncluded(),
                isUpdateControlEnabled,
                getLoopCount(),
                getEndOfPlaybackAction());

        sessions.add(newSession);

        return newSession;
    }

    /**
     * Generate the initial music then listen for song changes to be ready to generate updates.
     *
     * @param silent
     * @throws MusicGenerationException
     */
    @Override
    public void generate(boolean silent) throws MusicGenerationException
    {
        super.generate(silent);

        var song = getSongContext().getSong();
        songMusicGenerationListener = new SongMusicGenerationListener(song, getSongContext().getMidiMix(), 0);  // 0ms because we can't miss an event which might disable updates
        songMusicGenerationListener.addPropertyChangeListener(this);
    }

    @Override
    public void close()
    {
        super.close();
        if (songMusicGenerationListener != null)
        {
            songMusicGenerationListener.removePropertyChangeListener(this);
            songMusicGenerationListener.cleanup();
        }
        sessions.remove(this);
    }


    /**
     * True if updates are authorized/blocked depending on the PlaybackSettings auto update value.
     *
     * @return
     */
    public boolean isUpdateControlEnabled()
    {
        return isUpdateControlEnabled;
    }


    /**
     * Wait time before starting the music generation to provide an update.
     *
     * @return
     * @see MusicGenerationQueue#getPreUpdateBufferTimeMs()
     */
    public int getPreUpdateBufferTimeMs()
    {
        return preUpdateBufferTimeMs;
    }

    /**
     * Calls to this method are ignored if state is not NEW.
     *
     * @param preUpdateBufferTimeMs
     */
    public void setPreUpdateBufferTimeMs(int preUpdateBufferTimeMs)
    {
        this.preUpdateBufferTimeMs = preUpdateBufferTimeMs;
    }

    /**
     * Wait time after having generated an update.
     *
     * @return
     * @see MusicGenerationQueue#getPostUpdateSleepTimeMs()
     */
    public int getPostUpdateSleepTimeMs()
    {
        return postUpdateSleepTimeMs;
    }

    /**
     * Calls to this method are ignored if state is not NEW.
     *
     * @param postUpdateSleepTimeMs
     * @see
     */
    public void setPostUpdateSleepTimeMs(int postUpdateSleepTimeMs)
    {
        this.postUpdateSleepTimeMs = postUpdateSleepTimeMs;
    }

    /**
     * The handler for exception during music generation due to user error.
     * <p>
     * By default the handler displays the exception error message using StatusDisplayer.
     *
     * @param handler If null errors will be ignored
     */
    public void setUserErrorExceptionHandler(Consumer<UserErrorGenerationException> handler)
    {
        userErrorExceptionHandler = handler;
    }

    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        super.propertyChange(e);            // Important

        if (getState().equals(PlaybackSession.State.CLOSED) || !isUpdateProvisionEnabled())
        {
            return;
        }

        if (e.getSource() != songMusicGenerationListener || !e.getPropertyName().equals(SongMusicGenerationListener.PROP_CHANGED))
        {
            return;
        }


        //
        // If we're here it means that 
        // - song has changed in a way that impact music generation
        // - state==GENERATED
        //
        // LOGGER.fine("propertyChange() e=" + e);
        boolean dirty = false;
        boolean doUpdate = false;
        boolean doDisableUpdates = false;


        // Analyze the PROP_CHANGED change event origin
        String id = (String) e.getOldValue();
        Object data = e.getNewValue();


        LOGGER.log(Level.FINE, "propertyChange() -- id={0} data={1}", new Object[]
        {
            id, data
        });


        switch (id)
        {
            //
            // MidiMix source events
            //                

            case MidiMix.PROP_RHYTHM_VOICE_CHANNEL, MidiMix.PROP_RHYTHM_VOICE ->
            {
                doDisableUpdates = true;
            }
            case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX ->
            {
                if (data instanceof UserRhythmVoice)
                {
                    // We can accept user channel removal                    
                    doUpdate = true;
                } else
                {
                    doDisableUpdates = true;
                }
            }
            case MidiMix.PROP_CHANNEL_DRUMS_REROUTED, MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP, MidiMix.PROP_INSTRUMENT_TRANSPOSITION, MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT ->
            {
                doUpdate = true;
            }


            //
            // PlaybackSettings source events
            //       
            case PlaybackSettings.PROP_CLICK_PITCH_HIGH, PlaybackSettings.PROP_CLICK_PITCH_LOW, PlaybackSettings.PROP_CLICK_PREFERRED_CHANNEL, PlaybackSettings.PROP_CLICK_VELOCITY_HIGH, PlaybackSettings.PROP_CLICK_VELOCITY_LOW, PlaybackSettings.PROP_CLICK_PRECOUNT_MODE, PlaybackSettings.PROP_CLICK_PRECOUNT_ENABLED ->
            {
                dirty = true;
            }
            case PlaybackSettings.PROP_PLAYBACK_KEY_TRANSPOSITION, PlaybackSettings.PROP_PLAYBACK_CLICK_ENABLED ->
            {
                doUpdate = true;
            }


            //
            // Song source events
            //                                
            case Song.PROP_VETOABLE_USER_PHRASE ->
            {
                String phraseName = (String) data;
                if (getSongContext().getSong().getUserPhrase(phraseName) == null)
                {
                    // A user phrase was removed: this is supported by the UpdatableSongSession
                    doUpdate = true;
                } else
                {
                    // A user phrase was added
                    doDisableUpdates = true;
                }
            }
            case Song.PROP_VETOABLE_USER_PHRASE_CONTENT ->
            {
                doUpdate = true;
            }

            //
            // ChordLeadSheet source events
            //   
            case "setSize", "addSection", "removeSection", "moveSection", "insertBars", "deleteBars", "setSectionTimeSignature" ->
            {
                doDisableUpdates = true;
            }
            case "addItem", "removeItem", "moveItem", "changeItem" ->
            {
                doUpdate = true;
            }


            //
            // SongStructure source events
            //   
            case "addSongParts", "removeSongParts", "resizeSongParts", "replaceSongParts" ->
            {
                doDisableUpdates = true;
            }
            case "setRhythmParameterValue", "setRhythmParameterValueContent" ->
            {
                if (data instanceof RP_SYS_TempoFactor)
                {
                    // UpdatableSongSession can't update this in realtime                      
                    dirty = true;
                } else
                {
                    doUpdate = true;
                }
            }


            default ->
            {
                throw new IllegalStateException("id=" + id + " data=" + data);
            }
        }


        LOGGER.log(Level.FINE,
                "propertyChange() output: dirty={0} doUpdate={1} doDisableUpdates={2}", new Object[]
                {
                    dirty, doUpdate, doDisableUpdates
                }
        );

        if (doDisableUpdates)
        {
            disableUpdates();
        } else
        {
            synchronized (this)
            {
                // Synchonize because the UpdateGenerationTask thread might call setDirty(), so this makes sure
                // generateUpdate() won't be called if thread called setDirty() right after "if (dirty)" was executed with dirty==false.
                if (dirty)
                {
                    setDirty();
                } else if (doUpdate)
                {
                    generateUpdate();
                }
            }
        }

    }


    // ==========================================================================================================
    // UpdatableSongSession.UpdateProvider interface
    // ==========================================================================================================
    @Override
    public Update getLastUpdate()
    {
        return update;
    }

    @Override
    public boolean isUpdateProvisionEnabled()
    {
        return isUpdateProvisionEnabled;
    }


    // ==========================================================================================================
    // ControlTrackProvider implementation
    // ==========================================================================================================    
    @Override
    public ControlTrack getControlTrack()
    {
        return isControlTrackEnabled ? super.getControlTrack() : null;
    }


    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    private void generateUpdate()
    {
        LOGGER.log(Level.FINE, "generateUpdate() --  nanoTime()={0}", System.nanoTime());
        if (!getState().equals(PlaybackSession.State.GENERATED))
        {
            LOGGER.log(Level.FINE, "generateUpdate() aborted because getState()={0}", getState());
            return;
        }

        // We should update but may not be allowed by user
        if (isUpdateControlEnabled() && !PlaybackSettings.getInstance().isAutoUpdateEnabled())
        {
            // Make sure a new session is used when playback restarts or resumes.
            // Note that if song is playing and user restores auto-update while playing, an update will be generated 
            // to be in-sync again, so this setDirty() should have been avoided, but it's a small price to pay.
            setDirty();
            return;
        }


        // Start our update handler thread if first time
        if (musicGenerationQueue == null)
        {
            musicGenerationQueue = new MusicGenerationQueue(getPreUpdateBufferTimeMs(), getPostUpdateSleepTimeMs());
            musicGenerationQueue.start();
            musicGenerationQueue.addChangeListener(e -> musicGenerationResultReceived(musicGenerationQueue.getLastResult()));
        }


        // Make a copy of the SongContext so it can't be changed by user anymore
        int transpose = isPlaybackTranspositionEnabled() ? PlaybackSettings.getInstance().getPlaybackKeyTransposition() : 0;
        SongContext workContext = getContextCopy(getSongContext(), transpose);


        // Notify our update handler thread
        try
        {
            musicGenerationQueue.add(workContext);
        } catch (Exception e)
        {
            // Should never be here
            LOGGER.log(Level.WARNING, "generateUpdate() unexpected updateRequestsHandler.getLastAddedSongContext()={0}",
                    musicGenerationQueue.getLastAddedSongContext());
            Exceptions.printStackTrace(e);
        }

    }


    /**
     * Check that the specified ChordLeadSheet barIndex is part of our context.
     *
     * @param clsBarIndex
     * @return
     */
    private boolean isClsBarIndexPartOfContext(int clsBarIndex)
    {
        SongContext sgContext = getSongContext();
        ChordLeadSheet cls = sgContext.getSong().getChordLeadSheet();
        CLI_Section section = cls.getSection(clsBarIndex);
        return sgContext.getSongParts().stream().anyMatch(spt -> spt.getParentSection() == section);
    }


    /**
     * Like setDirty() plus it stops further updates and disable the control track.
     * <p>
     * Fire an UpdateProvider.PROP_UPDATE_PROVISION_ENABLED change event.
     */
    private void disableUpdates()
    {
        if (isUpdateProvisionEnabled())
        {
            LOGGER.fine("disableUpdates() -- ");
            isUpdateProvisionEnabled = false;
            setDirty();
            disableControlTrack();
            firePropertyChange(PROP_UPDATE_PROVISION_ENABLED, true, false);
        }
    }

    /**
     * This notifies listeners that control track is now invalid and should not be relied upon.
     * <p>
     * Typically an editor will stop highlighting the current playing position.
     */
    private void disableControlTrack()
    {
        if (isControlTrackEnabled && isControlTrackIncluded())
        {
            isControlTrackEnabled = false;
            firePropertyChange(ControlTrackProvider.ENABLED_STATE, true, false);
        }
    }

    /**
     * Called when MusicGenerationQueue completed a task.
     * <p>
     * Prepare an UpdatableSongSession.Update.
     *
     * @param result
     */
    private void musicGenerationResultReceived(MusicGenerationQueue.Result result)
    {
        assert result != null;

        // Check for errors
        if (result.userException() != null)
        {
            if (result.userException() instanceof UserErrorGenerationException ue)
            {
                if (userErrorExceptionHandler != null)
                {
                    userErrorExceptionHandler.accept(ue);
                }
                setDirty();
            } else
            {
                // More serious error
                disableUpdates();
            }
            return;
        }

        // Create a new control track
        ControlTrack cTrack = null;

        if (isControlTrackEnabled && isControlTrackIncluded())
        {
            var sessionCtrack = getControlTrack();      // Might be null if session was closed in the meantime (usually we're NOT on the Swing EDT!)
            if (sessionCtrack != null)
            {
                cTrack = new ControlTrack(result.songContext(), getControlTrack().getTrackId());
            }
        }


        // Create the update
        update = new Update(result.mapRvPhrases(), cTrack);


        // Notify listeners, typically an UpdatableSongSession
        firePropertyChange(UpdatableSongSession.UpdateProvider.PROP_UPDATE_AVAILABLE, false, true);
    }

    /**
     * Find an identical existing session in state NEW or GENERATED and not dirty.
     *
     * @return Null if not found
     */
    static private UpdateProviderSongSession findSession(SongContext sgContext,
            boolean enablePlaybackTransposition, boolean includeClickTrack, boolean includePrecount, boolean includeControlTrack,
            boolean enableUpdateControl,
            int loopCount,
            ActionListener endOfPlaybackAction)
    {
        for (var session : sessions)
        {
            if ((session.getState().equals(PlaybackSession.State.GENERATED) || session.getState().equals(PlaybackSession.State.NEW))
                    && !session.isDirty()
                    && sgContext.equals(session.getSongContext())
                    && enablePlaybackTransposition == session.isPlaybackTranspositionEnabled()
                    && includeClickTrack == session.isClickTrackIncluded()
                    && includePrecount == session.isPrecountTrackIncluded()
                    && includeControlTrack == session.isControlTrackIncluded()
                    && enableUpdateControl == session.isUpdateControlEnabled()
                    && loopCount == session.loopCount // Do NOT use getLoopCount(), because of PLAYBACK_SETTINGS_LOOP_COUNT handling
                    && endOfPlaybackAction == session.getEndOfPlaybackAction())
            {
                return session;
            }
        }
        return null;
    }


    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================  
    /**
     * Debug method.
     *
     * @return
     */
    static private String toDebugString(ChordLeadSheet cls)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(cls);
        for (var item : cls.getItems())
        {
            if (item instanceof CLI_Section)
            {
                sb.append('\n').append(" ").append(item.getData()).append(item.getPosition()).append(" : ");
            } else
            {
                sb.append(item.getData()).append(item.getPosition()).append(" ");
            }
        }
        return sb.toString();
    }

}

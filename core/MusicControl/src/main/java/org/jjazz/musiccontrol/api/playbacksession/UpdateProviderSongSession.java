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

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.musiccontrol.api.ControlTrack;
import org.jjazz.musiccontrol.api.MusicGenerationQueue;
import org.jjazz.musiccontrol.api.MusicGenerationQueue.Result;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.SongMusicGenerationListener;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession.Update;
import static org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession.UpdateProvider.PROP_UPDATE_PROVISION_ENABLED;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongMetaEvents.ClsSourceActionEvent;
import org.jjazz.song.api.SongMetaEvents.SgsSourceActionEvent;
import org.jjazz.songcontext.api.SongContext;
import static org.jjazz.songstructure.api.event.SgsActionEvent.API_ID.AddSongParts;
import static org.jjazz.songstructure.api.event.SgsActionEvent.API_ID.RemoveSongParts;
import static org.jjazz.songstructure.api.event.SgsActionEvent.API_ID.ReplaceSongParts;
import static org.jjazz.songstructure.api.event.SgsActionEvent.API_ID.ResizeSongParts;
import static org.jjazz.songstructure.api.event.SgsActionEvent.API_ID.SetRhythmParameterValue;
import org.openide.awt.StatusDisplayer;
import org.openide.util.*;

/**
 * A session to be used as a BaseSongSession for an UpdatableSongSession.
 * <p>
 * The session provides on-the-fly UpdatableSongSession.Updates for:<br>
 * - chord symbol changes (add/remove/change/moveAll)<br>
 * - rhythm parameter value changes<br>
 * - existing user phrase content changes (but not for add/remove user phrase events)<br>
 * - MidiMix instrument transposition/velocity changes, plus drum keymap and drum rerouting changes<br>
 * <p>
 * If change can't be handled as an update (eg a song part tempo factor change or a click setting), session is marked dirty (ie needs regeneration). If session
 * is dirty, editors can still show the playback point using the control track but the "dirty" changes are not heard.
 * <p>
 * A more serious change like a structural change of the song context will make the session dirty, plus it disables the control track and any future update. So
 * editors should stop showing the playback point, and any further change will not be heard (until a new session is generated).
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
    private boolean isUpdateProvisionEnabled;
    private boolean isControlTrackEnabled;
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
     * Sessions are cached: if a non-dirty session in the NEW or GENERATED state already exists for the same parameters then return it, otherwise a new session
     * is created.
     * <p>
     *
     * @param sgContext
     * @param sConfig
     * @param enableUpdateControl If true updates are authorized depending on the PlaybackSettings AutoUpdateEnabled value.
     * @param context
     * @return A session in the NEW or GENERATED state.
     */
    static public UpdateProviderSongSession getSession(SongContext sgContext, SessionConfig sConfig, boolean enableUpdateControl, Context context)
    {
        Objects.requireNonNull(sgContext);
        Objects.requireNonNull(sConfig);
        Objects.requireNonNull(context);
        UpdateProviderSongSession session = findSession(sgContext, sConfig, enableUpdateControl, context);
        if (session == null)
        {
            final UpdateProviderSongSession newSession = new UpdateProviderSongSession(sgContext, sConfig, enableUpdateControl, context);

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
     * Get a session using the default config with update control enabled.
     * <p>
     *
     * @param sgContext
     * @param context
     * @return A targetSession in the NEW or GENERATED state.
     */
    static public UpdateProviderSongSession getSession(SongContext sgContext, Context context)
    {
        return getSession(sgContext, new SessionConfig(), true, context);
    }


    private UpdateProviderSongSession(SongContext sgContext, SessionConfig sConfig, boolean enableUpdateControl, Context context)
    {
        super(sgContext, sConfig, true, context);
        isControlTrackEnabled = true;
        isUpdateProvisionEnabled = true;
        isUpdateControlEnabled = enableUpdateControl;
        userErrorExceptionHandler = e -> StatusDisplayer.getDefault().setStatusText(e.getLocalizedMessage());
    }

    @Override
    public UpdateProviderSongSession getFreshCopy(SongContext sgContext)
    {
        var newContext = sgContext == null ? getSongContext().clone() : sgContext;
        UpdateProviderSongSession newSession = new UpdateProviderSongSession(newContext, getSessionConfig(), isUpdateControlEnabled, getContext());
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
        if (musicGenerationQueue != null)
        {
            musicGenerationQueue.stop();
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

        if (e.getSource() != songMusicGenerationListener || !e.getPropertyName().equals(SongMusicGenerationListener.PROP_MUSIC_GENERATION_COMBINED))
        {
            return;
        }


        //
        // If we're here it means that 
        // - song has changed in a way that impacts music generation
        // - state==GENERATED
        //
        boolean dirty = false;
        boolean doUpdate = false;
        boolean doDisableUpdates = false;


        // Analyze the PROP_MUSIC_GENERATION_COMBINED change event origin
        Object sourceEvent = e.getOldValue();
        Object data = e.getNewValue();


        LOGGER.log(Level.FINE, "propertyChange() -- sourceEvent={0} data={1}", new Object[]
        {
            sourceEvent, data
        });

        switch (sourceEvent)
        {
            case ClsSourceActionEvent csae ->
            {
                switch (csae.getApiId())
                {
                    case SetSizeInBars, AddSection, RemoveSection, MoveSection, InsertBars, DeleteBars, SetSectionTimeSignature ->
                    {
                        doDisableUpdates = true;
                    }
                    case AddItem, RemoveItem, MoveItem, ChangeItem ->
                    {
                        doUpdate = true;
                    }
                    case SetSectionName ->
                    {
                        // Nothing
                    }
                    default ->
                    {
                        throw new IllegalArgumentException("cae=" + csae);
                    }
                }
            }
            case SgsSourceActionEvent ssae ->
            {
                switch (ssae.getApiId())
                {
                    case AddSongParts, RemoveSongParts, ResizeSongParts, ReplaceSongParts ->
                    {
                        doDisableUpdates = true;
                    }
                    case SetRhythmParameterValue, SetRhythmParameterMutableValue ->
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
                    case setSongPartsName ->
                    {
                        // Nothing
                    }
                    default ->
                    {
                        throw new IllegalArgumentException("sae=" + ssae);
                    }
                }
            }
            case String s ->
            {
                switch (s)
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
                    case PlaybackSettings.PROP_PLAYBACK_CLICK_ENABLED ->
                    {
                        doUpdate = true;
                    }

                    //
                    // Song property events
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
                    case Song.PROP_VETOABLE_USER_PHRASE_CONTENT, Song.PROP_TEMPO ->
                    {
                        doUpdate = true;
                    }
                    default ->
                    {
                        throw new IllegalArgumentException("s=" + s);
                    }
                }
            }
            default ->
            {
                throw new IllegalArgumentException("sourceEvent=" + sourceEvent);
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


        // Notify our update handler thread
        try
        {
            musicGenerationQueue.add(getSongContext());
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
        if (isControlTrackEnabled && getSessionConfig().includeControlTrack())
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
    private void musicGenerationResultReceived(Result result)
    {
        assert result != null;

        // Check for errors
        if (result.throwable() != null)
        {
            if (result.throwable() instanceof UserErrorGenerationException ue)
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

        if (isControlTrackEnabled && getSessionConfig().includeControlTrack())
        {
            var sessionCtrlTrack = getControlTrack();      // Might be null if session was closed in the meantime (usually we're NOT on the Swing EDT!)
            if (sessionCtrlTrack != null)
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
     * @param sgContext
     * @param sConfig
     * @param enableUpdateControl
     * @param context
     * @return Null if not found
     */
    static private UpdateProviderSongSession findSession(SongContext sgContext, SessionConfig sConfig, boolean enableUpdateControl, Context context)
    {
        for (var session : sessions)
        {
            if ((session.getState().equals(PlaybackSession.State.GENERATED) || session.getState().equals(PlaybackSession.State.NEW))
                    && !session.isDirty()
                    && sgContext.equals(session.getSongContext())
                    && enableUpdateControl == session.isUpdateControlEnabled()
                    && sConfig.equals(session.getSessionConfig())
                    && context == session.getContext())
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

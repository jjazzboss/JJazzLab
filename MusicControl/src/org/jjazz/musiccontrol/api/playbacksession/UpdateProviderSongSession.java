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

import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemAddedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemBarShiftedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemMovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemRemovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.SectionMovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.ControlTrack;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession.Update;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.RpValueChangedEvent;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptRenamedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
import org.openide.awt.StatusDisplayer;
import org.openide.util.*;

/**
 * A session to be used as a BaseSongSession for an UpdatableSongSession.
 * <p>
 * The session provides on-the-fly UpdatableSongSession.Updates for:<br>
 * - chord symbol changes (add/remove/change/move)<br>
 * - rhythm parameter value changes<br>
 * - existing user phrase content changes (but not for add/remove user phrase events)<br>
 * - PlaybackSettings playback transposition changes<br>
 * - MidiMix instrument transposition/velocity changes, plus drum keymap and drum rerouting changes<br>
 * <p>
 * If change can't be handled as an update (eg a tempo change or a click setting), session is marked dirty (ie needs
 * regeneration). If session is dirty, editors can still show the playback point using the control track but the "dirty" changes
 * are not heard.
 * <p>
 * A more serious change like a structural change of the song context will make the session dirty, plus it disables the control
 * track and any future update. So editors should stop showing the playback point, and any further change will not be heard (until
 * a new session is generated).
 */
public class UpdateProviderSongSession extends BaseSongSession implements UpdatableSongSession.UpdateProvider, SgsChangeListener, ClsChangeListener, VetoableChangeListener
{

    /**
     * Property change event fired when updates become disabled (it's enabled by default)
     */
    public static final String PROP_UPDATES_ENABLED = "PropUpdatesEnabled";
    public static final int DEFAULT_PRE_UPDATE_BUFFER_TIME_MS = 300;
    public static final int DEFAULT_POST_UPDATE_SLEEP_TIME_MS = 700;
    private int preUpdateBufferTimeMs = DEFAULT_PRE_UPDATE_BUFFER_TIME_MS;
    private int postUpdateSleepTimeMs = DEFAULT_POST_UPDATE_SLEEP_TIME_MS;
    private Update update;
    private ClsSgsChange currentClsChange;
    private ClsSgsChange currentSgsChange;
    private boolean isUpdatable = true;
    private boolean isControlTrackEnabled = true;
    private final boolean isUpdateControlEnabled;
    private MusicGenerationQueue musicGenerationQueue;
    private Consumer<UserErrorGenerationException> userErrorExceptionHandler;
    private static final List<UpdateProviderSongSession> sessions = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(UpdateProviderSongSession.class.getSimpleName());  


    /**
     * Create or reuse a session for the specified parameters.
     * <p>
     * <p>
     * Sessions are cached: if a non-dirty session in the NEW or GENERATED state already exists for the same parameters then
     * return it, otherwise a new session is created.
     * <p>
     *
     * @param sgContext
     * @param enablePlaybackTransposition If true apply the playback transposition
     * @param includeClickTrack           If true add the click track, and its muted/unmuted state will depend on the
     *                                    PlaybackSettings
     * @param includePrecountTrack        If true add the precount track, and loopStartTick will depend on the PlaybackSettings
     * @param includeControlTrack         if true add a control track (beat positions + chord symbol markers)
     * @param enableUpdateControl         If true updates are authorized depending on the PlaybackSettings AutoUpdateEnabled
     *                                    value.
     * @param loopCount                   See Sequencer.setLoopCount(). Use PLAYBACK_SETTINGS_LOOP_COUNT to rely on the
     *                                    PlaybackSettings instance value.
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
            return newSession;
        } else
        {
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
    public UpdateProviderSongSession getFreshCopy()
    {
        UpdateProviderSongSession newSession = new UpdateProviderSongSession(getSongContext().clone(),
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
        song.addVetoableChangeListener(this);       // user phrase add/remove/replaced
        song.getChordLeadSheet().addClsChangeListener(this);
        song.getSongStructure().addSgsChangeListener(this);
        for (var name : song.getUserPhraseNames())
        {
            song.getUserPhrase(name).addPropertyChangeListener(this);       // user phrase content changed
        }
    }

    @Override
    public void close()
    {
        super.close();

        var song = getSongContext().getSong();
        song.removeVetoableChangeListener(this);
        song.getChordLeadSheet().removeClsChangeListener(this);
        song.getSongStructure().removeSgsChangeListener(this);
        for (var name : song.getUserPhraseNames())
        {
            song.getUserPhrase(name).removePropertyChangeListener(this);
        }       
        sessions.remove(this);
    }

    /**
     * Check if session still listens to song changes and generate updates.
     *
     * @return
     */
    public boolean isUpdatable()
    {
        return isUpdatable;
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
     *
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
     *
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

        LOGGER.log(Level.FINE, "propertyChange() -- e={0}", e);

        if (getState().equals(State.CLOSED) || !isUpdatable())
        {
            return;
        }
        // If here it means that state==GENERATED


        // LOGGER.fine("propertyChange() e=" + e);
        boolean dirty = false;
        boolean doUpdate = false;
        boolean doDisableUpdates = false;


        if (e.getSource() == getSongContext().getMidiMix())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_INSTRUMENT_MUTE ->
                {
                    // Nothing
                }    
                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED, MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP, MidiMix.PROP_INSTRUMENT_TRANSPOSITION, MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT ->
                    doUpdate = true;
                default ->
                {
                    // PROP_CHANNEL_INSTRUMENT_MIX, PROP_RHYTHM_VOICE, PROP_RHYTHM_VOICE_CHANNEL, ...
                    doDisableUpdates = true;
                }
            }

        } else if (e.getSource() == PlaybackSettings.getInstance())
        {
            switch (e.getPropertyName())
            {
                case PlaybackSettings.PROP_PLAYBACK_KEY_TRANSPOSITION ->
                    doUpdate = true;

                case PlaybackSettings.PROP_CLICK_PITCH_HIGH, PlaybackSettings.PROP_CLICK_PITCH_LOW, PlaybackSettings.PROP_CLICK_PREFERRED_CHANNEL, PlaybackSettings.PROP_CLICK_VELOCITY_HIGH, PlaybackSettings.PROP_CLICK_VELOCITY_LOW, PlaybackSettings.PROP_CLICK_PRECOUNT_MODE, PlaybackSettings.PROP_CLICK_PRECOUNT_ENABLED ->
                    dirty = true;

                case PlaybackSettings.PROP_AUTO_UPDATE_ENABLED ->
                {
                    if (PlaybackSettings.getInstance().isAutoUpdateEnabled())
                    {
                        // Auto-Update switched to ON: try to update to be up-to-date again
                        doUpdate = true;
                    }
                }
                default ->
                {
                }
            }

        } else if (e.getSource() == getSongContext().getSong())
        {
            switch (e.getPropertyName())
            {
                case Song.PROP_VETOABLE_USER_PHRASE ->
                {
                    // A user phrase was added or removed, can't handle it
                    doDisableUpdates = true;
                    if (e.getNewValue() instanceof Phrase p)
                    {
                        // It's a removed phrase, unlisten
                        p.removePropertyChangeListener(this);
                    }
                }
                case Song.PROP_VETOABLE_USER_PHRASE_CONTENT ->
                {
                    // It's an update, we can handle it
                    doUpdate = true;

                    Phrase oldPhrase = (Phrase) e.getOldValue();
                    Phrase newPhrase = getSongContext().getSong().getUserPhrase((String) e.getNewValue());
                    assert newPhrase != null : "e=" + e;
                    oldPhrase.removePropertyChangeListener(this);
                    newPhrase.addPropertyChangeListener(this);
                }
                default ->
                {
                    // PROP_VETOABLE_PHRASE_NAME
                    // Nothing
                }
            }
        } else if (e.getSource() instanceof Phrase && !Phrase.isAdjustingEvent(e.getPropertyName()))
        {
            // User phrase is modified 
            doUpdate = true;
        }

        LOGGER.log(Level.FINE, "propertyChange() output: dirty={0} doUpdate={1} doDisableUpdates={2}", new Object[]
        {
            dirty, doUpdate, doDisableUpdates
        });


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
    // VetoableChangeListener interface
    // ==========================================================================================================
    @Override
    public void vetoableChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "vetoableChange() -- e={0}", e);
        propertyChange(e);
    }


    // ==========================================================================================================
    // UpdatableSongSession.UpdateProvider interface
    // ==========================================================================================================
    @Override
    public Update getUpdate()
    {
        return update;
    }

    // ==========================================================================================================
    // ClsChangeListener interface
    // ==========================================================================================================
    @Override
    public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        // NOTE: model changes can be generated outside the EDT!

        if (!getState().equals(PlaybackSession.State.GENERATED) || !isUpdatable())
        {
            return;
        }

        LOGGER.log(Level.FINE, "chordLeadSheetChanged()  -- event={0} nanoTime()={1}", new Object[]
        {
            event, System.nanoTime()
        });

        boolean disableUpdates = false;

        if (event instanceof ClsActionEvent)
        {
            var e = (ClsActionEvent) event;
            if (e.isActionStarted())
            {
                // New ClsActionEvent, save it
                LOGGER.log(Level.FINE, "chordLeadSheetChanged()  NEW ActionEvent({0})", e);
                assert currentClsChange == null : "currentClsChange=" + currentClsChange + " e=" + e;
                currentClsChange = new ClsSgsChange(e.getActionId());

            } else
            {
                // ClsActionEvent complete, check the status and update
                assert currentClsChange != null && e.getActionId().equals(currentClsChange.actionId) : "currentClsChange=" + currentClsChange + " e=" + e;
                LOGGER.log(Level.FINE, "chordLeadSheetChanged()  ActionEvent({0}) complete, doUpdate={1}", new Object[]
                {
                    currentClsChange.actionId, currentClsChange.doUpdate
                });
                if (currentClsChange.doUpdate)
                {
                    generateUpdate();
                }
                currentClsChange = null;
            }

        } else if (event instanceof SizeChangedEvent)
        {
            disableUpdates = true;

        } else if ((event instanceof ItemAddedEvent)
                || (event instanceof ItemRemovedEvent))
        {
            var contextItems = event.getItems().stream()
                    .filter(cli -> isClsBarIndexPartOfContext(cli.getPosition().getBar()))
                    .toList();
            disableUpdates = contextItems.stream().anyMatch(cli -> !(cli instanceof CLI_ChordSymbol));
            assert currentClsChange != null : "event=" + event;
            currentClsChange.doUpdate = contextItems.stream().allMatch(cli -> cli instanceof CLI_ChordSymbol);

        } else if (event instanceof ItemBarShiftedEvent)
        {
            // Bars were inserted/deleted
            disableUpdates = true;

        } else if (event instanceof ItemChangedEvent)
        {
            ItemChangedEvent e = (ItemChangedEvent) event;
            var item = e.getItem();
            if (isClsBarIndexPartOfContext(item.getPosition().getBar()))
            {
                if (item instanceof CLI_Section)
                {
                    Section newSection = (Section) e.getNewData();
                    Section oldSection = (Section) e.getOldData();
                    if (!newSection.getTimeSignature().equals(oldSection.getTimeSignature()))
                    {
                        disableUpdates = true;
                    }
                } else if (item instanceof CLI_ChordSymbol)
                {
                    assert currentClsChange != null : "event=" + event;
                    currentClsChange.doUpdate = true;
                }
            }

        } else if (event instanceof ItemMovedEvent)
        {
            ItemMovedEvent e = (ItemMovedEvent) event;
            if (isClsBarIndexPartOfContext(e.getNewPosition().getBar()) || isClsBarIndexPartOfContext(e.getOldPosition().getBar()))
            {
                assert currentClsChange != null : "event=" + event;
                currentClsChange.doUpdate = true;
            }

        } else if (event instanceof SectionMovedEvent)
        {
            disableUpdates = true;

        }

        LOGGER.log(Level.FINE, "chordLeadSheetChanged()  => disableUpdates={0} currentClsChange={1}", new Object[]
        {
            disableUpdates, currentClsChange
        });

        if (disableUpdates)
        {
            currentClsChange = null;
            disableUpdates();
        }


    }
    // ==========================================================================================================
    // SgsChangeListener interface
    // ==========================================================================================================

    @Override

    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void songStructureChanged(SgsChangeEvent event)
    {
        // NOTE: model changes can be generated outside the EDT!        

        if (!getState().equals(PlaybackSession.State.GENERATED) || !isUpdatable())
        {
            return;
        }

        LOGGER.log(Level.FINE, "songStructureChanged()  -- event={0}", event);


        boolean disableUpdates = false;
        boolean dirty = false;

        
        // Context song parts (at the time of the SongContext object creation)
        List<SongPart> contextSongParts = getSongContext().getSongParts();


        if (event instanceof SgsActionEvent)
        {
            var e = (SgsActionEvent) event;
            if (e.isActionStarted())
            {
                // New SgsActionEvent, save it
                LOGGER.log(Level.FINE, "songStructureChanged()  NEW ActionEvent({0})", e);
                assert currentSgsChange == null : "currentSgsChange=" + currentSgsChange + " e=" + e;
                currentSgsChange = new ClsSgsChange(e.getActionId());

            } else
            {
                // ClsActionEvent complete, check the status and update
                assert currentSgsChange != null && e.getActionId().equals(currentSgsChange.actionId) : "currentSgsChange=" + currentSgsChange + " e=" + e;
                LOGGER.log(Level.FINE, "songStructureChanged()  COMPLETED ActionEvent({0}), doUpdate={1}", new Object[]
                {
                    currentSgsChange.actionId, currentSgsChange.doUpdate
                });
                if (currentSgsChange.doUpdate)
                {
                    generateUpdate();
                }
                currentSgsChange = null;
            }

        } else if ((event instanceof SptRemovedEvent)
                || (event instanceof SptAddedEvent))
        {
            // Ok only if removed spt is after our context
            disableUpdates = event.getSongParts().stream()
                    .anyMatch(spt -> spt.getStartBarIndex() <= getSongContext().getBarRange().to);

        } else if (event instanceof SptReplacedEvent re)
        {
            // Ok if replaced spt is not in the context
            disableUpdates = re.getSongParts().stream()
                    .anyMatch(spt -> contextSongParts.contains(spt));

        } else if (event instanceof SptResizedEvent re)
        {
            // Ok if replaced spt is not in the context
            disableUpdates = re.getMapOldSptSize().getKeys().stream()
                    .anyMatch(spt -> contextSongParts.contains(spt));

        } else if (event instanceof SptRenamedEvent)
        {
            // Nothing
        } else if (event instanceof RpValueChangedEvent re)
        {
            assert currentSgsChange != null : "event=" + event;
            if (re.getRhythmParameter() instanceof RP_SYS_TempoFactor)
            {
                // Can't update this in realtime, would need to update track0, not easy
                dirty = true;
            } else
            {
                // Update if updated RP is for a context SongPart
                currentSgsChange.doUpdate = contextSongParts.contains(event.getSongPart());
            }

        }

        LOGGER.log(Level.FINE, "songStructureChanged()  => disableUpdates={0}", disableUpdates);


        if (disableUpdates)
        {
            disableUpdates();
        } else if (dirty)
        {
            setDirty();
        }

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
        if (!getState().equals(State.GENERATED))
        {
            LOGGER.log(Level.FINE, "generateUpdate() aborted because getState()=" + getState());
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
            LOGGER.warning("generateUpdate() unexpected updateRequestsHandler.getQueue().size()=" + musicGenerationQueue.getQueueSize());
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
     */
    private void disableUpdates()
    {
        if (isUpdatable())
        {
            LOGGER.fine("disableUpdates() -- ");
            isUpdatable = false;
            setDirty();
            disableControlTrack();
            firePropertyChange(PROP_UPDATES_ENABLED, true, false);
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
        if (isControlTrackEnabled && isControlTrackIncluded())  // Normaly useless since control track is disabled when updates are disabled
        {
            cTrack = new ControlTrack(result.songContext(), getControlTrack().getTrackId());
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
    static private class ClsSgsChange
    {

        boolean doUpdate;
        String actionId;

        private ClsSgsChange(String actionId)
        {
            this.actionId = actionId;
        }

        @Override
        public String toString()
        {
            return "<actionId=" + actionId + ", doUpdate=" + doUpdate + ">";
        }
    }


    /**
     * Debug method.
     *
     * @return
     */
    static private String toDebugString(ChordLeadSheet cls)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(cls);
        for (ChordLeadSheetItem<?> item : cls.getItems())
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

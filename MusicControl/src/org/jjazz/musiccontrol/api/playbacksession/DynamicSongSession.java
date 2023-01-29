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
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.RpChangedEvent;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptRenamedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
import org.jjazz.util.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.util.*;

/**
 * This SongSession listens to the context changes to provide on-the-fly updates for an UpdatableSongSession.
 * <p>
 * On-the-fly updates are provided for :<br>
 * - chord symbol changes (add/remove/change/move)<br>
 * - rhythm parameter value changes<br>
 * - PlaybackSettings playback transposition changes<br>
 * - MidiMix instrument transposition/velocity changes, plus drum keymap and drum rerouting changes<br>
 * <p>
 * If change can't be handled as an on-the-fly update, session is marked dirty. Song structural changes make the session dirty and
 * prevent any future update. Updates generation are blocked if PlaybackSettings.isAutoUpdateEnabled() is OFF.
 * <p>
 */
public class DynamicSongSession extends BaseSongSession implements UpdatableSongSession.UpdateProvider, SgsChangeListener, ClsChangeListener, VetoableChangeListener
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
    private Consumer<UserErrorGenerationException> userErrorExceptionHandler;
    private boolean isUpdatable = true;
    private boolean isControlTrackEnabled = true;
    private final boolean isUpdateControlEnabled;
    private UpdateRequestsHandler updateRequestsHandler;
    private static final List<DynamicSongSession> sessions = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(DynamicSongSession.class.getSimpleName());  //NOI18N


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
    static public DynamicSongSession getSession(SongContext sgContext,
            boolean enablePlaybackTransposition, boolean includeClickTrack, boolean includePrecountTrack, boolean includeControlTrack,
            boolean enableUpdateControl,
            int loopCount,
            ActionListener endOfPlaybackAction)
    {
        if (sgContext == null)
        {
            throw new IllegalArgumentException("sgContext=" + sgContext);
        }
        DynamicSongSession session = findSession(sgContext,
                enablePlaybackTransposition, includeClickTrack, includePrecountTrack, includeControlTrack,
                enableUpdateControl,
                loopCount,
                endOfPlaybackAction);
        if (session == null)
        {
            final DynamicSongSession newSession = new DynamicSongSession(sgContext,
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
    static public DynamicSongSession getSession(SongContext sgContext)
    {
        return getSession(sgContext, true, true, true, true, true, PLAYBACK_SETTINGS_LOOP_COUNT, null);
    }

    private DynamicSongSession(SongContext sgContext,
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

        // Our default handler        
        userErrorExceptionHandler = e -> StatusDisplayer.getDefault().setStatusText(e.getLocalizedMessage());
    }

    @Override
    public DynamicSongSession getFreshCopy()
    {
        DynamicSongSession newSession = new DynamicSongSession(getSongContext().clone(),
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

    @Override
    public void generate(boolean silent) throws MusicGenerationException
    {
        super.generate(silent);

        getSongContext().getSong().addVetoableChangeListener(this);
        getSongContext().getSong().getChordLeadSheet().addClsChangeListener(this);
        getSongContext().getSong().getSongStructure().addSgsChangeListener(this);
    }

    @Override
    public void close()
    {
        super.close();

        getSongContext().getSong().removeVetoableChangeListener(this);
        getSongContext().getSong().getChordLeadSheet().removeClsChangeListener(this);
        getSongContext().getSong().getSongStructure().removeSgsChangeListener(this);
        sessions.remove(this);
    }

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
     * Wait this time from the first received change before triggering an update.
     * <p>
     * This helps filtering out meaningless changes when a user action generates several changes (e.g. mouse drag).
     *
     * @return
     */
    public int getPreUpdateBufferTimeMs()
    {
        return preUpdateBufferTimeMs;
    }

    /**
     * @param preUpdateBufferTimeMs
     * @see DynamicSongSession#getPreUpdateBufferTimeMs()
     */
    public void setPreUpdateBufferTimeMs(int preUpdateBufferTimeMs)
    {
        this.preUpdateBufferTimeMs = preUpdateBufferTimeMs;
    }

    /**
     * The minimum delay between 2 consecutive updates.
     * <p>
     * This avoids too many sequencer changes in a short period of time, which can cause audio issues with notes muted/unmuted too
     * many times.
     * <p>
     * @return
     */
    public int getPostUpdateSleepTimeMs()
    {
        return postUpdateSleepTimeMs;
    }

    /**
     * @param postUpdateSleepTimeMs
     * @see DynamicSongSession#getPostUpdateSleepTimeMs()
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
     * @param handler Can be null
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
        // If here state=GENERATED


        // LOGGER.fine("propertyChange() e=" + e);
        boolean dirty = false;
        boolean doUpdate = false;
        boolean doDisableUpdates = false;


        if (e.getSource() == getSongContext().getMidiMix())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX:
                    // An instrument mix was added or removed (it can be the user channel)
                    // If it's the user channel there is no impact, and if it's an added/removed rhythm we'll get the change directly via our SgsChangeListener.
                    // But if it's user who directly changed track channels, we should disable updates by security.
                    doDisableUpdates = true;
                    break;

                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED:
                case MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP:
                case MidiMix.PROP_INSTRUMENT_TRANSPOSITION:
                case MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT:
                    doUpdate = true;
                    break;

                default:    // e.g.  case MidiMix.PROP_INSTRUMENT_MUTE:
                    // Nothing
                    break;
            }

        } else if (e.getSource() == PlaybackSettings.getInstance())
        {
            switch (e.getPropertyName())
            {
                case PlaybackSettings.PROP_PLAYBACK_KEY_TRANSPOSITION:
                    doUpdate = true;
                    break;

                case PlaybackSettings.PROP_CLICK_PITCH_HIGH:
                case PlaybackSettings.PROP_CLICK_PITCH_LOW:
                case PlaybackSettings.PROP_CLICK_PREFERRED_CHANNEL:
                case PlaybackSettings.PROP_CLICK_VELOCITY_HIGH:
                case PlaybackSettings.PROP_CLICK_VELOCITY_LOW:
                case PlaybackSettings.PROP_CLICK_PRECOUNT_MODE:
                case PlaybackSettings.PROP_CLICK_PRECOUNT_ENABLED:
                    dirty = true;
                    break;

                case PlaybackSettings.PROP_AUTO_UPDATE_ENABLED:
                    if (PlaybackSettings.getInstance().isAutoUpdateEnabled())
                    {
                        // Auto-Update switched to ON: try to update to be up-to-date again
                        doUpdate = true;
                    }
                    break;
                    
                default:   // PROP_VETO_PRE_PLAYBACK, PROP_LOOPCOUNT, PROP_PLAYBACK_CLICK_ENABLED
                    // Do nothing
                    break;
            }
        } else if (e.getSource() == getSongContext().getSong())
        {
            switch (e.getPropertyName())
            {
                case Song.PROP_VETOABLE_USER_PHRASE:
                    // A user phrase was added or removed, can't handle it
                    doDisableUpdates = true;
                    break;

                case Song.PROP_VETOABLE_USER_PHRASE_CONTENT:
                    // It's an update, we can handle it
                    doUpdate = true;
                    break;
            }
        }

        LOGGER.log(Level.FINE, "propertyChange() output: dirty=" + dirty + " doUpdate=" + doUpdate + " doDisableUpdates=" + doDisableUpdates);


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
        } else if (event instanceof RpChangedEvent re)
        {
            assert currentSgsChange != null : "event=" + event;
            if (re.getRhytmParameter() instanceof RP_SYS_TempoFactor)
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
        if (updateRequestsHandler == null)
        {
            updateRequestsHandler = new UpdateRequestsHandler(getPreUpdateBufferTimeMs(), getPostUpdateSleepTimeMs());
            updateRequestsHandler.start();
        }


        // Make a copy of the SongContext so it can't be changed by user anymore
        int transpose = isPlaybackTranspositionEnabled() ? PlaybackSettings.getInstance().getPlaybackKeyTransposition() : 0;
        SongContext workContext = getContextCopy(getSongContext(), transpose);


        // Notify our update handler thread
        try
        {
            updateRequestsHandler.getQueue().add(workContext);
        } catch (Exception e)
        {
            // Should never be here
            LOGGER.warning("generateUpdate() unexpected updateRequestsHandler.getQueue().size()=" + updateRequestsHandler.getQueue().size());
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

    private void disableControlTrack()
    {
        if (isControlTrackEnabled && isControlTrackIncluded())
        {
            isControlTrackEnabled = false;
            firePropertyChange(ControlTrackProvider.ENABLED_STATE, true, false);
        }
    }

    /**
     * Find an identical existing session in state NEW or GENERATED and not dirty.
     *
     * @return Null if not found
     */
    static private DynamicSongSession findSession(SongContext sgContext,
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
     * A thread to handle incoming update requests and start one music generation task at a time.
     * <p>
     * A user action can trigger several consecutive update requests in a short period of time, so buffer them to update only with
     * the last one.
     * <p>
     */
    private class UpdateRequestsHandler implements Runnable
    {

        private final Queue<SongContext> queue = new ConcurrentLinkedQueue<>();
        private ExecutorService executorService;
        private ScheduledExecutorService generationExecutorService;
        private Future<?> generationFuture;
        private UpdateGenerationTask generationTask;
        private SongContext pendingSongContext;
        private final int preUpdateBufferTimeMs;
        private final int postUpdateSleepTimeMs;
        private volatile boolean running;

        /**
         * Create the handler.
         *
         * @param preUpdateBufferTimeMs (milliseconds) Wait this time upon receiving the first request before starting the music
         *                              generation
         * @param postUpdateSleepTimeMs (milliseconds) Wait this time before restarting a music generation
         */
        public UpdateRequestsHandler(int preUpdateBufferTimeMs, int postUpdateSleepTimeMs)
        {
            this.preUpdateBufferTimeMs = preUpdateBufferTimeMs;
            this.postUpdateSleepTimeMs = postUpdateSleepTimeMs;
        }

        public Queue<SongContext> getQueue()
        {
            return queue;
        }

        public boolean isRunning()
        {
            return running;
        }

        public void start()
        {
            if (!running)
            {
                running = true;
                executorService = Executors.newSingleThreadExecutor();
                executorService.submit(this);
                generationExecutorService = Executors.newScheduledThreadPool(1);
            }
        }

        public void stop()
        {
            if (running)
            {
                running = false;
                Utilities.shutdownAndAwaitTermination(generationExecutorService, 1000, 100);
                Utilities.shutdownAndAwaitTermination(executorService, 1, 1);
            }
        }

        @Override
        public void run()
        {
            while (running)
            {
                SongContext incoming = queue.poll();           // Does not block if empty

                if (incoming != null)
                {
                    LOGGER.log(Level.FINE, "UpdateRequestsHandler.run() handling incoming={0} nanoTime()={1}", new Object[]
                    {
                        incoming, System.nanoTime()
                    });
                    // LOGGER.info("UpdateRequestsHandler.run() handling cls=" + toDebugString(incoming.getSong().getChordLeadSheet()));

                    // Handle new context, save as pending if handling failed
                    pendingSongContext = handleContext(incoming) ? null : incoming;

                } else if (pendingSongContext != null)
                {
                    // Handle the last pending context, reset it if handling was successful
                    if (handleContext(pendingSongContext))
                    {
                        LOGGER.log(Level.FINE, "UpdateRequestsHandler.run() handled pendingSongContext={0}", pendingSongContext);
                        pendingSongContext = null;
                    }
                }

                // Check every millisecond
                try
                {
                    Thread.sleep(1);
                } catch (InterruptedException ex)
                {
                    return;
                }
            }
        }


        /**
         * Try to start a new task or update existing task if possible.
         * <p>
         * If not possible, sgContext becomes the pendingContext.
         *
         * @param sgContext
         * @return True if task could be started or updated with sgContext, false otherwise
         */
        private boolean handleContext(SongContext sgContext)
        {
            boolean b;
            if (generationFuture == null)
            {
                // No generation task created yet, start one
                LOGGER.fine("handleContext() start generation FIRST TIME");
                startGenerationTask(sgContext);
                b = true;

            } else if (generationFuture.isDone())
            {
                // There is a generation task but it is complete, restart one
                LOGGER.fine("handleContext() start generation");
                startGenerationTask(sgContext);
                b = true;

            } else
            {
                // There is a generation task but not started yet (wait preUpdateBufferTimeMs), try to update it
                if (generationTask.changeContext(sgContext))
                {
                    LOGGER.fine("handleContext() changed context of current generation task");
                    // OK, task was waiting, we're done
                    b = true;

                } else
                {
                    // NOK, task is generating music
                    b = false;
                }
            }

            return b;
        }


        /**
         * Start a generation task after a fixed delay.
         *
         * @param sgContext
         */
        private void startGenerationTask(SongContext sgContext)
        {
            try
            {
                generationTask = new UpdateGenerationTask(sgContext, postUpdateSleepTimeMs);
                generationFuture = generationExecutorService.schedule(generationTask, preUpdateBufferTimeMs, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException ex)
            {
                // Task is being shutdown 
                generationFuture = null;
                generationTask = null;
            }
        }

    }

    /**
     * A task which creates the update and sleeps postUpdateSleepTime before notifying that the update is ready.
     */
    private class UpdateGenerationTask implements Runnable
    {

        private boolean started = false;
        private SongContext songContext;
        private final int postUpdateSleepTime;

        /**
         * Create an UpdateGenerator task for the given SongContext.
         * <p>
         *
         * @param sgContext           This must be an immutable instance (e.g. song must not be modified in parallel)
         * @param postUpdateSleepTime This delay avoids to have too many sequencer changes in a short period of time, which can
         *                            cause audio issues with notes muted/unmuted too many times.
         */
        UpdateGenerationTask(SongContext sgContext, int postUpdateSleepTime)
        {
            this.songContext = sgContext;
            this.postUpdateSleepTime = postUpdateSleepTime;
        }

        /**
         * Change the context for which to generate the update
         * <p>
         * Once the task has started (run() was called) the context can't be changed anymore.
         *
         * @param sgContext This must be an immutable instance (e.g. song must not be modified in parallel)
         * @return True if context could be changed (task is not started yet)
         */
        synchronized boolean changeContext(SongContext sgContext)
        {
            if (!started)
            {
                this.songContext = sgContext;
                return true;
            }
            return false;
        }


        @Override
        public void run()
        {
            synchronized (this)
            {
                started = true;
            }


            LOGGER.log(Level.FINE, "UpdateGenerationTask.run() >>> STARTING generation nanoTime()={0}", System.nanoTime());
            // LOGGER.info("UpdateGenerationTask.run() >>> STARTING generation cls=" + toDebugString(songContext.getSong().getChordLeadSheet()));


            // Recompute the RhythmVoice phrases
            Map<RhythmVoice, Phrase> mapRvPhrases;
            SongSequenceBuilder sgBuilder = new SongSequenceBuilder(songContext);
            try
            {
                mapRvPhrases = sgBuilder.buildMapRvPhrase(true);
            } catch (UserErrorGenerationException ex)
            {
                // Pass user error to handler if one is registered
                LOGGER.warning(ex.getMessage());
                if (userErrorExceptionHandler != null)
                {
                    userErrorExceptionHandler.accept(ex);
                }

                // Make sure that a regeneration will be done on next start
                setDirty();
                return;
            } catch (MusicGenerationException ex)
            {
                // This is not normal (e.g. rhythm generation failure), notify user
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                return;
            } catch (Exception e)           // To make sure we catch other programming exceptions, sometimes not seen because in thread
            {
                e.printStackTrace();
                return;
            }


            // Create a new control track
            ControlTrack cTrack = null;
            if (isControlTrackEnabled && isControlTrackIncluded())  // Normaly useless since control track is disabled when updates are disabled
            {
                cTrack = new ControlTrack(songContext, getControlTrack().getTrackId());
            }


            // Create the update
            update = new Update(mapRvPhrases, cTrack);


            // Notify listeners, normally an UpdatableSongSession
            firePropertyChange(UpdatableSongSession.UpdateProvider.PROP_UPDATE_AVAILABLE, false, true);


            try
            {
                Thread.sleep(postUpdateSleepTime);
            } catch (InterruptedException ex)
            {
                LOGGER.warning("UpdateGenerator.run() Unexpected UpdateGenerator thread.sleep interruption ex=" + ex.getMessage());
                return;
            }


            LOGGER.log(Level.FINE, "UpdateGenerationTask.run() <<< ENDING generation  nanoTime={0}", System.nanoTime());

        }


    }

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

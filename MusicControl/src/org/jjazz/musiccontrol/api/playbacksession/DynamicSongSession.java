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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
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
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.ContextChordSequence;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.RpChangedEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptRenamedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
import org.openide.util.Exceptions;

/**
 * This SongSession listens to the context changes to provide on-the-fly updates for an UpdatableSongSession.
 * <p>
 * On-the-fly updates are provided for :<br>
 * - chord symbol changes (add/remove/changed/move)<br>
 * - rhythm parameter value changes<br>
 * - PlaybackSettings playback transposition changes<br>
 * - MidiMix instrument transposition/velocity changes, plus drum keymap changes<br>
 * <p>
 * If change can't be handled as an on-the-fly update, session is marked dirty. Song structural changes make the session dirty and
 * prevent any future update.
 *
 * @todo Check drumsrerouting changes impact on updates
 * @todo RP Tempo factor => need update of track0 SongSequenceBuilder buildSequence
 */
public class DynamicSongSession extends BaseSongSession implements UpdatableSongSession.UpdateProvider, SgsChangeListener, ClsChangeListener
{

    private Map<RhythmVoice, Phrase> mapRvPhrases;
    private boolean isUpdatable = true;
    private boolean isControlTrackEnabled = true;
    private static final List<DynamicSongSession> sessions = new ArrayList<>();
    private static final ClosedSessionsListener CLOSED_SESSIONS_LISTENER = new ClosedSessionsListener();
    private static final Logger LOGGER = Logger.getLogger(DynamicSongSession.class.getSimpleName());  //NOI18N


    /**
     * Create or reuse a session for the specified parameters.
     * <p>
     * <p>
     * Sessions are cached: if an existing session in the NEW or GENERATED state already exists for the same parameters then
     * return it, otherwise a new session is created.
     * <p>
     *
     * @param sgContext
     * @param enablePlaybackTransposition If true apply the playback transposition
     * @param enableClickTrack If true add the click track, and its muted/unmuted state will depend on the PlaybackSettings
     * @param enablePrecountTrack If true add the precount track, and loopStartTick will depend on the PlaybackSettings
     * @param enableControlTrack if true add a control track (beat positions + chord symbol markers)
     * @param loopCount See Sequencer.setLoopCount(). Use PLAYBACK_SETTINGS_LOOP_COUNT to rely on the PlaybackSettings instance
     * value.
     * @param endOfPlaybackAction Action executed when playback is stopped. Can be null.
     * @return A session in the NEW or GENERATED state.
     */
    static public DynamicSongSession getSession(SongContext sgContext,
            boolean enablePlaybackTransposition, boolean enableClickTrack, boolean enablePrecountTrack, boolean enableControlTrack,
            int loopCount,
            ActionListener endOfPlaybackAction)
    {
        if (sgContext == null)
        {
            throw new IllegalArgumentException("sgContext=" + sgContext);
        }
        DynamicSongSession session = findSession(sgContext,
                enablePlaybackTransposition, enableClickTrack, enablePrecountTrack, enableControlTrack,
                loopCount,
                endOfPlaybackAction);
        if (session == null)
        {
            final DynamicSongSession newSession = new DynamicSongSession(sgContext,
                    enablePlaybackTransposition, enableClickTrack, enablePrecountTrack, enableControlTrack,
                    loopCount,
                    endOfPlaybackAction);

            newSession.addPropertyChangeListener(CLOSED_SESSIONS_LISTENER);
            sessions.add(newSession);
            return newSession;
        } else
        {
            return session;
        }
    }

    /**
     * Same as getSession(sgContext, true, true, true, true, PLAYBACK_SETTINGS_LOOP_COUNT, null);
     * <p>
     *
     * @param sgContext
     * @return A targetSession in the NEW or GENERATED state.
     */
    static public DynamicSongSession getSession(SongContext sgContext)
    {
        return getSession(sgContext, true, true, true, true, PLAYBACK_SETTINGS_LOOP_COUNT, null);
    }

    private DynamicSongSession(SongContext sgContext, boolean enablePlaybackTransposition, boolean enableClickTrack, boolean enablePrecountTrack, boolean enableControlTrack, int loopCount, ActionListener endOfPlaybackAction)
    {
        super(sgContext, enablePlaybackTransposition, enableClickTrack, enablePrecountTrack, enableControlTrack, loopCount, endOfPlaybackAction);


        // Listen to detailed changes
        sgContext.getSong().getChordLeadSheet().addClsChangeListener(this);
        sgContext.getSong().getSongStructure().addSgsChangeListener(this);
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        getSongContext().getSong().getChordLeadSheet().removeClsChangeListener(this);
        getSongContext().getSong().getSongStructure().removeSgsChangeListener(this);
    }

    // ==========================================================================================================
    // PropertyChangeListener interface
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        super.propertyChange(e);            // Important


        if (!getState().equals(PlaybackSession.State.GENERATED))
        {
            return;
        }

        // LOGGER.fine("propertyChange() e=" + e);

        boolean dirty = false;
        boolean update = false;


        if (e.getSource() == getSongContext().getMidiMix())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX:
                    // An instrument mix was added or removed (it can be the user channel)
                    // In both cases we don't care: if it's the user channel there is no impact, and if it's an added/removed rhythm 
                    // we'll get the change directly via our SgsChangeListener
                    break;

                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED:
                    // An instrument mix is rerouted on/off
                    dirty = true;

                    break;
                case MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP:
                case MidiMix.PROP_INSTRUMENT_TRANSPOSITION:
                case MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT:
                    update = true;

                default:    // e.g.  case MidiMix.PROP_INSTRUMENT_MUTE:
                    // Nothing
                    break;
            }

        } else if (e.getSource() == PlaybackSettings.getInstance())
        {
            switch (e.getPropertyName())
            {
                case PlaybackSettings.PROP_PLAYBACK_KEY_TRANSPOSITION:
                    update = true;  // Control track is not impacted
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

                default:   // PROP_VETO_PRE_PLAYBACK, PROP_LOOPCOUNT, PROP_PLAYBACK_CLICK_ENABLED
                    // Do nothing
                    break;
            }

        }

        if (isUpdatable)
        {
            if (dirty)
            {
                setDirty();
            } else if (update)
            {
                prepareUpdate(false);
            }
        }
    }

    // ==========================================================================================================
    // UpdatableSongSession.UpdateProvider interface
    // ==========================================================================================================
    @Override
    public Map<RhythmVoice, Phrase> getUpdate()
    {
        return mapRvPhrases;
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

        if (!getState().equals(PlaybackSession.State.GENERATED) || !isUpdatable)
        {
            return;
        }

        LOGGER.info("chordLeadSheetChanged()  -- event=" + event);

        boolean update = false;
        boolean disableUpdates = false;


        if (event instanceof SizeChangedEvent)
        {
            disableUpdates = true;

        } else if ((event instanceof ItemAddedEvent)
                || (event instanceof ItemRemovedEvent))
        {

            var contextItems = event.getItems().stream()
                    .filter(cli -> isClsBarIndexPartOfContext(cli.getPosition().getBar()))
                    .collect(Collectors.toList());
            disableUpdates = contextItems.stream().anyMatch(cli -> !(cli instanceof CLI_ChordSymbol));
            update = contextItems.stream().allMatch(cli -> cli instanceof CLI_ChordSymbol);

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
                    update = true;
                }
            }

        } else if (event instanceof ItemMovedEvent)
        {
            ItemMovedEvent e = (ItemMovedEvent) event;
            if (isClsBarIndexPartOfContext(e.getNewPosition().getBar()) || isClsBarIndexPartOfContext(e.getOldPosition().getBar()))
            {
                update = true;
            }

        } else if (event instanceof SectionMovedEvent)
        {
            disableUpdates = true;

        }

        LOGGER.info("chordLeadSheetChanged()  => disableUpdates=" + disableUpdates + " update=" + update);

        if (disableUpdates)
        {
            disableUpdates();
        } else if (update)
        {
            prepareUpdate(true);    // Chord symbols have changed, control track is impacted
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
    public void songStructureChanged(SgsChangeEvent e)
    {
        // NOTE: model changes can be generated outside the EDT!        

        if (!getState().equals(PlaybackSession.State.GENERATED) || !isUpdatable)
        {
            return;
        }

        LOGGER.info("songStructureChanged()  -- e=" + e);


        boolean disableUpdates = false;
        boolean update = false;

        // Context song parts (at the time of the SongContext object creation)
        List<SongPart> contextSongParts = getSongContext().getSongParts();


        if ((e instanceof SptRemovedEvent)
                || (e instanceof SptAddedEvent))
        {
            // Ok only if removed spt is after our context
            disableUpdates = e.getSongParts().stream()
                    .anyMatch(spt -> spt.getStartBarIndex() <= getSongContext().getBarRange().to);
            
        } else if (e instanceof SptReplacedEvent)
        {
            // Ok if replaced spt is not in the context
            SptReplacedEvent re = (SptReplacedEvent) e;
            disableUpdates = re.getSongParts().stream()
                    .anyMatch(spt -> contextSongParts.contains(spt));

        } else if (e instanceof SptResizedEvent)
        {
            // Ok if replaced spt is not in the context
            SptResizedEvent re = (SptResizedEvent) e;
            disableUpdates = re.getMapOldSptSize().getKeys().stream()
                    .anyMatch(spt -> contextSongParts.contains(spt));

        } else if (e instanceof SptRenamedEvent)
        {
            // Nothing
        } else if (e instanceof RpChangedEvent)
        {
            // Update if updated RP is for a context SongPart
            update = contextSongParts.contains(e.getSongPart());
        }

        LOGGER.info("songStructureChanged()  => disableUpdates=" + disableUpdates + " update=" + update);


        if (disableUpdates)
        {
            disableUpdates();
        } else if (update)
        {
            prepareUpdate(false);
        }

    }

    // ==========================================================================================================
    // ControlTrackProvider implementation
    // ==========================================================================================================    
    @Override
    public List<Position> getSongPositions()
    {
        return isControlTrackEnabled ? super.getSongPositions() : null;
    }

    @Override
    public ContextChordSequence getContextChordGetSequence()
    {
        return isControlTrackEnabled ? super.getContextChordGetSequence() : null;
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================
    private void prepareUpdate(boolean controlTrackImpacted)
    {
        LOGGER.info("prepareUpdate() -- ");
        if (!getState().equals(State.GENERATED))
        {
            LOGGER.info("prepareUpdate()   => ignored because state=" + getState());
            return;
        }

        SongContext workContext = getSongContext();
        int t = PlaybackSettings.getInstance().getPlaybackKeyTransposition();
        if (isPlaybackTranspositionEnabled() && t != 0)
        {
            workContext = buildTransposedContext(getSongContext(), t);
        }

        // Recompute the RhythmVoice phrases
        SongSequenceBuilder sgBuilder = new SongSequenceBuilder(workContext);
        try
        {
            mapRvPhrases = sgBuilder.buildMapRvPhrase(true);
        } catch (MusicGenerationException ex)
        {
            // TODO improve this!
            Exceptions.printStackTrace(ex);
            return;
        }


//        Drums Rerouting ??
//        Control track if chord symbol have changed!!
        LOGGER.info("prepareUpdate()   => done, notifying...");

        // Notify listeners, probably an UpdatableSongSession
        firePropertyChange(UpdatableSongSession.UpdateProvider.PROP_UPDATE_AVAILABLE, false, true);
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
     * This also disables position and chord symbol providing.
     */
    private void disableUpdates()
    {
        if (isUpdatable)
        {
            LOGGER.info("disableUpdates() -- ");
            isUpdatable = false;
            setDirty();
            isControlTrackEnabled = false;
            firePropertyChange(ControlTrackProvider.PROP_DISABLED, false, true);
        }
    }
    
    /**
     * Find an identical existing session in state NEW or GENERATED.
     *
     * @return Null if not found
     */
    static private DynamicSongSession findSession(SongContext sgContext,
            boolean enablePlaybackTransposition, boolean enableClickTrack, boolean enablePrecount, boolean enableControlTrack,
            int loopCount,
            ActionListener endOfPlaybackAction)
    {
        for (var session : sessions)
        {
            if ((session.getState().equals(PlaybackSession.State.GENERATED) || session.getState().equals(PlaybackSession.State.NEW))
                    && sgContext.equals(session.getSongContext())
                    && enablePlaybackTransposition == session.isPlaybackTranspositionEnabled()
                    && enableClickTrack == (session.getClickTrackId() != -1)
                    && enablePrecount == (session.getPrecountTrackId() != -1)
                    && enableControlTrack == (session.getControlTrackId() != -1)
                    && loopCount == session.loopCount      // Do NOT use getLoopCount(), because of PLAYBACK_SETTINGS_LOOP_COUNT handling
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
    private static class ClosedSessionsListener implements PropertyChangeListener
    {

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            DynamicSongSession session = (DynamicSongSession) evt.getSource();
            if (evt.getPropertyName().equals(PlaybackSession.PROP_STATE) && session.getState().equals(PlaybackSession.State.CLOSED))
            {
                sessions.remove(session);
                session.removePropertyChangeListener(this);
            }
        }
    }

}

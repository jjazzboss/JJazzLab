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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.sound.midi.Sequence;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.ClickManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.MidiSequenceBuilder;
import org.jjazz.rhythmmusicgeneration.api.SongContext;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;

/**
 * A basic session based on a SongContext.
 * <p>
 * <p>
 */
public class BasicSongContextSession implements PropertyChangeListener, PlaybackSession, SongContextProvider, EndOfPlaybackActionProvider
{

    protected State state = State.NEW;
    protected SongContext sgContext;
    protected Sequence sequence;
    protected long loopStartTick;
    protected long loopEndTick;
    protected int loopCount = 1;
    protected ActionListener actionListener;
    protected MusicGenerator.PostProcessor[] postProcessors;
    static private List<BasicSongContextSession> sessions = new ArrayList<>();

    /**
     * The sequence track id (index) for each rhythm voice, for the given context.
     * <p>
     * If a song uses rhythms R1 and R2 and context is only on R2 bars, then the map only contains R2 rhythm voices and track id.
     */
    private HashMap<RhythmVoice, Integer> mapRvTrackId;
    private final HashMap<Integer, Boolean> mapRvMuted = new HashMap<>();
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);


    /**
     * If an existing session in the NEW or GENERATED state already exists for the same parameters then return it, otherwise a new
     * session is created.
     * <p>
     *
     * @param sgContext
     * @param loopCount
     * @param endOfPlaybackAction
     * @param postProcessors
     * @return A session in the NEW or GENERATED state.
     */
    static public BasicSongContextSession getSession(SongContext sgContext, int loopCount, ActionListener endOfPlaybackAction, MusicGenerator.PostProcessor... postProcessors)
    {
        if (sgContext == null)
        {
            throw new IllegalArgumentException("sgContext=" + sgContext);
        }
        BasicSongContextSession session = findBasicSongContextSession(sgContext, 0, -1, loopCount, endOfPlaybackAction, postProcessors);
        if (session == null)
        {
            final BasicSongContextSession newSession = new BasicSongContextSession(sgContext, 0, -1, loopCount, endOfPlaybackAction, postProcessors);
            registerSession(newSession);
            return newSession;
        } else
        {
            return session;
        }
    }

    /**
     * If an existing session in the NEW or GENERATED state already exists for the same parameters then return it, otherwise a new
     * session is created.
     * <p>
     *
     * @param sgContext
     * @param loopStart
     * @param loopEnd If &lt; 0 set the loop tick end position to the the end of the generated sequence
     * @param loopCount
     * @param endOfPlaybackAction
     * @param postProcessors
     * @return A session in the NEW or GENERATED state.
     */
    static public BasicSongContextSession getSession(SongContext sgContext, long loopStart, long loopEnd, int loopCount, ActionListener endOfPlaybackAction, MusicGenerator.PostProcessor... postProcessors)
    {
        if (sgContext == null)
        {
            throw new IllegalArgumentException("sgContext=" + sgContext);
        }
        BasicSongContextSession session = findBasicSongContextSession(sgContext, loopStart, loopEnd, loopCount, endOfPlaybackAction, postProcessors);
        if (session == null)
        {
            final BasicSongContextSession newSession = new BasicSongContextSession(sgContext, loopStart, loopEnd, loopCount, endOfPlaybackAction, postProcessors);
            registerSession(newSession);
            return newSession;
        } else
        {
            return session;
        }
    }


    /**
     *
     * @param sgContext
     * @param loopStart
     * @param loopEnd If &lt; 0 set the loopEnd to the the end of the generated sequence
     * @param loopCount
     * @param endOfPlaybackAction
     * @param postProcessors
     */
    protected BasicSongContextSession(SongContext sgContext, long loopStart, long loopEnd, int loopCount, ActionListener endOfPlaybackAction, MusicGenerator.PostProcessor... postProcessors)
    {
        if (sgContext == null || loopStart < 0 || loopEnd < 0 || loopEnd < loopStart)
        {
            throw new IllegalArgumentException("sgContext=" + sgContext + " loopStart=" + loopStart + " loopEnd=" + loopEnd + " loopCount=" + loopCount + " endOfPlaybackAction=" + endOfPlaybackAction);
        }
        this.sgContext = sgContext;
        this.postProcessors = postProcessors;
        this.actionListener = endOfPlaybackAction;
        this.loopStartTick = loopStart;
        this.loopEndTick = loopEnd;
        this.loopCount = loopCount;

        this.sgContext.getSong().addPropertyChangeListener(this);
        this.sgContext.getMidiMix().addPropertyChangeListener(this);
    }


    @Override
    public State getState()
    {
        return state;
    }

    @Override
    public void generate() throws MusicGenerationException
    {
        if (!state.equals(State.NEW))
        {
            throw new IllegalStateException("state=" + state);
        }

        // Build the sequence
        MidiSequenceBuilder seqBuilder = new MidiSequenceBuilder(sgContext, postProcessors);
        sequence = seqBuilder.buildSequence(false); // Can raise MusicGenerationException
        if (sequence == null)
        {
            // If unexpected error, assertion error etc.
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ERR_BuildSeqError"));
        }


        // If loopEndTick not already defined, define it to the end of the sequence
        if (loopEndTick < 0)
        {
            loopEndTick = Math.round(sgContext.getBeatRange().size() * MidiConst.PPQ_RESOLUTION);
        }


        // Used to identify a RhythmVoice's track
        mapRvTrackId = seqBuilder.getRvTrackIdMap();


        // Save the mute status of each RhythmVoice track
        MidiMix mm = sgContext.getMidiMix();
        for (RhythmVoice rv : mapRvTrackId.keySet())
        {
            mapRvMuted.put(mapRvTrackId.get(rv), mm.getInstrumentMixFromKey(rv).isMute());
        }


        // Update the sequence if rerouting needed
        rerouteDrumsChannels(sequence, sgContext.getMidiMix());


        // Change state
        State old = state;
        state = State.GENERATED;
        pcs.firePropertyChange(PROP_STATE, old, state);


    }

    @Override
    public int getTempo()
    {
        return sgContext.getSong().getTempo();
    }


    @Override
    public Sequence getSequence()
    {
        return state.equals(State.GENERATED) ? sequence : null;
    }

    @Override
    public long getLoopStartTick()
    {
        return state.equals(State.GENERATED) ? loopStartTick : -1;
    }


    @Override
    public long getLoopEndTick()
    {
        return state.equals(State.GENERATED) ? loopEndTick : -1;
    }

    @Override
    public int getLoopCount()
    {
        return loopCount;
    }


    @Override
    public long getTick(int barIndex)
    {
        if (!state.equals(State.GENERATED))
        {
            return -1;
        }

        long tick = sgContext.getRelativeTick(new Position(barIndex, 0));
        if (tick != -1)
        {
            tick += loopStartTick;
        }
        return tick;
    }

    @Override
    public IntRange getBarRange()
    {
        return state.equals(State.GENERATED) || state.equals(State.OUTDATED) ? sgContext.getBarRange() : null;
    }

    /**
     * Include the click track.
     *
     * @return
     */
    @Override
    public HashMap<Integer, Boolean> getTracksMuteStatus()
    {
        return state.equals(State.GENERATED) || state.equals(State.OUTDATED) ? new HashMap<>(mapRvMuted) : null;
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


    @Override
    public void cleanup()
    {
        ClickManager.getInstance().removePropertyChangeListener(this);
        MusicController.getInstance().removePropertyChangeListener(this);  // playback key transposition        
        sgContext.getSong().removePropertyChangeListener(this);
        sgContext.getMidiMix().removePropertyChangeListener(this);
    }


    public List<MusicGenerator.PostProcessor> getPostProcessors()
    {
        return Arrays.asList(postProcessors);
    }

    // ==========================================================================================================
    // SongContextProvider implementation
    // ==========================================================================================================    
    @Override
    public SongContext getSongContext()
    {
        return sgContext;
    }

    // ==========================================================================================================
    // EndofPlaybackActionProvider implementation
    // ==========================================================================================================    

    @Override
    public ActionListener getEndOfPlaybackAction()
    {
        return actionListener;
    }

    // ==========================================================================================================
    // PropertyChangeListener implementation
    // ==========================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (state.equals(State.CLOSED))
        {
            return;
        }

        State old = state;      // NEW, GENERATED or OUTDATED
        boolean outdated = false;

        if (e.getSource() == sgContext.getSong())
        {
            if (e.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED))
            {
                if ((Boolean) e.getNewValue() == true)
                {
                    outdated = true;        // Even if State is NEW, sgContext bar range might be not compatible with the updated song
                }
            } else if (e.getPropertyName().equals(Song.PROP_TEMPO))
            {
                pcs.firePropertyChange(PROP_TEMPO, (Integer) e.getOldValue(), (Integer) e.getNewValue());

            } else if (e.getPropertyName().equals(Song.PROP_CLOSED))
            {
                state = State.CLOSED;
                pcs.firePropertyChange(PROP_STATE, old, state);

            }
        } else if (e.getSource() == sgContext.getMidiMix())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_INSTRUMENT_MUTE:
                    if (!state.equals(State.NEW))
                    {
                        InstrumentMix insMix = (InstrumentMix) e.getOldValue();
                        MidiMix mm = sgContext.getMidiMix();
                        RhythmVoice rv = mm.geRhythmVoice(insMix);
                        Integer trackId = mapRvTrackId.get(rv);     // Can be null if state==outdated
                        if (trackId != null)
                        {
                            mapRvMuted.put(trackId, insMix.isMute());
                            pcs.firePropertyChange(PROP_MUTED_TRACKS, false, true);
                        }
                    }
                    break;

                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX:
                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED:
                case MidiMix.PROP_INSTRUMENT_TRANSPOSITION:
                case MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT:
                case MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP:
                    if (state.equals(State.GENERATED))
                    {
                        outdated = true;
                    }
                    break;

                default:
                    // eg MidiMix.PROP_USER_CHANNEL: do nothing
                    break;
            }
        }

        if (outdated)
        {
            state = State.OUTDATED;
            pcs.firePropertyChange(PROP_STATE, old, state);
        }

    }

    @Override
    public String toString()
    {
        return "BasicSongSession=[state=" + state + ", " + sgContext + ", " + Arrays.asList(postProcessors) + "]";
    }

    // ==========================================================================================================
    // Private methods
    // ==========================================================================================================

    private void rerouteDrumsChannels(Sequence seq, MidiMix mm)
    {
        List<Integer> toBeRerouted = mm.getDrumsReroutedChannels();
        MidiUtilities.rerouteShortMessages(seq, toBeRerouted, MidiConst.CHANNEL_DRUMS);
    }


    /**
     * Find an identical existing BasicSongContextSession in state NEW or GENERATED.
     *
     * @param loopCount
     * @param endOfPlaybackAction
     * @param sgContext
     * @param postProcessors
     * @return Null if not found
     */
    static private BasicSongContextSession findBasicSongContextSession(SongContext sgContext, long loopStart, long loopEnd, int loopCount, ActionListener endOfPlaybackAction, MusicGenerator.PostProcessor... postProcessors)
    {
        for (var s : sessions)
        {
            if (!(s instanceof BasicSongContextSession))
            {
                continue;
            }
            var session = (BasicSongContextSession) s;
            if ((session.getState().equals(PlaybackSession.State.GENERATED) || session.getState().equals(PlaybackSession.State.NEW))
                    && sgContext.equals(session.getSongContext())
                    && loopStart == session.loopStartTick
                    && (loopEnd == -1 || loopEnd == session.loopEndTick)
                    && Objects.equals(session.getPostProcessors(), Arrays.asList(postProcessors))
                    && loopCount == session.getLoopCount()
                    && endOfPlaybackAction == session.getEndOfPlaybackAction())
            {
                return session;
            }
        }
        return null;
    }


    private static void registerSession(final BasicSongContextSession newSession)
    {
        newSession.addPropertyChangeListener(evt ->
        {
            if (evt.getPropertyName().equals(PlaybackSession.PROP_STATE) && newSession.getState().equals(PlaybackSession.State.CLOSED))
            {
                sessions.remove(newSession);
            }
        });
        sessions.add(newSession);
    }
}

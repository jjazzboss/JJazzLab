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
package org.jjazz.musiccontrol.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import static org.jjazz.musiccontrol.api.PlaybackSettings.PROP_CLICK_PITCH_LOW;
import org.jjazz.songcontext.api.SongContext;
import org.openide.util.NbPreferences;
import org.jjazz.songstructure.api.SongPart;

/**
 * Playback settings (click, precount, looping, transposition, auto-update mode) and related helper methods.
 * <p>
 * Property change events are fired when settings are modified.
 */
public class PlaybackSettings
{

    public enum PrecountMode
    {
        ONE_BAR, TWO_BARS, AUTO;

        /**
         * Same as valueOf except it can't fail whatever s.
         *
         * @param name
         * @param defValue Returned value if name is not valid
         * @return
         */
        static public PrecountMode valueOf(String name, PrecountMode defValue)
        {
            PrecountMode mode = defValue;
            try
            {
                mode = PrecountMode.valueOf(name);
            } catch (IllegalArgumentException | NullPointerException ex)
            {
                // Nothing
            }
            return mode;
        }
    }
    private static PlaybackSettings INSTANCE = null;
    public static String CLICK_TRACK_NAME = "JJazzClickTrack";
    public static String PRECOUNT_CLICK_TRACK_NAME = "JJazzPreCountClickTrack";
    public static final String PROP_LOOPCOUNT = "PropLoopCount";
    public static final String PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION = "DisplayTransposition";
    public static final String PROP_CLICK_PITCH_HIGH = "ClickPitchHigh";
    public static final String PROP_CLICK_PITCH_LOW = "ClickPitchLow";
    public static final String PROP_CLICK_VELOCITY_HIGH = "ClickVelocityHigh";
    public static final String PROP_CLICK_VELOCITY_LOW = "ClickVelocityLow";
    public static final String PROP_CLICK_PREFERRED_CHANNEL = "ClickChannel";
    public static final String PROP_CLICK_PRECOUNT_ENABLED = "ClickPrecountEnabled";
    public static final String PROP_CLICK_PRECOUNT_MODE = "ClickPrecountMode";
    public static final String PROP_PLAYBACK_CLICK_ENABLED = "PlaybackClickEnabled";
    public static final String PROP_AUTO_UPDATE_ENABLED = "AutoUpdateEnabled";
    /**
     * Fired each time a parameter that can impact music generation is modified .
     * <p>
     * OldValue=the property name that triggered the musical change, newValue=optional data
     */
    public static final String PROP_MUSIC_GENERATION = "PlaybackSettingsMusicGeneration";

    private int loopCount = 0;
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static Preferences prefs = NbPreferences.forModule(PlaybackSettings.class);
    protected static final Logger LOGGER = Logger.getLogger(PlaybackSettings.class.getSimpleName());

    static public PlaybackSettings getInstance()
    {
        synchronized (PlaybackSettings.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new PlaybackSettings();
            }
        }
        return INSTANCE;
    }

    private PlaybackSettings()
    {

    }

    public int getLoopCount()
    {
        return loopCount;
    }

    /**
     * Set the loop count of the playback.
     * <p>
     *
     * @param loopCount If 0, play the song once (no loop). Use Sequencer.LOOP_CONTINUOUSLY for endless loop.
     */
    public void setLoopCount(int loopCount)
    {
        if (loopCount != Sequencer.LOOP_CONTINUOUSLY && loopCount < 0)
        {
            throw new IllegalArgumentException("loopCount=" + loopCount);
        }

        int old = this.loopCount;
        this.loopCount = loopCount;
        pcs.firePropertyChange(PROP_LOOPCOUNT, old, this.loopCount);
    }

    /**
     * Get the key transposition applied to chord symbols shown in the UI.
     *
     * @return [0, 12) Default is 0.
     */
    public int getChordSymbolsDisplayTransposition()
    {
        return - prefs.getInt(PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION, 0); // Saved as negative range for backwards compatibility
    }

    /**
     * Set the key transposition applied to chord symbols shown in the UI.
     * <p>
     * Ex: if transposition=2, chord=B7 will be shown as C#7.
     *
     * @param transposition int in range [0, 12)
     */
    public void setChordSymbolsDisplayTransposition(int transposition)
    {
        Preconditions.checkArgument(transposition >= 0 && transposition < 12, "transposition=%s", transposition);

        int old = getChordSymbolsDisplayTransposition();
        prefs.putInt(PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION, - transposition); // Save as negative range for backwards compatibility
        pcs.firePropertyChange(PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION, old, transposition);
    }

    /**
     * Set if playing backing track should be automatically updated in real-time when song changes.
     *
     * @param b
     *
     */
    public void setAutoUpdateEnabled(boolean b)
    {
        boolean old = isAutoUpdateEnabled();
        prefs.putBoolean(PROP_AUTO_UPDATE_ENABLED, b);
        pcs.firePropertyChange(PROP_AUTO_UPDATE_ENABLED, old, b);
    }

    /**
     * True if playing backing track should be automatically updated in real-time when song changes.
     *
     * @return
     */
    public boolean isAutoUpdateEnabled()
    {
        return prefs.getBoolean(PROP_AUTO_UPDATE_ENABLED, true);
    }

    /**
     * Enable the click during playback.
     * <p>
     *
     * @param b
     */
    public void setPlaybackClickEnabled(boolean b)
    {
        boolean old = isPlaybackClickEnabled();
        prefs.putBoolean(PROP_PLAYBACK_CLICK_ENABLED, b);
        pcs.firePropertyChange(PROP_PLAYBACK_CLICK_ENABLED, old, b);
        fireIsMusicGenerationModified(PROP_PLAYBACK_CLICK_ENABLED, b);
    }

    public boolean isPlaybackClickEnabled()
    {
        return prefs.getBoolean(PROP_PLAYBACK_CLICK_ENABLED, false);
    }

    /**
     *
     * @param b If true a click precount is used before playing the song.
     */
    public void setClickPrecountEnabled(boolean b)
    {
        boolean old = isClickPrecountEnabled();
        prefs.putBoolean(PROP_CLICK_PRECOUNT_ENABLED, b);
        pcs.firePropertyChange(PROP_CLICK_PRECOUNT_ENABLED, old, b);
        fireIsMusicGenerationModified(PROP_CLICK_PRECOUNT_ENABLED, b);
    }

    public boolean isClickPrecountEnabled()
    {
        return prefs.getBoolean(PROP_CLICK_PRECOUNT_ENABLED, true);
    }

    /**
     * Set the precount mode.
     *
     * @param mode
     */
    public void setClickPrecountMode(PrecountMode mode)
    {
        if (mode == null)
        {
            throw new NullPointerException("mode");
        }
        PrecountMode old = getClickPrecountMode();
        if (old != mode)
        {
            prefs.put(PROP_CLICK_PRECOUNT_MODE, mode.name());
            pcs.firePropertyChange(PROP_CLICK_PRECOUNT_MODE, old, mode);
            fireIsMusicGenerationModified(PROP_CLICK_PRECOUNT_MODE, mode);
        }
    }

    /**
     * Get the precount mode.
     * <p>
     * @return
     */
    public PrecountMode getClickPrecountMode()
    {
        String s = prefs.get(PROP_CLICK_PRECOUNT_MODE, PrecountMode.ONE_BAR.name());
        PrecountMode mode = PrecountMode.valueOf(s, PrecountMode.ONE_BAR);
        return mode;
    }

    /**
     * Get the number of precount bars.
     * <p>
     * The parameters are only used if precount mode is set to AUTO.<br>
     * Example in AUTO mode: a very fast tempo song will use 2 bars, a 4/4 will use 1 bar up to mid-range tempo etc.
     *
     * @param ts    Ignored if precount mode is not AUTO.
     * @param tempo Ignored if precount mode is not AUTO.
     * @return Can be 1 or 2 bars.
     */
    public int getClickPrecountNbBars(TimeSignature ts, int tempo)
    {
        switch (getClickPrecountMode())
        {
            case ONE_BAR:
                return 1;
            case TWO_BARS:
                return 2;
            case AUTO:
                if (ts == null || tempo < 0)
                {
                    throw new IllegalArgumentException("ts=" + ts + " tempo=" + tempo);
                }
                float nBeats = ts.getNbNaturalBeats();
                int res;
                if (nBeats <= 3)
                {
                    res = tempo < 55 ? 1 : 2;
                } else if (nBeats <= 4)
                {
                    res = tempo < 100 ? 1 : 2;
                } else
                {
                    res = tempo < 120 ? 1 : 2;
                }
                return res;
            default:
                throw new IllegalStateException("getClickPrecountMode()=" + getClickPrecountMode());
        }
    }

    /**
     *
     * @param channel
     */
    public void setPreferredClickChannel(int channel)
    {
        if (channel < MidiConst.CHANNEL_MIN || channel > MidiConst.CHANNEL_MAX)
        {
            throw new IllegalArgumentException("channel=" + channel);
        }
        int old = getPreferredClickChannel();
        if (old != channel)
        {
            prefs.putInt(PROP_CLICK_PREFERRED_CHANNEL, channel);
            pcs.firePropertyChange(PROP_CLICK_PREFERRED_CHANNEL, old, channel);
            fireIsMusicGenerationModified(PROP_CLICK_PREFERRED_CHANNEL, channel);
        }
    }

    /**
     * The preferred click channel.
     *
     * @return
     */
    public int getPreferredClickChannel()
    {
        return prefs.getInt(PROP_CLICK_PREFERRED_CHANNEL, MidiConst.CHANNEL_DRUMS);
    }

    /**
     * The actual Midi channel to be used with he specified MidiMix.
     * <p>
     * If in the midiMix channel=getPreferredClickChannel() is used and is not a drums/percussion instrument, return the Midi channel MidiConst.CHANNEL_DRUMS.
     * Otherwise return getPreferredClickChannel().
     *
     * @param midiMix
     * @return
     */
    public int getClickChannel(MidiMix midiMix)
    {
        int prefChannel = getPreferredClickChannel();
        InstrumentMix insMix = midiMix.getInstrumentMix(prefChannel);
        if (insMix == null || midiMix.getRhythmVoice(prefChannel).isDrums())
        {
            return prefChannel;
        }
        LOGGER.log(Level.WARNING, "getClickChannel() Can''t use preferred click channel {0}{1}, using channel {2}{3} instead", new Object[]
        {
            prefChannel,
            1, MidiConst.CHANNEL_DRUMS, 1
        });
        return MidiConst.CHANNEL_DRUMS;
    }

    /**
     *
     * @param pitch value must be [35-81] (GM1 drum map)
     */
    public void setClickPitchHigh(int pitch)
    {
        if (pitch < 35 || pitch > 81)
        {
            throw new IllegalArgumentException("pitch=" + pitch);
        }
        int old = getClickPitchHigh();
        if (old != pitch)
        {
            prefs.putInt(PROP_CLICK_PITCH_HIGH, pitch);
            pcs.firePropertyChange(PROP_CLICK_PITCH_HIGH, old, pitch);
            fireIsMusicGenerationModified(PROP_CLICK_PITCH_HIGH, pitch);
        }
    }

    public int getClickPitchHigh()
    {
        return prefs.getInt(PROP_CLICK_PITCH_HIGH, MidiConst.SIDE_STICK);
    }

    /**
     *
     * @param pitch value must be [35-81] (GM1 drum map)
     */
    public void setClickPitchLow(int pitch)
    {
        if (pitch < 35 || pitch > 81)
        {
            throw new IllegalArgumentException("pitch=" + pitch);
        }
        int old = getClickPitchLow();
        if (old != pitch)
        {
            prefs.putInt(PROP_CLICK_PITCH_LOW, pitch);
            pcs.firePropertyChange(PROP_CLICK_PITCH_LOW, old, pitch);
            fireIsMusicGenerationModified(PROP_CLICK_PITCH_LOW, pitch);
        }
    }

    public int getClickPitchLow()
    {
        return prefs.getInt(PROP_CLICK_PITCH_LOW, MidiConst.SIDE_STICK);
    }

    public void setClickVelocityHigh(int v)
    {
        if (v < 0 || v > 127)
        {
            throw new IllegalArgumentException("v=" + v);
        }
        int old = getClickVelocityHigh();
        if (old != v)
        {
            prefs.putInt(PROP_CLICK_VELOCITY_HIGH, v);
            pcs.firePropertyChange(PROP_CLICK_VELOCITY_HIGH, old, v);
            fireIsMusicGenerationModified(PROP_CLICK_VELOCITY_HIGH, v);
        }
    }

    public int getClickVelocityHigh()
    {
        return prefs.getInt(PROP_CLICK_VELOCITY_HIGH, 120);
    }

    public void setClickVelocityLow(int v)
    {
        if (v < 0 || v > 127)
        {
            throw new IllegalArgumentException("v=" + v);
        }
        int old = getClickVelocityLow();
        if (old != v)
        {
            prefs.putInt(PROP_CLICK_VELOCITY_LOW, v);
            pcs.firePropertyChange(PROP_CLICK_VELOCITY_LOW, old, v);
            fireIsMusicGenerationModified(PROP_CLICK_VELOCITY_LOW, v);
        }
    }

    public int getClickVelocityLow()
    {
        return prefs.getInt(PROP_CLICK_VELOCITY_LOW, 90);
    }

    /**
     * Add a click track using the current settings.
     * <p>
     *
     * @param sequence The sequence for which we add the control track.
     * @param context
     * @return the index of the track in the sequence.
     */
    public int addClickTrack(Sequence sequence, SongContext context)
    {
        if (sequence == null || context == null)
        {
            throw new IllegalArgumentException("seq=" + sequence + " context=" + context);
        }
        MidiMix midiMix = context.getMidiMix();
        Track track = sequence.createTrack();
        int clickChannel = getClickChannel(midiMix);

        // Add track name
        MidiEvent me = new MidiEvent(MidiUtilities.getTrackNameMetaMessage(CLICK_TRACK_NAME), 0);
        track.add(me);

        long tick = 0;
        // Scan all SongParts in the context
        for (SongPart spt : context.getSongParts())
        {
            TimeSignature ts = spt.getRhythm().getTimeSignature();
            int nbBars = context.getSptBarRange(spt).size();
            tick = addClickEvents(track, clickChannel, tick, nbBars, ts);
        }

        // Set EndOfTrack
        MidiUtilities.setEndOfTrackPosition(track, tick);

        return Arrays.asList(sequence.getTracks()).indexOf(track);
    }

    /**
     * Add a precount click track to the sequence for the specified song.
     * <p>
     * Except for the cases below, all existing sequence MidiEvents are shifted 1 or 2 bars later in order to leave room for the precount bars.
     * <p>
     * The following initial events (at tick 0) are not moved:<br>
     * - Meta track name<br>
     * - Meta time signature<br>
     * - Meta tempo<br>
     * - Meta copyright<br>
     * <p>
     *
     *
     * @param sequence The sequence for which we add the precount click track. Resolution must be MidiConst.PPQ.
     * @param context
     * @return The tick position of the start of the song.
     */
    public long addPrecountClickTrack(Sequence sequence, SongContext context)
    {
        if (sequence == null || context == null || sequence.getDivisionType() != Sequence.PPQ || sequence.getResolution() != MidiConst.PPQ_RESOLUTION)
        {
            throw new IllegalArgumentException("seq=" + sequence + " context=" + context);
        }

        TimeSignature ts = context.getSongParts().get(0).getRhythm().getTimeSignature();
        int nbPrecountBars = getClickPrecountNbBars(ts, context.getSong().getTempo());
        long songStartTick = (long) (nbPrecountBars * ts.getNbNaturalBeats() * MidiConst.PPQ_RESOLUTION);


        for (Track track : sequence.getTracks())
        {
            // Save all the events as we may remove some of them from the track
            List<MidiEvent> trackEvents = MidiUtilities.getMidiEvents(track, evt -> true, null);
            List<MidiEvent> tick0SpecialEvents = new ArrayList<>();

            // Remove events which should not be shifted, so they can be re-added after: this ensures tick order
            // is preserved in the track (fix bug Issue #247)
            for (var me : trackEvents)
            {
                long tick = me.getTick();
                MidiMessage mm = me.getMessage();

                if (tick == 0)
                {
                    // Special handling for initial events
                    if (mm instanceof MetaMessage)
                    {
                        int type = ((MetaMessage) mm).getType();
                        if (type == MidiConst.META_TRACKNAME
                                || type == MidiConst.META_TIME_SIGNATURE
                                || type == MidiConst.META_TEMPO
                                || type == MidiConst.META_COPYRIGHT)
                        {
                            tick0SpecialEvents.add(me);
                            track.remove(me);
                            continue;
                        }
                    }
                }

                // Shift the event
                me.setTick(tick + songStartTick);
            }

            // Re-add the special events at tick 0
            tick0SpecialEvents.forEach(me -> track.add(me));

        }

        // Add the precount click track
        Track track = sequence.createTrack();
        MidiEvent me = new MidiEvent(MidiUtilities.getTrackNameMetaMessage(PRECOUNT_CLICK_TRACK_NAME), 0);
        track.add(me);
        addClickEvents(track, getClickChannel(context.getMidiMix()), 0, nbPrecountBars, ts);
        return songStartTick;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String propName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propName, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String propName, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(propName, l);
    }

    // ============================================================================
    // Private methods
    // ============================================================================
    /**
     * Add click events to the specified track based on the section size and time signature.
     *
     * @param track
     * @param channel
     * @param tickOffset
     * @param nbBars
     * @param ts
     * @return The tick position corresponding to the start of bar (nbBars+1).
     */
    private long addClickEvents(Track track, int channel, long tickOffset, int nbBars, TimeSignature ts)
    {
        if (track == null || !MidiConst.checkMidiChannel(channel) || tickOffset < 0)
        {
            throw new IllegalArgumentException("track=" + track + " channel=" + channel + " tickOffset=" + tickOffset);
        }
        float nbNaturalBeatsPerBar = ts.getNbNaturalBeats();
        float nbNaturalBeats = nbBars * nbNaturalBeatsPerBar;
        
        try
        {
            if (nbBars == 2 && ts == TimeSignature.FOUR_FOUR)
            {
                // Special case, bar1=click-rest-click-rest, bar2=click click click click
                for (int beat = 0; beat < 4; beat++)
                {
                    int velocity = (beat % 2) == 0 ? getClickVelocityHigh() : getClickVelocityLow() / 4;
                    int pitch = (beat % 2) == 0 ? getClickPitchHigh() : getClickPitchLow();
                    ShortMessage smOn = new ShortMessage(ShortMessage.NOTE_ON, channel, pitch, velocity);
                    ShortMessage smOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
                    long tick = tickOffset + beat * MidiConst.PPQ_RESOLUTION;
                    track.add(new MidiEvent(smOn, tick));
                    track.add(new MidiEvent(smOff, tick + MidiConst.PPQ_RESOLUTION / 2));  // Half-beat duration
                }

                for (int beat = 4; beat < 8; beat++)
                {
                    int velocity = beat == 4 ? getClickVelocityHigh() : getClickVelocityLow();
                    int pitch = beat == 4 ? getClickPitchHigh() : getClickPitchLow();
                    ShortMessage smOn = new ShortMessage(ShortMessage.NOTE_ON, channel, pitch, velocity);
                    ShortMessage smOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
                    long tick = tickOffset + beat * MidiConst.PPQ_RESOLUTION;
                    track.add(new MidiEvent(smOn, tick));
                    track.add(new MidiEvent(smOff, tick + MidiConst.PPQ_RESOLUTION / 2));  // Half-beat duration
                }

            } else
            {
                for (int beat = 0; beat < nbNaturalBeats; beat++)
                {
                    int velocity = (beat % nbNaturalBeatsPerBar) == 0 ? getClickVelocityHigh() : getClickVelocityLow();
                    int pitch = (beat % nbNaturalBeatsPerBar) == 0 ? getClickPitchHigh() : getClickPitchLow();
                    ShortMessage smOn = new ShortMessage(ShortMessage.NOTE_ON, channel, pitch, velocity);
                    ShortMessage smOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
                    long tick = tickOffset + beat * MidiConst.PPQ_RESOLUTION;
                    track.add(new MidiEvent(smOn, tick));
                    track.add(new MidiEvent(smOff, tick + MidiConst.PPQ_RESOLUTION / 2));  // Half-beat duration
                }
            }
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        // Next section tick
        long nextTick = tickOffset + (long) (nbNaturalBeats * MidiConst.PPQ_RESOLUTION);
        return nextTick;
    }

    private void fireIsMusicGenerationModified(String id, Object data)
    {
        pcs.firePropertyChange(PROP_MUSIC_GENERATION, id, data);
    }
}

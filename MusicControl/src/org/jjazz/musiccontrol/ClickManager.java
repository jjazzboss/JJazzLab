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
package org.jjazz.musiccontrol;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiUtilities;
import org.jjazz.song.api.Song;
import org.openide.util.NbPreferences;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * Click related methods.
 */
public class ClickManager
{

    public static String CLICK_TRACK_NAME = "JJazzClickTrack";
    public static String PRECOUNT_CLICK_TRACK_NAME = "JJazzPreCountClickTrack";
    private static ClickManager INSTANCE = null;

    public static String PROP_CLICK_PITCH = "ClickPitch";
    public static String PROP_CLICK_VELOCITY_HIGH = "ClickVelocityHigh";
    public static String PROP_CLICK_VELOCITY_LOW = "ClickVelocityLow";
    public static String PROP_CLICK_CHANNEL = "ClickChannel";
    public static String PROP_CLICK_PRECOUNT = "ClickPrecount";

    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static Preferences prefs = NbPreferences.forModule(ClickManager.class);
    protected static final Logger LOGGER = Logger.getLogger(ClickManager.class.getSimpleName());

    static public ClickManager getInstance()
    {
        synchronized (ClickManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ClickManager();
            }
        }
        return INSTANCE;
    }

    private ClickManager()
    {

    }

    /**
     *
     * @param b If true a click precount is used before playing the song.
     */
    public void setClickPrecount(boolean b)
    {
        boolean old = isClickPrecount();
        if (old != b)
        {
            prefs.putBoolean(PROP_CLICK_PRECOUNT, b);
            pcs.firePropertyChange(PROP_CLICK_PRECOUNT, old, b);
        }
    }

    public boolean isClickPrecount()
    {
        return prefs.getBoolean(PROP_CLICK_PRECOUNT, true);
    }

    /**
     * Get the number of precount bars for a song using specified parameters.
     * <p>
     * For example: a very fast tempo song will use 2 bars, a 4/4 will use 1 bar up to mid-range tempo etc.
     *
     * @param ts
     * @param tempo
     * @return Can be 1 or 2 bars.
     */
    public int getClickPrecountNbBars(TimeSignature ts, int tempo)
    {
        if (ts == null || tempo < 0)
        {
            throw new IllegalArgumentException("ts=" + ts + " tempo=" + tempo);
        }
        int res = 2;
        switch (ts.getNbNaturalBeats())
        {
            case 2:
                break;
            case 3:
                res = tempo < 55 ? 1 : 2;
                break;
            case 4:
                res = tempo < 100 ? 1 : 2;
                break;
            case 5:
                res = tempo < 120 ? 1 : 2;
                break;
            case 6:
                res = tempo < 120 ? 1 : 2;
                break;
            case 7:
                res = tempo < 120 ? 1 : 2;
                break;
            default:
                res = 2;
        }
        return res;
    }

    /**
     *
     * @param channel
     */
    public void setChannel(int channel)
    {
        if (channel < MidiConst.CHANNEL_MIN || channel > MidiConst.CHANNEL_MAX)
        {
            throw new IllegalArgumentException("channel=" + channel);
        }
        int old = getChannel();
        if (old != channel)
        {
            prefs.putInt(PROP_CLICK_CHANNEL, channel);
            pcs.firePropertyChange(PROP_CLICK_CHANNEL, old, channel);
        }
    }

    public int getChannel()
    {
        return prefs.getInt(PROP_CLICK_CHANNEL, MidiConst.CHANNEL_DRUMS);
    }

    /**
     *
     * @param pitch value must be [35-81] (GM1 drum map)
     */
    public void setClickPitch(int pitch)
    {
        if (pitch < 35 || pitch > 81)
        {
            throw new IllegalArgumentException("pitch=" + pitch);
        }
        int old = getClickPitch();
        if (old != pitch)
        {
            prefs.putInt(PROP_CLICK_PITCH, pitch);
            pcs.firePropertyChange(PROP_CLICK_PITCH, old, pitch);
        }
    }

    public int getClickPitch()
    {
        return prefs.getInt(PROP_CLICK_PITCH, MidiConst.SIDE_STICK);
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
        }
    }

    public int getClickVelocityHigh()
    {
        return prefs.getInt(PROP_CLICK_VELOCITY_HIGH, 95);
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
        }
    }

    public int getClickVelocityLow()
    {
        return prefs.getInt(PROP_CLICK_VELOCITY_LOW, 60);
    }

    /**
     * Add a click track using the current settings.
     * <p>
     *
     * @param sequence The sequence for which we add the control track.
     * @param song The song for which we add control events.
     * @return the index of the track in the sequence.
     */
    public int addClickTrack(Sequence sequence, Song song)
    {
        if (sequence == null || song == null)
        {
            throw new IllegalArgumentException("seq=" + sequence + " song=" + song);
        }
        Track track = sequence.createTrack();

        // Add track name
        MidiEvent me = new MidiEvent(MidiUtilities.getTrackNameMetaMessage(CLICK_TRACK_NAME), 0);
        track.add(me);

        SongStructure sgs = song.getSongStructure();
        long tick = 0;
        // Scan all SongParts
        for (SongPart spt : sgs.getSongParts())
        {
            CLI_Section section = spt.getParentSection();
            int sectionSize = sgs.getParentChordLeadSheet().getSectionSize(section);
            TimeSignature ts = section.getData().getTimeSignature();
            tick = addClickEvents(track, getChannel(), tick, sectionSize, ts);
        }

        // Set EndOfTrack
        long lastTick = (song.getSongStructure().getSizeInBeats() * MidiConst.PPQ_RESOLUTION) + 1;
        MidiUtilities.setEndOfTrackPosition(track, lastTick);

        return Arrays.asList(sequence.getTracks()).indexOf(track);
    }

    /**
     * Add a precount click track to the sequence for the specified song.
     * <p>
     * Except for the cases below, all existing sequence MidiEvents are shifted 1 or 2 bars later in order to leave room for the
     * precount bars. Meta events Track name, Tempo and Time signature are not moved.
     *
     * @param sequence The sequence for which we add the precount click track.
     * @param song
     * @return The tick position of the start of the song.
     */
    public long addPreCountClickTrack(Sequence sequence, Song song)
    {
        if (sequence == null || song == null)
        {
            throw new IllegalArgumentException("seq=" + sequence + " song=" + song);
        }

        TimeSignature ts = song.getSongStructure().getSongPart(0).getParentSection().getData().getTimeSignature();
        int nbPrecountBars = getClickPrecountNbBars(ts, song.getTempo());
        long songStartTick = (nbPrecountBars * ts.getNbNaturalBeats() * MidiConst.PPQ_RESOLUTION);

        // Shift all existing MidiEvents except some meta events
        for (Track track : sequence.getTracks())
        {
            for (int i = track.size() - 1; i >= 0; i--)
            {
                MidiEvent me = track.get(i);
                MidiMessage mm = me.getMessage();
                if (mm instanceof MetaMessage)
                {
                    int type = ((MetaMessage) mm).getType();
                    // Track name=3, tempo=81, time signature=88                    
                    if (type == 3 || type == 81 || type == 88)
                    {
                        continue;
                    }
                }
                me.setTick(me.getTick() + songStartTick);
            }
        }

        // Add the precount click track
        Track track = sequence.createTrack();
        MidiEvent me = new MidiEvent(MidiUtilities.getTrackNameMetaMessage(PRECOUNT_CLICK_TRACK_NAME), 0);
        track.add(me);
        addClickEvents(track, getChannel(), 0, nbPrecountBars, ts);

        return songStartTick;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
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
     * @return The tick position corresponding to the start of next section.
     */
    private long addClickEvents(Track track, int channel, long tickOffset, int nbBars, TimeSignature ts)
    {
        if (track == null || !MidiConst.checkMidiChannel(channel) || tickOffset < 0)
        {
            throw new IllegalArgumentException("track=" + track + " channel=" + channel + " tickOffset=" + tickOffset);
        }
        int nbNaturalBeatsPerBar = ts.getNbNaturalBeats();
        int nbNaturalBeats = nbBars * nbNaturalBeatsPerBar;
        try
        {
            for (int beat = 0; beat < nbNaturalBeats; beat++)
            {
                int velocity = ((beat % nbNaturalBeatsPerBar) == 0) ? getClickVelocityHigh() : getClickVelocityLow();  // First bar beat is stronger
                ShortMessage smOn = new ShortMessage(ShortMessage.NOTE_ON, channel, getClickPitch(), velocity);
                ShortMessage smOff = new ShortMessage(ShortMessage.NOTE_OFF, channel, getClickPitch(), 0);
                long tick = tickOffset + beat * MidiConst.PPQ_RESOLUTION;
                track.add(new MidiEvent(smOn, tick));
                track.add(new MidiEvent(smOff, tick + MidiConst.PPQ_RESOLUTION / 2));  // Half-beat duration
            }
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        // Next section tick
        long nextTick = tickOffset + nbNaturalBeats * MidiConst.PPQ_RESOLUTION;
        return nextTick;
    }

}

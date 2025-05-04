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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.swing.Timer;
import org.jjazz.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongMetaEvents;
import org.jjazz.song.api.SongMetaEvents.ClsSourceActionEvent;
import org.jjazz.song.api.SongMetaEvents.SgsSourceActionEvent;
import org.jjazz.songstructure.api.event.SgsActionEvent;

/**
 * Notify listeners via the PROP_MUSIC_GENERATION_COMBINED change event when a Song or its MidiMix or PlaybackSettings have changed in a way that impacts the
 * music generation for that song.
 * <p>
 * Relies on SongMetaEvents.PROP_MUSIC_GENERATION, MidiMix.PROP_MUSIC_GENERATION and PlaybackSettings.PROP_MUSIC_GENERATION.
 * <p>
 * A black-list mechanism can be used to filter out some PROP_MUSIC_GENERATION source events. A delay can be set before firing the ChangeEvent, in order to
 * automatically filter out rapid successive changes.
 * <p>
 */
public class SongMusicGenerationListener implements PropertyChangeListener
{

    /**
     * Song or MidiMix or PlaybackSettings have changed in a way that impacts music generation.
     * <p>
     * oldValue = the source change event. It can be a ClsSourceActionEvent, a SgsSourceActionEvent or a Song/MidiMix/PlaybackSettings property name.<br>
     * newValue = optional data associated to the source change event.
     */
    public static final String PROP_MUSIC_GENERATION_COMBINED = "PropMusicGenerationCombined";
    private Set<Object> blackList;
    private final Song song;
    private final MidiMix midiMix;
    private final int preFireChangeEventDelayMs;
    private final Timer timer;
    private Object lastSourceEvent;
    private Object lastData;
    private final SongMetaEvents songMetaEvents;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);


    /**
     * Construct a SongMusicGenerationListener.
     *
     * @param song
     * @param midiMix
     * @param preFireChangeEventDelayMs The delay in ms before firing a PROP_MUSIC_GENERATION_COMBINED event.
     * @see #getPreFireChangeEventDelayMs()
     */
    public SongMusicGenerationListener(Song song, MidiMix midiMix, int preFireChangeEventDelayMs)
    {
        Preconditions.checkArgument(preFireChangeEventDelayMs >= 0, "preFireChangeEventDelayMs=%s", preFireChangeEventDelayMs);
        this.blackList = new HashSet<>();
        this.song = song;
        this.midiMix = midiMix;
        this.preFireChangeEventDelayMs = preFireChangeEventDelayMs;


        // Listen to changes
        this.songMetaEvents = SongMetaEvents.getInstance(song);
        this.songMetaEvents.addPropertyChangeListener(SongMetaEvents.PROP_MUSIC_GENERATION, this);
        this.midiMix.addPropertyChangeListener(MidiMix.PROP_MUSIC_GENERATION, this);
        PlaybackSettings.getInstance().addPropertyChangeListener(PlaybackSettings.PROP_MUSIC_GENERATION, this);


        if (preFireChangeEventDelayMs > 0)
        {
            timer = new Timer(preFireChangeEventDelayMs, e -> timerElapsed());
            timer.setRepeats(false);
        } else
        {
            timer = null;
        }
    }

    public Song getSong()
    {
        return song;
    }

    public MidiMix getMidiMix()
    {
        return midiMix;
    }

    public void cleanup()
    {
        this.songMetaEvents.removePropertyChangeListener(SongMetaEvents.PROP_MUSIC_GENERATION, this);
        this.midiMix.removePropertyChangeListener(MidiMix.PROP_MUSIC_GENERATION, this);
        PlaybackSettings.getInstance().removePropertyChangeListener(PlaybackSettings.PROP_MUSIC_GENERATION, this);
    }


    /**
     * The delay to wait before firing a PROP_MUSIC_GENERATION_COMBINED event.
     * <p>
     * Delay is activated when receiving a PROP_MUSIC_GENERATION event. When the delay expires a PROP_MUSIC_GENERATION_COMBINED event is fired using the last
     * PROP_MUSIC_GENERATION received while the delay was running.
     *
     * @return A value in milliseconds.
     */
    public int getPreFireChangeEventDelayMs()
    {
        return preFireChangeEventDelayMs;
    }

    /**
     * The blacklisted source PROP_MUSIC_GENERATION event types.
     *
     * @return Can not be null.
     * @see #setBlackList(java.util.Set)
     */
    public Set<Object> getBlackList()
    {
        return Collections.unmodifiableSet(blackList);
    }

    /**
     * Black list some source PROP_MUSIC_GENERATION event types.
     * <p>
     *
     * @param blackList Can not be null. If not empty must contain Song/MidiMix/PlaybackSettings property names or ClsActionEvent.API_IDs or
     *                  SgsActionEvent.API_IDs
     */
    public void setBlackList(Set<Object> blackList)
    {
        Objects.requireNonNull(blackList);
        Preconditions.checkArgument(blackList.stream().allMatch(o
                -> o instanceof String || o instanceof ClsActionEvent.API_ID || o instanceof SgsActionEvent.API_ID),
                "blackList=%s", blackList);
        this.blackList = blackList;
    }

    /**
     * Add a listener to be notified when a music generation impacting change has occured.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    // =================================================================================================================
    // PropertyChangeListener interface
    // =================================================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // evt is a PROP_MUSIC_GENERATION event from SongMetaEvents, MidiMix or PlaybackListener
        Object srcEvent = evt.getOldValue();
        Object key = switch (srcEvent)
        {
            case String s ->
                s;
            case ClsSourceActionEvent csae ->
                csae.getApiId();
            case SgsSourceActionEvent ssae ->
                ssae.getApiId();
            default -> throw new IllegalStateException("srcEvent=" + srcEvent);
        };
        if (blackList.contains(key))
        {
            return;
        }

        fireChangeEventMaybe(srcEvent, evt.getNewValue());
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================

    private void fireChangeEventMaybe(Object sourceEvent, Object data)
    {
        if (timer == null)
        {
            fireChangeEvent(sourceEvent, data);
            return;
        }

        synchronized (this)
        {
            if (!timer.isRunning())
            {
                timer.start();
            }

            lastSourceEvent = sourceEvent;
            lastData = data;
        }
    }

    private void fireChangeEvent(Object sourceEvent, Object data)
    {
        pcs.firePropertyChange(PROP_MUSIC_GENERATION_COMBINED, sourceEvent, data);
    }

    private void timerElapsed()
    {
        assert lastSourceEvent != null;
        Object sourceEvent;
        Object data;
        synchronized (this)
        {
            sourceEvent = lastSourceEvent;
            data = lastData;
        }
        fireChangeEvent(sourceEvent, data);
    }

}

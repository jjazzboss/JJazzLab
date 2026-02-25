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
import java.util.Objects;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongMetaEvents;
import org.jjazz.utilities.api.CoalescingTaskScheduler;

/**
 * Notify listeners via the PROP_MUSIC_GENERATION_COMBINED change event when a Song component or PlaybackSettings have changed in a way that impacts the
 * music generation for that song.
 * <p>
 * Relies on SongMetaEvents.PROP_MUSIC_GENERATION, MidiMix.PROP_MUSIC_GENERATION and PlaybackSettings.PROP_MUSIC_GENERATION.
 * <p>
 * A delay can be set before firing the ChangeEvent, in order to automatically filter out rapid successive changes.
 * <p>
 */
public class SongMusicGenerationListener implements PropertyChangeListener
{

    /**
     * Song or MidiMix or PlaybackSettings have changed in a way that impacts music generation.
     * <p>
     * oldValue = the source change event. It can be a ClsChangeEvent, a SgsChangeEvent, a SongPropertyChangeEvent, or MidiMix/PlaybackSettings property change event.<br>
     * newValue = optional data associated to the source change event.
     */
    public static final String PROP_MUSIC_GENERATION_COMBINED = "PropMusicGenerationCombined";
    private final Song song;
    private final MidiMix midiMix;
    private final CoalescingTaskScheduler coalescingTaskScheduler;
    private final SongMetaEvents songMetaEvents;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);


    /**
     * Construct a SongMusicGenerationListener.
     *
     * @param song
     * @param midiMix
     * @param preFireChangeEventDelayMs The delay in ms before firing a PROP_MUSIC_GENERATION_COMBINED event using the last source event received during the delay.
     * @see #getPreFireChangeEventDelayMs()
     */
    public SongMusicGenerationListener(Song song, MidiMix midiMix, int preFireChangeEventDelayMs)
    {
        Objects.requireNonNull(song);
        Objects.requireNonNull(midiMix);
        Preconditions.checkArgument(preFireChangeEventDelayMs >= 0, "preFireChangeEventDelayMs=%s", preFireChangeEventDelayMs);
        
        this.song = song;
        this.midiMix = midiMix;
        this.coalescingTaskScheduler = new CoalescingTaskScheduler(preFireChangeEventDelayMs);
        
        // Listen to changes
        this.songMetaEvents = SongMetaEvents.getInstance(song);
        this.songMetaEvents.addPropertyChangeListener(SongMetaEvents.PROP_MUSIC_GENERATION, this);
        this.midiMix.addPropertyChangeListener(MidiMix.PROP_MUSIC_GENERATION, this);
        PlaybackSettings.getInstance().addPropertyChangeListener(PlaybackSettings.PROP_MUSIC_GENERATION, this);

        
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
        assert evt.getSource() == songMetaEvents
                || evt.getSource() == midiMix
                || evt.getSource() == PlaybackSettings.getInstance() : "evt=" + evt;
        
        assert evt.getPropertyName().equals(SongMetaEvents.PROP_MUSIC_GENERATION)
                || evt.getPropertyName().equals(MidiMix.PROP_MUSIC_GENERATION)
                || evt.getPropertyName().equals(PlaybackSettings.PROP_MUSIC_GENERATION) : "evt=" + evt;
        
        
        Object srcEvent = evt.getOldValue();
        fireChangeLater(srcEvent, evt.getNewValue());
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================

    private void fireChangeLater(Object sourceEvent, Object data)
    {
        Runnable task = () -> 
        {
            pcs.firePropertyChange(PROP_MUSIC_GENERATION_COMBINED, sourceEvent, data);
        };
        coalescingTaskScheduler.request(task);
    }
    
}

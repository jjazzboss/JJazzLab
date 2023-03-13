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
package org.jjazz.musiccontrol.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Set;
import javax.swing.Timer;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;

/**
 * A helper class to be notified when a song and other elements have changed in a way that will impact music generation for that song.
 * <p>
 * The class fires a PROP_CHANGED change event when it receives a PROP_MUSIC_GENERATION property change from Song, MidiMix, and
 * PlaybackSettings.
 * <p>
 * A black-list mechanism can be used to filter out some PROP_MUSIC_GENERATION source events. A delay can be set before firing the
 * ChangeEvent, in order to automatically filter out rapid successive changes.
 */
public class SongMusicGenerationListener implements PropertyChangeListener
{

    /**
     * oldValue = the name of the Song/MidiMix/PlaybackSettings source property change event (PROP_MUSIC_GENERATION).<br>
     * newValue = optional data associated to the source property change event (PROP_MUSIC_GENERATION).
     */
    public static final String PROP_CHANGED = "PropChanged";
    private Set<String> blackList;

    private final Song song;
    private final MidiMix midiMix;
    private final int preFireChangeEventDelayMs;
    private final Timer timer;
    private String lastSourcePropName;
    private Object lastData;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);


    public SongMusicGenerationListener(Song song, MidiMix midiMix, int preFireChangeEventDelayMs)
    {
        Preconditions.checkArgument(preFireChangeEventDelayMs >= 0, "preFireChangeEventDelayMs=%d", preFireChangeEventDelayMs);
        this.song = song;
        this.midiMix = midiMix;
        this.preFireChangeEventDelayMs = preFireChangeEventDelayMs;
        this.song.addPropertyChangeListener(Song.PROP_MUSIC_GENERATION, this);
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
        this.song.removePropertyChangeListener(Song.PROP_MUSIC_GENERATION, this);
        this.midiMix.removePropertyChangeListener(MidiMix.PROP_MUSIC_GENERATION, this);
        PlaybackSettings.getInstance().removePropertyChangeListener(PlaybackSettings.PROP_MUSIC_GENERATION, this);
    }


    /**
     * The delay to wait before firing a change event.
     * <p>
     * All PROP_MUSIC_GENERATION change events received while the delay is running are discarded.
     *
     * @return A value in milliseconds.
     */
    public int getPreFireChangeEventDelayMs()
    {
        return preFireChangeEventDelayMs;
    }

    public Set<String> getBlackList()
    {
        return blackList;
    }

    /**
     * Black list some source PROP_MUSIC_GENERATION events by their property name or actionId: those source events won't trigger a
     * ChangeEvent from this instance.
     * <p>
     * Property names or actionId of Song, ChordLeadSheet, SongStructure, MidiMix, or PlaybackSettings.
     *
     * @param blackList Can be null
     */
    public void setBlackList(Set<String> blackList)
    {
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
        // evt is a PROP_MUSIC_GENERATION event: oldValue = source property name or actionId, newValue=optional data
        if (blackList != null && blackList.contains(evt.getOldValue().toString()))
        {
            return;
        }

        fireChangeEventMaybe(evt.getOldValue().toString(), evt.getNewValue());
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================

    private void fireChangeEventMaybe(String sourcePropName, Object data)
    {
        if (timer == null)
        {
            pcs.firePropertyChange(PROP_CHANGED, sourcePropName, data);
            return;
        }

        if (!timer.isRunning())
        {
            timer.start();
        }

        lastSourcePropName = sourcePropName;
        lastData = data;
    }

    private void timerElapsed()
    {
        assert lastSourcePropName != null;
        pcs.firePropertyChange(PROP_CHANGED, lastSourcePropName, lastData);
    }

}

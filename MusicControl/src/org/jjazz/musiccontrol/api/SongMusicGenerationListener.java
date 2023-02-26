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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.openide.util.ChangeSupport;

/**
 * A helper class to be notified when a song and other elements have changed in a way that will impact music generation for that song.
 * <p>
 * Listen to PROP_MUSIC_GENERATION property change in Song, MidiMix, and PlaybackSettings.
 * <p>
 * A black-list mechanism can be used to filter out some PROP_MUSIC_GENERATION source events.
 * <p>
 */
public class SongMusicGenerationListener implements PropertyChangeListener
{

    private Set<String> blackList;
    private final ChangeSupport cs = new ChangeSupport(this);
    private final Song song;
    private final MidiMix midiMix;

    public SongMusicGenerationListener(Song song, MidiMix midiMix)
    {
        this.song = song;
        this.midiMix = midiMix;
        this.song.addPropertyChangeListener(Song.PROP_MUSIC_GENERATION, this);
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
        this.song.removePropertyChangeListener(Song.PROP_MUSIC_GENERATION, this);
        this.midiMix.removePropertyChangeListener(MidiMix.PROP_MUSIC_GENERATION, this);
        PlaybackSettings.getInstance().removePropertyChangeListener(PlaybackSettings.PROP_MUSIC_GENERATION, this);
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
    public void addChangeListener(ChangeListener listener)
    {
        cs.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener)
    {
        cs.removeChangeListener(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // evt is a PROP_MUSIC_GENERATION event: newValue = source property name or actionId
        if (blackList != null && blackList.contains(evt.getNewValue().toString()))
        {
            return;
        }
        cs.fireChange();
    }
}

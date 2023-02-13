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
import javax.swing.event.ChangeListener;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.openide.util.ChangeSupport;

/**
 * A helper class to be notified when a song and other elements have changed in a way that will impact music generation for that
 * song.
 */
public class SongMusicGenerationListener implements PropertyChangeListener
{

    private final SongContext songContext;
    private final ChangeSupport cs = new ChangeSupport(this);

    public SongMusicGenerationListener(SongContext sgContext)
    {
        this.songContext = sgContext;
        this.songContext.getSong().addPropertyChangeListener(Song.PROP_MUSIC_GENERATION, this);
        this.songContext.getMidiMix().addPropertyChangeListener(MidiMix.PROP_MUSIC_GENERATION, this);
        PlaybackSettings.getInstance().addPropertyChangeListener(this);
    }

    public void cleanup()
    {
        this.songContext.getSong().removePropertyChangeListener(Song.PROP_MUSIC_GENERATION, this);
        this.songContext.getMidiMix().removePropertyChangeListener(MidiMix.PROP_MUSIC_GENERATION, this);
        PlaybackSettings.getInstance().removePropertyChangeListener(this);
    }

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
        boolean changed = false;
        if (evt.getSource() == songContext.getSong()
                || evt.getSource() == songContext.getMidiMix())
        {
            changed = true;
        } else if (evt.getSource() == PlaybackSettings.getInstance())
        {
            switch (evt.getPropertyName())
            {
                case PlaybackSettings.PROP_CLICK_PITCH_HIGH, PlaybackSettings.PROP_CLICK_PITCH_LOW, PlaybackSettings.PROP_CLICK_VELOCITY_HIGH, PlaybackSettings.PROP_CLICK_VELOCITY_LOW, PlaybackSettings.PROP_PLAYBACK_KEY_TRANSPOSITION ->
                    changed = true;
                default ->
                {
                }
            }
        }

        if (changed)
        {
            cs.fireChange();
        }
    }
}

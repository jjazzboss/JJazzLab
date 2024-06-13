/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.activesong.spi;

import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.openide.util.Lookup;

/**
 * A service provider which manages the active song and MidiMix.
 * <p>
 * Midi messages are sent upon MidiMix changes depending on getSendMessagePolicy(). If last song is closed, active song is reset to null.
 */
public interface ActiveSongManager
{

    /**
     * Get the default implementation.
     *
     * @return Can't be null
     */
    public static ActiveSongManager getDefault()
    {
        var res = Lookup.getDefault().lookup(ActiveSongManager.class);
        if (res == null)
        {
            throw new IllegalStateException("No ActiveSongBackgroundMusicBuilder instance found");
        }
        return res;
    }

    /**
     * oldValue=MidiMix, newValue=Song
     */
    public static final String PROP_ACTIVE_SONG = "ActiveSongAndMidiMix";

    /**
     * When to send Midi Messages.
     */
    public enum SendMidiMessagePolicy
    {
        MIX_CHANGE, // Each time a MidiMix parameter is modified               
        PLAY, // Before playing music
        ACTIVATION, // Upong MidiMix activation
    }


    MidiMix getActiveMidiMix();

    Song getActiveSong();

    /**
     * Set the specified song and MidiMix as active: <br>
     * - send MidiMessages for all MidiMix parameters at activation <br>
     * - listen to MidiMix changes and send the related Midi messages according to the SendPolicy <br>
     * - reset MusicController session<br>
     * - Fire a PROP_ACTIVE_SONG change event (oldValue=mm, newValue=sg) <br>
     *
     * @param sg If null, mm will be set to null as well.
     * @param mm
     * @return False is mm could not be activated.
     */
    boolean setActive(Song sg, MidiMix mm);


    EnumSet<SendMidiMessagePolicy> getSendMidiMessagePolicy();

    /**
     * @param sg
     * @return Null if song can be activated, otherwise a string explaining the reason why it can not be activated.
     */
    String isActivable(Song sg);

    /**
     * Listen the active song changes.
     *
     * @param l
     */
    void addPropertyListener(PropertyChangeListener l);

    void addPropertyListener(String prop, PropertyChangeListener l);

    void removePropertyListener(PropertyChangeListener l);

    void removePropertyListener(String prop, PropertyChangeListener l);

    /**
     * Send the Midi messages upon activation of a MidiMix.
     */
    void sendActivationMessages();

    /**
     * Send the midi messages to initialize all the instrument mixes of the active MidiMix.
     */
    void sendAllMidiMixMessages();

    /**
     * Send the midi messages to set the volume of all the instruments of the active MidiMix.
     */
    void sendAllMidiVolumeMessages();


    void setSendMidiMessagePolicy(EnumSet<SendMidiMessagePolicy> policy);

}

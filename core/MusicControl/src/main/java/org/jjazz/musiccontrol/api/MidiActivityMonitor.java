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
package org.jjazz.musiccontrol.api;

import java.util.ArrayList;
import java.util.logging.Logger;
import org.jjazz.midi.api.MidiConst;

/**
 * A global service to be notified of Midi notes activity for a given channel for visual purpose.
 * <p>
 * Implementation relies on MusicController and PlaybackListener.midiActivity().
 */
public class MidiActivityMonitor
{

    public interface Listener
    {

        void showMidiActivity(int channel);
    }

    private static MidiActivityMonitor INSTANCE;
    private final ListenerList[] listenerLists;
    private final MyPlaybackListener playbackListener;
    private static final Logger LOGGER = Logger.getLogger(MidiActivityMonitor.class.getSimpleName());

    public static MidiActivityMonitor getInstance()
    {
        synchronized (MidiActivityMonitor.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new MidiActivityMonitor();
            }
        }
        return INSTANCE;
    }

    private MidiActivityMonitor()
    {
        listenerLists = new ListenerList[MidiConst.CHANNEL_MAX + 1];
        playbackListener = new MyPlaybackListener();
        MusicController.getInstance().addPlaybackListener(playbackListener);
    }

    /**
     * Add a listener for the specified channels.
     *
     * @param listener
     * @param channels if empty, listen to all channels
     */
    public void addListener(Listener listener, int... channels)
    {
        for (int channel : (channels.length == 0 ? MidiConst.CHANNELS_ALL : channels))
        {
            addListenerImpl(channel, listener);
        }
    }

    /**
     * Remove listener for the specified channels.
     *
     * @param listener
     * @param channels if empty, remove listener for all channels
     *
     */
    public void removeListener(Listener listener, int... channels)
    {
        for (int channel : channels.length == 0 ? MidiConst.CHANNELS_ALL : channels)
        {
            removeListenerImpl(channel, listener);
        }
    }

    // =============================================================================================
    // Private methods
    // =============================================================================================
    private void addListenerImpl(int channel, Listener listener)
    {
        var listenerList = listenerLists[channel];
        if (listenerList == null)
        {
            listenerList = new ListenerList();
            listenerLists[channel] = listenerList;
        }
        if (!listenerList.contains(listener))
        {
            listenerList.add(listener);
        }
    }

    private void removeListenerImpl(int channel, Listener listener)
    {
        var listenerList = listenerLists[channel];
        if (listenerList == null)
        {
            return;
        }
        listenerList.remove(listener);
        if (listenerList.isEmpty())
        {
            listenerLists[channel] = null;
        }
    }
    // =============================================================================================
    // Inner classes
    // =============================================================================================

    private class MyPlaybackListener extends PlaybackListenerAdapter
    {

        @Override
        public void midiActivity(long tick, int channel)
        {
            var listenerList = listenerLists[channel];
            if (listenerList != null)
            {
                listenerList.forEach(l -> l.showMidiActivity(channel));
            }
        }
    };

    static private class ListenerList extends ArrayList<Listener>
    {

    }

}

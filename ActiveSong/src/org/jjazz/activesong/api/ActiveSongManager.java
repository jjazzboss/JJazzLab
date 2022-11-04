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
package org.jjazz.activesong.api;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.EnumSet;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.SongContextProvider;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.song.api.Song;
import org.jjazz.util.api.ResUtil;

/**
 * Manage the active song and MidiMix.
 * <p>
 * Midi messages are sent upon MidiMix changes depending on getSendMessagePolicy(). If last song is closed, active song is reset
 * to null.
 */
public class ActiveSongManager implements PropertyChangeListener, VetoableChangeListener
{

    /**
     * oldValue=MidiMix, newValue=Song
     */
    public static final String PROP_ACTIVE_SONG = "ActiveSongAndMidiMix";   //NOI18N 

    /**
     * When to send Midi Messages.
     */
    public enum SendMidiMessagePolicy
    {
        MIX_CHANGE, // Each time a MidiMix parameter is modified               
        PLAY, // Before playing music
        ACTIVATION, // Upong MidiMix activation
    }
    /**
     * By default set to all conditions
     */
    private EnumSet<SendMidiMessagePolicy> sendMidiMessagePolicy = EnumSet.allOf(SendMidiMessagePolicy.class);
    private static ActiveSongManager INSTANCE;
    private MidiMix activeMidiMix;
    private Song activeSong;
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(ActiveSongManager.class.getSimpleName());

    public static ActiveSongManager getInstance()
    {
        synchronized (ActiveSongManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ActiveSongManager();
            }
        }
        return INSTANCE;
    }

    private ActiveSongManager()
    {
        // Listen to Midi out and master volume changes
        JJazzMidiSystem.getInstance().addPropertyChangeListener(this);
        // Listen to pre-playback events
        PlaybackSettings.getInstance().addPlaybackStartVetoableListener(this);

    }

    public EnumSet<SendMidiMessagePolicy> getSendMidiMessagePolicy()
    {
        return sendMidiMessagePolicy;
    }

    public void setSendMidiMessagePolicy(EnumSet<SendMidiMessagePolicy> policy)
    {
        sendMidiMessagePolicy = policy;
    }

    /**
     * @param sg
     * @return Null if song can be activated, otherwise a string explaining the reason why it can not be activated.
     */
    public String isActivable(Song sg)
    {
        if (sg == null)
        {
            throw new NullPointerException("sg");
        }
        String err = null;
        MusicController mc = MusicController.getInstance();
        PlaybackSession session = mc.getPlaybackSession();
        SongContext sgContext = session instanceof SongContextProvider ? ((SongContextProvider) session).getSongContext() : null;
        if (mc.getState() == MusicController.State.PLAYING && (sgContext == null || sg != sgContext.getSong()))
        {
            err = ResUtil.getString(getClass(), "ErrSongIsPlaying");
        }
        return err;
    }

    /**
     * Set the specified song and MidiMix as active: <br>
     * - send MidiMessages for all MidiMix parameters at activation <br>
     * - listen to MidiMix changes and send the related Midi messages according to the SendPolicy <br>
     * - Fire a PROP_ACTIVE_SONG change event (oldValue=mm, newValue=sg) <br>
     *
     * @param sg If null, mm will be set to null as well.
     * @param mm
     * @return False is mm could not be activated.
     */
    public boolean setActive(Song sg, MidiMix mm)
    {
        if (sg != null && mm == null)
        {
            throw new IllegalArgumentException("sg=" + sg + " mm=" + mm);   //NOI18N
        }
        if (activeSong == sg)
        {
            return true;
        }
        if (sg != null && isActivable(sg) != null)
        {
            return false;
        }
        if (activeMidiMix != null)
        {
            unregisterMidiMix(activeMidiMix);
        }
        if (activeSong != null)
        {
            activeSong.removePropertyChangeListener(this);
        }
        activeSong = sg;
        activeMidiMix = (sg == null) ? null : mm;
        if (activeMidiMix != null)
        {
            registerMidiMix(activeMidiMix);              // Listen to added/replace/removed instruments, or settings changes
            activeSong.addPropertyChangeListener(this);  // Listen to close song events
            if (sendMidiMessagePolicy.contains(SendMidiMessagePolicy.ACTIVATION))
            {
                sendActivationMessages();
                sendAllMidiMixMessages();
            }
        }
        pcs.firePropertyChange(PROP_ACTIVE_SONG, activeMidiMix, activeSong);
        return true;
    }

    public MidiMix getActiveMidiMix()
    {
        return activeMidiMix;
    }

    public Song getActiveSong()
    {
        return activeSong;
    }

    /**
     * Send the midi messages to initialize all the instrument mixes of the active MidiMix.
     */
    public void sendAllMidiMixMessages()
    {
        LOGGER.fine("sendAllMidiMixMessages()");   //NOI18N
        if (activeMidiMix != null)
        {
            for (Integer channel : activeMidiMix.getUsedChannels())
            {
                InstrumentMix insMix = activeMidiMix.getInstrumentMixFromChannel(channel);
                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
                jms.sendMidiMessagesOnJJazzMidiOut(insMix.getAllMidiMessages(channel));
            }
        }
    }

    /**
     * Send the midi messages to set the volume of all the instruments of the active MidiMix.
     */
    public void sendAllMidiVolumeMessages()
    {
        LOGGER.fine("sendAllMidiVolumeMessages()");   //NOI18N
        if (activeMidiMix != null)
        {
            for (Integer channel : activeMidiMix.getUsedChannels())
            {
                InstrumentMix insMix = activeMidiMix.getInstrumentMixFromChannel(channel);
                InstrumentSettings insSet = insMix.getSettings();
                JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(insSet.getVolumeMidiMessages(channel));
            }
        }
    }

    /**
     * Send the Midi messages upon activation of a MidiMix.
     */
    public void sendActivationMessages()
    {
        // nothing
    }

    public void addPropertyListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ----------------------------------------------------------------------------
    // VetoableChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException
    {
        if (evt.getSource() == PlaybackSettings.getInstance())
        {
            if (evt.getPropertyName().equals(PlaybackSettings.PROP_VETO_PRE_PLAYBACK))
            {
                if (sendMidiMessagePolicy.contains(SendMidiMessagePolicy.PLAY))
                {
                    OutputSynthManager.getInstance().getDefaultOutputSynth().getUserSettings().sendModeOnUponPlaySysexMessages();
                    sendAllMidiMixMessages();
                }
            }
        }
    }

    // ----------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.fine("propertyChange() -- evt=" + evt);   //NOI18N
        if (evt.getSource() == activeMidiMix)
        {
            MidiMix mm = (MidiMix) evt.getSource();
            if (evt.getPropertyName().equals(MidiMix.PROP_CHANNEL_INSTRUMENT_MIX))
            {
                // New , replaced or removed InstrumentMix
                int channel = (int) evt.getNewValue();
                InstrumentMix oldInsMix = (InstrumentMix) evt.getOldValue();
                if (oldInsMix != null)
                {
                    // oldInsMix removed, unregister
                    oldInsMix.removePropertyChangeListener(this);
                    oldInsMix.getSettings().removePropertyChangeListener(this);
                }
                InstrumentMix insMix = mm.getInstrumentMixFromChannel(channel);
                if (insMix != null)
                {
                    // insMix added (new or replacing oldInsMix)                    
                    insMix.addPropertyChangeListener(this);
                    insMix.getSettings().addPropertyChangeListener(this);
                    if (sendMidiMessagePolicy.contains(SendMidiMessagePolicy.MIX_CHANGE))
                    {
                        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
                        jms.sendMidiMessagesOnJJazzMidiOut(insMix.getAllMidiMessages(channel));

                    }
                } else
                {
                    // oldInsMix removed but nothing replaced it, nothing to do
                }
            }
            return;
        } else if (evt.getSource() == activeSong)
        {
            if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                setActive(null, null);
            }
        } else if (evt.getSource() == JJazzMidiSystem.getInstance())
        {
            if (evt.getPropertyName().equals(JJazzMidiSystem.PROP_MIDI_OUT))
            {
                // Midi Out has changed, resend init messages on the new Midi device        
                sendActivationMessages();
                sendAllMidiMixMessages();
            } else if (evt.getPropertyName().equals(JJazzMidiSystem.PROP_MASTER_VOL_FACTOR))
            {
                // Master volume has changed, resend volume messages
                sendAllMidiVolumeMessages();
            } else if (evt.getPropertyName().equals(JJazzMidiSystem.PROP_MIDI_OUT_FILTERING))
            {
                // If Midi filtering switched back to OFF, make sure to resend the settings
                // in case the user has modified volume etc. during filtering was ON.
                boolean b = (boolean) evt.getNewValue();
                if (!b)
                {
                    sendAllMidiMixMessages();
                }
            }
        }

        if (!sendMidiMessagePolicy.contains(SendMidiMessagePolicy.MIX_CHANGE))
        {
            // No need to listen to instrument or settings changes as handled below
            return;
        }

        if (evt.getSource() instanceof InstrumentMix)
        {
            InstrumentMix insMix = (InstrumentMix) evt.getSource();
            int channel = activeMidiMix.getChannel(insMix);
            if (evt.getPropertyName() == InstrumentMix.PROP_INSTRUMENT || evt.getPropertyName() == InstrumentMix.PROP_INSTRUMENT_ENABLED)
            {
                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
                jms.sendMidiMessagesOnJJazzMidiOut(insMix.getInstrumentMidiMessages(channel));
            }
        } else if (evt.getSource() instanceof InstrumentSettings)
        {
            InstrumentSettings insSet = (InstrumentSettings) evt.getSource();
            int channel = activeMidiMix.getChannel(insSet.getContainer());
            if (null != evt.getPropertyName())
            {
                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
                switch (evt.getPropertyName())
                {
                    case InstrumentSettings.PROPERTY_CHORUS:
                    case InstrumentSettings.PROPERTY_CHORUS_ENABLED:
                        jms.sendMidiMessagesOnJJazzMidiOut(insSet.getChorusMidiMessages(channel));
                        break;
                    case InstrumentSettings.PROPERTY_REVERB:
                    case InstrumentSettings.PROPERTY_REVERB_ENABLED:
                        jms.sendMidiMessagesOnJJazzMidiOut(insSet.getReverbMidiMessages(channel));
                        break;
                    case InstrumentSettings.PROPERTY_VOLUME:
                    case InstrumentSettings.PROPERTY_VOLUME_ENABLED:
                        jms.sendMidiMessagesOnJJazzMidiOut(insSet.getVolumeMidiMessages(channel));
                        break;
                    case InstrumentSettings.PROPERTY_PANORAMIC:
                    case InstrumentSettings.PROPERTY_PANORAMIC_ENABLED:
                        jms.sendMidiMessagesOnJJazzMidiOut(insSet.getPanoramicMidiMessages(channel));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // ----------------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------------
    /**
     * Register the specified MidiMix and all its InstrumentMix and related Settings.
     *
     * @param mm
     */
    private void registerMidiMix(MidiMix mm)
    {
        mm.addPropertyChangeListener(this);
        for (Integer channel : mm.getUsedChannels())
        {
            InstrumentMix insMix = mm.getInstrumentMixFromChannel(channel);
            insMix.addPropertyChangeListener(this);
            insMix.getSettings().addPropertyChangeListener(this);
        }
    }

    private void unregisterMidiMix(MidiMix mm)
    {
        mm.removePropertyChangeListener(this);
        for (Integer channel : mm.getUsedChannels())
        {
            InstrumentMix insMix = mm.getInstrumentMixFromChannel(channel);
            insMix.removePropertyChangeListener(this);
            insMix.getSettings().removePropertyChangeListener(this);
        }
    }
}

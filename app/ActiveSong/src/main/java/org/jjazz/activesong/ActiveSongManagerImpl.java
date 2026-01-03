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
package org.jjazz.activesong;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.SongContextProvider;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.Exceptions;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession.Context;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.outputsynth.spi.OutputSynthManager;

@ServiceProvider(service = ActiveSongManager.class)
public class ActiveSongManagerImpl implements PropertyChangeListener, ActiveSongManager
{

    /**
     * By default set to all conditions
     */
    private EnumSet<SendMidiMessagePolicy> sendMidiMessagePolicy = EnumSet.allOf(SendMidiMessagePolicy.class);
    private MidiMix activeMidiMix;
    private Song activeSong;
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(ActiveSongManager.class.getSimpleName());


    public ActiveSongManagerImpl()
    {
        // Listen to master volume changes
        JJazzMidiSystem.getInstance().addPropertyChangeListener(this);

        // Listen to OutputSynth changes
        OutputSynthManager.getDefault().addPropertyChangeListener(this);

        // Listen to pre-playback events
        MusicController.getInstance().addPropertyChangeListener(this);

        LOGGER.info("ActiveSongManagerImpl() Started");

    }

    @Override
    public EnumSet<SendMidiMessagePolicy> getSendMidiMessagePolicy()
    {
        return sendMidiMessagePolicy;
    }

    @Override
    public void setSendMidiMessagePolicy(EnumSet<SendMidiMessagePolicy> policy)
    {
        sendMidiMessagePolicy = policy;
    }

    /**
     * @param sg
     * @return Null if song can be activated, otherwise a string explaining the reason why it can not be activated.
     */
    @Override
    public String isActivable(Song sg)
    {
        Objects.requireNonNull(sg);
        String err = null;
        MusicController mc = MusicController.getInstance();
        PlaybackSession session = mc.getPlaybackSession();
        SongContext sgContext = session instanceof SongContextProvider scp ? scp.getSongContext() : null;
        if (mc.isPlaying() && (sgContext == null || sg != sgContext.getSong()))
        {
            err = ResUtil.getString(getClass(), "ErrSongIsPlaying");
        }
        return err;
    }

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
    @Override
    public boolean setActive(Song sg, MidiMix mm)
    {
        if (sg != null && mm == null)
        {
            throw new IllegalArgumentException("sg=" + sg + " mm=" + mm);
        }

        synchronized (this)
        {
            if (activeSong == sg)
            {
                return true;
            }
            String err;
            if (sg != null && (err = isActivable(sg)) != null)
            {
                LOGGER.log(Level.WARNING, "setActive() sg={0} is not activable: {1}", new Object[]
                {
                    sg, err
                });
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

            // Change state
            activeSong = sg;
            activeMidiMix = (sg == null) ? null : mm;


            // Reset MusicController
            var mc = MusicController.getInstance();
            mc.stop();
            try
            {
                mc.setPlaybackSession(null, false);
            } catch (MusicGenerationException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
            }


            // Register
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
        }
        
        pcs.firePropertyChange(PROP_ACTIVE_SONG, activeMidiMix, activeSong);
        return true;
    }


    @Override
    public synchronized MidiMix getActiveMidiMix()
    {
        return activeMidiMix;
    }

    @Override
    public synchronized Song getActiveSong()
    {
        return activeSong;
    }

    /**
     * Send the midi messages to initialize all the instrument mixes of the active MidiMix.
     */
    @Override
    public void sendAllMidiMixMessages()
    {
        LOGGER.fine("sendAllMidiMixMessages()");
        if (activeMidiMix != null)
        {
            activeMidiMix.sendAllMidiMixMessages();
        }
    }

    /**
     * Send the midi messages to set the volume of all the instruments of the active MidiMix.
     */
    @Override
    public void sendAllMidiVolumeMessages()
    {
        LOGGER.fine("sendAllMidiVolumeMessages()");
        if (activeMidiMix != null)
        {
            activeMidiMix.sendAllMidiVolumeMessages();
        }
    }

    /**
     * Send the Midi messages upon activation of a MidiMix.
     */
    @Override
    public void sendActivationMessages()
    {
        // nothing
    }

    @Override
    public void addPropertyListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    public void addPropertyListener(String prop, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(prop, l);
    }

    @Override
    public void removePropertyListener(String prop, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(prop, l);
    }

    // ----------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "propertyChange() -- evt={0}", evt);
        String propName = evt.getPropertyName();


        if (evt.getSource() == activeMidiMix)
        {
            MidiMix mm = (MidiMix) evt.getSource();
            switch (propName)
            {
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX ->
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
                    InstrumentMix insMix = mm.getInstrumentMix(channel);
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
                case MidiMix.PROP_RHYTHM_VOICE_CHANNEL ->
                {
                    if (sendMidiMessagePolicy.contains(SendMidiMessagePolicy.MIX_CHANGE))
                    {
                        int channel = (Integer) evt.getNewValue();
                        InstrumentMix insMix = mm.getInstrumentMix(channel);
                        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
                        jms.sendMidiMessagesOnJJazzMidiOut(insMix.getAllMidiMessages(channel));
                    }
                }
                default ->
                {
                    // Nothing
                }
            }
            return;

        } else if (evt.getSource() == activeSong)
        {
            if (propName.equals(Song.PROP_CLOSED))
            {
                setActive(null, null);
            }

        } else if (evt.getSource() == JJazzMidiSystem.getInstance())
        {
            switch (propName)
            {
                case JJazzMidiSystem.PROP_MASTER_VOL_FACTOR ->
                {
                    sendAllMidiVolumeMessages();  // Master volume has changed, resend volume messages
                }
                case JJazzMidiSystem.PROP_MIDI_OUT_FILTERING ->
                {
                    // If Midi filtering switched back to OFF, make sure to resend the settings
                    // in case the user has modified volume etc. during filtering was ON.
                    boolean b = (boolean) evt.getNewValue();
                    if (!b)
                    {
                        sendAllMidiMixMessages();
                    }
                }
                default ->
                {
                    // Nothing
                }
            }

        } else if (evt.getSource() == OutputSynthManager.getDefault())
        {
            if (propName.equals(OutputSynthManager.PROP_DEFAULT_OUTPUTSYNTH))
            {
                // OutputSynth has changed, resend init messages on the new Midi device        
                sendActivationMessages();
                sendAllMidiMixMessages();
            }

        } else if (evt.getSource() == MusicController.getInstance())
        {
            if (propName.equals(MusicController.PROP_PLAYBACK_SESSION))
            {
                // A new PlaybackSession was set, make sure instruments are set
                boolean b = needsInstrumentsReset((PlaybackSession) evt.getNewValue());
                if (sendMidiMessagePolicy.contains(SendMidiMessagePolicy.PLAY) && b)
                {
                    OutputSynthManager.getDefault().getDefaultOutputSynth().getUserSettings().sendModeOnUponPlaySysexMessages();
                    sendAllMidiMixMessages();
                }
            }
        }


        if (!sendMidiMessagePolicy.contains(SendMidiMessagePolicy.MIX_CHANGE))
        {
            // No need to listen to instrument or settings changes as handled below
            return;
        }

        if (evt.getSource() instanceof InstrumentMix insMix)
        {
            int channel = activeMidiMix.getChannel(insMix);
            if (propName.equals(InstrumentMix.PROP_INSTRUMENT) || propName.equals(
                    InstrumentMix.PROP_INSTRUMENT_ENABLED))
            {
                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
                jms.sendMidiMessagesOnJJazzMidiOut(insMix.getInstrumentMidiMessages(channel));
            }
        } else if (evt.getSource() instanceof InstrumentSettings insSet)
        {
            int channel = activeMidiMix.getChannel(insSet.getContainer());
            if (null != propName)
            {
                JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
                switch (propName)
                {
                    case InstrumentSettings.PROPERTY_CHORUS, InstrumentSettings.PROPERTY_CHORUS_ENABLED ->
                        jms.sendMidiMessagesOnJJazzMidiOut(insSet.getChorusMidiMessages(channel));
                    case InstrumentSettings.PROPERTY_REVERB, InstrumentSettings.PROPERTY_REVERB_ENABLED ->
                        jms.sendMidiMessagesOnJJazzMidiOut(insSet.getReverbMidiMessages(channel));
                    case InstrumentSettings.PROPERTY_VOLUME, InstrumentSettings.PROPERTY_VOLUME_ENABLED ->
                        jms.sendMidiMessagesOnJJazzMidiOut(insSet.getVolumeMidiMessages(channel));
                    case InstrumentSettings.PROPERTY_PANORAMIC, InstrumentSettings.PROPERTY_PANORAMIC_ENABLED ->
                        jms.sendMidiMessagesOnJJazzMidiOut(insSet.getPanoramicMidiMessages(channel));
                    default ->
                    {
                    }
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
            InstrumentMix insMix = mm.getInstrumentMix(channel);
            insMix.addPropertyChangeListener(this);
            insMix.getSettings().addPropertyChangeListener(this);
        }
    }

    private void unregisterMidiMix(MidiMix mm)
    {
        mm.removePropertyChangeListener(this);
        for (Integer channel : mm.getUsedChannels())
        {
            InstrumentMix insMix = mm.getInstrumentMix(channel);
            insMix.removePropertyChangeListener(this);
            insMix.getSettings().removePropertyChangeListener(this);
        }
    }

    private boolean needsInstrumentsReset(PlaybackSession session)
    {
        boolean b = false;
        if (session != null)
        {
            b = session instanceof SongContextProvider
                    && EnumSet.of(Context.SONG, Context.ARRANGER, Context.RHYTHM_PREVIEW).contains(session.getContext());
        }
        return b;
    }

}

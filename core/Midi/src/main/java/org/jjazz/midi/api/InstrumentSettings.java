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
package org.jjazz.midi.api;

import com.thoughtworks.xstream.XStream;
import java.beans.PropertyChangeListener;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * The variables which impact the way an Instrument is rendered.
 */
public class InstrumentSettings implements Serializable
{

    public final static String PROPERTY_TRANSPOSITION = "Transposition";
    public final static String PROPERTY_VELOCITY_SHIFT = "VelocityShift";
    public final static String PROPERTY_VOLUME = "Volume";
    public final static String PROPERTY_PANORAMIC = "Panoramic";
    public final static String PROPERTY_REVERB = "Reverb";
    public final static String PROPERTY_CHORUS = "Chorus";
    public final static String PROPERTY_VOLUME_ENABLED = "VolumeEnabled";
    public final static String PROPERTY_PANORAMIC_ENABLED = "PanoramicEnabled";
    public final static String PROPERTY_REVERB_ENABLED = "ReverbEnabled";
    public final static String PROPERTY_CHORUS_ENABLED = "ChorusEnabled";

    private int transposition;
    private int velocityShift;
    private int volume;
    private int panoramic;
    private int reverb;
    private int chorus;
    private boolean panoramicEnabled = true;
    private boolean reverbEnabled = true;
    private boolean chorusEnabled = true;
    private boolean volumeEnabled = true;
    private transient InstrumentMix container;

    private transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(InstrumentSettings.class.getSimpleName());

    /**
     * Use default values.
     * <p>
     * Pan, reverb and chorus are enabled by default. see MidiConst file.
     */
    public InstrumentSettings()
    {
        this(MidiConst.VOLUME_STD, 0, MidiConst.PANORAMIC_STD, MidiConst.REVERB_STD, MidiConst.CHORUS_STD, 0);
    }

    /**
     *
     * @param vol      Volume (0-127)
     * @param t        Transposition (-24 to +24)
     * @param pan      Panoramic (0-127)
     * @param rev      Reverb effect send (0-127)
     * @param cho      Chorus effect send (0-127)
     * @param velShift Velocity shift (-40 to +40)
     */
    public InstrumentSettings(int vol, int t, int pan, int rev, int cho, int velShift)
    {
        setPanoramic(pan);
        setReverb(rev);
        setChorus(cho);
        setVolume(vol);
        setTransposition(t);
        this.setVelocityShift(velShift);
    }

    public InstrumentSettings(InstrumentSettings is)
    {
        this();
        set(is);
    }

    public void set(InstrumentSettings is)
    {
        setReverbEnabled(is.reverbEnabled);
        setChorusEnabled(is.chorusEnabled);
        setPanoramicEnabled(is.panoramicEnabled);
        setVolumeEnabled(is.volumeEnabled);
        setPanoramic(is.panoramic);
        setReverb(is.reverb);
        setChorus(is.chorus);
        setVolume(is.volume);
        setTransposition(is.transposition);
        setVelocityShift(is.velocityShift);
    }

    public void setContainer(InstrumentMix insMix)
    {
        container = insMix;
    }

    public InstrumentMix getContainer()
    {
        return container;
    }

    /**
     * Get all the MidiMessages for enabled parameters.
     *
     * @param channel
     * @return
     */
    public MidiMessage[] getAllMidiMessages(int channel)
    {
        ArrayList<MidiMessage> res = new ArrayList<>();
        Collections.addAll(res, getVolumeMidiMessages(channel));
        Collections.addAll(res, getPanoramicMidiMessages(channel));
        Collections.addAll(res, getReverbMidiMessages(channel));
        Collections.addAll(res, getChorusMidiMessages(channel));
        return res.toArray(new MidiMessage[0]);
    }

    /**
     * Get the Midi messages to be sent to initialize this parameter.
     * <p>
     * If parameter is not enabled return an empty array.
     *
     * @param channel
     * @return
     */
    public MidiMessage[] getVolumeMidiMessages(int channel)
    {
        List<MidiMessage> res = new ArrayList<>();
        if (isVolumeEnabled())
        {
            res.add(MidiUtilities.getVolumeMessage(channel, getVolume()));
        }
        return res.toArray(new MidiMessage[0]);
    }

    /**
     * Get the Midi messages to be sent to initialize this parameter.
     * <p>
     * If parameter is not enabled return an empty array.
     *
     * @param channel
     * @return
     */
    public MidiMessage[] getPanoramicMidiMessages(int channel)
    {
        List<MidiMessage> res = new ArrayList<>();
        if (isPanoramicEnabled())
        {
            res.add(MidiUtilities.getPanoramicMessage(channel, getPanoramic()));
        }
        return res.toArray(new MidiMessage[0]);
    }

    /**
     * Get the Midi messages to be sent to initialize this parameter.
     * <p>
     * If parameter is not enabled return an empty array.
     *
     * @param channel
     * @return
     */
    public MidiMessage[] getReverbMidiMessages(int channel)
    {
        List<MidiMessage> res = new ArrayList<>();
        if (isReverbEnabled())
        {
            res.add(MidiUtilities.getReverbMessage(channel, getReverb()));
        }
        return res.toArray(new MidiMessage[0]);
    }

    /**
     * Get the Midi messages to be sent to initialize this parameter.
     * <p>
     * If parameter is not enabled return an empty array.
     *
     * @param channel
     * @return
     */
    public MidiMessage[] getChorusMidiMessages(int channel)
    {
        List<MidiMessage> res = new ArrayList<>();
        if (isChorusEnabled())
        {
            res.add(MidiUtilities.getChorusMessage(channel, getChorus()));
        }
        return res.toArray(new MidiMessage[0]);
    }

    public int getVolume()
    {
        return volume;
    }

    public final void setVolume(int v)
    {
        if (!MidiConst.check(v))
        {
            throw new IllegalArgumentException("v=" + v);
        }
        if (volume != v)
        {
            int old = volume;
            volume = v;
            pcs.firePropertyChange(PROPERTY_VOLUME, old, volume);
        }
    }

    public int getPanoramic()
    {
        return panoramic;
    }

    public final void setPanoramic(int v)
    {
        if (!MidiConst.check(v))
        {
            throw new IllegalArgumentException("v=" + v);
        }
        if (panoramic != v)
        {
            int old = panoramic;
            panoramic = v;
            pcs.firePropertyChange(PROPERTY_PANORAMIC, old, panoramic);
        }
    }

    public int getReverb()
    {
        return reverb;
    }

    public final void setReverb(int v)
    {
        if (!MidiConst.check(v))
        {
            throw new IllegalArgumentException("v=" + v);
        }
        if (reverb != v)
        {
            int old = reverb;
            reverb = v;
            pcs.firePropertyChange(PROPERTY_REVERB, old, reverb);
        }
    }

    public int getChorus()
    {
        return chorus;
    }

    public final void setChorus(int v)
    {
        if (!MidiConst.check(v))
        {
            throw new IllegalArgumentException("v=" + v);
        }
        if (chorus != v)
        {
            int old = chorus;
            chorus = v;
            pcs.firePropertyChange(PROPERTY_CHORUS, old, chorus);
        }
    }

    public int getVelocityShift()
    {
        return velocityShift;
    }

    /**
     * Set the Midi velocity shift value.
     * <p>
     * Default is 0.
     *
     * @param v [-64;+64]
     */
    public final void setVelocityShift(int v)
    {
        if (v < -64 || v > 64)
        {
            throw new IllegalArgumentException("v=" + v);
        }
        if (velocityShift != v)
        {
            int save = velocityShift;
            velocityShift = v;
            pcs.firePropertyChange(PROPERTY_VELOCITY_SHIFT, save, velocityShift);
        }
    }

    public int getTransposition()
    {
        return transposition;
    }

    /**
     * Set the transposition value in semitones.
     *
     * @param t [-36; +36]
     */
    public final void setTransposition(int t)
    {
        if (t < -36 || t > 36)
        {
            throw new IllegalArgumentException("t=" + t);
        }
        if (transposition != t)
        {
            int save = transposition;
            transposition = t;
            pcs.firePropertyChange(PROPERTY_TRANSPOSITION, save, transposition);
        }
    }

    public boolean isVolumeEnabled()
    {
        return volumeEnabled;
    }

    /**
     * Enable or disable the volume setting.
     */
    public final void setVolumeEnabled(boolean b)
    {
        if (volumeEnabled != b)
        {
            volumeEnabled = b;
            pcs.firePropertyChange(PROPERTY_VOLUME_ENABLED, !volumeEnabled, volumeEnabled);
        }
    }

    public boolean isPanoramicEnabled()
    {
        return panoramicEnabled;
    }

    /**
     * Enable or disable the panoramic setting.
     */
    public final void setPanoramicEnabled(boolean b)
    {
        if (panoramicEnabled != b)
        {
            panoramicEnabled = b;
            pcs.firePropertyChange(PROPERTY_PANORAMIC_ENABLED, !panoramicEnabled, panoramicEnabled);
        }
    }

    public boolean isReverbEnabled()
    {
        return reverbEnabled;
    }

    /**
     * Enable or disable the reverb setting.
     */
    public final void setReverbEnabled(boolean b)
    {
        if (reverbEnabled != b)
        {
            reverbEnabled = b;
            pcs.firePropertyChange(PROPERTY_REVERB_ENABLED, !reverbEnabled, reverbEnabled);
        }
    }

    public boolean isChorusEnabled()
    {
        return chorusEnabled;
    }

    /**
     * Enable or disable the chorus setting.
     */
    public final void setChorusEnabled(boolean b)
    {
        if (chorusEnabled != b)
        {
            chorusEnabled = b;
            pcs.firePropertyChange(PROPERTY_CHORUS_ENABLED, !chorusEnabled, chorusEnabled);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    public String toString()
    {
        return "(v=" + volume + " t=" + transposition + " r=" + reverb + " c=" + chorus + " p=" + panoramic + ")";
    }

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {
        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    // Nothing
                }

                case MIDIMIX_LOAD, MIDIMIX_SAVE ->
                {
                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files                    
                    xstream.alias("InstrumentSettings", InstrumentSettings.class);
                    xstream.alias("InstrumentSettingsSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spVolume");
                    xstream.useAttributeFor(SerializationProxy.class, "spTransposition");
                    xstream.useAttributeFor(SerializationProxy.class, "spVelocityShift");
                }
                default -> throw new AssertionError(instanceId.name());
            }
        }
    }
    // --------------------------------------------------------------------- 
    // Serialization
    // ---------------------------------------------------------------------
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Serialization proxy.
     * 
     * spVERSION 2 introduces new XStream aliases (see XStreamConfig)
     */
    private static class SerializationProxy implements Serializable
    {
        private static final long serialVersionUID = -297226301726L;
        private int spVERSION = 2;      // Do not make final!
        private int spTransposition;
        private int spVelocityShift;
        private int spVolume;
        private int spPanoramic;
        private int spReverb;
        private int spChorus;
        private boolean spPanoramicEnabled;
        private boolean spReverbEnabled;
        private boolean spChorusEnabled;
        private boolean spVolumeEnabled;

        private SerializationProxy(InstrumentSettings is)
        {
            spTransposition = is.transposition;
            spVelocityShift = is.velocityShift;
            spVolume = is.volume;
            spPanoramic = is.panoramic;
            spReverb = is.reverb;
            spChorus = is.chorus;
            spPanoramicEnabled = is.panoramicEnabled;
            spReverbEnabled = is.reverbEnabled;
            spChorusEnabled = is.chorusEnabled;
            spVolumeEnabled = is.volumeEnabled;
        }

        private Object readResolve() throws ObjectStreamException
        {
            InstrumentSettings is = new InstrumentSettings(spVolume, spTransposition, spPanoramic, spReverb, spChorus, spVelocityShift);
            is.setPanoramicEnabled(spPanoramicEnabled);
            is.setReverbEnabled(spReverbEnabled);
            is.setChorusEnabled(spChorusEnabled);
            is.setVolumeEnabled(spVolumeEnabled);
            return is;
        }
    }
}

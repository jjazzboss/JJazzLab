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
import javax.sound.midi.MidiMessage;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * An Instrument with its InstrumentSettings.
 * <p>
 * Manage the Mute/Solo status: Mute ON turns Solo OFF and inversely.
 */
public class InstrumentMix implements Serializable
{

    public static final String PROP_INSTRUMENT = "PropInstrument";
    public static final String PROP_INSTRUMENT_ENABLED = "PropInstrumentEnabled";
    public static final String PROP_MUTE = "PropMute";
    public static final String PROP_SOLO = "PropSolo";

    private Instrument instrument;
    private InstrumentSettings settings;
    private boolean instrumentEnabled = true;
    private boolean mute = false;
    private transient boolean solo = false;
    private transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    public InstrumentMix(Instrument instrument, InstrumentSettings settings)
    {
        setInstrument(instrument);
        if (settings == null)
        {
            throw new NullPointerException("instrument=" + instrument + " settings=" + settings);
        }
        this.settings = settings;
        this.settings.setContainer(this);
    }

    /**
     * Create a copy of specified InstrumentMix.
     * <p>
     * Instrument is directly reused. InstrumentSettings are deeply copied.
     *
     * @param im
     */
    public InstrumentMix(InstrumentMix im)
    {
        if (im == null)
        {
            throw new NullPointerException("im");
        }
        setMute(im.isMute());
        setSolo(im.isSolo());
        setInstrument(im.getInstrument());
        setInstrumentEnabled(im.isInstrumentEnabled());
        this.settings = new InstrumentSettings(im.getSettings());
        this.settings.setContainer(this);
    }

    /**
     * Get the instrument.
     *
     * @return Can't be null.
     */
    public Instrument getInstrument()
    {
        return instrument;
    }

    /**
     * Set the instrument.
     *
     * @param instrument Can't be null.
     */
    public final void setInstrument(Instrument instrument)
    {
        if (instrument == null)
        {
            throw new NullPointerException("instrument");
        }
        Instrument old = this.instrument;
        this.instrument = instrument;
        pcs.firePropertyChange(PROP_INSTRUMENT, old, this.instrument);
    }

    /**
     * @return the settings
     */
    public InstrumentSettings getSettings()
    {
        return settings;
    }

    /**
     * Enable or disable the instrument setting.
     */
    public final void setInstrumentEnabled(boolean b)
    {
        if (instrumentEnabled != b)
        {
            instrumentEnabled = b;
            pcs.firePropertyChange(PROP_INSTRUMENT_ENABLED, !instrumentEnabled, instrumentEnabled);
        }
    }

    public boolean isInstrumentEnabled()
    {
        return instrumentEnabled;
    }

    /**
     * @param mute If true also switch off the Solo status
     */
    public final void setMute(boolean mute)
    {
        boolean old = this.mute;
        this.mute = mute;
        pcs.firePropertyChange(PROP_MUTE, old, this.mute);
        if (this.mute)
        {
            setSolo(false);
        }
    }

    /**
     * Get all the midi messages to initialize this InstrumentMix: bank/program change, volume, reverb, chorus, pan.
     * <p>
     * No message returned for disabled parameters.
     *
     * @param channel
     * @return
     */
    public MidiMessage[] getAllMidiMessages(int channel)
    {
        List<MidiMessage> mms = new ArrayList<>();
        Collections.addAll(mms, getInstrumentMidiMessages(channel));
        Collections.addAll(mms, settings.getVolumeMidiMessages(channel));
        Collections.addAll(mms, settings.getReverbMidiMessages(channel));
        Collections.addAll(mms, settings.getChorusMidiMessages(channel));
        Collections.addAll(mms, settings.getPanoramicMidiMessages(channel));
        return mms.toArray(new MidiMessage[0]);
    }

    /**
     * Get only the Midi messages to be sent to initialize the instrument (patch/bank changes).
     * <p>
     * If instrument is not enabled return an empty array.
     *
     * @param channel
     * @return
     */
    public MidiMessage[] getInstrumentMidiMessages(int channel)
    {
        List<MidiMessage> res = new ArrayList<>();
        if (isInstrumentEnabled())
        {
            Collections.addAll(res, instrument.getMidiMessages(channel));
        }
        return res.toArray(new MidiMessage[0]);
    }

    /**
     * @return the solo
     */
    public boolean isSolo()
    {
        return solo;
    }

    /**
     * @param solo If true also switch off the Mute status
     */
    public final void setSolo(boolean solo)
    {
        boolean old = this.solo;
        this.solo = solo;
        pcs.firePropertyChange(PROP_SOLO, old, this.solo);
        if (this.solo)
        {
            setMute(false);
        }
    }

    @Override
    public String toString()
    {
        return "[ins=" + instrument.getPatchName() + ", settings=" + settings + "]";
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    /**
     * @return the mute
     */
    public boolean isMute()
    {
        return mute;
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
                    xstream.alias("InstrumentMix", InstrumentMix.class);
                    xstream.alias("InstrumentMixSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spInsEnabled");
                    xstream.useAttributeFor(SerializationProxy.class, "spMute");
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
     * Need to reassign the InstrumentSettings container.
     * <p>
     * spVERSION 2 introduces new XStream aliases (see XStreamConfig)
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -201729371001L;
        private int spVERSION = 2;      // Do not make final!
        private Instrument spIns;
        private InstrumentSettings spInsSettings;
        private boolean spInsEnabled;
        private boolean spMute;

        public SerializationProxy(InstrumentMix insMix)
        {
            spIns = insMix.getInstrument();
            spInsSettings = insMix.getSettings();
            spInsEnabled = insMix.isInstrumentEnabled();
            spMute = insMix.isMute();
        }

        private Object readResolve() throws ObjectStreamException
        {
            InstrumentMix insMix = new InstrumentMix(spIns, spInsSettings);
            insMix.setInstrumentEnabled(spInsEnabled);
            insMix.setMute(spMute);
            return insMix;
        }
    }
}

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
package org.jjazz.midi.api.synths;

import com.thoughtworks.xstream.XStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import javax.sound.midi.MidiMessage;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * A special "void" instrument: no bank change or program change is associated to this instrument.
 * <p>
 * When used, the system should not send any Midi bank select or program change messages for this instrument.
 */
public class VoidInstrument extends GM1Instrument implements Serializable
{

    private static VoidInstrument INSTANCE;
    private InstrumentBank<?> myBank;

    /**
     * Should be only called via a NotSetBank: this way the bank/synth are correctly set.
     *
     * @return
     */
    static protected VoidInstrument getInstance()
    {
        synchronized (VoidInstrument.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new VoidInstrument(0, "!Not Set!");
            }
        }
        return INSTANCE;
    }

    private VoidInstrument(int programChange, String patchName)
    {
        super(programChange, patchName, InstrumentFamily.Piano);
    }

    @Override
    public void setBank(InstrumentBank<?> bank)
    {
        this.myBank = bank;
    }

    @Override
    public InstrumentBank<?> getBank()
    {
        return this.myBank;
    }

    @Override
    public String getFullName()
    {
        return "Void Instrument";
    }

    /**
     * Overridden : return an empty array.
     *
     * @return
     */
    @Override
    public MidiMessage[] getMidiMessages(int channel)
    {
        return new MidiMessage[0];
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
                    xstream.alias("VoidInstrument", VoidInstrument.class);
                    xstream.alias("VoidInstrumentSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spProgChange");
                }
                default -> throw new AssertionError(instanceId.name());
            }
        }
    }
    /* ---------------------------------------------------------------------
     * Serialization
     * --------------------------------------------------------------------- */
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Special serialization process to not use the default GM1Instrument one.
     * 
     * spVERSION 2 introduces alias XStreamConfig
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -82099017429816L;
        private int spVERSION = 2;      // Do not make final!
        int spProgChange;

        private SerializationProxy(VoidInstrument ins)
        {
            spProgChange = ins.getMidiAddress().getProgramChange();       // Just to save something but useless...
        }

        private Object readResolve() throws ObjectStreamException
        {
            return NotSetBank.getInstance().getVoidInstrument();
        }
    }

}

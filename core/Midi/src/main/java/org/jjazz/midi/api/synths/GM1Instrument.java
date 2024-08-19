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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * An Instrument from the GM1 bank.
 */
public class GM1Instrument extends Instrument implements Serializable
{

    private static final Logger LOGGER = Logger.getLogger(GM1Instrument.class.getSimpleName());
    private final InstrumentFamily family;

    /**
     * Create a GM1 Instrument.
     *
     * @param programChange
     * @param patchName
     * @param f
     */
    protected GM1Instrument(int programChange, String patchName, InstrumentFamily f)
    {
        super(programChange, patchName);
        if (f == null)
        {
            throw new NullPointerException("f");
        }
        family = f;
    }

    /**
     * Overridden to make sure specified bank is the GM1Bank.
     *
     * @param bank
     */
    @Override
    public void setBank(InstrumentBank<?> bank)
    {
        if (!(bank instanceof GM1Bank))
        {
            throw new IllegalArgumentException("bank=" + bank);
        }
        super.setBank(bank);
    }

    @Override
    public GM1Instrument getSubstitute()
    {
        return this;
    }

    /**
     * The Family value for this instrument.
     *
     * @return A non-null value.
     */
    public InstrumentFamily getFamily()
    {
        return family;
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
                    xstream.alias("GM1Instrument", GM1Instrument.class);
                    xstream.alias("GM1InstrumentSP", SerializationProxy.class);
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

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * If Instrument's bank is null serialization will fail.
     * <p>
     * spVERSION 2 introduces alias XStreamConfig
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 79972630152521L;
        private int spVERSION = 2;      // Do not make final!
        private int spProgChange;

        private SerializationProxy(GM1Instrument ins)
        {
            if (ins.getBank() == null)
            {
                throw new IllegalStateException("ins=" + ins);
            }
            spProgChange = ins.getMidiAddress().getProgramChange();
        }

        private Object readResolve() throws ObjectStreamException
        {
            Instrument ins;
            GM1Bank gm1Bank = GM1Bank.getInstance();
            if (spProgChange < 0 || spProgChange > 127)
            {
                LOGGER.log(Level.WARNING, "readResolve() Can''t find GM1 instrument with PC={0}. Replacing with default instrument.", spProgChange);
                ins = gm1Bank.getInstrument(0);
            } else
            {
                ins = gm1Bank.getInstrument(spProgChange);
            }
            return ins;
        }
    }

}

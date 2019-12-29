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
package org.jjazz.defaultinstruments;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.rhythm.api.RvType;

/**
 * A special delegate DrumsInstrument which returns Midi data from one of the default DrumsInstruments defined by DefaultInstruments.
 * <p>
 * The getBankSelect*(), sendMidiMessages(), getProgramChange() methods will use the methods from the default instrument.
 * <p>
 * getBank() will return the JJazzSynth bank.<br>
 * getPatchName() will return "Default Drums" for drums etc., and "Default User" for user.
 * <p>
 */
public class Delegate2DefaultDrumsInstrument extends Instrument implements Serializable
{

    private static final HashMap<RvType, Delegate2DefaultDrumsInstrument> INSTANCES = new HashMap<>();
    private static Delegate2DefaultDrumsInstrument USER_DELEGATE_INSTANCE = null;
    private final RvType rvType;
    private InstrumentBank<?> myBank;
    private static final Logger LOGGER = Logger.getLogger(Delegate2DefaultDrumsInstrument.class.getSimpleName());

    /**
     * Get a shared instance which delegates to the default instrument for rvType managed by DefaultInstruments.
     * <p>
     * Should be only called by JazzSynth: this way the bank/synth are correctly set.
     *
     * @param rvType
     * @return
     */
    static protected Delegate2DefaultDrumsInstrument getInstance(RvType rvType)
    {
        synchronized (Delegate2DefaultDrumsInstrument.class)
        {
            if (rvType == null)
            {
                throw new NullPointerException("rvType");
            }
            Delegate2DefaultDrumsInstrument instance = INSTANCES.get(rvType);
            if (instance == null)
            {
                instance = new Delegate2DefaultDrumsInstrument(rvType);
                INSTANCES.put(rvType, instance);
            }
            return instance;
        }
    }

    /**
     * Get a shared instance which delegates to the default instrument for user type managed by DefaultInstruments.
     * <p>
     * Should be only called by JazzSynth: this way the bank/synth are correctly set.
     *
     * @return
     */
    static protected Delegate2DefaultDrumsInstrument getInstanceUser()
    {
        synchronized (Delegate2DefaultDrumsInstrument.class)
        {
            if (USER_DELEGATE_INSTANCE == null)
            {
                USER_DELEGATE_INSTANCE = new Delegate2DefaultDrumsInstrument();
            }
            return USER_DELEGATE_INSTANCE;
        }
    }

    /**
     * Create a DelegateInstrument to the Default User Instrument (rvType=null).
     * <p>
     */
    private Delegate2DefaultDrumsInstrument()
    {
        super(0, "Default User");
        this.rvType = null;
    }

    /**
     * Create the DelegateInstrument for specified type.
     *
     * @param rvType
     */
    private Delegate2DefaultDrumsInstrument(RvType rvType)
    {
        super(0, "Default " + rvType.toLongString());
        this.rvType = rvType;
    }

    public RvType getRvType()
    {
        return rvType;
    }

    /**
     * Delegate to default instrument's method.
     *
     * @return
     */
    @Override
    public int getProgramChange()
    {
        Instrument ins = getTargetDefaultInstrument();
        return ins.getProgramChange();
    }

    /**
     * Delegate to default instrument's method.
     *
     * @return
     */
    @Override
    public int getBankSelectMSB()
    {
        Instrument ins = getTargetDefaultInstrument();
        return ins.getBankSelectMSB();
    }

    /**
     * Delegate to default instrument's method.
     *
     * @return
     */
    @Override
    public int getBankSelectLSB()
    {
        Instrument ins = getTargetDefaultInstrument();
        return ins.getBankSelectLSB();
    }

    /**
     * Delegate to default instrument's method.
     *
     * @return
     */
    @Override
    public InstrumentBank.BankSelectMethod getBankSelectMethod()
    {
        Instrument ins = getTargetDefaultInstrument();
        return ins.getBankSelectMethod();
    }

    /**
     * Delegate to target instrument's method.
     *
     * @return
     */
    @Override
    public MidiMessage[] getMidiMessages(int channel)
    {
        Instrument ins = getTargetDefaultInstrument();
        return ins.getMidiMessages(channel);
    }

    /**
     * The default instrument for which we're a delegate.
     *
     * @return
     */
    public Instrument getTargetDefaultInstrument()
    {
        DefaultInstruments di = DefaultInstruments.getInstance();
        Instrument ins;
        if (rvType != null)
        {
            ins = di.getInstrument(rvType);   // Can't be null    
        } else
        {
            ins = di.getUserInstrument();
        }
        return ins;
    }

    // ==================================================================================
    // Private methods
    // ==================================================================================
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
     * Do not reuse Instrument's serialization process, we can make it simpler.
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 287196001L;
        private final int spVERSION = 1;
        RvType spRvType;

        private SerializationProxy(Delegate2DefaultDrumsInstrument ins)
        {
            spRvType = ins.getRvType();
        }

        private Object readResolve() throws ObjectStreamException
        {
            Instrument ins;
            if (spRvType != null)
            {
                ins = JJazzSynth.getDelegate2DefaultInstrument(spRvType);
            } else
            {
                ins = JJazzSynth.getDelegate2DefaultInstrumentUser();
            }
            return ins;
        }
    }

}

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
import javax.sound.midi.MidiMessage;
import static org.jjazz.defaultinstruments.Bundle.CTL_VoidInstrument;
import org.jjazz.midi.Instrument;
import org.openide.util.NbBundle;

/**
 * A special "void" instrument: no bank change or program change is associated to this instrument.
 * <p>
 * When used the system should not send any Midi bank select or program change messages for this instument.
 */
@NbBundle.Messages(
        {
            "CTL_VoidInstrument=!Not Set!"
        })
public class VoidInstrument extends Instrument implements Serializable
{
    private static VoidInstrument INSTANCE;

    /**
     * Should be only called by JazzSynth: this way the bank/synth are correctly set.
     *
     * @return
     */
    static protected VoidInstrument getInstance()
    {
        synchronized (VoidInstrument.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new VoidInstrument();
            }
        }
        return INSTANCE;
    }

    private VoidInstrument()
    {
        super(0, CTL_VoidInstrument());
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
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -82099017429816L;
        private final int spVERSION = 1;
        int spProgChange;

        private SerializationProxy(VoidInstrument ins)
        {
            spProgChange = ins.getMidiAddress().getProgramChange();       // Just to save something but useless...
        }

        private Object readResolve() throws ObjectStreamException
        {
            return JJazzSynth.getVoidInstrument();
        }
    }

}

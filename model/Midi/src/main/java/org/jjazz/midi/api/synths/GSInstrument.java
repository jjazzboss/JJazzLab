/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midi.api.synths;

import com.thoughtworks.xstream.XStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.SysexMessage;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.MidiAddress;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 * A special class for GS instruments.
 * <p>
 * Because GSDrumsInstruments send SysEx to turn channel into drums mode, normal GS Instruments must also do the same to make sure channel
 * is in normal mode (if channel was previously used in drums mode).
 */
public class GSInstrument extends Instrument implements Serializable
{

    /**
     * GS Sysex messages to turn channel X into a normal channel.
     * <p>
     * http://www.grandgent.com/tom/scug/drummaps.txt
     */
    private static final byte[][] SYSEX_SET_NORMAL_CHANNEL =
    {
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x11, 0x15, 0x00, 0x1A, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x12, 0x15, 0x00, 0x19, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x13, 0x15, 0x00, 0x18, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x14, 0x15, 0x00, 0x17, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x15, 0x15, 0x00, 0x16, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x16, 0x15, 0x00, 0x15, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x17, 0x15, 0x00, 0x14, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x18, 0x15, 0x00, 0x13, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x19, 0x15, 0x00, 0x12, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x10, 0x15, 0x00, 0x1B, (byte) 0xF7  // Channel 10(9), normally useless
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1A, 0x15, 0x00, 0x11, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1B, 0x15, 0x00, 0x10, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1C, 0x15, 0x00, 0x0F, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1D, 0x15, 0x00, 0x0E, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1E, 0x15, 0x00, 0x0D, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1F, 0x15, 0x00, 0x0C, (byte) 0xF7
        }
    };
    private static final Logger LOGGER = Logger.getLogger(GSInstrument.class.getSimpleName());

    /**
     *
     * @param patchName
     * @param bank
     * @param ma Must have BankSelectMethod set to MSB_ONLY
     * @param kit
     * @param substitute
     */
    public GSInstrument(String patchName, InstrumentBank<?> bank, MidiAddress ma, DrumKit kit, GM1Instrument substitute)
    {
        super(patchName, bank, ma, kit, substitute);
        if (!ma.getBankSelectMethod().equals(MidiAddress.BankSelectMethod.MSB_ONLY))
        {
            throw new IllegalArgumentException("patchName=" + patchName + " bank=" + bank + " ma=" + ma);
        }
    }

    @Override
    public Instrument getCopy()
    {
        return new GSInstrument(getPatchName(), getBank(), getMidiAddress(), getDrumKit(), getSubstitute());
    }

    /**
     * Overridden to use GS SysEx messages to turn channel in a normal mode.
     * <p>
     *
     * @param channel
     * @return
     */
    @Override
    public MidiMessage[] getMidiMessages(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);
        }
        MidiMessage[] messages = new MidiMessage[3];
        MidiMessage[] msgs = MidiUtilities.getPatchMessages(channel, this);
        assert msgs.length == 2 : "msgs.length=" + msgs.length + " this=" + getFullName(); // GS Instrument use MSB_ONLY addressing mode   

        byte[] bytes = SYSEX_SET_NORMAL_CHANNEL[channel];
        SysexMessage sysMsg = new SysexMessage();
        try
        {
            sysMsg.setMessage(bytes, bytes.length);
        }
        catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        messages[0] = sysMsg;
        messages[1] = msgs[0];
        messages[2] = msgs[1];
        LOGGER.log(Level.FINE, "getMidiMessages() Sending SysEx messages to set melodic mode on channel {0}", channel);
        return messages;
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
                    if (instanceId.equals(MIDIMIX_LOAD))
                    {
//                        // From 4.1.0 Position was moved from ChordLeadSheet module to Harmony module
//                        xstream.alias("org.jjazz.chordleadsheet.api.item.Position$SerializationProxy", Position.SerializationProxy.class);
//                        // At some point the "leadsheet" part was dropped in the package name
//                        xstream.alias("org.jjazz.leadsheet.chordleadsheet.api.item.Position$SerializationProxy", Position.SerializationProxy.class);
                    }

                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files                    
                    xstream.alias("GSInstrument", GSInstrument.class);
                    xstream.alias("GSInstrumentSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spSaveString");
                }
                default ->
                    throw new AssertionError(instanceId.name());
            }
        }
    }

    // --------------------------------------------------------------------- 
    // Serialization
    // --------------------------------------------------------------------- 

    private Object writeReplace()
    {
        // SHOULD BE: return new Instrument.SerializationProxy(this); !! So no use of our own SerializationProxy
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
        throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Our own serialization proxy.
     * <p>
     * ==> BAD! writeReplace() should juste use "return new Instrument.SerializationProxy(this);", no need for our own SerializationProxy
     * !!! But too late to change because user .mix files now contain GSInstrument.SerializationProxy instances. If we change, the .mix will
     * not be readable again.
     *
     * spVERSION 2 introduces alias XStreamConfig
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 872001761L;
        private int spVERSION = 2;      // Do not make final!
        private String spSaveString;

        private SerializationProxy(GSInstrument ins)
        {
            if (ins.getBank() == null || ins.getBank().getMidiSynth() == null)
            {
                throw new IllegalStateException("ins=" + ins + " ins.getBank()=" + ins.getBank());
            }
            spSaveString = ins.saveAsString();
        }

        private Object readResolve() throws ObjectStreamException
        {
            Instrument ins = Instrument.loadFromString(spSaveString);
            if (ins == null || !(ins instanceof GSInstrument))
            {
                LOGGER.log(Level.WARNING, "readResolve() Could not retrieve a GSInstrument from saved string=\"{0}\". Using default instrument instead.", spSaveString);
                ins = GSBank.getInstance().getInstrument(0);
            }
            return (GSInstrument) ins;
        }
    }

}

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
 * A special class for GS drums instruments.
 * <p>
 * GS expects drums channel only on channel 10. To set a drums channel on other channels, SysEx messages must be sent.
 */
public class GSDrumsInstrument extends Instrument implements Serializable
{

    /**
     * GS Sysex messages to turn channel X into a drums channel.
     * <p>
     * https://www.pgmusic.com/forums/ubbthreads.php?ubb=showflat&Number=490421 http://www.synthfont.com/SysEx.txt <br>
     * "The following SysEx messages will allow you to set channel 9 or 11 to drums (in addition to channel 10). This will work for Roland GS compatible devices
     * such as the Roland VSC and Roland SD-20. <br>
     * Set channel 9 to drums: F0 41 10 42 12 40 19 15 02 10 F7 <br>
     * Set channel 11 to drums: F0 41 10 42 12 40 1A 15 02 0F F7 <br>
     * http://www.grandgent.com/tom/scug/drummaps.txt
     */
    private static final byte[][] SYSEX_SET_DRUMS_CHANNEL =
    {
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x11, 0x15, 0x02, 0x18, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x12, 0x15, 0x02, 0x17, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x13, 0x15, 0x02, 0x16, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x14, 0x15, 0x02, 0x15, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x15, 0x15, 0x02, 0x14, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x16, 0x15, 0x02, 0x13, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x17, 0x15, 0x02, 0x12, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x18, 0x15, 0x02, 0x11, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x19, 0x15, 0x02, 0x10, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x10, 0x15, 0x02, 0x19, (byte) 0xF7  // Channel 10(9), normally useless
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1A, 0x15, 0x02, 0x0F, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1B, 0x15, 0x02, 0x0E, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1C, 0x15, 0x02, 0x0D, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1D, 0x15, 0x02, 0x0C, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1E, 0x15, 0x02, 0x0B, (byte) 0xF7
        },
        {
            (byte) 0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x1F, 0x15, 0x02, 0x0A, (byte) 0xF7
        }
    };
    private static final Logger LOGGER = Logger.getLogger(GSDrumsInstrument.class.getSimpleName());

    /**
     *
     * @param patchName
     * @param bank
     * @param ma         Must have BankSelectMethod set to PC_ONLY
     * @param kit
     * @param substitute
     */
    public GSDrumsInstrument(String patchName, InstrumentBank<?> bank, MidiAddress ma, DrumKit kit, GM1Instrument substitute)
    {
        super(patchName, bank, ma, kit, substitute);
        if (!ma.getBankSelectMethod().equals(MidiAddress.BankSelectMethod.PC_ONLY))
        {
            throw new IllegalArgumentException("patchName=" + patchName + " bank=" + bank + " ma=" + ma);
        }
    }

    @Override
    public Instrument getCopy()
    {
        return new GSDrumsInstrument(getPatchName(), getBank(), getMidiAddress(), getDrumKit(), getSubstitute());
    }

    /**
     * Overridden to use GS SysEx messages to enable Drums on any channel.
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

        MidiMessage[] messages = new MidiMessage[2];
        MidiMessage[] msgs = MidiUtilities.getPatchMessages(channel, this);
        assert msgs.length == 1 : "msgs.length=" + msgs.length + " this=" + getFullName(); // GS DrumsInstrument use PC_ONLY addressing mode           

        byte[] bytes = SYSEX_SET_DRUMS_CHANNEL[channel];
        SysexMessage sysMsg = new SysexMessage();
        try
        {
            sysMsg.setMessage(bytes, bytes.length);
        } catch (InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        messages[0] = sysMsg;
        messages[1] = msgs[0];
        LOGGER.log(Level.FINE, "getMidiMessages() Sending SysEx messages to set drums mode on channel {0}", channel);
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
                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files                    
                    xstream.alias("GSDrumsInstrument", GSDrumsInstrument.class);
                    xstream.alias("GSDrumsInstrumentSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spSaveString");
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

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Rely on Instrument serialization mechanism.
     * 
     * spVERSION 2 introduces alias XStreamConfig
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -9269L;
        private int spVERSION = 2;      // Do not make final!
        private String spSaveString;

        private SerializationProxy(GSDrumsInstrument ins)
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
            if (ins == null || !(ins instanceof GSDrumsInstrument))
            {
                throw new InvalidObjectException("readResolve() Can not retrieve a GSDrumsInstrument from saved string=" + spSaveString + ", ins=" + ins);
            }
            return (GSDrumsInstrument) ins;
        }
    }


}

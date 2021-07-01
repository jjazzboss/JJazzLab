/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midi.api.synths;

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
import org.openide.util.Exceptions;

/**
 * A special class for GS instruments.
 * <p>
 * Because GSDrumsInstruments send SysEx to turn channel into drums mode, normal GS Instruments must also do the same to make sure
 * channel is in normal mode (if channel was previously used in drums mode).
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
            throw new IllegalArgumentException("patchName=" + patchName + " bank=" + bank + " ma=" + ma);   //NOI18N
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
            throw new IllegalArgumentException("channel=" + channel);   //NOI18N
        }
        MidiMessage[] messages = new MidiMessage[3];
        MidiMessage[] msgs = MidiUtilities.getPatchMessages(channel, this);
        assert msgs.length == 2 : "msgs.length=" + msgs.length + " this=" + getFullName(); // GS Instrument use MSB_ONLY addressing mode   //NOI18N

        byte[] bytes = SYSEX_SET_NORMAL_CHANNEL[channel];
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
        messages[2] = msgs[1];
        LOGGER.log(Level.FINE, "getMidiMessages() Sending SysEx messages to set melodic mode on channel " + channel);   //NOI18N
        return messages;
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
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 872001761L;
        private final int spVERSION = 1;
        private String spSaveString;

        private SerializationProxy(GSInstrument ins)
        {
            if (ins.getBank() == null || ins.getBank().getMidiSynth() == null)
            {
                throw new IllegalStateException("ins=" + ins + " ins.getBank()=" + ins.getBank());   //NOI18N
            }
            spSaveString = ins.saveAsString();
        }

        private Object readResolve() throws ObjectStreamException
        {
            Instrument ins = Instrument.loadFromString(spSaveString);
            if (ins == null || !(ins instanceof GSInstrument))
            {
                throw new InvalidObjectException("readResolve() Can not retrieve a GSInstrument from saved string=" + spSaveString + ", ins=" + ins);
            }
            return (GSInstrument) ins;
        }
    }

}

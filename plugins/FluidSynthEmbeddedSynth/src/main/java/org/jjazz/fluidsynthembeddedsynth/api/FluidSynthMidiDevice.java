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
package org.jjazz.fluidsynthembeddedsynth.api;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

/**
 * A special MidiDevice which redirects incoming Midi messages to a FluidSynth instance.
 */
public class FluidSynthMidiDevice implements MidiDevice
{

    private static class FluidSynthMidiDeviceInfo extends MidiDevice.Info
    {

        public FluidSynthMidiDeviceInfo(String mdName)
        {
            super(mdName, "Jerome Lelasseux", "JJazzLab embedded audio synth", "1");
        }
    }
    public static final String NAME = "FluidSynth_MD";
    public static final FluidSynthMidiDeviceInfo INFO = new FluidSynthMidiDeviceInfo(NAME);
    private final List<FluidSynthReceiver> receivers = new ArrayList<>();
    private boolean open;
    private final FluidSynthEmbeddedSynth embeddedSynth;
    private static final Logger LOGGER = Logger.getLogger(FluidSynthMidiDevice.class.getSimpleName());

    public FluidSynthMidiDevice(FluidSynthEmbeddedSynth synth)
    {
        Preconditions.checkNotNull(synth);
        embeddedSynth = synth;
    }

    /**
     * Open this MidiDevice.
     * <p>
     *
     * @throws MidiUnavailableException If the FluidSynthJava is not already opened.
     */
    @Override
    public void open() throws MidiUnavailableException
    {
        if (!embeddedSynth.isOpen())
        {
            throw new MidiUnavailableException("embeddedSynth is not opened");
        }

        open = true;
    }

    @Override
    public Info getDeviceInfo()
    {
        return INFO;
    }

    @Override
    public void close()
    {
        if (open)
        {
            open = false;
            for (var receiver : receivers.toArray(Receiver[]::new))
            {
                receiver.close();
            }
            receivers.clear();
            LOGGER.info("close()");
        }
    }

    @Override
    public int getMaxReceivers()
    {
        return -1;
    }

    @Override
    public int getMaxTransmitters()
    {
        return 0;
    }

    @Override
    public long getMicrosecondPosition()
    {
        return 0;
    }

    @Override
    public List<Receiver> getReceivers()
    {
        return new ArrayList<>(receivers);
    }

    @Override
    public List<Transmitter> getTransmitters()
    {
        return Collections.emptyList();
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException
    {
        if (!open)
        {
            throw new IllegalStateException();
        }

        return new FluidSynthReceiver();    // This will update the receivers global variable
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException
    {
        throw new MidiUnavailableException();
    }

    @Override
    public boolean isOpen()
    {
        return open;
    }

    /**
     * Redirect incoming messages to our FluidSynth instance.
     */
    private class FluidSynthReceiver implements Receiver
    {

        private boolean closed;

        public FluidSynthReceiver()
        {
            receivers.add(this);
        }

        @Override
        public void close()
        {
            closed = true;
            receivers.remove(this);
        }

        @Override
        public void send(MidiMessage mm, long timeStamp)
        {
            if (closed)
            {
                throw new IllegalStateException();
            }

            if (mm instanceof ShortMessage sm)
            {
                embeddedSynth.getFluidSynthJava().sendShortMessage(sm);
            } else if (mm instanceof SysexMessage sm)
            {
                embeddedSynth.getFluidSynthJava().sendSysexMessage(sm);
            }
        }
    }
}

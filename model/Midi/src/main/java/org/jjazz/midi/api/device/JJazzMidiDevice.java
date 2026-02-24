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
package org.jjazz.midi.api.device;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;

/**
 * A generic MidiDevice that should be subclassed to create your own MidiDevice.
 */
public class JJazzMidiDevice implements MidiDevice
{
    protected String name;
    protected DeviceInfo devInfo;
    protected boolean isOpen = true;
    protected ArrayList<Transmitter> transmitters = new ArrayList<>();
    protected ArrayList<Receiver> receivers = new ArrayList<>();

    private static final Logger LOGGER = Logger.getLogger(JJazzMidiDevice.class.getSimpleName());

    //~ Constructors ================================================================================
    public JJazzMidiDevice(String s)
    {
        name = s;
        devInfo = new DeviceInfo(s);
    }

    //~ Methods =====================================================================================
    @Override
    public MidiDevice.Info getDeviceInfo()
    {
        return devInfo;
    }

    @Override
    public void close()
    {
        LOGGER.fine("close()");   
        isOpen = false;
        for (Transmitter t : getTransmitters())
        {
            t.close();
        }
        for (Receiver r : getReceivers())
        {
            r.close();
        }
    }

    @Override
    public void open()
    {
        LOGGER.fine("open()");   
        isOpen = true;
    }

    @Override
    public boolean isOpen()
    {
        return isOpen;
    }

    @Override
    public int getMaxTransmitters()
    {
        return -1;
    }

    @Override
    public int getMaxReceivers()
    {
        return -1;
    }

    @Override
    public long getMicrosecondPosition()
    {
        return -1;
    }

    @Override
    public Transmitter getTransmitter()
    {
        JJazzTransmitter mt = new JJazzTransmitter();
        transmitters.add(mt);
        open();
        LOGGER.log(Level.FINE, "getTransmitter() mt={0}", mt);   
        return mt;
    }

    /**
     * Must be overridden by subclasses.
     */
    @Override
    public Receiver getReceiver()
    {
        throw new UnsupportedOperationException("JJazzMidiDevice.getReceiver() should be overridden by subclasses.");
    }

    @Override
    public List<Transmitter> getTransmitters()
    {
        return Collections.unmodifiableList(transmitters);
    }

    @Override
    public List<Receiver> getReceivers()
    {
        return Collections.unmodifiableList(receivers);
    }

    //~ Classes =====================================================================================
    /**
     * The device info of the MidiDevice.
     */
    protected class DeviceInfo extends MidiDevice.Info
    {

        public DeviceInfo(String name)
        {
            super(name, "", "", "");
        }
    }

    /**
     * A basic transmitter of the MidiDevice.
     */
    protected class JJazzTransmitter implements Transmitter
    {

        Receiver rcv;

        @Override
        public void setReceiver(Receiver r)
        {
            rcv = r;
        }

        @Override
        public Receiver getReceiver()
        {
            return rcv;
        }

        @Override
        public void close()
        {
            LOGGER.fine("JJazzTransmitter.close()");   
            rcv = null;
            transmitters.remove(this);
        }
    }
}

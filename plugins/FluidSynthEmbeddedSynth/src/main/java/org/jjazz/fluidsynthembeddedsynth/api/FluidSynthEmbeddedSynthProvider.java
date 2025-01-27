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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.fluidsynthjava.api.FluidSynthJava;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.outputsynth.spi.OutputSynthManager;
import org.jjazz.midi.spi.MidiSynthManager;
import org.jjazz.outputsynth.api.OutputSynth;

@ServiceProvider(service = EmbeddedSynthProvider.class)
public class FluidSynthEmbeddedSynthProvider implements EmbeddedSynthProvider
{

    public static final String ID = "FluidSynthEmbeddedSynthProviderId";
    private final FluidSynthEmbeddedSynth embeddedSynth;
    private final MidiDevice midiDevice;
    private boolean enabled;
    private boolean active;
    private String saveMidiDeviceName;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private static final Logger LOGGER = Logger.getLogger(FluidSynthEmbeddedSynthProvider.class.getSimpleName());

    public FluidSynthEmbeddedSynthProvider()
    {
        enabled = FluidSynthJava.isPlatformSupported() && FluidSynthJava.isLibrariesLoadedOk();
        if (!enabled)
        {
            LOGGER.warning("FluidSynthEmbeddedSynthProvider() FluidSynthJava not supported on this platform");
        }
        embeddedSynth = new FluidSynthEmbeddedSynth();
        midiDevice = new FluidSynthMidiDevice(embeddedSynth);
    }

    @Override
    public FluidSynthEmbeddedSynth getEmbeddedSynth()
    {
        if (!enabled)
        {
            return null;
        }
        return embeddedSynth;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public MidiDevice getOutMidiDevice()
    {
        if (!enabled)
        {
            return null;
        }
        return midiDevice;
    }

    /**
     * Make FluidSynthJava active.
     * <p>
     * Open the FluidSynthJava instance and set FluidSynthMidiDevice as the default Midi OUT device (so that out MidiMessages are redirected to the
     * FluidSynthJava instance).
     *
     * @param b
     * @throws EmbeddedSynthException
     */
    @Override
    public void setEmbeddedSynthActive(boolean b) throws EmbeddedSynthException
    {
        if (b == active)
        {
            return;
        } else if (!enabled)
        {
            throw new EmbeddedSynthException("FluidSynthEmbeddedSynthProvider is disabled");
        }

        if (b)
        {
            // Open midiDevice and embeddedSynth
            openSynthAndDevice();       // throws EmbeddedSynthException. 

            //
            // Connect to the JJazzLab application
            //
            var osm = OutputSynthManager.getDefault();
            String mdName = midiDevice.getDeviceInfo().getName();
            // MidiSynth needs to be added manually as it is read from the jar file
            MidiSynthManager.getDefault().addMidiSynth(embeddedSynth.getOutputSynth().getMidiSynth());

            
            // Try to restore a previous saved FluidSynth output synth with its configuration (e.g. audio latency, remap table)
            OutputSynth os = osm.getMidiDeviceOutputSynth(mdName);
            if (os.getMidiSynth().getName().equals("FluidSynth"))
            {
                // Restore OK, update embeddedSynth
                embeddedSynth.setOutputSynth(os);
            }else
            {
                // Restore failed, set a new OutputSynth ourselves
                os = embeddedSynth.getOutputSynth();
                osm.setMidiDeviceOutputSynth(midiDevice.getDeviceInfo().getName(), os);
            }


            // Use our special MidiDevice
            var jms = JJazzMidiSystem.getInstance();
            var md = jms.getDefaultOutDevice();
            saveMidiDeviceName = md == null ? null : md.getDeviceInfo().getName();
            try
            {
                jms.setDefaultOutDevice(midiDevice);    // This will notify OutputSynthManager to set fsOutputSynth as the default OutputSynth
            } catch (MidiUnavailableException ex)
            {
                // Should never be there, our midiDevice does nothing upon open...
                Exceptions.printStackTrace(ex);
            }

        } else
        {
            // Desactivate

            // Try to restore the previous MidiDevice
            var jms = JJazzMidiSystem.getInstance();
            var md = jms.getMidiDevice(jms.getOutDeviceList(), saveMidiDeviceName);
            try
            {
                jms.setDefaultOutDevice(md);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.log(Level.WARNING, "setEmbeddedSynthActive() Can''t restore previous OUT MidiDevice. ex={0}", ex.getLocalizedMessage());
            }

            embeddedSynth.close();
            midiDevice.close();

        }

        LOGGER.log(Level.INFO, "setEmbeddedSynthActive() b={0}", b);
        active = b;
        pcs.firePropertyChange(PROP_EMBEDDED_SYNTH_ACTIVE, !b, b);
    }

    @Override
    public boolean isEmbeddedSynthActive()
    {
        return active;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // =====================================================================================================
    // Private methods
    // =====================================================================================================
    private void disable()
    {
        if (!enabled)
        {
            return;
        }
        enabled = false;
        pcs.firePropertyChange(PROP_PROVIDER_ENABLED, true, false);
    }


    private void openSynthAndDevice() throws EmbeddedSynthException
    {
        class LongTask implements Runnable
        {

            private EmbeddedSynthException exception = null;

            @Override
            public void run()
            {
                try
                {
                    embeddedSynth.open();       // throws EmbeddedSynthException
                    midiDevice.open();          // throws MidiUnavailableException
                } catch (EmbeddedSynthException ex)
                {
                    // Possible problems: native library loading, wrong soundfont, ...
                    exception = ex;

                } catch (MidiUnavailableException ex)
                {
                    // Should never be here !
                    Exceptions.printStackTrace(ex);
                }
            }
        }


        LongTask openTask = new LongTask();
        BaseProgressUtils.showProgressDialogAndRun(openTask, ResUtil.getString(getClass(), "LoadingFluidSynth"));


        if (openTask.exception instanceof EmbeddedSynthException)
        {
            disable();
            embeddedSynth.close();
            midiDevice.close();
            throw openTask.exception;
        }
    }

}

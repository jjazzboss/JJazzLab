package org.jjazz.fluidsynthembeddedsynth;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.embeddedsynth.api.EmbeddedSynth;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.fluidsynthjava.api.FluidSynthJava;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.outputsynth.api.MidiSynthManager;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = EmbeddedSynthProvider.class)
public class FluidSynthEmbeddedSynthProvider implements EmbeddedSynthProvider
{

    private final FluidSynthEmbeddedSynth embeddedSynth;
    private final MidiDevice midiDevice;
    private boolean enabled;
    private boolean active;
    private String saveMidiDeviceName;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private static final Logger LOGGER = Logger.getLogger(FluidSynthEmbeddedSynthProvider.class.getSimpleName());

    public FluidSynthEmbeddedSynthProvider()
    {
        enabled = FluidSynthJava.isPlatformSupported() && FluidSynthJava.LIBRARIES_LOADED_OK;
        if (!enabled)
        {
            LOGGER.warning("FluidSynthEmbeddedSynthProvider() FluidSynthJava not supported on this platform");
        }
        embeddedSynth = new FluidSynthEmbeddedSynth();
        midiDevice = new FluidSynthMidiDevice(embeddedSynth);
    }

    @Override
    public EmbeddedSynth getEmbeddedSynth()
    {
        if (!enabled)
        {
            return null;
        }
        return embeddedSynth;
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

        var jms = JJazzMidiSystem.getInstance();

        if (b)
        {
            openSynthAndDevice();       // throws EmbeddedSynthException


            // Connect to the JazzLab application
            MidiSynthManager.getInstance().addMidiSynth(embeddedSynth.getOutputSynth().getMidiSynth());
            OutputSynthManager.getInstance().setOutputSynth(midiDevice.getDeviceInfo().getName(), embeddedSynth.getOutputSynth());


            // Use our special MidiDevice
            var md = jms.getDefaultOutDevice();
            saveMidiDeviceName = md == null ? null : md.getDeviceInfo().getName();
            try
            {
                jms.setDefaultOutDevice(midiDevice);
            } catch (MidiUnavailableException ex)
            {
                // Should never be there, our midiDevice does nothing upon open...
                Exceptions.printStackTrace(ex);
            }

        } else
        {
            // Desactivate

            // Try to restore the previous MidiDevice
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
        BaseProgressUtils.showProgressDialogAndRun(openTask, ResUtil.getString(getClass(), "LoadingJJSynth"));


        if (openTask.exception instanceof EmbeddedSynthException)
        {
            disable();
            embeddedSynth.close();
            midiDevice.close();
            throw openTask.exception;
        }
    }

}

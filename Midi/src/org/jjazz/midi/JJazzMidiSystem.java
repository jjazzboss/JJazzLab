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
package org.jjazz.midi;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import org.jjazz.midi.device.MidiFilter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midi.device.JJazzMidiDevice;
import org.jjazz.upgrade.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * Manage the Midi IN and OUT devices for the JJazz application, plus some convenience methods.
 * <p>
 * Scan the available Midi IN/OUT devices at startup. Restore the default Midi IN/OUT devices when possible using Preferences.
 * <p>
 * The application should only connect to JJazzMidiIn and JJazzMidiOut virtual devices. These devices are implemented by a MidiFilter
 * object, enabling filtering and dumping. These devices are connected internally to the selected physical MIDI In/Out devices.
 * <p>
 * Manage a Midi master volume: a factor between 0 and 2 (default=1) which is used on all volume Midi messages.
 */
@Messages(
        "ERR_ProblemSynthFile=Problem reading sound file for the Java internal synth"
)
public final class JJazzMidiSystem
{

    private static JJazzMidiSystem INSTANCE = null;
    public final static String NOT_SET = "NotSet";
    public final static String JAVA_INTERNAL_SYNTH_NAME = "Java Internal Synth";
    // Properties are also used as Preferences key
    public final static String PROP_MIDI_IN = "MidiInProp";
    public final static String PROP_MIDI_OUT = "MidiOutProp";
    public final static String PROP_MIDI_THRU = "MidiThruProp";
    public final static String PROP_MASTER_VOL_FACTOR = "MasterVolumeFactor";
    public final static String PROP_MIDI_OUT_FILTERING = "MidiOutFiltering";
    public final static String PROP_SEQUENCER_LOCK = "SequencerLock";
    public final static String PREF_JAVA_SYNTH_SOUNDFONT_FILE = "JavaSynthSoundFontFile";

    /**
     * The default MIDI IN device.
     */
    private MidiDevice defaultInDevice;
    /**
     * The MidiFilter used to implement the thru mode.
     */
    private MidiFilter thruFilter;
    /**
     * The transmitter of JJazzMidiOut for the physical Midi OUT device.
     */
    private Transmitter transmitterJJazzOut2PhysicalOut;
    /**
     * the receiver of the JJazzOutDevice.
     */
    private Receiver receiverJJazzOut;
    /**
     * The receiver of JJazzMidiIn for the physical Midi IN device.
     */
    private Receiver receiverPhysicalIn2JJazzIn;
    /**
     * The default MIDI OUT device.
     */
    private MidiDevice defaultOutDevice;

    private Object sequencerLock;
    /**
     * The default system Sequencer
     */
    private Sequencer defaultSequencer;
    /**
     * The default system synth
     */
    private Synthesizer defaultSynth;
    private EnumSet<MidiFilter.Config> saveFilterConfig;

    /**
     * The Midi In/out virtual devices connected to the rest of the application.
     */
    private MidiFilter jjazzMidiIn;
    private MidiFilter jjazzMidiOut;
    private Soundbank lastLoadedSoundbank;
    private File lastLoadedSoundbankFile;
    private float masterVolumeFactor = 1;
    private List<Runnable> startupTasks;

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(JJazzMidiSystem.class);
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(JJazzMidiSystem.class.getSimpleName());

    static public JJazzMidiSystem getInstance()
    {
        synchronized (JJazzMidiSystem.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new JJazzMidiSystem();
            }
        }
        return INSTANCE;
    }

    /**
     * Collect midi devices information, open the default sequencer, restore the default Midi devices, thru mode, send startup
     * initialization messages, etc.
     */
    private JJazzMidiSystem()
    {
        // The IN/OUT virtual devices to be used by the application
        jjazzMidiIn = new MidiFilter("[JJazz Midi IN device]");
        jjazzMidiOut = new MidiFilter("[JJazz Midi OUT device]");
        transmitterJJazzOut2PhysicalOut = jjazzMidiOut.getTransmitter();
        receiverJJazzOut = jjazzMidiOut.getReceiver();
        receiverPhysicalIn2JJazzIn = jjazzMidiIn.getReceiver();

        // Connect MidiIn to MidiOut to manage MidiThru
        thruFilter = new MidiFilter("[Midi Thru filter]");
        jjazzMidiIn.getTransmitter().setReceiver(thruFilter.getReceiver());
        thruFilter.getTransmitter().setReceiver(jjazzMidiOut.getReceiver());

        // Restore thru mode
        thruFilter.setFilterConfig(isThruMode() ? EnumSet.noneOf(MidiFilter.Config.class) : EnumSet.of(MidiFilter.Config.FILTER_EVERYTHING));

        // Get the sequencer
        try
        {
            // Open the default sequencer which must be here.
            defaultSequencer = MidiSystem.getSequencer(false);
            defaultSequencer.open();

            // Connect the sequencer to the JJazzMidiOut device
            defaultSequencer.getTransmitter().setReceiver(jjazzMidiOut.getReceiver());
        } catch (MidiUnavailableException ex)
        {
            LOGGER.log(Level.SEVERE, "JJazzMidiSystem() No sequencer found on this system. Music can not be played ! " + ex.getLocalizedMessage());
            defaultSequencer = null;
        }

        // Get the default synth         
        try
        {
            defaultSynth = MidiSystem.getSynthesizer();
            defaultSynth.open();
        } catch (MidiUnavailableException ex)
        {
            LOGGER.log(Level.WARNING, "JJazzMidiSystem() problem getting Java default synthesizer: {0}", ex.getLocalizedMessage());
            defaultSynth = null;
        }

        // Try to restore default Midi OUT
        List<MidiDevice> outDevices = getOutDeviceList();
        LOGGER.info("JJazzMidiSystem() Midi out devices=" + getDeviceListAsString(outDevices));
        defaultOutDevice = null;
        MidiDevice md = null;
        String mdName = prefs.get(PROP_MIDI_OUT, NOT_SET);
        try
        {
            if (mdName.equals(NOT_SET) || mdName.equals(JAVA_INTERNAL_SYNTH_NAME))
            {
                // By default use the default Java synth
                md = defaultSynth;
            } else
            {
                md = getMidiDevice(outDevices, mdName);
            }
            setDefaultOutDevice(md);

        } catch (MidiUnavailableException ex)
        {
            LOGGER.log(Level.WARNING, "JJazzMidiSystem() Can''t set default Midi OUT device to {0}: {1}", new Object[]
            {
                md.getDeviceInfo().getName(), ex.getLocalizedMessage()
            });
        }

        // Try to restore default Midi IN
        List<MidiDevice> inDevices = getInDeviceList();
        defaultInDevice = null;
        mdName = prefs.get(PROP_MIDI_IN, NOT_SET);
        if (!mdName.equals(NOT_SET))
        {
            try
            {
                md = getMidiDevice(inDevices, mdName);
                setDefaultInDevice(md);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.log(Level.WARNING, "JJazzMidiSystem() Can''t set default Midi IN device to {0}: {1}", new Object[]
                {
                    mdName, ex.getLocalizedMessage()
                });
            }
        }

        // Load Java synth soundfont file if any
        lastLoadedSoundbank = null;
        lastLoadedSoundbankFile = null;
        File synthFile = getDefaultJavaSynthPreferredSoundFontFile();
        if (synthFile != null)
        {
            if (!synthFile.canRead())
            {
                // File has moved or something else: fix the preference
                prefs.put(PREF_JAVA_SYNTH_SOUNDFONT_FILE, NOT_SET);
                LOGGER.warning("JJazzMidiSystem() can't access SoundFont file: " + synthFile.getAbsolutePath());
            } else
            {
                loadSoundbankFileOnSynth(synthFile, true);      // Run in silent mode, return value is useless
            }
        }
    }

    /**
     * Scan the MidiSystem for IN MidiDevices.
     *
     * @return A List containing the MIDI out devices. Java Sequencer is excluded.
     */
    public List<MidiDevice> getInDeviceList()
    {
        ArrayList<MidiDevice> r = new ArrayList<>();
        MidiDevice.Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : deviceInfos)
        {
            MidiDevice device;
            try
            {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.log(Level.WARNING, "getInDeviceList() Can''t access Midi device : {0}", ex.getLocalizedMessage());
                continue;
            }
            if (!(device instanceof Sequencer) && device.getMaxTransmitters() != 0)
            {
                r.add(device);
            }
        }
        return r;
    }

    /**
     * Scan the MidiSystem for OUT MidiDevices.
     *
     * @return A List containing the available MIDI out devices, excluding the Java synth and the Java Sequencer.
     */
    public List<MidiDevice> getOutDeviceList()
    {
        ArrayList<MidiDevice> r = new ArrayList<>();
        MidiDevice.Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : deviceInfos)
        {
            MidiDevice device;
            try
            {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.log(Level.WARNING, "getOutDeviceList() Can''t access Midi device : {0}", ex.getLocalizedMessage());
                continue;
            }
            if ((device instanceof Sequencer) || (device instanceof Synthesizer))
            {
                continue;
            }
            if (device.getMaxReceivers() != 0)
            {
                r.add(device);
            }
        }
        return r;
    }

    /**
     * @return The Midi IN device to be used by the application.
     */
    public JJazzMidiDevice getJJazzMidiInDevice()
    {
        return jjazzMidiIn;
    }

    /**
     * @return The Midi OUT device to be used by the application.
     */
    public JJazzMidiDevice getJJazzMidiOutDevice()
    {
        return jjazzMidiOut;
    }

    /**
     * @return The default MIDI in device. Null if not set.
     */
    public MidiDevice getDefaultInDevice()
    {
        return defaultInDevice;
    }

    /**
     * @return The default MIDI out device. Null if not set.
     */
    public MidiDevice getDefaultOutDevice()
    {
        return defaultOutDevice;
    }

    /**
     * If available, get the system java sequencer opened and ready to play music on the JJazzMidiOutDevice.
     * <p>
     * When done with the sequencer, caller must call releaseSequencer(lock), so sequencer can be used by others.<p>
     * If lock is changed the method fires a PROP_SEQUENCER_LOCK property change with old value=null and new value=lock.
     *
     * @param lock A non-null object used as a lock on this sequencer.
     * @return Null if sequencer has already a different lock
     */
    public synchronized Sequencer getSequencer(Object lock)
    {
        if (lock == null)
        {
            throw new NullPointerException("lock");
        }
        if (sequencerLock == lock)
        {
            return defaultSequencer;
        } else if (sequencerLock == null)
        {
            sequencerLock = lock;
            pcs.firePropertyChange(PROP_SEQUENCER_LOCK, null, lock);
            return defaultSequencer;
        }
        return null;
    }

    /**
     * Get the current sequencer lock object.
     *
     * @return null if no lock.
     */
    public synchronized Object getSequencerLock()
    {
        return sequencerLock;
    }

    /**
     * Release the specified sequencer lock.
     * <p>
     * Fire a PROP_SEQUENCER_LOCK property change with old value=lock and new value=null.
     *
     * @param lock
     * @throws IllegalArgumentException If lock is not the current sequencer lock
     */
    public synchronized void releaseSequencer(Object lock)
    {
        if (lock == null || sequencerLock != lock)
        {
            throw new IllegalArgumentException("lock=" + lock + " sequencerLock=" + sequencerLock);
        }
        sequencerLock = null;
        pcs.firePropertyChange(PROP_SEQUENCER_LOCK, lock, null);
    }

    /**
     * Get direct access to the system java sequencer opened and ready to play music on the JJazzMidiOutDevice.
     * <p>
     * In general getSequencer(Object lock) should be preferred, as it allows for access synchronization between various users.
     *
     * @return
     */
    public Sequencer getSystemSequencer()
    {
        return defaultSequencer;
    }

    /**
     * The default Java synth.
     *
     * @return Can be null if no java synth.
     */
    public Synthesizer getDefaultJavaSynth()
    {
        return defaultSynth;
    }

    /**
     * The default internal synth soundfont/DLS file stored in the Preferences.
     *
     * @return Can be null if no soundfont/DLS file set.
     */
    public File getDefaultJavaSynthPreferredSoundFontFile()
    {
        String path = prefs.get(PREF_JAVA_SYNTH_SOUNDFONT_FILE, NOT_SET);
        if (path.equals(NOT_SET))
        {
            return null;
        }
        File f = new File(path);
        return f;
    }

    /**
     * This unloads all previously loaded instruments in the Default Java Synth.
     * <p>
     * Note that the builtin GM instruments remain.
     */
    public void resetSynth()
    {
        Synthesizer synth = getDefaultJavaSynth();
        if (lastLoadedSoundbank == null || synth == null)
        {
            return;
        }
        try
        {
            synth.unloadAllInstruments(lastLoadedSoundbank);
            lastLoadedSoundbank = null;
            lastLoadedSoundbankFile = null;
            // Save the new preference
            prefs.put(PREF_JAVA_SYNTH_SOUNDFONT_FILE, NOT_SET);

            // Need to reload the default soundbank, it may have been discarded if previously loaded bank was a GM bank.
            Soundbank sb = synth.getDefaultSoundbank();
            if (sb != null)
            {
                synth.loadAllInstruments(sb);
            }
        } catch (IllegalArgumentException ex)
        {
            LOGGER.warning("resetSynth() Can't unload instruments from soundbank " + lastLoadedSoundbank.getName() + ":" + ex.getLocalizedMessage());
        }
    }

    /**
     * Try to load the soundfont2 (or DLS) file in the default Java synth.
     * <p>
     * Previous soundbank instruments are unloaded first. This triggers a specific task since loading a soundfont can take some time.
     *
     * @param f
     * @param silentRun If false wait until completion of the task and show progress bar. If true nothing is shown and method immediatly
     *                  returns true.
     * @return true If success. If silentRun=true always return true.
     */
    public boolean loadSoundbankFileOnSynth(final File f, boolean silentRun)
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);
        }

        // Same file ?
        if (f.equals(lastLoadedSoundbankFile))
        {
            return true;         // Nothing to do
        }

        // Prepare task
        Runnable run;
        final AtomicBoolean resultOk = new AtomicBoolean(false);       // False by default
        run = new Runnable()
        {
            @Override
            public void run()
            {
                Soundbank newSb = null;
                Synthesizer synth = null;
                try
                {
                    newSb = MidiSystem.getSoundbank(f);      // throw InvalidMidiDataException + IOException
                    synth = getDefaultJavaSynth();
                    if (synth == null)
                    {
                        return;
                    }
                    if (!synth.isOpen())
                    {
                        synth.open();                                   // throw MidiUnavailableException
                    }
                } catch (InvalidMidiDataException | IOException | MidiUnavailableException ex)
                {
                    LOGGER.log(Level.WARNING, "loadSoundbankFileOnSynth() {0}", ex.getLocalizedMessage());
                    return;
                }

                if (!synth.isSoundbankSupported(newSb))
                {
                    return;
                }

                if (lastLoadedSoundbank != null)
                {
                    try
                    {
                        synth.unloadAllInstruments(lastLoadedSoundbank);
                    } catch (IllegalArgumentException ex)
                    {
                        LOGGER.warning("loadSoundbankFileOnSynth() Problem unloading Soundbank " + lastLoadedSoundbank.getName() + " : " + ex.getLocalizedMessage());
                        return;
                    }
                }
                try
                {
                    LOGGER.info("loadSoundbankFileOnSynth() start loading... Java Synth sound file " + f.getAbsolutePath());
                    synth.loadAllInstruments(newSb);       // Ignore return value (false is incomplete loading) throw IllegalArgumentException
                } catch (IllegalArgumentException ex)
                {
                    // Reload previous soundbank
                    LOGGER.warning("loadSoundbankFileOnSynth() Problem loading Soundbank " + newSb.getName() + " : " + ex.getLocalizedMessage());
                    if (lastLoadedSoundbank != null)
                    {
                        synth.loadAllInstruments(lastLoadedSoundbank);
                    }
                    return;
                }
                lastLoadedSoundbank = newSb;
                lastLoadedSoundbankFile = f;
                // Save the new preference
                prefs.put(PREF_JAVA_SYNTH_SOUNDFONT_FILE, f.getAbsolutePath());
                resultOk.set(true);
                LOGGER.info("loadSoundbankFileOnSynth() successfully loaded Java Synth sound file " + f.getAbsolutePath());
            }
        };
        if (silentRun)
        {
            new Thread(run).start();
        } else
        {
            BaseProgressUtils.showProgressDialogAndRun(run, "Loading Java Synth sound file " + f.getName() + "...");     // Call is sectioning
        }
        return silentRun || resultOk.get();
    }

    /**
     * Connect the output of JJazzMidiOut device to this device.
     * <p>
     * Fire a PROP_MIDI_OUT property chane event.property change event if device is modified.
     *
     * @param md Can be null.
     * @throws javax.sound.midi.MidiUnavailableException If exception occurs, the default IN device is unchanged.
     */
    public void setDefaultOutDevice(MidiDevice md) throws MidiUnavailableException
    {
        MidiDevice oldDevice = defaultOutDevice;
        if (md == null)
        {
            if (defaultOutDevice == null)
            {
                return;
            }
            closeDefaultOutDevice();
            defaultOutDevice = null;
        } else if (md.equals(defaultOutDevice))
        {
            return;
        } else
        {
            closeDefaultOutDevice(); // Must close *before* opening the new : they might depend on the same hardware
            Receiver r = null;
            try
            {
                if (!md.isOpen())
                {
                    md.open();
                }
                r = md.getReceiver();
            } catch (MidiUnavailableException ex)
            {
                // Restore previous state
                md.close();
                defaultOutDevice = oldDevice;
                LOGGER.warning("setDefaultOutDevice() ex=" + ex.getLocalizedMessage());
                throw new MidiUnavailableException(ex.getLocalizedMessage());
            }

            defaultOutDevice = md;
            // Reconnect the output of our internal JJazz MidiFilter to the receiver of the new midi OUT device            
            transmitterJJazzOut2PhysicalOut = jjazzMidiOut.getTransmitter();
            transmitterJJazzOut2PhysicalOut.setReceiver(r);
        }
        String s = NOT_SET;
        if (defaultOutDevice != null)
        {
            if (defaultOutDevice == getDefaultJavaSynth())
            {
                s = JAVA_INTERNAL_SYNTH_NAME;
            } else
            {
                s = defaultOutDevice.getDeviceInfo().getName();
            }
        }
        prefs.put(PROP_MIDI_OUT, s);
        pcs.firePropertyChange(PROP_MIDI_OUT, oldDevice, defaultOutDevice);
        LOGGER.log(Level.INFO, "setDefaultOutDevice() oldDevice={0} newDevice={1}", new Object[]
        {
            oldDevice, s
        });
    }

    /**
     * Close the default out device. Special handling of the Java Internal Synth.
     */
    public void closeDefaultOutDevice()
    {
        LOGGER.fine("closeDefaultOutDevice()");
        if (defaultOutDevice == null)
        {
            return;
        }

        // Make sure no remaining notes is sounding on this device
        panic();

        Receiver r = transmitterJJazzOut2PhysicalOut.getReceiver();
        if (r != null)
        {
            r.close();
        }
        transmitterJJazzOut2PhysicalOut.close();            // Close the connection between jjazzMidiOut and defaultOutDevice

        // We don't want to close the unique internal synth, otherwise we discard the loaded soundbank
        if (defaultOutDevice != getDefaultJavaSynth())
        {
            // Close everything except the internal Java Synth
            defaultOutDevice.close();
        }
    }

    /**
     * Set the default MIDI In device.
     * <p>
     * Fire a PROP_MIDI_IN property change event if device is modified.
     *
     * @param md Can be null.
     * @throws javax.sound.midi.MidiUnavailableException If exception occurs, the default IN device is unchanged.
     */
    public void setDefaultInDevice(MidiDevice md) throws MidiUnavailableException
    {
        MidiDevice oldDevice = defaultInDevice;
        if (md == null)
        {
            if (defaultInDevice == null)
            {
                return;
            }
            closeDefaultInDevice();
            defaultInDevice = null;
        } else if (md.equals(defaultInDevice))
        {
            return;
        } else
        {
            closeDefaultInDevice(); // Must close *before* opening the new : new device might depend on the same hardware than old one
            Transmitter t = null;
            try
            {
                if (!md.isOpen())
                {
                    md.open();
                }
                t = md.getTransmitter();
            } catch (MidiUnavailableException ex)
            {
                // Restore previous state
                md.close();
                defaultInDevice = oldDevice;
                LOGGER.warning("setDefaultInDevice() ex=" + ex.getLocalizedMessage());
                throw ex;
            }
            defaultInDevice = md;
            t.setReceiver(receiverPhysicalIn2JJazzIn);
        }
        prefs.put(PROP_MIDI_IN, defaultInDevice == null ? NOT_SET : defaultInDevice.getDeviceInfo().getName());
        pcs.firePropertyChange(PROP_MIDI_IN, oldDevice, defaultInDevice);
        LOGGER.log(Level.INFO, "setDefaultInDevice() oldDevice={0} newDevice={1}", new Object[]
        {
            oldDevice, defaultInDevice
        });
    }

    public void closeDefaultInDevice()
    {
        if (defaultInDevice == null)
        {
            return;
        }
        defaultInDevice.close();
    }

    /**
     * Close default In and Out devices.
     */
    public void closeAll()
    {
        closeDefaultInDevice();
        closeDefaultOutDevice();
    }

    /**
     * Enable/Disable redirection of MIDI in to out (MIDI thru).
     * <p>
     * Fire the PROP_MIDI_THRU property change event.
     *
     * @param b Enable if true, disable if false.
     */
    public void setThruMode(boolean b)
    {
        if (b == isThruMode())
        {
            return;
        }
        if (b)
        {
            jjazzMidiOut.setFilterConfig(EnumSet.noneOf(MidiFilter.Config.class));
        } else
        {
            jjazzMidiOut.setFilterConfig(EnumSet.of(MidiFilter.Config.FILTER_EVERYTHING));
        }
        prefs.putBoolean(PROP_MIDI_THRU, b);
        LOGGER.info("setThruMode() b=" + b);
        pcs.firePropertyChange(PROP_MIDI_THRU, !b, b);
    }

    /**
     * @return True if thru mode is enabled.
     */
    public boolean isThruMode()
    {
        return prefs.getBoolean(PROP_MIDI_THRU, false);
    }

    /**
     * The Midi panic method.
     * <p>
     * Send ALL_NOTES_OFF + SUSTAIN OFF + RESET_ALL_CONTROLLERS on all channels on the default JJazz midi out device.
     */
    public void panic()
    {
        LOGGER.info("panic()");
        ArrayList<ShortMessage> sms = new ArrayList<>();
        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
        {
            sms.add(MidiUtilities.buildMessage(ShortMessage.CONTROL_CHANGE, i, MidiConst.CTRL_CHG_SUSTAIN, 0));
            sms.add(MidiUtilities.buildMessage(ShortMessage.CONTROL_CHANGE, i, MidiConst.CTRL_CHG_ALL_NOTES_OFF, 0));
            sms.add(MidiUtilities.buildMessage(ShortMessage.CONTROL_CHANGE, i, MidiConst.CTRL_CHG_RESET_ALL_CONTROLLERS, 0));
        }
        sendMidiMessagesOnJJazzMidiOut(sms.toArray(new ShortMessage[0]));
    }

    /**
     * @return the masterVolumeFactor A value between 0 and 2. Default is 1.
     */
    public float getMasterVolumeFactor()
    {
        return masterVolumeFactor;
    }

    /**
     * Set the master volume factor.
     * <p>
     * All volume Midi messages are multiplicated by this factor.<br>
     * Fire the PROP_MASTER_VOL_FACTOR property chane event.
     *
     * @param f The master volume Factor to set. Must be between 0 and 2.
     */
    public void setMasterVolumeFactor(float f)
    {
        if (f < 0 || f > 2)
        {
            throw new IllegalArgumentException("f=" + f);
        }
        float old = masterVolumeFactor;
        if (f == old)
        {
            return;
        }
        masterVolumeFactor = f;
        pcs.firePropertyChange(PROP_MASTER_VOL_FACTOR, old, masterVolumeFactor);
    }

    /**
     * Get the default Jazz Midi Out device MidiFilter log config.
     * <p>
     * Can be used to adjust Midi Out log settings, e.g: getMidiOutLogConfig().add(MidiFilter.ConfigLog.LOG_ALL_PASSED);
     *
     * @return
     */
    public EnumSet<MidiFilter.ConfigLog> getMidiOutLogConfig()
    {
        return jjazzMidiOut.configLog;
    }

    /**
     * Filter out (discard) all Midi messages sent to Midi Out.
     * <p>
     * Fire a PROP_MIDI_OUT_FILTERING property change event.
     *
     * @param b Filtering is ON is b is true, OFF otherwise.
     */
    public void setMidiOutFiltering(boolean b)
    {
        if (b == isMidiOutFilteringOn())
        {
            return;
        }
        if (b)
        {
            panic();       // Stop all notes first
            saveFilterConfig = jjazzMidiOut.getFilterConfig();
            jjazzMidiOut.setFilterConfig(EnumSet.of(MidiFilter.Config.FILTER_EVERYTHING));
        } else
        {
            jjazzMidiOut.setFilterConfig(saveFilterConfig);
        }
        LOGGER.info("setMidiOutFiltering() b=" + b);
        pcs.firePropertyChange(PROP_MIDI_OUT_FILTERING, !b, b);
    }

    /**
     *
     * @return True if Midi out filtering is ON.
     */
    public boolean isMidiOutFilteringOn()
    {
        return jjazzMidiOut.getFilterConfig().contains(MidiFilter.Config.FILTER_EVERYTHING);
    }

    /**
     * Send the specified MidiMessages on the JJazzMidiOut device with timing -1 (immediate play).
     * <p>
     * Midi volume messages are multiplicated by the master volume factor.
     *
     * @param mms
     */
    public void sendMidiMessagesOnJJazzMidiOut(MidiMessage... mms)
    {
        if (receiverJJazzOut != null)
        {
            for (MidiMessage mm : mms)
            {
                if (masterVolumeFactor != 1f && (mm instanceof ShortMessage))
                {
                    ShortMessage sm = (ShortMessage) mm;
                    if (sm.getCommand() == ShortMessage.CONTROL_CHANGE && sm.getData1() == MidiConst.CTRL_CHG_VOLUME_MSB)
                    {
                        // It's a volume change, update the message
                        int vol = sm.getData2();
                        int newVol = Math.round(vol * masterVolumeFactor);
                        newVol = Math.min(127, newVol);
                        int channel = sm.getChannel();
                        try
                        {
                            sm.setMessage(ShortMessage.CONTROL_CHANGE, channel, MidiConst.CTRL_CHG_VOLUME_MSB, newVol);
                        } catch (InvalidMidiDataException ex)
                        {
                            LOGGER.warning("sendMidiMessagesOnJJazzMidiOut() problem applying master volume :" + ex.getLocalizedMessage());
                        }
                    }
                }
                receiverJJazzOut.send(mm, -1);
            }
        } else
        {
            LOGGER.warning("sendMidiMessagesOnJJazzMidiOut() receiverJJazzOut=null: no midi message sent.");
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    /**
     * Get a friendly name for a MidiDevice.
     * <p>
     * For now only used to rename the Java default synth (sometimes "Gervill") to JAVA_INTERNAL_SYNTH_NAME. Use DeviceInfo.name otherwise.
     *
     * @param md
     * @return
     */
    public String getDeviceFriendlyName(MidiDevice md)
    {
        if (md == null)
        {
            throw new NullPointerException("md");
        }
        String name = md.getDeviceInfo().getName();
        if (md == getDefaultJavaSynth())
        {
            name = JAVA_INTERNAL_SYNTH_NAME;
        }
        return name;
    }

    // ======================================================================================
    // Private methods
    // ======================================================================================
    private List<String> getDeviceListAsString(List<MidiDevice> mds)
    {
        List<String> res = new ArrayList<>();
        for (MidiDevice md : mds)
        {
            String s = "{name=" + md.getDeviceInfo() + ",maxReceivers=" + md.getMaxReceivers() + ",maxTransmitters=" + md.getMaxTransmitters() + "}";
            res.add(s);
        }
        return res;
    }

    /**
     * Find the MidiDevice whose Device.Info.name() is equals to mdName in the devices list.
     *
     * @param midiDevices
     * @param mdName
     * @return Null if not found.
     */
    private MidiDevice getMidiDevice(List<MidiDevice> midiDevices, String mdName)
    {
        MidiDevice res = null;
        for (MidiDevice md : midiDevices)
        {
            if (md.getDeviceInfo().getName().equals(mdName))
            {
                res = md;
                break;
            }
        }
        return res;
    }

    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {
            UpgradeManager um = UpgradeManager.getInstance();
            um.duplicateOldPreferences(prefs);
        }

    }

}

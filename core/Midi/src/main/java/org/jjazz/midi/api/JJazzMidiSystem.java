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
package org.jjazz.midi.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import org.jjazz.midi.api.device.MidiFilter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
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
import org.jjazz.midi.api.device.JJazzMidiDevice;
import org.jjazz.midi.api.device.MidiFilter.Config;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.modules.OnStop;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

/**
 * Manage the Midi IN and OUT devices for the JJazz application, plus some convenience methods.
 * <p>
 * Scan the available Midi IN/OUT devices at startup. Restore the default Midi IN/OUT devices when possible using Preferences.
 * <p>
 * The application should only connect to JJazzMidiIn and JJazzMidiOut virtual devices. These devices are implemented by a MidiFilter object, enabling filtering
 * and dumping. These devices are connected internally to the selected physical MIDI In/Out devices.
 * <p>
 * Manage a Midi master volume: a factor between 0 and 2 (default=1) which is used on all volume Midi messages.
 */
public final class JJazzMidiSystem
{

    private static JJazzMidiSystem INSTANCE = null;
    public final static String NOT_SET = "NotSet";
    public final static String JAVA_INTERNAL_SYNTH_NAME = "Java Synth (Gervill)";
    // Properties are also used as Preferences key
    public final static String PROP_MIDI_IN = "MidiInProp";
    public final static String PROP_MIDI_OUT = "MidiOutProp";
    public final static String PROP_MIDI_THRU = "MidiThruProp";
    public final static String PROP_MASTER_VOL_FACTOR = "MasterVolumeFactor";
    public final static String PROP_MIDI_OUT_FILTERING = "MidiOutFiltering";
    public final static String PREF_JAVA_SYNTH_SOUNDFONT_FILE = "JavaSynthSoundFontFile";
    public final static String PREF_EXTERNAL_MIDI_EDITOR_PATH = "MidiEditorPath";

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

    /**
     * The default system Sequencer
     */
    private Sequencer defaultSequencer;
    /**
     * The default system synth
     */
    private Synthesizer javaInternalSynth;
    private EnumSet<MidiFilter.Config> saveFilterConfig;

    /**
     * The Midi In/out virtual devices connected to the rest of the application.
     */
    private MidiFilter jjazzMidiIn;
    private MidiFilter jjazzMidiOut;
    private Soundbank lastLoadedSoundbank;
    private File lastLoadedSoundbankFile;
    private float masterVolumeFactor = 1;

    /**
     * The Preferences of this object.
     */
    private static final Preferences prefs = NbPreferences.forModule(JJazzMidiSystem.class);
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
     * Collect midi devices information, open the default sequencer, restore the default Midi devices, thru mode, send startup initialization messages, etc.
     */
    private JJazzMidiSystem()
    {
        // The IN/OUT virtual devices to be used by the application
        jjazzMidiIn = new MidiFilter("[JJazz Midi IN device]");

        // Discard some messages we should never need
        jjazzMidiIn.setFilterConfig(EnumSet.of(Config.FILTER_ACTIVE_SENSING,
                Config.FILTER_CHANNEL_PRESSURE,
                Config.FILTER_MIDI_TIME_CODE,
                Config.FILTER_POLY_PRESSURE,
                Config.FILTER_TIMING_CLOCK,
                Config.FILTER_TUNE_REQUEST
        ));

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

        // Get the sequencer : should get our implementation=JJazzLabSequencer (see JJazzLabSequencerProvider)        
        try
        {
            // Open the default sequencer which must be here.
            defaultSequencer = MidiSystem.getSequencer(false);
            defaultSequencer.open();

            // Connect the sequencer to the JJazzMidiOut device
            defaultSequencer.getTransmitter().setReceiver(jjazzMidiOut.getReceiver());
        } catch (MidiUnavailableException ex)
        {
            LOGGER.log(Level.SEVERE, "JJazzMidiSystem() No sequencer found on this system. Music can not be played ! {0}", ex.getMessage());
            defaultSequencer = null;
        }

        // Get the Java internal synth         
        try
        {
            javaInternalSynth = MidiSystem.getSynthesizer();
            javaInternalSynth.open();
        } catch (MidiUnavailableException ex)
        {
            LOGGER.log(Level.WARNING, "JJazzMidiSystem() problem getting Java internal synthesizer: {0}", ex.getMessage());

            // On Windows this can happen when there is no audio output available on the system, so it's worth notifying the user            
            NotifyDescriptor nd = new NotifyDescriptor.Message("Problem getting Java internal synth. Please check system audio output.",
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);

            javaInternalSynth = null;
        }

        // Try to restore default Midi OUT
        List<MidiDevice> outDevices = getOutDeviceList();
        LOGGER.log(Level.INFO, "JJazzMidiSystem() Midi out devices={0}", getDeviceListAsString(outDevices));
        defaultOutDevice = null;
        MidiDevice md = null;
        String mdName = prefs.get(PROP_MIDI_OUT, NOT_SET);
        try
        {
            if (mdName.equals(NOT_SET) || mdName.equals(JAVA_INTERNAL_SYNTH_NAME))
            {
                // By default use the default Java synth
                md = javaInternalSynth;
            } else
            {
                md = getMidiDevice(outDevices, mdName);
            }
            setDefaultOutDevice(md);

        } catch (MidiUnavailableException ex)
        {
            LOGGER.log(Level.WARNING, "JJazzMidiSystem() Can''t set default Midi OUT device to {0}: {1}", new Object[]
            {
                md.getDeviceInfo().getName(), ex.getMessage()
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
                    mdName, ex.getMessage()
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
                LOGGER.log(Level.WARNING, "JJazzMidiSystem() can''t access SoundFont file: {0}", synthFile.getAbsolutePath());
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
                LOGGER.log(Level.WARNING, "getInDeviceList() Can''t access Midi device : {0}", ex.getMessage());
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
                LOGGER.log(Level.WARNING, "getOutDeviceList() Can''t access Midi device : {0}", ex.getMessage());
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
     * Get the java sequencer opened and ready to play music on the JJazzMidiOutDevice.
     * <p>
     * In general you should use MusicController.acquireSequencer(Object lock), as it allows for access synchronization between various users.
     *
     * @return
     */
    public Sequencer getDefaultSequencer()
    {
        return defaultSequencer;
    }

    /**
     * The internal Java synth.
     *
     * @return Can be null if no java synth.
     */
    public Synthesizer getJavaInternalSynth()
    {
        return javaInternalSynth;
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
     * This unloads all previously loaded instruments in the internal Java Synth.
     * <p>
     * Note that the builtin GM instruments remain.
     */
    public void resetJavaInternalSynth()
    {
        Synthesizer synth = getJavaInternalSynth();
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
            LOGGER.log(Level.WARNING, "resetSynth() Can''t unload instruments from soundbank {0}:{1}", new Object[]
            {
                lastLoadedSoundbank.getName(),
                ex.getMessage()
            });
        }
    }

    /**
     * Edit the specified file with the registered editor, and wait for editor to exit.
     *
     * @param midiFile
     * @throws IOException
     */
    public void editMidiFileWithExternalEditor(File midiFile) throws IOException
    {
        checkNotNull(midiFile);
        File editorFile = getExternalMidiEditor();

        if (editorFile == null)
        {
            return;
        }

        // Check usual errors to get more friendly error messages
        if (editorFile.getName().isBlank())
        {
            String msg = ResUtil.getString(getClass(), "ErrNoExternalMidiEditorSet");
            throw new IOException(msg);
        }
        if (!editorFile.exists())
        {
            String msg = ResUtil.getString(getClass(), "ErrExternalMidiEditorNotFound", editorFile.getAbsolutePath());
            throw new IOException(msg);
        }
        if (!editorFile.canExecute() || editorFile.isDirectory())
        {
            String msg = ResUtil.getString(getClass(), "ErrCantExecuteMidiEditor", editorFile.getAbsolutePath());
            throw new IOException(msg);
        }
        if (!midiFile.exists())
        {
            String msg = ResUtil.getString(getClass(), "ErrMidiFileNotFound", midiFile.getAbsolutePath());
            throw new IOException(msg);
        }

        // Start command
        LOGGER.log(Level.INFO, "editMidiFileWithExternalEditor() starting external editor with: {0} {1}", new Object[]
        {
            editorFile.getAbsolutePath(),
            midiFile.getAbsolutePath()
        });
        ProcessBuilder builder = new ProcessBuilder(editorFile.getAbsolutePath(), midiFile.getAbsolutePath());
        Process process = builder.start();         // Throw IOException
        try
        {
            process.waitFor();      // Block until editor is closed
        } catch (InterruptedException ex)
        {
            throw new IOException(ex);
        }
    }

    /**
     * Set the path to an external Midi file editor.
     *
     * @param editorFile
     */
    public void setExternalMidiEditor(File editorFile)
    {
        Preconditions.checkNotNull(editorFile);
        String s = editorFile.getName().isBlank() ? "" : editorFile.getAbsolutePath();
        LOGGER.log(Level.FINE, "setExternalMidiEditor() -- s={0}", s);
        prefs.put(PREF_EXTERNAL_MIDI_EDITOR_PATH, s);
    }

    /**
     * Get the path to an external Midi file editor.
     * <p>
     *
     * @return Can be an empty path
     */
    public File getExternalMidiEditor()
    {
        File f = new File(prefs.get(PREF_EXTERNAL_MIDI_EDITOR_PATH, ""));
        LOGGER.log(Level.FINE, "getExternalMidiEditor() return value={0}", f.getName());
        return f;
    }

    /**
     * Try to load the soundfont2 (or DLS) file in the default Java synth.
     * <p>
     * Previous soundbank instruments are unloaded first. This triggers a specific task since loading a soundfont can take some time.
     *
     * @param f
     * @param silentRun If false wait until completion of the task and show progress bar. If true nothing is shown and method immediatly returns true.
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
        final AtomicBoolean resultOk = new AtomicBoolean(false);       // False by default
        Runnable run = () -> 
        {
            Soundbank newSb;
            Synthesizer synth;
            try
            {
                newSb = MidiSystem.getSoundbank(f);      // throw InvalidMidiDataException + IOException
                synth = getJavaInternalSynth();
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
                LOGGER.log(Level.WARNING, "loadSoundbankFileOnSynth() {0}", ex.getMessage());
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
                    LOGGER.log(Level.WARNING, "loadSoundbankFileOnSynth() Problem unloading Soundbank {0} : {1}", new Object[]
                    {
                        lastLoadedSoundbank.getName(),
                        ex.getMessage()
                    });
                    return;
                }
            }
            try
            {
                LOGGER.log(Level.INFO, "loadSoundbankFileOnSynth() start loading... Java Synth sound file {0}", f.getAbsolutePath());
                synth.loadAllInstruments(newSb);       // Ignore return value (false is incomplete loading) throw IllegalArgumentException
            } catch (IllegalArgumentException ex)
            {
                // Reload previous soundbank
                LOGGER.log(Level.WARNING, "loadSoundbankFileOnSynth() Problem loading Soundbank {0} : {1}", new Object[]
                {
                    newSb.getName(),
                    ex.getMessage()
                });
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
            LOGGER.log(Level.INFO, "loadSoundbankFileOnSynth() successfully loaded Java Synth sound file {0}", f.getAbsolutePath());
        };
        if (silentRun)
        {
            new Thread(run).start();
        } else
        {
            String msg = ResUtil.getString(getClass(), "LoadingJavaSynthSoundFile", f.getName());
            BaseProgressUtils.showProgressDialogAndRun(run, msg);     // Call is sectioning
        }
        return silentRun || resultOk.get();
    }

    /**
     * Connect the output of JJazzMidiOut device to this device.
     * <p>
     * Fire a PROP_MIDI_OUT property change event.property change event if device is modified.
     *
     * @param md Can be null.
     * @throws javax.sound.midi.MidiUnavailableException If exception occurs, the default OUT device is unchanged.
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
            } catch (MidiUnavailableException | SecurityException ex)
            {
                // Restore previous state
                md.close();
                defaultOutDevice = oldDevice;
                LOGGER.log(Level.WARNING, "setDefaultOutDevice() ex={0}", ex.getMessage());
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
            if (defaultOutDevice == getJavaInternalSynth())
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
     * Close the default out device.
     * <p>
     * Special handling of the Java Internal Synth.
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
        if (defaultOutDevice != getJavaInternalSynth())
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
                LOGGER.log(Level.WARNING, "setDefaultInDevice() ex={0}", ex.getMessage());
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
        thruFilter.setFilterConfig(b ? EnumSet.noneOf(MidiFilter.Config.class) : EnumSet.of(MidiFilter.Config.FILTER_EVERYTHING));
        prefs.putBoolean(PROP_MIDI_THRU, b);
        LOGGER.log(Level.INFO, "setThruMode() b={0}", b);
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
        sendMidiMessagesOnJJazzMidiOut(sms.toArray(ShortMessage[]::new));
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
        LOGGER.log(Level.INFO, "setMidiOutFiltering() b={0}", b);
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
                            LOGGER.log(Level.WARNING, "sendMidiMessagesOnJJazzMidiOut() problem applying master volume :{0}",
                                    ex.getMessage());
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

    /**
     * Find the MidiDevice whose Device.Info.name() is equals to mdName in the devices list.
     *
     * @param midiDevices
     * @param mdName      Can be null
     * @return Null if not found.
     */
    public MidiDevice getMidiDevice(List<MidiDevice> midiDevices, String mdName)
    {
        Preconditions.checkNotNull(midiDevices);
        return midiDevices.stream()
                .filter(md -> md.getDeviceInfo().getName().equals(mdName))
                .findAny()
                .orElse(null);
    }

    /**
     * Get the VirtualMidiSynth MidiDevice, if present on a Windows system.
     *
     * @return Can be null.
     */
    public MidiDevice getVirtualMidiSynthDevice()
    {
        if (!Utilities.isWindows())
        {
            return null;
        }

        Predicate<MidiDevice> isVirtualMidiSynth = md
                -> md != null && md.getDeviceInfo().getName().toLowerCase().contains("virtualmidisynt");

        MidiDevice res = null;
        MidiDevice md = getDefaultOutDevice();
        if (isVirtualMidiSynth.test(md))
        {
            res = md;
        } else
        {
            // Search the available MidiDevices for VMS
            for (MidiDevice mdi : getOutDeviceList())
            {
                if (isVirtualMidiSynth.test(mdi))
                {
                    res = mdi;
                    break;
                }
            }
        }
        return res;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String propName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propName, l);
    }

    public void removePropertyChangeListener(String propName, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(propName, l);
    }

    /**
     * Get a friendly name for a MidiDevice.
     * <p>
     * Remove any trailing "_MD" from the MidiDevice name. Use JAVA_INTERNAL_SYNTH_NAME for the "Gervill" Java internal synth. Use DeviceInfo.name otherwise.
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
        if (md == getJavaInternalSynth())
        {
            name = JAVA_INTERNAL_SYNTH_NAME;
        }
        else if (name.endsWith("_MD"))
        {
            name = name.substring(0, name.length() - 3);
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

    // ========================================================================================================
    // Private classes
    // ========================================================================================================
    /**
     * Close the default devices upon shutdown.
     */
    @OnStop
    static public class CloseMidiDevicesTask implements Runnable
    {
        @Override
        public void run()
        {
            LOGGER.info("CloseDevicesTask.run() Closing Midi devices");
            JJazzMidiSystem.getInstance().closeAll();
        }
    }


}

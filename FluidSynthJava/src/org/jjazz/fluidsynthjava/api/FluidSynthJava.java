/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2022 Jerome Lelasseux. All rights reserved.
 *
 *  You can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  Software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  Contributor(s): 
 */
package org.jjazz.fluidsynthjava.api;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import jdk.incubator.foreign.*;
import static org.jjazz.fluidsynthjava.jextract.fluidsynth_h.*;
import org.jjazz.utilities.api.Utilities;
import org.openide.modules.InstalledFileLocator;

/**
 * A Java wrapper of a FluidSynth instance.
 */
public final class FluidSynthJava
{

    // Static variables must be declared BEFORE the static block
    // IMPORTANT: libs order must be in reverse dependency order (e.g. libfluidsynth is last)
    private static final String[] LIBS_WIN_AMD64 = new String[]
    {
        "win/amd64/libintl-8.dll",
        "win/amd64/libglib-2.0-0.dll",
        "win/amd64/libgthread-2.0-0.dll",
        "win/amd64/libgobject-2.0-0.dll",
        "win/amd64/libsndfile-1.dll",
        "win/amd64/libgcc_s_sjlj-1.dll",
        "win/amd64/libwinpthread-1.dll",
        "win/amd64/libgomp-1.dll",
        "win/amd64/libstdc++-6.dll",
        "win/amd64/libinstpatch-2.dll",
        "win/amd64/libfluidsynth-3.dll"
    };

    private static final String[] LIBS_WIN_X86 = new String[]
    {
    };

    private static final String COMMAND_LINE_PROPERTY_FLUIDSYNTH_LIB = "fluidsynth.lib";
    private static final String LIB_NAME_MAC = "fluidsynth";

    public static final String PROP_CHORUS = "propChorus";
    public static final String PROP_REVERB = "propReverb";
    public static final String PROP_GAIN = "propGain";
    public static final boolean LIBRARIES_LOADED_OK;

    private static final Logger LOGGER = Logger.getLogger(FluidSynthJava.class.getSimpleName());


    private MemoryAddress fluid_settings_ma;
    private MemoryAddress fluid_synth_ma;
    private MemoryAddress fluid_driver_ma;
    private Reverb reverb;
    private Chorus chorus;
    private File lastLoadedSoundFontFile;
    private int lastLoadedSoundFontFileId = -1;
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);

    /**
     * Load the DLLs upon startup
     */
    static
    {
        LIBRARIES_LOADED_OK = loadNativeLibraries();
    }


    /**
     * Create a JavaFluidSynth object.
     * <p>
     * Use open() to allocate the native resources.
     */
    public FluidSynthJava()
    {
    }

    /**
     * Create a JavaFluidSynth object from another one.
     * <p>
     * If jfs native resources are allocated, create the native resources initialized with the same values for: reverb, chorus,
     * gain, soundfont file (if loaded), synth.device-id.
     *
     * @param jfs               Must be already open
     * @param createAudioDriver If true create the associated audio driver
     * @throws org.jjazz.fluidsynthjava.api.FluidSynthException
     */
    public FluidSynthJava(FluidSynthJava jfs, boolean createAudioDriver) throws FluidSynthException
    {
        assert jfs.isOpen();


        fluid_settings_ma = new_fluid_settings();
        fluid_synth_ma = new_fluid_synth(fluid_settings_ma);
        if (fluid_synth_ma == null)
        {
            throw new FluidSynthException("Can't create native FluidSynth instance");
        }
        setGain(jfs.getGain());
        setReverb(jfs.getReverb());
        setChorus(jfs.getChorus());
        assert setSetting("synth.device-id", jfs.getSettingInt("synth.device-id"));


        if (createAudioDriver && jfs.getNativeAudioDriverInstance() != null)
        {
            fluid_driver_ma = new_fluid_audio_driver(fluid_settings_ma, fluid_synth_ma);
            // TODO copy driver settings! 
        }


        File f = jfs.getLastLoadedSoundFontFile();
        if (f != null)
        {
            try
            {
                lastLoadedSoundFontFileId = loadSoundFont(f);
            } catch (FluidSynthException ex)
            {
                close();
                throw ex;
            }
        }
    }

    /**
     * Allocate the native resources : settings, synth and audio driver.
     *
     * @param createAudioDriver If true create the associated audio driver
     * @throws org.jjazz.fluidsynthjava.api.FluidSynthException
     */
    public void open(boolean createAudioDriver) throws FluidSynthException
    {
        if (!LIBRARIES_LOADED_OK)
        {
            throw new FluidSynthException("FluidSynth libraries not loaded, please check the log messages");
        }

        if (isOpen())
        {
            return;
        }

        fluid_settings_ma = new_fluid_settings();
        fluid_synth_ma = new_fluid_synth(fluid_settings_ma);
        if (fluid_synth_ma == null)
        {
            throw new FluidSynthException("Error creating native FluidSynth synth instance");
        }

        setDeviceIdForXGCompatibility();


        if (createAudioDriver)
        {
            fluid_driver_ma = new_fluid_audio_driver(fluid_settings_ma, fluid_synth_ma);
            if (fluid_driver_ma == null)
            {
                close();
                throw new FluidSynthException("Error creating native FluidSynth audio driver");
            }
        }

        LOGGER.info("open() Native FluidSynth instance initialized");
    }

    /**
     * Set the synth device Id for XG System ON compatibility.
     * <p>
     * IMPORTANT: FluidSynth 2.3.0 (should be fixed in 2.3.1) expects a special XG System ON message (3rd byte is NOT 0001nnnn
     * with n the deviceId), which differs from the standard one See https://github.com/FluidSynth/fluidsynth/issues/1092 Changing
     * the deviceId to 16 is a trick to make it react to the standard XG System ON
     */
    private void setDeviceIdForXGCompatibility()
    {
        assert setSetting("synth.device-id", 16);
    }


    /**
     * Check if the default FluidSynth instance is opened.
     *
     * @return
     */
    public boolean isOpen()
    {
        return fluid_synth_ma != null;
    }

    /**
     * Close and release the native resources.
     */
    public void close()
    {
        if (fluid_driver_ma != null)
        {
            delete_fluid_audio_driver(fluid_driver_ma);
        }
        if (fluid_synth_ma != null)
        {
            delete_fluid_synth(fluid_synth_ma);
        }
        if (fluid_settings_ma != null)
        {
            delete_fluid_settings(fluid_settings_ma);
        }
        fluid_driver_ma = fluid_synth_ma = fluid_settings_ma = null;
        lastLoadedSoundFontFile = null;
        lastLoadedSoundFontFileId = -1;

        LOGGER.info("close() Native FluidSynth instance closed");
    }

    public MemoryAddress getNativeFluidSynthInstance()
    {
        return fluid_synth_ma;
    }

    public MemoryAddress getNativeSettingsInstance()
    {
        return fluid_settings_ma;
    }

    public MemoryAddress getNativeAudioDriverInstance()
    {
        return fluid_driver_ma;
    }


    /**
     * The last successfully loaded soundfont file.
     *
     * @return Can be null
     * @see #loadSoundFont(java.io.File)
     */
    public File getLastLoadedSoundFontFile()
    {
        return lastLoadedSoundFontFile;
    }

    /**
     * The id of the last successfully loaded soundfont file.
     *
     * @return -1 if not available
     * @see #loadSoundFont(java.io.File)
     */
    public int getLastLoadedSoundFontFileId()
    {
        return lastLoadedSoundFontFileId;
    }

    /**
     * Send a ShortMessage to the native FluidSynth instance.
     *
     * @param sm
     */
    public void sendShortMessage(ShortMessage sm)
    {
        switch (sm.getCommand())
        {
            case ShortMessage.NOTE_ON ->
            {
                int vel = sm.getData2();
                if (vel > 0)
                {
                    fluid_synth_noteon(fluid_synth_ma, sm.getChannel(), sm.getData1(), vel);
                } else
                {
                    fluid_synth_noteoff(fluid_synth_ma, sm.getChannel(), sm.getData1());
                }
            }

            case ShortMessage.NOTE_OFF ->
                fluid_synth_noteoff(fluid_synth_ma, sm.getChannel(), sm.getData1());

            case ShortMessage.PROGRAM_CHANGE ->
                fluid_synth_program_change(fluid_synth_ma, sm.getChannel(), sm.getData1());

            case ShortMessage.CONTROL_CHANGE ->
                fluid_synth_cc(fluid_synth_ma, sm.getChannel(), sm.getData1(), sm.getData2());

            default ->
            {
                int status = sm.getStatus();
                if (status == ShortMessage.SYSTEM_RESET)
                {
                    fluid_synth_system_reset(fluid_synth_ma);
                }
            }
        }
    }

    /**
     * Send a SysexMessage to the native FluidSynth instance.
     * <p>
     * For XG ON sysex message to works, synth.device-id must be 16 (up to FluidSynth 2.3.0) ! See
     * https://github.com/FluidSynth/fluidsynth/issues/1092
     *
     * @param sm
     */
    public void sendSysexMessage(SysexMessage sm)
    {
        byte[] data = sm.getData(); // Does not contain the leading byte 0xF0 

        // FluidSynth does not expect the leading 0xF0 nor the last 0xF7 
        byte[] fluidData = Arrays.copyOfRange(data, 0, data.length - 1);

        try ( var scope = ResourceScope.newConfinedScope())
        {
            SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
            var fluidData_ma = allocator.allocateArray(CLinker.C_CHAR, fluidData);
            var handled_seg = SegmentAllocator.ofScope(scope).allocate(CLinker.C_INT, 0);
            fluid_synth_sysex(fluid_synth_ma,
                    fluidData_ma,
                    fluidData.length,
                    MemoryAddress.NULL, MemoryAddress.NULL, handled_seg, 0);
            int handled = handled_seg.toIntArray()[0];
        }

    }

    /**
     * Set a setting which use a String value on the native settings instance.
     *
     * @param setting
     * @param value
     *
     * @return True if operation was successful
     */
    public boolean setSetting(String setting, String value)
    {
        try ( var scope = ResourceScope.newConfinedScope())
        {
            var setting_seg = CLinker.toCString(setting, scope);
            var value_seg = CLinker.toCString(value, scope);
            return fluid_settings_setstr(fluid_settings_ma, setting_seg, value_seg) == FLUID_OK();
        }
    }

    /**
     * Set a setting which use a double value on the native Settings instance.
     *
     * @param setting
     * @param value
     * @return True if operation was successful
     */
    public boolean setSetting(String setting, double value)
    {
        try ( var scope = ResourceScope.newConfinedScope())
        {
            var setting_seg = CLinker.toCString(setting, scope);
            return fluid_settings_setnum(fluid_settings_ma, setting_seg, value) == FLUID_OK();
        }
    }


    /**
     * Set a setting which use an int value on the native Settings instance.
     *
     * @param setting
     * @param value
     * @return True if operation was successful
     */
    public boolean setSetting(String setting, int value)
    {
        try ( var scope = ResourceScope.newConfinedScope())
        {
            var setting_seg = CLinker.toCString(setting, scope);
            return fluid_settings_setint(fluid_settings_ma, setting_seg, value) == FLUID_OK();
        }
    }

    /**
     * Get a string value setting of the native Settings instance.
     *
     * @param setting
     * @return
     */
    public String getSettingString(String setting)
    {
        try ( var scope = ResourceScope.newConfinedScope())
        {
            var setting_seg = CLinker.toCString(setting, scope);
            var value_seg = SegmentAllocator.ofScope(scope).allocate(256);
            fluid_settings_copystr(fluid_settings_ma, setting_seg, value_seg, 256);
            return CLinker.toJavaString(value_seg);
        }
    }

    /**
     * Get an int value setting of the native Settings instance.
     *
     * @param setting
     * @return
     */
    public int getSettingInt(String setting)
    {
        try ( var scope = ResourceScope.newConfinedScope())
        {
            var setting_seg = CLinker.toCString(setting, scope);
            var value_seg = SegmentAllocator.ofScope(scope).allocate(CLinker.C_INT, 0);
            fluid_settings_getint(fluid_settings_ma, setting_seg, value_seg);
            return value_seg.toIntArray()[0];
        }
    }


    /**
     * Get a double value setting from the native Settings instance.
     *
     * @param setting
     * @return
     */
    public double getSettingDouble(String setting)
    {
        try ( var scope = ResourceScope.newConfinedScope())
        {
            var setting_seg = CLinker.toCString(setting, scope);
            var value_seg = SegmentAllocator.ofScope(scope).allocate(CLinker.C_DOUBLE, 0d);
            fluid_settings_getnum(fluid_settings_ma, setting_seg, value_seg);
            return value_seg.toDoubleArray()[0];
        }
    }

    /**
     * Set gain for the native FluidSynth instance.
     *
     * @param gain 0 to 10
     */
    public void setGain(float gain)
    {
        float old = getGain();
        if (old != gain)
        {
            // Better to not call set_gain() if unchanged value, it seems it still impacts sound
            fluid_synth_set_gain(fluid_synth_ma, gain);
            pcs.firePropertyChange(PROP_GAIN, old, gain);
        }
    }


    /**
     * Get the gain of the native FluidSynth instance.
     * <p>
     * @return
     */
    public float getGain()
    {
        return fluid_synth_get_gain(fluid_synth_ma);
    }


    /**
     * Set the Reverb of the native synth instance.
     *
     * @param newReverb
     * @return True if reverb was successfully modified.
     */
    public boolean setReverb(Reverb newReverb)
    {
        Reverb old = reverb;
        if (Objects.equals(newReverb, old))
        {
            // Better to not call setSetting() if value unchanged, because it still changes the sound for a short time
            return true;
        }
        boolean b = setSetting("synth.reverb.damp", newReverb.damp());
        b &= setSetting("synth.reverb.level", newReverb.level());
        b &= setSetting("synth.reverb.room-size", newReverb.room());
        b &= setSetting("synth.reverb.width", newReverb.width());

        reverb = newReverb;
        pcs.firePropertyChange(PROP_REVERB, old, reverb);

        return b;
    }

    /**
     * Get the Reverb of the native synth instance.
     *
     * @return
     */
    public Reverb getReverb()
    {
        if (reverb != null)
        {
            // Reverb was explicitly set, just return it
            return reverb;
        }

        // Get data  from the synth        
        float damp = (float) getSettingDouble("synth.reverb.damp");
        float level = (float) getSettingDouble("synth.reverb.level");
        float room = (float) getSettingDouble("synth.reverb.room-size");
        float width = (float) getSettingDouble("synth.reverb.width");
        return new Reverb(null, room, damp, width, level);
    }

    /**
     * Set the Chorus of the native Synth instances.
     *
     * @param newChorus
     * @return True if chorus was successfully modified.
     */
    public boolean setChorus(Chorus newChorus)
    {
        Chorus old = chorus;
        if (Objects.equals(newChorus, old))
        {
            // Better to not call setSetting() even if unchanged value, because it still changes the sound for a short time
            return true;
        }
        boolean b = setSetting("synth.chorus.depth", newChorus.depth());
        b &= setSetting("synth.chorus.level", newChorus.level());
        b &= setSetting("synth.chorus.nr", newChorus.nr());
        b &= setSetting("synth.chorus.speed", newChorus.speed());
        b &= (fluid_synth_set_chorus_group_type(fluid_synth_ma, -1, newChorus.type()) == FLUID_OK());

        chorus = newChorus;
        pcs.firePropertyChange(PROP_CHORUS, old, chorus);

        return b;
    }


    /**
     * Get the Chorus of the native Synth instance.
     *
     * @return
     */
    public Chorus getChorus()
    {
        if (chorus != null)
        {
            // Chorus was explicitly set vis setChorus(Chorus), just return it
            return chorus;
        }

        // Get data  from the synth
        float depth = (float) getSettingDouble("synth.chorus.depth");
        float level = (float) getSettingDouble("synth.chorus.level");
        int nr = getSettingInt("synth.chorus.nr");
        float speed = (float) getSettingDouble("synth.chorus.speed");
        int type = 0;
        try ( var scope = ResourceScope.newConfinedScope())
        {
            var value_seg = SegmentAllocator.ofScope(scope).allocate(CLinker.C_INT, 0);
            fluid_synth_get_chorus_group_type(fluid_synth_ma, -1, value_seg);
            type = value_seg.toIntArray()[0];
        }
        return new Chorus(null, nr, speed, depth, type, level);
    }


    /**
     * Generate a .wav file from midiFile.
     * <p>
     * From "Fast file renderer for non-realtime MIDI file rendering" https://www.fluidsynth.org/api/FileRenderer.html.
     *
     * @param midiFile The input Midi file
     * @param wavFile  The wav file to be created
     * @throws org.jjazz.fluidsynthjava.api.FluidSynthException
     */
    public void generateWavFile(File midiFile, File wavFile) throws FluidSynthException
    {
        if (!midiFile.canRead())
        {
            throw new FluidSynthException("Can't access midiFile=" + midiFile.getAbsolutePath());
        }
        String midiFilePath = midiFile.getAbsolutePath();
        String wavFilePath = wavFile.getAbsolutePath();


        // Create a synth copy
        FluidSynthJava synthCopy = new FluidSynthJava(this, false);


        // specify the file to store the audio to
        // make sure you compiled fluidsynth with libsndfile to get a real wave file
        // otherwise this file will only contain raw s16 stereo PCM
        assert synthCopy.setSetting("audio.file.name", wavFilePath);
        assert synthCopy.setSetting("audio.file.type", "wav");
        // use number of samples processed as timing source, rather than the system timer
        assert synthCopy.setSetting("player.timing-source", "sample");
        // Since this is a non-realtime scenario, there is no need to pin the sample data
        assert synthCopy.setSetting("synth.lock-memory", 0);     // 1 by default                    


        // Prepare the player
        MemoryAddress synth_ma = synthCopy.getNativeFluidSynthInstance();
        MemoryAddress fluid_player_ma = new_fluid_player(synth_ma);
        assert fluid_player_ma != null;
        try ( var scope = ResourceScope.newConfinedScope())
        {
            var midiPath_seg = CLinker.toCString(midiFilePath, scope);
            assert fluid_player_add(fluid_player_ma, midiPath_seg) == FLUID_OK();
        }
        assert fluid_player_play(fluid_player_ma) == FLUID_OK();


        // Render music to file using FluidSynth's own player
        boolean error = false;
        MemoryAddress renderer_ma = new_fluid_file_renderer(synth_ma);
        assert renderer_ma != null;
        while (fluid_player_get_status(fluid_player_ma) == FLUID_PLAYER_PLAYING())
        {
            // LOGGER.severe(" - playing...");
            if (fluid_file_renderer_process_block(renderer_ma) != FLUID_OK())
            {
                error = true;
                break;
            }
        }


        // just for sure: stop the playback explicitly and wait until finished
        assert fluid_player_stop(fluid_player_ma) == FLUID_OK();
        assert fluid_player_join(fluid_player_ma) == FLUID_OK();
        delete_fluid_file_renderer(renderer_ma);
        delete_fluid_player(fluid_player_ma);


        synthCopy.close();


        if (error)
        {
            throw new FluidSynthException("Problem while generating wav file " + wavFile.getAbsolutePath());
        }

    }


    /**
     * Load a soundfont file in the native FluidSynth instance.
     *
     * @param f
     * @return The soundfont id
     * @throws FluidSynthException
     */
    public int loadSoundFont(File f) throws FluidSynthException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);
        }

        var sfont_path_native = CLinker.toCString(f.getAbsolutePath(), ResourceScope.newImplicitScope());
        lastLoadedSoundFontFileId = fluid_synth_sfload(fluid_synth_ma,
                sfont_path_native,
                1); // 1: re-assign presets for all MIDI channels (equivalent to calling fluid_synth_program_reset())           

        if (lastLoadedSoundFontFileId == FLUID_FAILED())
        {
            String msg = "Loading soundfont failed f=" + f.getAbsolutePath();
            LOGGER.log(Level.SEVERE, "loadSoundFont() {0}", msg);
            lastLoadedSoundFontFile = null;
            throw new FluidSynthException(msg);
        }


        LOGGER.log(Level.INFO, "loadSoundFont() SoundFont successfully loaded {0}", f.getAbsolutePath());

        lastLoadedSoundFontFile = f;
        return lastLoadedSoundFontFileId;
    }

    /**
     * Unload a soundfont from the native FluidSynth instance.
     *
     * @param sfId A Soundfont id returned by loadSoundfont().
     * @throws FluidSynthException
     * @see #loadSoundFont(java.io.File)
     */
    public void unloadSoundfont(int sfId) throws FluidSynthException
    {
        if (fluid_synth_sfunload(fluid_synth_ma, sfId, 1) == FLUID_FAILED())
        {
            String msg = "Unloading soundfont id=" + sfId + " failed";
            LOGGER.log(Level.SEVERE, "unloadSoundfont() {0}", msg);
            throw new FluidSynthException(msg);
        }

    }


    public void playTestNotes()
    {

        for (int i = 0; i < 12; i++)
        {
            int key = 60 + i;
            fluid_synth_noteon(fluid_synth_ma, 0, key, 80);
            try
            {
                Thread.sleep(500);
            } catch (InterruptedException ex)
            {
                Logger.getLogger(FluidSynthJava.class.getName()).log(Level.SEVERE, null, ex);
            }
            fluid_synth_noteoff(fluid_synth_ma, 0, key);
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
     * Check if the current platform is supported.
     *
     * @return
     */
    static public boolean isPlatformSupported()
    {
        return Utilities.isWindows() || Utilities.isLinux() || Utilities.isMac();
    }

// =============================================================================================
// Private methods
// =============================================================================================    
// =============================================================================================
// STATIC Private methods
// ============================================================================================= 
    /**
     * Load the DLLs.
     *
     * @return True if load was succesfull
     */
    static private boolean loadNativeLibraries()
    {
        boolean error = false;

        if (Utilities.isWindows())
        {
            error = !loadNativeLibrariesWin();
        } else if (Utilities.isLinux())
        {
            error = !loadNativeLibrariesLinuxMac();
        } else if (Utilities.isMac())
        {
            error = !loadNativeLibrariesLinuxMac();
        } else
        {
            LOGGER.log(Level.WARNING, "loadNativeLibraries() Platform not supported os.name={0}", System.getProperty("os.name", "XX").toLowerCase(Locale.ENGLISH));
        }

        if (!error)
        {
            LOGGER.info("loadNativeLibraries() Success");
        }

        return !error;
    }

    /**
     * Load on Linux or Mac.
     * <p>
     * Can't manage to make System.loadLibrary("fluidsynth") to work, see
     * https://stackoverflow.com/questions/74604651/system-loadlibrary-cant-find-a-shared-library-on-linux
     * <p>
     * But System.load(path_to_fluidsynth.so.xx) works fine on linux and mac, and no need to explicitly load dependencies like on windows.
     *
     * @return
     */
    private static boolean loadNativeLibrariesLinuxMac()
    {
        String libPath = System.getProperty(COMMAND_LINE_PROPERTY_FLUIDSYNTH_LIB, null);
        if (libPath == null)
        {
            LOGGER.warning("loadNativeLibrariesLinux() " + COMMAND_LINE_PROPERTY_FLUIDSYNTH_LIB + " is not defined");
            return false;
        }
                
        try
        {
            System.load(libPath);
        } catch (SecurityException | UnsatisfiedLinkError ex)
        {
            LOGGER.log(Level.SEVERE, "loadNativeLibrariesLinux() Can''t load native library {0}: {1}", new Object[]{libPath, ex.getMessage()});
            return false;
        }

        return true;
    }

    private static boolean loadNativeLibrariesMac()
    {
        try
        {
            System.loadLibrary(LIB_NAME_MAC);
        } catch (SecurityException | UnsatisfiedLinkError ex)
        {
            LOGGER.log(Level.SEVERE,"loadNativeLibrariesMac() Can't load native library " + LIB_NAME_MAC + " ({0}). ex={1}", new Object[]{System.mapLibraryName(LIB_NAME_MAC),
                ex.getMessage()});
            return false;
        }

        return true;
    }

    private static boolean loadNativeLibrariesWin()
    {
        boolean error = false;

        // System.loadLibrary("libfluidsynth-3.dll") does not work within IDE (.dll file not found, maybe it works if deployed), but in addition there is the problem
        // of additional dependent dlls, which are loaded using native system => Need to manually load them in reverse dependence order.
        // Use "cycheck -v ./xxx.dll" to get the DLL dependency tree.
        String[] libs = getWinFluidSynthLibs();
        if (libs.length == 0)
        {
            LOGGER.log(Level.SEVERE, "loadNativeLibraries() No libs found for os={0} and arch={1}", new Object[]{System.getProperty("os.name"),
                System.getProperty("os.arch")});
            error = true;

        } else
        {

            for (String lib : libs)
            {
                String modulePath = "modules/fluidsynth/" + lib;
                File f = InstalledFileLocator.getDefault().locate(modulePath, "org.jjazz.fluidsynthjava", false);
                if (f == null)
                {
                    LOGGER.log(Level.SEVERE, "loadNativeLibraries() Can''t find lib from modulePath={0}", modulePath);
                    error = true;
                    break;
                }
                String path = f.getAbsolutePath();
                try
                {
                    LOGGER.log(Level.FINE, "loadNativeLibraries() loading {0}", path);
                    System.load(path);
                } catch (SecurityException | UnsatisfiedLinkError ex)
                {
                    LOGGER.log(Level.SEVERE, "loadNativeLibraries() Can''t load lib={0}. Ex={1}", new Object[]{path, ex.getMessage()});
                    error = true;
                    break;
                }
            }

        }
        return !error;
    }

    /**
     * Get the relative path for the required FluidSynth libs in *reverse* dependency order.
     * <p>
     *
     * @return Empty if no relevant libraries found.
     */
    static private String[] getWinFluidSynthLibs()
    {
        String[] res = new String[0];

        switch (System.getProperty("os.arch"))
        {
            case "amd64" ->
                res = LIBS_WIN_AMD64;
            case "x86" ->
                res = LIBS_WIN_X86;
        }

        return res;
    }


}

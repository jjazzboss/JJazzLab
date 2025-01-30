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

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.embeddedsynth.api.EmbeddedSynth;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.jjazz.fluidsynthjava.api.Chorus;
import org.jjazz.fluidsynthjava.api.FluidSynthException;
import org.jjazz.fluidsynthjava.api.FluidSynthJava;
import org.jjazz.fluidsynthjava.api.Reverb;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.utilities.api.ResUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;
import org.openide.util.NbPreferences;

/**
 * A FluidSynth-based implementation of an EmbeddedSynth.
 */
public class FluidSynthEmbeddedSynth implements EmbeddedSynth, PropertyChangeListener
{

    private static final String SOUNDFONT_FILE = "modules/soundfont/JJazzLab-SoundFont.sf2";    // SoundFont name in local config directory
    private static final String SOUNDFONT_FILE_COMMAND_LINE_PROPERTY = "FluidSynthSoundFontFile";  // Command line property to override default SOUNDFONT_FILE
    private static final String PREF_REVERB = "PrefReverb";
    private static final String PREF_CHORUS = "PrefChorus";
    private static final String PREF_GAIN = "PrefGain";
    private static final float DEFAULT_GAIN = 2.0f;
    private static final Reverb DEFAULT_REVERB = Reverb.ROOM_REVERB;
    private static final Chorus DEFAULT_CHORUS = Chorus.NORMAL_CHORUS;
    private static final List<Reverb> REVERB_PRESETS = Arrays.asList(Reverb.ZERO_REVERB, Reverb.SMALL_ROOM_REVERB, Reverb.ROOM_REVERB,
            Reverb.HALL_REVERB, Reverb.LARGE_HALL_REVERB);
    private static final List<Chorus> CHORUS_PRESETS = Arrays.asList(Chorus.ZERO_CHORUS, Chorus.NORMAL_CHORUS, Chorus.SLOW_CHORUS,
            Chorus.THICK_CHORUS);

    private FluidSynthJava fluidSynth;
    private File soundFontFile;
    private OutputSynth outputSynth;
    private static final Preferences prefs = NbPreferences.forModule(FluidSynthEmbeddedSynth.class);
    private static final Logger LOGGER = Logger.getLogger(FluidSynthEmbeddedSynth.class.getSimpleName());

    public FluidSynthEmbeddedSynth()
    {
        assert REVERB_PRESETS.contains(DEFAULT_REVERB);
        assert CHORUS_PRESETS.contains(DEFAULT_CHORUS);
    }


    /**
     * Redirect to setSoundFontFile().
     *
     * @param config Must be a soundfont file.
     * @see #setSoundFontFile(java.io.File)
     */
    @Override
    public void configure(Object config)
    {
        if (config instanceof File file)
        {
            setSoundFontFile(file);
        } else
        {
            LOGGER.log(Level.WARNING, "configure() Invalid config file, ignored. config={0}", config);
        }
    }

    /**
     * Set the soundfont file to be used when opening the device.
     * <p>
     * If synth is already opened, the file will be used only on the next open().
     *
     * @param f Can be null.
     * @see #getSoundFontFile()
     */
    public void setSoundFontFile(File f)
    {
        soundFontFile = f;
    }

    /**
     * Get the SoundFont file to be used when opening the synth.
     * <p>
     * The method uses the 3 possibilities in this specified order: <br>
     * 1/ If it's defined, use file from the SOUNDFONT_FILE_COMMAND_LINE_PROPERTY command line property<br>
     * 2/ If non-null, use the file set using configure() or setSoundFontFile()<br>
     * 3/ If it exists, use the file bundled in this module at location SOUNDFONT_FILE<br>
     *
     * @return
     * @throws org.jjazz.fluidsynthjava.api.FluidSynthException
     */
    public File getSoundFontFile() throws FluidSynthException
    {
        File f = null;
        ModuleInfo ownerModule = Modules.getDefault().ownerOf(getClass());  // Might be null if class is in a normal jar

        // First check command line parameter
        String cmdLinePath = System.getProperty(SOUNDFONT_FILE_COMMAND_LINE_PROPERTY);
        if (cmdLinePath != null)
        {
            f = new File(cmdLinePath);
            LOGGER.log(Level.FINE, "Using SoundFont file from property " + SOUNDFONT_FILE_COMMAND_LINE_PROPERTY + "={0}", cmdLinePath);
        } else if (soundFontFile != null)
        {
            f = soundFontFile;
            LOGGER.log(Level.FINE, "Using SoundFont file {0}", soundFontFile.getAbsolutePath());
        } else if (ownerModule != null)
        {
            f = InstalledFileLocator.getDefault().locate(SOUNDFONT_FILE, ownerModule.getCodeNameBase(), false);
            LOGGER.log(Level.FINE, "Using SoundFont file from current module (" + SOUNDFONT_FILE + ")");
        }

        if (!fastCheckSoundFontFile(f))
        {
            throw new FluidSynthException(ResUtil.getString(getClass(), "InvalidSoundFontFile", (f == null ? "null" : f.getAbsolutePath())));
        }

        return f;

    }

    /**
     * Initialize the FluidSynth java bridge and load the soundfont.
     *
     * @throws EmbeddedSynthException
     */
    @Override
    public void open() throws EmbeddedSynthException
    {

        fluidSynth = new FluidSynthJava();

        try
        {
            fluidSynth.open(true);      // throws FluidSynthException

            File f = getSoundFontFile();    // throws FluidSynthException
            fluidSynth.loadSoundFont(f);    // throws FluidSynthException


            // Listen to reverb/chorus/gain changes to save them as preferences
            fluidSynth.addPropertyChangeListener(this);

            // Restore settings from preferences
            fluidSynth.setGain(prefs.getFloat(PREF_GAIN, DEFAULT_GAIN));
            Reverb r = getReverbPreset(prefs.get(PREF_REVERB, null));
            fluidSynth.setReverb(r == null ? DEFAULT_REVERB : r);
            Chorus c = getChorusPreset(prefs.get(PREF_CHORUS, null));
            fluidSynth.setChorus(r == null ? DEFAULT_CHORUS : c);

        } catch (FluidSynthException ex)
        {
            close();
            throw new EmbeddedSynthException(ex.getMessage());
        }

        outputSynth = OS_FluidSynthEmbedded.getInstance();
    }

    @Override
    public boolean isOpen()
    {
        return fluidSynth != null;
    }

    public FluidSynthJava getFluidSynthJava()
    {
        return fluidSynth;
    }

    /**
     * Get the associated OutputSynth.
     *
     * @return Null if synth is not opened
     */
    @Override
    public final OutputSynth getOutputSynth()
    {
        return outputSynth;
    }

    @Override
    public void close()
    {
        if (isOpen())
        {
            fluidSynth.close();
            fluidSynth.removePropertyChangeListener(this);
            fluidSynth = null;
        }
    }

    @Override
    public String getName()
    {
        return "FluidSynth";
    }

    @Override
    public String getVersion()
    {
        return "1.0";
    }

    @Override
    public void showSettings(Component c)
    {
        var dialog = new SettingsDialog(this);
        dialog.setLocationRelativeTo(c);
        dialog.setVisible(true);
    }

    @Override
    public void generateWavFile(File midiFile, File wavFile) throws EmbeddedSynthException
    {
        try
        {
            fluidSynth.generateWavFile(midiFile, wavFile);
        } catch (FluidSynthException ex)
        {
            throw new EmbeddedSynthException(ex.getMessage());
        }
    }

    /**
     * Set the associated OutputSynth.
     *
     * @param os
     */
    public void setOutputSynth(OutputSynth os)
    {
        Objects.requireNonNull(os);
        outputSynth = os;
    }

    /**
     * Fast check that this is the expected soundfont file.
     *
     * @param f
     * @return
     */
    static public boolean fastCheckSoundFontFile(File f)
    {
        if (f == null || !f.isFile())
        {
            return false;
        }
        long fileSize = f.length();
        return fileSize > 10;
    }

    protected Reverb[] getReverbPresets()
    {
        return REVERB_PRESETS.toArray(Reverb[]::new);
    }

    protected Chorus[] getChorusPresets()
    {
        return CHORUS_PRESETS.toArray(Chorus[]::new);
    }

    // ===========================================================================================
    // PropertyChangeListener interface
    // ===========================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == fluidSynth)
        {
            switch (evt.getPropertyName())
            {
                case FluidSynthJava.PROP_CHORUS ->
                    prefs.put(PREF_CHORUS, ((Chorus) evt.getNewValue()).name());
                case FluidSynthJava.PROP_REVERB ->
                    prefs.put(PREF_REVERB, ((Reverb) evt.getNewValue()).name());
                case FluidSynthJava.PROP_GAIN ->
                    prefs.putFloat(PREF_GAIN, (float) evt.getNewValue());
                default ->
                    throw new IllegalStateException("evt.getPropertyName()=" + evt.getPropertyName());
            }
        }
    }
    // ===========================================================================================
    // Private methods
    // ===========================================================================================

    private Reverb getReverbPreset(String name)
    {
        return REVERB_PRESETS.stream()
                .filter(r -> r.name().equals(name))
                .findAny()
                .orElse(null);
    }

    private Chorus getChorusPreset(String name)
    {
        return CHORUS_PRESETS.stream()
                .filter(r -> r.name().equals(name))
                .findAny()
                .orElse(null);
    }

}

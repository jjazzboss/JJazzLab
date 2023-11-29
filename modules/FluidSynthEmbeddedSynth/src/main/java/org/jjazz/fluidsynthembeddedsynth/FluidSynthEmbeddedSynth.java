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
package org.jjazz.fluidsynthembeddedsynth;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;
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
import org.openide.util.NbPreferences;

/**
 * A FluidSynth-based implementation of an EmbeddedSynth.
 */
public class FluidSynthEmbeddedSynth implements EmbeddedSynth, PropertyChangeListener
{

    private static final String SOUNDFONT_FILE = "modules/soundfont/JJazzLab-SoundFont.sf2";    // SoundFont name in local config directory
    private static final String SOUNDFONT_FILE_COMMAND_LINE_PROPERTY = "jjSynthSoundFontFile";  // Command line property to override default SOUNDFONT_FILE
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
    private static final Preferences prefs = NbPreferences.forModule(FluidSynthEmbeddedSynth.class);
    private static final Logger LOGGER = Logger.getLogger(FluidSynthEmbeddedSynth.class.getSimpleName());

    public FluidSynthEmbeddedSynth()
    {
        assert REVERB_PRESETS.contains(DEFAULT_REVERB);
        assert CHORUS_PRESETS.contains(DEFAULT_CHORUS);
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
    }

    @Override
    public boolean isOpen()
    {
        return fluidSynth != null;
    }

    protected FluidSynthJava getFluidSynthJava()
    {
        return fluidSynth;
    }

    @Override
    public final OutputSynth getOutputSynth()
    {
        return OS_FluidSynthEmbedded.getInstance();
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
        return "jjSynth";
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
     * Fast check that this is the expected soundfont file.
     *
     * @param f
     * @return
     */
    static public boolean fastCheckSoundFontFile(File f)
    {
        if (f == null || !f.exists() || !f.isFile())
        {
            return false;
        }
        long fileSize = f.length();
        return fileSize > 356719460 && fileSize < 356720650;
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


    /**
     * Retrieve the SoundFont file.
     *
     * @return
     */
    private File getSoundFontFile() throws FluidSynthException
    {
        File f = null;

        // First check command line parameter
        String cmdLinePath = System.getProperty(SOUNDFONT_FILE_COMMAND_LINE_PROPERTY);

        if (cmdLinePath != null)
        {
            f = new File(cmdLinePath);
            LOGGER.log(Level.INFO, "Using SoundFont file from property " + SOUNDFONT_FILE_COMMAND_LINE_PROPERTY + "={0}", cmdLinePath);

        } else
        {

            // Then check standard location for bundled file
            f = InstalledFileLocator.getDefault().locate(SOUNDFONT_FILE, "org.jjazzlab.org.jjazz.fluidsynthembeddedsynth", false);
        }

        if (!fastCheckSoundFontFile(f))
        {
            throw new FluidSynthException(ResUtil.getString(getClass(), "InvalidSoundFontFile",
                    (f == null ? "null" : f.getAbsolutePath())));
        }
        return f;

    }


}

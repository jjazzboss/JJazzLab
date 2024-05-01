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
package org.jjazz.embeddedsynth.api;

import java.awt.Component;
import java.io.File;
import org.jjazz.outputsynth.api.OutputSynth;

/**
 * Interface for a JJazzLab embedded synth.
 */
public interface EmbeddedSynth
{

    /**
     * Give the possibility to configure the synth before being opened.
     * 
     * @param config An implementation-specific configuration object, eg a SoundFont file for a SoundFont-based synth.
     */
    void configure(Object config);

    /**
     * Initialize the synth (load resources...).
     *
     * @throws org.jjazz.embeddedsynth.api.EmbeddedSynthException
     */
    void open() throws EmbeddedSynthException;

    /**
     * Release the resources of the synth.
     */
    void close();

    boolean isOpen();

    String getName();

    String getVersion();

    /**
     * Get the OutputSynth corresponding to this embedded synth.
     *
     * @return
     */
    OutputSynth getOutputSynth();

    /**
     * Display a dialog to alter embedded synth settings.
     * <p>
     * Settings might be serialized by the embedded synth instance.
     *
     * @param c The related component to locate the dialog.
     */
    void showSettings(Component c);

    /**
     * Generate a .wav file from the specified midiFile.
     * <p>
     *
     * @param midiFile
     * @param wavFile
     * @throws org.jjazz.embeddedsynth.api.EmbeddedSynthException
     */
    void generateWavFile(File midiFile, File wavFile) throws EmbeddedSynthException;

}

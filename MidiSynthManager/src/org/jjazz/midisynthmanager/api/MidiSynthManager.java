/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midisynthmanager.api;

import java.io.File;
import java.util.List;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midisynthmanager.MidiSynthManagerImpl;


/**
 * A central place to manage MidiSynths.
 */
public interface MidiSynthManager
{

    // The builtin synth names
    static String JJAZZLAB_SOUNDFONT_GM2_SYNTH_NAME = "JJazzLab SoundFont (GM2)";
    static String JJAZZLAB_SOUNDFONT_GS_SYNTH_NAME = "JJazzLab SoundFont (GS)";
    static String JJAZZLAB_SOUNDFONT_XG_SYNTH_NAME = "JJazzLab SoundFont (XG)";
    static String YAMAHA_REF_SYNTH_NAME = "Tyros5 Synth";


    public static MidiSynthManager getDefault()
    {
        return MidiSynthManagerImpl.getInstance();
    }

    /**
     * The list of all JJazzLab builtin synths.
     * <p>
     * GM, GM2, XG, GS, JJazzLabSoundFont-based synths, YamahaRef, etc.
     *
     * @return
     */
    List<MidiSynth> getBuiltinSynths();

    /**
     * Search the builtin synths and the synths loaded via loadSynths() to find a synth whose name is synthName.
     *
     * @param synthName
     * @return Can be null.
     */
    MidiSynth getMidiSynth(String synthName);

    /**
     * Read the specified file to load one or more MidiSynths.
     * <p>
     * Errors are notified to user. A WeakReference of the loaded MidiSynths is kept.
     *
     * @param synthFile
     * @return A list of loaded MidiSynths. Can be empty. MidiSynths have their getFile() property set to synthFile.
     */
    List<MidiSynth> loadSynths(File synthFile);

    /**
     * Show a dialog to select a MidiSynth definition file.
     * <p>
     * Use the file extensions managed by the MidiSynthFileReaders found in the global lookup.
     *
     * @return The selected file. Null if user cancelled or no selection.
     */
    File showSelectSynthFileDialog();
}

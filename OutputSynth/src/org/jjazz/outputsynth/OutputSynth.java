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
package org.jjazz.outputsynth;

import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.StdSynth;
import org.openide.util.NbPreferences;

/**
 * The information about the MidiSynth connected to the Midi output of JJazzLab.
 */
public class OutputSynth
{

    private static final String MIDISYNTH_FILES_DEST_DIRNAME = "MidiSynthFiles";
    private static final String MIDISYNTH_FILES_RESOURCE_ZIP = "resources/MidiSynthFiles.zip";
    private final static String SGM_SOUNDFONT_INS = "resources/SGM-v2.01.ins";

    /* Compatibility with the standards. */
    private boolean isGM, isGM2, isGS, isXG;
    private MidiSynth synth;

    private static Preferences prefs = NbPreferences.forModule(OutputSynth.class);
    private static final Logger LOGGER = Logger.getLogger(OutputSynth.class.getSimpleName());

    /**
     * @param isGM the isGM to set
     */
    public void setGM(boolean isGM)
    {
        this.isGM = isGM;
    }

    /**
     * @return True if the MidiSynth is compatible with GM
     */
    public boolean isGM()
    {
        return isGM;
    }

    /**
     * @return True if the MidiSynth is compatible with GS
     */
    public boolean isGS()
    {
        return isGS;
    }

    /**
     * @param isGS the isGS to set
     */
    public void setGS(boolean isGS)
    {
        this.isGS = isGS;
    }

    /**
     * @return True if the MidiSynth is compatible with GM2
     */
    public boolean isGM2()
    {
        return isGM2;
    }

    /**
     * @param isGM2 the isGM2 to set
     */
    public void setGM2(boolean isGM2)
    {
        this.isGM2 = isGM2;
    }

    /**
     * @return True if the MidiSynth is compatible with XG
     */
    public boolean isXG()
    {
        return isXG;
    }

    /**
     * @param isXG the isXG to set
     */
    public void setIsXG(boolean isXG)
    {
        this.isXG = isXG;
    }

    /**
     * @param synth the synth to set
     */
    public void setSynth(MidiSynth synth)
    {
        this.synth = synth;
    }

    /**
     * @return the synth
     */
    public MidiSynth getMidiSynth()
    {
        return synth;
    }

    // ========================================================================================
    // Private methods
    // ========================================================================================
    /**
     * Make athe isXX() methods usable.
     */
    private void scanForStandardSupport(MidiSynth synth)
    {
        isGM = false;
        isGM2 = false;
        isGS = false;
        isXG = false;
        for (InstrumentBank<?> bank : synth.getBanks())
        {
            if (bank == StdSynth.getInstance().getGM1Bank())
            {
                isGM = true;
            } else if (bank == StdSynth.getInstance().getGM2Bank())
            {
                isGM2 = true;
            } else if (bank == StdSynth.getInstance().getXGBank())
            {
                isXG = true;
            } else if (bank == StdSynth.getInstance().getGSBank())
            {
                isGS = true;
            }
            for (Instrument ins : bank.getInstruments())
            {
                throw new UnsupportedOperationException();
            }
        }
    }
}

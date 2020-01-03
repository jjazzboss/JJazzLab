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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.StdSynth;
import org.jjazz.midiconverters.api.ConvertersManager;
import org.jjazz.rhythm.api.RhythmVoice;
import org.openide.util.NbPreferences;

/**
 * The information about the MidiSynth connected to the Midi output of JJazzLab.
 */
public class OutputSynth implements Serializable
{

    private static final String MIDISYNTH_FILES_DEST_DIRNAME = "MidiSynthFiles";
    private static final String MIDISYNTH_FILES_RESOURCE_ZIP = "resources/MidiSynthFiles.zip";
    private final static String SGM_SOUNDFONT_INS = "resources/SGM-v2.01.ins";

    private final List<InstrumentBank<?>> compatibleStdBanks = new ArrayList<>();
    private final List<MidiSynth> customSynths = new ArrayList<>();
    private GM1RemapTable remapTable = new GM1RemapTable();
    private static Preferences prefs = NbPreferences.forModule(OutputSynth.class);
    private static final Logger LOGGER = Logger.getLogger(OutputSynth.class.getSimpleName());

    /**
     * Construct a default OutputSynth compatible with the GM1 Bank and with no custom MidiSynth.
     */
    public OutputSynth()
    {
        compatibleStdBanks.add(StdSynth.getGM1Bank());
    }

    public GM1RemapTable getGM1RemapTable()
    {
        return remapTable;
    }

    /**
     * Get the list of InstrumentBanks from StdSynth which are compatible with this OutputSynth.
     *
     * @return Can be an empty list.
     */
    public List<InstrumentBank<?>> getCompatibleStdBanks()
    {
        return new ArrayList<>(compatibleStdBanks);
    }

    /**
     * Add a standard bank compatible with this OutputSynth.
     *
     * @param stdBank Must belong to the StdSynth instance.
     */
    public void addCompatibleStdBank(InstrumentBank<?> stdBank)
    {
        if (stdBank == null || stdBank.getMidiSynth() != StdSynth.getInstance())
        {
            throw new IllegalArgumentException("stdBank=" + stdBank);
        }

        if (!compatibleStdBanks.contains(this))
        {
            compatibleStdBanks.add(stdBank);
        }
    }

    /**
     * Remove a standard bank compatible with this OutputSynth.
     *
     * @param stdBank Must belong to the StdSynth instance.
     */
    public void removeCompatibleStdBank(InstrumentBank<?> stdBank)
    {
        if (stdBank == null || stdBank.getMidiSynth() != StdSynth.getInstance())
        {
            throw new IllegalArgumentException("stdBank=" + stdBank);
        }
        compatibleStdBanks.remove(stdBank);
    }

    /**
     * Get the list of custom MidiSynths which are compatible with this OutputSynth.
     *
     * @return Can be an empty list.
     */
    public List<MidiSynth> getCustomSynths()
    {
        return new ArrayList<>(customSynths);
    }

    /**
     * Add a custom MidiSynth compatible with this OutputSynth.
     * <p>
     * Scan the synth to possibly add compatible standard banks.
     *
     * @param synth
     */
    public void addCustomSynth(MidiSynth synth)
    {
        if (synth == null || synth == StdSynth.getInstance())
        {
            throw new IllegalArgumentException("stdBank=" + synth);
        }

        if (!customSynths.contains(synth))
        {
            customSynths.add(synth);
            this.scanAndAddCompatibleStdBanks(synth);
        }
    }

    /**
     * Remove a standard bank compatible with this OutputSynth.
     * <p>
     * This might remove some compatible standard banks.
     *
     * @param stdBank Must belong to the StdSynth instance.
     */
    public void removeCustomSynth(MidiSynth synth)
    {
        if (synth == null || synth == StdSynth.getInstance())
        {
            throw new IllegalArgumentException("stdBank=" + synth);
        }
        customSynths.remove(synth);
        resetCompatibleStdBanksFromCustomSynths();
    }

    /**
     * Get an instrument compatible with this OutputSynth for the specified rhythm voice.
     * <p>
     * Start with the instruments from the custom synths then from the compatible standard banks.
     *
     * @param rv
     * @return Can't be null. It may be the VoidInstrument for drums/percussion.
     */
    public Instrument getInstrument(RhythmVoice rv)
    {
        Instrument rvIns = rv.getPreferredInstrument();
        ConvertersManager cm = ConvertersManager.getInstance();
        Instrument ins = null;
        // Try with custom synths first
        for (MidiSynth synth : customSynths)
        {
            ins = cm.convertInstrument(rvIns, synth);
            if (ins != null)
            {
                break;
            }
        }
        if (ins == null)
        {
            // Try with the standard banks
            ins = cm.convertInstrument(rvIns, StdSynth.getInstance());
        }
        if (ins == null)
        {
            // Use the GM1Substitute or its remaps
            switch (rv.getType())
            {
                case DRUMS:
                    ins = remapTable.getDrumsInstrument();
                    if (ins == null)
                    {
                        ins = StdSynth.getInstance().getVoidInstrument();
                    }
                    break;
                case PERCUSSION:
                    ins = remapTable.getPercussionInstrument();
                    if (ins == null)
                    {
                        ins = StdSynth.getInstance().getVoidInstrument();
                    }
                    break;
                default:
                    // Use the default GM1 instrument
                    ins = remapTable.getInstrument(rvIns.getSubstitute());
                    if (ins == null)
                    {
                        // No remap: use the substitute
                        ins = rvIns.getSubstitute();
                    }
                    break;
            }
        }
        assert ins != null : "rv=" + rv;
        return ins;
    }
    // ========================================================================================
    // Private methods
    // ========================================================================================

    private void resetCompatibleStdBanksFromCustomSynths()
    {
        compatibleStdBanks.clear();
        for (MidiSynth synth : customSynths)
        {
            scanAndAddCompatibleStdBanks(synth);
        }
    }

    private void scanAndAddCompatibleStdBanks(MidiSynth synth)
    {
        List<InstrumentBank<?>> stdBanks = StdSynth.getInstance().scanCompatibleBanks(synth, 0.8f);
        for (InstrumentBank<?> stdBank : stdBanks)
        {
            addCompatibleStdBank(stdBank);
        }
    }
}

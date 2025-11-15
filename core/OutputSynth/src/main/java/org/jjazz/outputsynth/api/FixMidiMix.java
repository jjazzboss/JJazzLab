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
package org.jjazz.outputsynth.api;

import java.awt.GraphicsEnvironment;
import org.jjazz.outputsynth.FixMidiMixDialog;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.outputsynth.FixMidiMixDialog.FixChoice;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.outputsynth.spi.OutputSynthManager;

/**
 * Helper class to check and fix MidiMix/OutputSynth consistency.
 * <p>
 */
public class FixMidiMix
{

    private static FixMidiMixDialog DIALOG;
    private static final Logger LOGGER = Logger.getLogger(FixMidiMix.class.getSimpleName());
    private static FixChoice savedChoice = null;


    /**
     * Check and possibly fix midiMix to be consistent with the default OutputSynth.
     *
     * @param midiMix
     * @param showFixMidiMixDialog If true (and if not running in headless mode), show user a confirmation dialog. User might choose to not fix the midiMix.
     * @return True if midiMix was fixed
     */
    static public boolean checkAndPossiblyFix(MidiMix midiMix, boolean showFixMidiMixDialog)
    {
        return checkAndPossiblyFix(midiMix, OutputSynthManager.getDefault().getDefaultOutputSynth(), showFixMidiMixDialog);
    }

    /**
     * Check and possibly fix midiMix to be consistent with the specified outputSynth.
     *
     * @param midiMix
     * @param outputSynth
     * @param showFixMidiMixDialog If true (and if not running in headless mode), show user a confirmation dialog. User might choose to not fix the midiMix.
     * @return True if midiMix was fixed
     */
    static public boolean checkAndPossiblyFix(MidiMix midiMix, OutputSynth outputSynth, boolean showFixMidiMixDialog)
    {
        Objects.requireNonNull(midiMix);
        Objects.requireNonNull(outputSynth);
        LOGGER.log(Level.FINE, "checkAndPossiblyFix() -- midiMix={0} outputSynth={1} showFixMidiMixDialog={2}", new Object[]
        {
            midiMix, outputSynth,
            showFixMidiMixDialog
        });

        HashMap<Integer, Instrument> mapNewInstruments = outputSynth.getNeedFixInstruments(midiMix);
        List<Integer> reroutableChannels = midiMix.getChannelsNeedingDrumsRerouting(mapNewInstruments);

        if (mapNewInstruments.isEmpty() && reroutableChannels.isEmpty())
        {
            // No fix required
            return false;
        }

        boolean doFix = savedChoice == null ? true : savedChoice == FixChoice.FIX;

        if (savedChoice == null && showFixMidiMixDialog && !GraphicsEnvironment.isHeadless())
        {
            var dialog = getDialog();
            dialog.preset(mapNewInstruments, reroutableChannels, midiMix);
            dialog.setVisible(true);
            FixChoice choice = dialog.getUserChoice();
            doFix = choice == FixChoice.FIX;
            if (dialog.isRememberChoiceSelected())
            {
                savedChoice = choice;
            }
        }

        if (doFix)
        {
            outputSynth.fixInstruments(midiMix, true);
        }

        return doFix;
    }

    static private FixMidiMixDialog getDialog()
    {
        if (DIALOG == null)
        {
            DIALOG = new FixMidiMixDialog();
        }
        return DIALOG;
    }

}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynthmanagerimpl;

import org.jjazz.outputsynthmanagerimpl.api.MidiSynthManagerImpl;
import java.util.logging.Logger;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.outputsynth.api.OutputSynth;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.outputsynth.spi.OutputSynthManager;
import org.jjazz.midi.spi.MidiSynthManager;
import org.jjazz.outputsynth.api.DefaultOutputSynthManager;


/**
 * Extends DefaultOutputSynthManager with a few features.
 */
@ServiceProvider(service = OutputSynthManager.class)
public class OutputSynthManagerImpl extends DefaultOutputSynthManager
{

    private static final Logger LOGGER = Logger.getLogger(OutputSynthManagerImpl.class.getSimpleName());


    @Override
    public OutputSynth getStandardOutputSynth(String stdName)
    {
        var res = super.getStandardOutputSynth(stdName);
        if (res != null)
        {
            return res;
        }


        MidiSynth synth = null;
        OutputSynth.UserSettings.SendModeOnUponPlay mode = null;
        switch (stdName)
        {
            case OutputSynthManager.STD_JJAZZLAB_SOUNDFONT_GS ->
            {
                synth = MidiSynthManager.getDefault().getMidiSynth(MidiSynthManagerImpl.JJAZZLAB_SOUNDFONT_GS_SYNTH_NAME);
                mode = OutputSynth.UserSettings.SendModeOnUponPlay.GS;
            }
            case OutputSynthManager.STD_JJAZZLAB_SOUNDFONT_XG ->
            {
                synth = MidiSynthManager.getDefault().getMidiSynth(MidiSynthManagerImpl.JJAZZLAB_SOUNDFONT_XG_SYNTH_NAME);
                mode = OutputSynth.UserSettings.SendModeOnUponPlay.XG;
            }
            case OutputSynthManager.STD_YAMAHA_TYROS_REF ->
            {
                synth = MidiSynthManager.getDefault().getMidiSynth(MidiSynthManagerImpl.YAMAHA_REF_SYNTH_NAME);
                mode = OutputSynth.UserSettings.SendModeOnUponPlay.OFF;
            }
            default ->
            {
                return null;
            }
        }

        res = new OutputSynth(synth);
        res.getUserSettings().setSendModeOnUponPlay(mode);

        return res;
    }

}

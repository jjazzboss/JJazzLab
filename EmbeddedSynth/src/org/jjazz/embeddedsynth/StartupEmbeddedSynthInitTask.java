/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.embeddedsynth;

import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.outputsynth.api.MidiSynthManager;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.upgrade.api.UpgradeManager;
import org.openide.modules.OnStart;

/**
 * If EmbeddedSynth is present, connect it to the JJazzLab application.
 */
@OnStart
public class StartupEmbeddedSynthInitTask implements Runnable
{

    private static final Logger LOGGER = Logger.getLogger(StartupEmbeddedSynthInitTask.class.getSimpleName());

    @Override
    public void run()
    {

        var eSynth = EmbeddedSynthProvider.getDefaultSynth();
        if (eSynth == null)
        {
            // No embedded synth
            return;
        }
        
            // If fresh start, make it the default OUT MidiDevice
        if (UpgradeManager.getInstance().isFreshStart())
        {
            try
            {
                JJazzMidiSystem.getInstance().setDefaultOutDevice(eSynth.getOutMidiDevice());
            } catch (MidiUnavailableException ex)
            {                
                LOGGER.severe("run() " + ex);                
                EmbeddedSynthProvider.getDefaultProvider().disable();
                return;                
            }
        }

        // Register the embedded MidiSynth
        var msm = MidiSynthManager.getInstance();
       msm.addMidiSynth(eSynth.getOutputSynth().getMidiSynth());
        

        // Register the MidiDevice with the OutputSynth
        OutputSynthManager.getInstance().setOutputSynth(eSynth.getOutMidiDevice().getDeviceInfo().getName(), eSynth.getOutputSynth());
        
    
    }

}

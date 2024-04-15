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

import java.util.logging.Logger;
import org.jjazz.outputsynth.api.OutputSynth;
import org.netbeans.api.annotations.common.StaticResource;
import org.jjazz.midi.spi.MidiSynthManager;

/**
 * The builtin OutputSynth for our embedded synth (based on OS_JJazzLabSoundFont_XG since FluidSynth can handle XG bank select).
 */
public class OS_FluidSynthEmbedded extends OutputSynth
{

    @StaticResource(relative = true)
    private static final String INS_PATH = "resources/JJazzLabSoundFont_FluidSynth.ins";
    private static OS_FluidSynthEmbedded INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(OS_FluidSynthEmbedded.class.getSimpleName());

    public static OS_FluidSynthEmbedded getInstance()
    {
        synchronized (OS_FluidSynthEmbedded.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new OS_FluidSynthEmbedded();
            }
        }
        return INSTANCE;
    }

    private OS_FluidSynthEmbedded()
    {
        super(MidiSynthManager.loadFromResource(OS_FluidSynthEmbedded.class, INS_PATH));
        getUserSettings().setSendModeOnUponPlay(UserSettings.SendModeOnUponPlay.XG);
    }
}

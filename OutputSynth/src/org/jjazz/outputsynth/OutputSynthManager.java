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
import org.jjazz.midi.GSSynth;
import org.jjazz.midi.StdSynth;

/**
 * Management of the OutputSynth.
 * <p>
 */
public class OutputSynthManager
{
    
    private static OutputSynthManager INSTANCE;
    private OutputSynth outputSynth;
    
    private static final Logger LOGGER = Logger.getLogger(OutputSynthManager.class.getSimpleName());
    
    public static OutputSynthManager getInstance()
    {
        synchronized (OutputSynthManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new OutputSynthManager();
            }
        }
        return INSTANCE;
    }
    
    private OutputSynthManager()
    {
        outputSynth = new OutputSynth();
        outputSynth.addCompatibleStdBank(StdSynth.getXGBank());
        outputSynth.addCustomSynth(GSSynth.getInstance());
    }

    /**
     * The current OutputSynth.
     *
     * @return Can't be null.
     */
    public OutputSynth getOutputSynth()
    {
        return outputSynth;
    }
    
    public void setOutputSynth(OutputSynth outSynth)
    {
        if (outSynth == null)
        {
            throw new IllegalArgumentException("outSynth=" + outSynth);
        }
        outputSynth = outSynth;
    }
}

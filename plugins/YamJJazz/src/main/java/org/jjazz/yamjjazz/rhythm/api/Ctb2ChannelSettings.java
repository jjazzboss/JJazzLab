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
package org.jjazz.yamjjazz.rhythm.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.Note;

/**
 * Ctab/Ctb2 settings.
 * <p>
 * In SFF1: CtabChannelSettings uses only one Ctb2 instance. In SFF2 up to 3 Ctb2 instances (low, main, high).
 */
public class Ctb2ChannelSettings
{

    public enum NoteTranspositionRule
    {
        // ROOT_TRANSPOSITION, ROOT_FIXED;   SFF1
        ROOT_TRANSPOSITION, ROOT_FIXED, GUITAR;
    }

    public enum NoteTranspositionTable
    {
        // BYPASS, MELODY, CHORD, BASS, MELODIC_MINOR, HARMONIC_MINOR;     SFF1
        BYPASS, MELODY, CHORD, MELODIC_MINOR, MELODIC_MINOR_5, HARMONIC_MINOR, HARMONIC_MINOR_5,
        NATURAL_MINOR, NATURAL_MINOR_5, DORIAN, DORIAN_5, 
        ALL_PURPOSE, STROKE, ARPEGGIO;      // These 3 ones for byte value 0/1/2, to be used only if NRT=GUITAR
        
    }

    public enum RetriggerRule
    {
        STOP, PITCH_SHIFT, PITCH_SHIFT_TO_ROOT, RETRIGGER, RETRIGGER_TO_ROOT, NOTE_GENERATOR;
    }
    public Note chordRootUpperLimit;
    public Note noteLowLimit;
    public Note noteHighLimit;
    public RetriggerRule rtr;
    public NoteTranspositionRule ntr;
    public NoteTranspositionTable ntt;
    public boolean bassOn;
    private static final Logger LOGGER = Logger.getLogger(Ctb2ChannelSettings.class.getSimpleName());

    /**
     *
     * @param b1 [0;2]
     */
    public void setNtr(int b1)
    {
        if (b1 >= 3 || b1 < 0)
        {
            throw new IllegalArgumentException("b1=" + b1);   //NOI18N
        }
        ntr = NoteTranspositionRule.values()[b1];
    }

    /**
     *
     * @param b1 [0;10]
     */
    public void setNtt(int b1)
    {
        if (b1 >= 11 || b1 < 0)
        {
            throw new IllegalArgumentException("b1=" + b1);   //NOI18N
        }
        ntt = NoteTranspositionTable.values()[b1];
    }

    public void setRetriggerRule(int b1)
    {
        rtr = RetriggerRule.values()[b1];
    }

    public void dump()
    {
        LOGGER.log(Level.INFO, "  ntr={0}", ntt);
        LOGGER.log(Level.INFO, "  ntt={0}", ntr);
        LOGGER.log(Level.INFO, "  chordRootUpperLimit={0}", chordRootUpperLimit);
        LOGGER.log(Level.INFO, "  noteLowLimit={0}", noteLowLimit);
        LOGGER.log(Level.INFO, "  noteHighLimit={0}", noteHighLimit);
        LOGGER.log(Level.INFO, "  rtr={0}", rtr);
    }
}

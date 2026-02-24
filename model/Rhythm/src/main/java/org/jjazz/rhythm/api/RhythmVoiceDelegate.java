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
package org.jjazz.rhythm.api;

/**
 * A special RhythmVoice which is a delegate for another source RhythmVoice.
 * <p>
 * Delegates are used for example by AdaptedRhythms to enable rhythm music generation while sparing "Midi channel space" in the MidiMix.
 */
public class RhythmVoiceDelegate extends RhythmVoice
{

    private final RhythmVoice rvSource;

    /**
     * Create a delegate for rvSource.
     * <p>
     * All rvSource parameters are reused by the created instance, except for the container and name.
     *
     * @param container
     * @param rvSource
     * @return
     * @throws IllegalArgumentException If container==rvSource.getContainer()
     */
    static public RhythmVoiceDelegate createInstance(Rhythm container, RhythmVoice rvSource)
    {
        if (container == null || rvSource == null || rvSource.getContainer() == container)
        {
            throw new IllegalArgumentException("container=" + container + " rvSource=" + rvSource);
        }
        if (!rvSource.isDrums())
        {
            return new RhythmVoiceDelegate(container, rvSource);
        } else
        {
            return new RhythmVoiceDelegate(container, rvSource, true);
        }

    }


    private RhythmVoiceDelegate(Rhythm container, RhythmVoice rvSource)
    {
        super(container, rvSource.getType(), rvSource.getName(), rvSource.getPreferredInstrument(), rvSource.getPreferredChannel());
        this.rvSource = rvSource;
    }

    private RhythmVoiceDelegate(Rhythm container, RhythmVoice rvSource, boolean forDrums)
    {
        super(rvSource.getDrumKit(), container, rvSource.getType(), rvSource.getName(), rvSource.getPreferredInstrument(), rvSource.getPreferredChannel());
        this.rvSource = rvSource;
    }

    public RhythmVoice getSource()
    {
        return rvSource;
    }

    @Override
    public String toString()
    {
        return "RvD[" + getName() + ", r=" + getContainer() + "]";
    }
}

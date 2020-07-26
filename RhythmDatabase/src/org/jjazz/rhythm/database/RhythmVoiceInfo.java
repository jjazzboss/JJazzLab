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
package org.jjazz.rhythm.database;

import org.jjazz.midi.DrumKit;
import org.jjazz.midi.Instrument;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice.Type;


/**
 * A description of a RhythmVoice for catalog purpose.
 * <p>
 */
public class RhythmVoiceInfo
{
    private final String name;
    private Instrument instrument;
    private final int preferredChannel;
    private final DrumKit drumKit;
    private final Type type;

    public RhythmVoiceInfo(RhythmVoice rv)
    {
        name = rv.getName();
        instrument = rv.getPreferredInstrument();
        preferredChannel = rv.getPreferredChannel();
        drumKit = rv.getDrumKit();
        type = rv.getType();
    }

    public String getName()
    {
        return name;
    }

    /**
     * @return the instrument
     */
    public Instrument getInstrument()
    {
        return instrument;
    }

    /**
     * @return the preferredChannel
     */
    public int getPreferredChannel()
    {
        return preferredChannel;
    }

    /**
     * @return the drumKit
     */
    public DrumKit getDrumKit()
    {
        return drumKit;
    }

    /**
     * @return the type
     */
    public Type getType()
    {
        return type;
    }
}

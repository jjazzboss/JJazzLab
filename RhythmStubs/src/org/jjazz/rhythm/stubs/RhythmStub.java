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
package org.jjazz.rhythm.stubs;

import org.jjazz.harmony.TimeSignature;
import org.jjazz.midi.GM1Bank;
import org.jjazz.midi.GMSynth;
import org.jjazz.rhythm.api.AbstractRhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RvType;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythm.parameters.RP_STD_Variation;
import org.jjazz.rhythmmusicgeneration.DummyGenerator;
import org.openide.util.lookup.Lookups;

public class RhythmStub extends AbstractRhythm
{

    public RhythmStub(TimeSignature ts)
    {
        super("AbstractStubRhythmID-" + ts.toString(), "Dummy rhythm " + ts.toString(), "Dummy rhythm", "JJazzLab", "1", Feel.BINARY, ts, 120, TempoRange.MEDIUM, null, "dummy");

        // Our Rhythm Parameters
        rhythmParameters.add(new RP_STD_Variation());

        // Rhythm voices
        GM1Bank gmb = GMSynth.getInstance().getGM1Bank();
        rhythmVoices.add(new RhythmVoice(this, RvType.Drums, "Drums", null));
        rhythmVoices.add(new RhythmVoice(this, RvType.Bass, "Bass", gmb.getDefaultInstrument(GM1Bank.Family.Bass)));

        lookup = Lookups.fixed(new DummyGenerator(this));
    }
}

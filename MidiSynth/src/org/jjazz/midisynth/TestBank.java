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
package org.jjazz.midisynth;

import org.jjazz.midi.AbstractInstrumentBank;
import org.jjazz.midi.Instrument;

/**
 * Sample InstrumentBank.
 */
public class TestBank extends AbstractInstrumentBank<Instrument>
{

    public TestBank()
    {
        super("Test Bank", null, 10, 20);
        addInstrument(new Instrument(0, "JLInst1"));
        addInstrument(new Instrument(1, "JLBright Piano"));
        addInstrument(new Instrument(2, "JLEl.Grand Piano"));
        addInstrument(new Instrument(5, "JLElectric Piano 2"));
        addInstrument(new Instrument(6, "JLHarpsichord"));
        addInstrument(new Instrument(7, "JLClavinet"));
        addInstrument(new Instrument(8, "JLCelesta"));
        addInstrument(new Instrument(9, "JLGlockenspiel"));
        addInstrument(new Instrument(10, "JLMusic Box"));
        addInstrument(new Instrument(23, "JLBandoneon"));
        addInstrument(new Instrument(24, "JLNylon Guitar 1"));
        addInstrument(new Instrument(25, "JLSteel Guitar"));
        addInstrument(new Instrument(26, "JLJazz Guitar"));
        addInstrument(new Instrument(27, "JLClean Guitar"));
        addInstrument(new Instrument(28, "JLMuted Guitar"));
        addInstrument(new Instrument(29, "JLOverdrive Guitar"));
        addInstrument(new Instrument(30, "JLDistortion Guitar"));
        addInstrument(new Instrument(31, "JLGuitar Harmonics"));
        addInstrument(new Instrument(32, "JLAcoustic Bass"));
        addInstrument(new Instrument(33, "JLFingered Bass"));
        addInstrument(new Instrument(34, "JLPicked Bass"));
        addInstrument(new Instrument(35, "JLFretless Bass"));
        addInstrument(new Instrument(36, "JLSlap Bass 1"));
        addInstrument(new Instrument(37, "JLSlap Bass 2"));
        addInstrument(new Instrument(112, "JLTinkle Bell"));
        addInstrument(new Instrument(113, "JLAgogo"));
        addInstrument(new Instrument(114, "JLSteel Drums"));
        addInstrument(new Instrument(115, "JLWoodsection"));
        addInstrument(new Instrument(116, "JLTaiko"));
        addInstrument(new Instrument(117, "JLMelodic Tom 1"));
        addInstrument(new Instrument(118, "JLSynth Drum"));
        addInstrument(new Instrument(119, "JLReverse Cymbal"));
        addInstrument(new Instrument(120, "JLGtr.Fret Noise"));
        addInstrument(new Instrument(121, "JLBreath Noise"));
        addInstrument(new Instrument(122, "JLSeashore"));
        addInstrument(new Instrument(123, "JLBird Tweet"));
        addInstrument(new Instrument(124, "JLTelephone Rin"));
        addInstrument(new Instrument(125, "JLHelicopter"));
        addInstrument(new Instrument(126, "JLApplause"));
        addInstrument(new Instrument(127, "JLGunShot"));
    }
}

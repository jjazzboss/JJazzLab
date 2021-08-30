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
package org.jjazz.phrase.api;

import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.util.api.FloatRange;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jerome
 */
public class NoteEventTest
{

    public NoteEventTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }


    /**
     * Test of toMidiEvents method, of class NoteEvent.
     */
    @Test
    public void testToMidiEvents()
    {
        System.out.println("testToMidiEvents() --");

        for (int i = 0; i < 50000000; i++)
        {
            float pos = (float) (Math.random() * 100f);
            float dur = (float) (Math.random() * 4f)+0.0001f;
//        float pos = 9.861717f;
//        float dur = 3.7923956f;
            //System.out.println("ORIGINAL: pos=" + pos + ", dur=" + dur);
            NoteEvent ne1 = new NoteEvent(64, dur, 100, pos);
            float ne1Pos = ne1.getPositionInBeats();
            float ne1Dur = ne1.getDurationInBeats();
            //System.out.println("          ne1.pos=" + ne1Pos + ", ne1.dur=" + ne1Dur);
//        long tickOn = Math.round(ne1Pos * MidiConst.PPQ_RESOLUTION);
//        long tickOff = Math.round((ne1Pos + ne1Dur) * MidiConst.PPQ_RESOLUTION);
            long tickOn = (long) (ne1Pos * MidiConst.PPQ_RESOLUTION);
            long tickOff = (long) ((ne1Pos + ne1Dur) * MidiConst.PPQ_RESOLUTION);
            //System.out.println("          tickOn=" + tickOn + ", tickOff=" + tickOff + ", tickDur=" + (tickOff - tickOn));

            float newDur = ((float) tickOff - tickOn) / MidiConst.PPQ_RESOLUTION;
            float newPos = ((float) tickOn / MidiConst.PPQ_RESOLUTION);
            // System.out.println("AFTER:  =>  newPos=" + newPos + " newDur=" + newDur);
            List<MidiEvent> midiEvents = ne1.toMidiEvents(0);
            Phrase p = new Phrase(0);
            p.add(midiEvents, 0);
            NoteEvent ne2 = p.get(0);
            if (!ne1.equals(ne2))
            {
                System.out.println("Difference! ne1=" + ne1 + " ne2=" + ne2);
            }
            // System.out.println("AFTER:  =>  ne2=" + ne2);
        }
    }


}

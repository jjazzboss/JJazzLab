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
package org.jjazz.test;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import org.jjazz.harmony.spi.ChordTypeDatabase;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.yamjjazz.rhythm.api.YamChord;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.TestAction")
//@ActionRegistration(displayName = "Test Action")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 50000)
//        })
public final class TestAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(TestAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        showDraggingThreshold();
    }

    private void showDraggingThreshold()
    {
        LOGGER.log(Level.INFO, "showDraggingThreshold() -- ");
        var toolkit = Toolkit.getDefaultToolkit();
        Object o = toolkit.getDesktopProperty("DnD.gestureMotionThreshold");
        LOGGER.log(Level.INFO, "getDesktopProperty(DnD.gestureMotionThreshold)={0}", o);
        String s = System.getProperty("awt.dnd.drag.threshold");
        LOGGER.log(Level.INFO, "SystemProperty(awt.dnd.drag.threshold)={0}", s);
        if (s != null)
        {
            int t = Integer.parseInt(s) - 2;
            System.setProperty("awt.dnd.drag.threshold", String.valueOf(t));
        }
    }

    private void testYamChords()
    {
        LOGGER.log(Level.INFO, "testYamChords() -- ");
        var ctDb = ChordTypeDatabase.getDefault();
        for (var ct : ctDb.getChordTypes())
        {
            YamChord yc = YamChord.get(ct.getName());
            if (yc == null)
            {
                LOGGER.log(Level.SEVERE, "Missing yc for ct={0}", ct);
            }
        }
    }

    private class MyRun implements Runnable
    {

        @Override
        public void run()
        {
            try
            {
                LOGGER.log(Level.INFO, "TestAction.actionPerformed() called");
                JJazzMidiSystem mss = JJazzMidiSystem.getInstance();
                MidiDevice md = mss.getDefaultOutDevice();
                LOGGER.log(Level.INFO, "Default out device {0}", md.getDeviceInfo().getName());
                LOGGER.log(Level.INFO, "Default out device class {0} isInstanceOfSynth={1}", new Object[]
                {
                    md.getClass(),
                    md instanceof Synthesizer
                });
                File dir = RhythmDirsLocator.getDefault().getUserRhythmsDirectory();
                File soundFontFile = new File(dir, "FluidR3_GM.SF2");
                Soundbank newSb = MidiSystem.getSoundbank(soundFontFile);
                LOGGER.log(Level.INFO, "newSb={0}, {1}", new Object[]
                {
                    newSb.getName(), newSb.getDescription()
                });
//            LOGGER.log(Level.INFO, "newSb insts=" + Arrays.asList(newSb.getInstruments()));

                // Synthesizer synth = MidiSystem.getSynthesizer();
                Synthesizer synth = (Synthesizer) md;
                LOGGER.log(Level.INFO, "synth={0} {1} isOpen={2}", new Object[]
                {
                    synth.getDeviceInfo().getName(),
                    synth.getDeviceInfo().getDescription(), synth.isOpen()
                });
//            Soundbank oldSb = synth.getDefaultSoundbank();
//            LOGGER.log(Level.INFO, "synth default sb=" + oldSb.getName() + ", " + oldSb.getDescription());
//            LOGGER.log(Level.INFO, "synth instruments=" + Arrays.asList(synth.getLoadedInstruments()));
//            LOGGER.log(Level.INFO, "=> synth.open(), then loadAllInstruments(soundbank)  synth.isSupported(soundbank)=" + synth.isSoundbankSupported(newSb));

                // mss.setDefaultOutDevice(synth);
                // synth.open();  // Done by setDefaultOutDevice(synth)
//            synth.unloadAllInstruments(oldSb);
                boolean loadOk = synth.loadAllInstruments(newSb);
                LOGGER.log(Level.INFO, "synth loadOk={0} insts={1}", new Object[]
                {
                    loadOk, Arrays.asList(synth.getLoadedInstruments())
                });
            } catch (InvalidMidiDataException | IOException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
    }

}

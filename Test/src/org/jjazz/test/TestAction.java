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
package org.jjazz.test;

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
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.TestAction")
//@ActionRegistration(displayName = "Test Action")
//@ActionReferences(
//        {
//           @ActionReference(path = "Menu/Edit", position = 50000)
//        })
public final class TestAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(TestAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        Runnable r = new MyRun();
        BaseProgressUtils.showProgressDialogAndRun(r, "Executing testAction");
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
                LOGGER.log(Level.INFO, "Default out device class {0} isInstanceOfSynth={1}", new Object[]{md.getClass(),
                    md instanceof Synthesizer});   
                FileDirectoryManager fdm = FileDirectoryManager.getInstance();
                File dir = fdm.getUserRhythmDirectory();
                File soundFontFile = new File(dir, "FluidR3_GM.SF2");
                Soundbank newSb = MidiSystem.getSoundbank(soundFontFile);
                LOGGER.log(Level.INFO, "newSb={0}, {1}", new Object[]{newSb.getName(), newSb.getDescription()});   
//            LOGGER.log(Level.INFO, "newSb insts=" + Arrays.asList(newSb.getInstruments()));

                // Synthesizer synth = MidiSystem.getSynthesizer();
                Synthesizer synth = (Synthesizer) md;
                LOGGER.log(Level.INFO, "synth={0} {1} isOpen={2}", new Object[]{synth.getDeviceInfo().getName(),   
                    synth.getDeviceInfo().getDescription(), synth.isOpen()});   
//            Soundbank oldSb = synth.getDefaultSoundbank();
//            LOGGER.log(Level.INFO, "synth default sb=" + oldSb.getName() + ", " + oldSb.getDescription());
//            LOGGER.log(Level.INFO, "synth instruments=" + Arrays.asList(synth.getLoadedInstruments()));
//            LOGGER.log(Level.INFO, "=> synth.open(), then loadAllInstruments(soundbank)  synth.isSupported(soundbank)=" + synth.isSoundbankSupported(newSb));

                // mss.setDefaultOutDevice(synth);
                // synth.open();  // Done by setDefaultOutDevice(synth)
//            synth.unloadAllInstruments(oldSb);
                boolean loadOk = synth.loadAllInstruments(newSb);
                LOGGER.log(Level.INFO, "synth loadOk={0} insts={1}", new Object[]{loadOk, Arrays.asList(synth.getLoadedInstruments())});   
            } catch (InvalidMidiDataException | IOException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    // @Override
    public void actionPerformed2(ActionEvent ae)
    {
        LOGGER.log(Level.INFO, "TestAction.actionPerformed() called");   
        Lookup l1, l2, lc, lc1, lc2;
        InstanceContent ic;
        l1 = Lookups.fixed("L1");
        l2 = Lookups.fixed("L2");

        ic = new InstanceContent();
        lc = new AbstractLookup(ic);
        ic.add(1);
        ic.add(1f);

        lc1 = new ProxyLookup(l1, lc);
        lc2 = new ProxyLookup(l2, lc);

        String s = lc1.lookup(String.class);
        Integer i = lc1.lookup(Integer.class);
        LOGGER.log(Level.INFO, "lc1 s={0} i={1}", new Object[]{s, i});   
        s = lc2.lookup(String.class);
        Float f = lc1.lookup(Float.class);
        LOGGER.log(Level.INFO, "lc2 s={0} f={1}", new Object[]{s, f});   
    }
}

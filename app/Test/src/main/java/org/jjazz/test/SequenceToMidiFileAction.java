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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.MusicController;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.sequencetomidifile")
//@ActionRegistration(displayName = "Generate Midi file from last sequence")
//@ActionReferences(
//        {
//           @ActionReference(path = "Menu/Edit", position = 4224),
//        })
public final class SequenceToMidiFileAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(SequenceToMidiFileAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.info("actionPerformed() --");   
        
        Sequencer s = JJazzMidiSystem.getInstance().getDefaultSequencer();
        if (s == null)
        {
            LOGGER.info("actionPerformed() can't acquire sequencer");   
            return;
        }
        Sequence seq = s.getSequence();
        if (seq == null)
        {
            LOGGER.warning("No sequence");   
            return;
        }

        Thread t = new Thread(new Run(seq));
        t.start();
    }

    private class Run implements Runnable
    {

        Sequence sequence;

        Run(Sequence s)
        {
            sequence = s;
        }

        @Override
        public void run()
        {
            File midiTempFile;
            try
            {
                midiTempFile = File.createTempFile("jjazzMidiTempFile", ".mid");
            } catch (IOException ex)
            {
                Exceptions.printStackTrace(ex);
                return;
            }

            int[] fileTypes = MidiSystem.getMidiFileTypes(sequence);
            for (Integer i : fileTypes)
            {
                LOGGER.log(Level.INFO, " supported fileType={0}", i);   
            }
            if (fileTypes.length == 0)
            {
                LOGGER.info(" NO fileTypes supported for this sequence !");   
            }

            LOGGER.log(Level.INFO, "writing sequence to Midi File: {0}", midiTempFile.getAbsolutePath());   
            try
            {
                MidiSystem.write(sequence, 1, midiTempFile);
            } catch (IOException ex)
            {
                Exceptions.printStackTrace(ex);
                return;
            } 
            
            LOGGER.info("Starting Midi Editor...");   
            ProcessBuilder pb = new ProcessBuilder("C:\\Program Files (x86)\\MidiEditor\\MidiEditor.exe", midiTempFile.getAbsolutePath());
            try
            {
                pb.start();
            } catch (IOException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }
    }

}

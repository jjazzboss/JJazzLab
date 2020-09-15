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
package org.jjazz.musiccontrol;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.midi.MidiUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "File", id = "org.jjazz.musiccontrol.TestAction")
@ActionRegistration(displayName = "Dump Sequence", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/Edit", position = 123124),
        })
public final class TestDumpSequenceAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(TestDumpSequenceAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        Sequencer sequencer = jms.getDefaultSequencer();
        Sequence sequence = sequencer.getSequence();
        LOGGER.info(MidiUtilities.toString(sequence));
    }

}

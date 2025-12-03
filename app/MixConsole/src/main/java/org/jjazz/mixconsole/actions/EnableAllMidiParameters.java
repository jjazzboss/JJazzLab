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
package org.jjazz.mixconsole.actions;

import org.jjazz.midimix.api.MidiMix;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.FluidSynthUtils;
import org.jjazz.song.api.Song;
import org.jjazz.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "MixConsole", id = "org.jjazz.mixconsole.actions.EnableAllMidiParameters")
@ActionRegistration(displayName = "#CTL_EnableAllMidiParameters", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/Midi", position = 100)
        })
public class EnableAllMidiParameters extends AbstractAction implements PropertyChangeListener
{

    private final MidiMix songMidiMix;
    private final String undoText = ResUtil.getString(getClass(), "CTL_EnableAllMidiParameters");
    private static final Logger LOGGER = Logger.getLogger(EnableAllMidiParameters.class.getSimpleName());

    public EnableAllMidiParameters()
    {
        ActiveSongManager asm = ActiveSongManager.getDefault();
        songMidiMix = asm.getActiveMidiMix();

        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_EnableAllMidiParametersDescription"));

        this.setEnabled(!FluidSynthUtils.IS_FLUID_SYNTH_IN_USE());

        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        jms.addPropertyChangeListener(JJazzMidiSystem.PROP_MIDI_OUT, this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.log(Level.FINE, "actionPerformed() songMidiMix={0}", songMidiMix);

        Song song = MixConsoleTopComponent.getInstance().getEditor().getSong();

        JJazzUndoManagerFinder.getDefault().get(song).startCEdit(undoText);

        DisableAllMidiParameters.setAllMidiParametersEnabled(true, songMidiMix);

        JJazzUndoManagerFinder.getDefault().get(song).endCEdit(undoText);

        Analytics.logEvent("Enable All Midi Parameters");
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        this.setEnabled(!FluidSynthUtils.IS_FLUID_SYNTH((MidiDevice) evt.getNewValue()));
    }
}

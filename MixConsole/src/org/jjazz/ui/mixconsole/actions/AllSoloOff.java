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
package org.jjazz.ui.mixconsole.actions;

import org.jjazz.midimix.MidiMix;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.util.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.allsolooff")
// Need lazy=false for tooltip to work
@ActionRegistration(displayName = "#CTL_AllSoloOff", lazy = false)
@ActionReferences(
        {
            // @ActionReference(path = "Actions/MixConsole/Midi", position = 200)
            // ,@ActionReference(path = "Actions/MixConsole/Master", position = 100)
        })
public class AllSoloOff extends AbstractAction
{

    private final String undoText = ResUtil.getString(getClass(), "CTL_AllSoloOff");
    private static final Logger LOGGER = Logger.getLogger(AllSoloOff.class.getSimpleName());

    public AllSoloOff()
    {
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_AllSoloOffToolTip"));
        // putValue("hideActionText", true);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MidiMix songMidiMix = MixConsoleTopComponent.getInstance().getEditor().getMidiMix();
        if (songMidiMix == null || songMidiMix != ActiveSongManager.getInstance().getActiveMidiMix())
        {
            return;
        }
        for (Integer channel : songMidiMix.getUsedChannels())
        {
            InstrumentMix insMix = songMidiMix.getInstrumentMixFromChannel(channel);
            insMix.setSolo(false);
        }
    }
}

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
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.mixconsole.api.MixConsole;
import org.jjazz.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "MixConsole", id = "org.jjazz.mixconsole.actions.switchallmute")
// Need lazy=false to get the tooltip working...
@ActionRegistration(displayName = "#CTL_SwitchAllMute", lazy = false)
@ActionReferences(
        {
            // @ActionReference(path = "Actions/MixConsole/Midi", position = 200)
            // ,@ActionReference(path = "Actions/MixConsole/Master", position = 100)
        })
public class SwitchAllMute extends AbstractAction
{

    private final String undoText = ResUtil.getString(getClass(), "CTL_SwitchAllMute");    
    private static final Logger LOGGER = Logger.getLogger(SwitchAllMute.class.getSimpleName());

    public SwitchAllMute()
    {
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_SwitchAllMuteToolTip"));   
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MixConsole mixConsole = MixConsoleTopComponent.getInstance().getEditor();
        MidiMix songMidiMix = mixConsole.getMidiMix();
        if (songMidiMix == null || songMidiMix != ActiveSongManager.getDefault().getActiveMidiMix())
        {
            return;
        }

        boolean targetMuteState = true;
        Rhythm visibleRhythm = mixConsole.getVisibleRhythm(); // Null means all rhythms are visible
        // Unmute all if there at least one channel muted
        for (Integer channel : songMidiMix.getUsedChannels(visibleRhythm))
        {
            InstrumentMix insMix = songMidiMix.getInstrumentMix(channel);
            if (insMix.isMute())
            {
                targetMuteState = false;
                break;
            }
        }
        for (Integer channel : songMidiMix.getUsedChannels(visibleRhythm))
        {
            InstrumentMix insMix = songMidiMix.getInstrumentMix(channel);
            insMix.setMute(targetMuteState);
        }
    }

}

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

import org.jjazz.midimix.api.MidiMix;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.ui.mixconsole.api.MixConsole;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.switchallmute")
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
        if (songMidiMix == null || songMidiMix != ActiveSongManager.getInstance().getActiveMidiMix())
        {
            return;
        }

        boolean targetMuteState = true;
        Rhythm visibleRhythm = mixConsole.getVisibleRhythm(); // Null means all rhythms are visible
        // Unmute all if there at least one channel muted
        for (Integer channel : songMidiMix.getUsedChannels(visibleRhythm))
        {
            InstrumentMix insMix = songMidiMix.getInstrumentMixFromChannel(channel);
            if (insMix.isMute())
            {
                targetMuteState = false;
                break;
            }
        }
        for (Integer channel : songMidiMix.getUsedChannels(visibleRhythm))
        {
            InstrumentMix insMix = songMidiMix.getInstrumentMixFromChannel(channel);
            insMix.setMute(targetMuteState);
        }
    }

}

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
import org.jjazz.analytics.api.Analytics;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.song.api.Song;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.DisableAllMidiParameters")
@ActionRegistration(displayName = "#CTL_DisableAllMidiParameters", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/Midi", position = 200)
        })
public class DisableAllMidiParameters extends AbstractAction
{

    private MidiMix songMidiMix;
    private final String undoText = ResUtil.getString(getClass(), "CTL_DisableAllMidiParameters");
    private static final Logger LOGGER = Logger.getLogger(DisableAllMidiParameters.class.getSimpleName());

    public DisableAllMidiParameters(MidiMix context)
    {
        songMidiMix = context;
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_DisableAllMidiParametersDescription"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.fine("actionPerformed() songMidiMix=" + songMidiMix);   //NOI18N

        Song song = MixConsoleTopComponent.getInstance().getEditor().getSong();

        JJazzUndoManagerFinder.getDefault().get(song).startCEdit(undoText);

        setAllMidiParametersEnabled(false, songMidiMix);

        JJazzUndoManagerFinder.getDefault().get(song).endCEdit(undoText);
        
        Analytics.logEvent("Disable All Midi Parameters");           
    }

    static public void setAllMidiParametersEnabled(boolean b, MidiMix mm)
    {
        for (Integer channel : mm.getUsedChannels())
        {
            InstrumentMix insMix = mm.getInstrumentMixFromChannel(channel);
            InstrumentSettings insSettings = insMix.getSettings();
            insMix.setInstrumentEnabled(b);
            insSettings.setVolumeEnabled(b);
            insSettings.setPanoramicEnabled(b);
            insSettings.setReverbEnabled(b);
            insSettings.setChorusEnabled(b);

            // Only disable. The FixMidiAction helper will automatically propose to re-enable it upon playback if needed
            if (!b)
            {
                mm.setDrumsReroutedChannel(false, channel);
            }
        }
    }

}

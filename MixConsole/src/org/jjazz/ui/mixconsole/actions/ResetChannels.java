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
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.InstrumentSettings;
import org.jjazz.midimix.UserChannelRhythmVoiceKey;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.ui.mixconsole.MixConsole;
import static org.jjazz.ui.mixconsole.actions.Bundle.*;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.resetchannels")
@ActionRegistration(displayName = "#CTL_ResetChannels", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/Edit", position = 350)
        })
@NbBundle.Messages(
        {
            "CTL_ResetChannels=Reset channels",
            "CTL_ResetChannelsDescription=Restore the default instrument and settings for all current channels"
        })
public class ResetChannels extends AbstractAction
{

    private MidiMix songMidiMix;
    private String undoText = CTL_ResetChannels();
    private static final Logger LOGGER = Logger.getLogger(ResetChannels.class.getSimpleName());

    public ResetChannels(MidiMix context)
    {
        songMidiMix = context;
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, Bundle.CTL_ResetChannelsDescription());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.fine("actionPerformed() songMidiMix=" + songMidiMix);
        MixConsole mixConsole = MixConsoleTopComponent.getInstance().getEditor();
        Rhythm rhythm = mixConsole.getVisibleRhythm();
        Song song = mixConsole.getSong();
        assert song != null;
        JJazzUndoManagerFinder.getDefault().get(song).startCEdit(undoText);
        for (Integer channel : songMidiMix.getUsedChannels())
        {
            RhythmVoice rv = songMidiMix.getKey(channel);
            if (rhythm == null || rv instanceof UserChannelRhythmVoiceKey || rhythm == rv.getContainer())
            {
                InstrumentMix insMix = new InstrumentMix(songMidiMix.getInstrumentMixFromChannel(channel));
                resetInstrument(insMix, rv);
                resetSettings(insMix.getSettings(), rv);
                songMidiMix.setInstrumentMix(channel, rv, insMix);
            }
        }
        JJazzUndoManagerFinder.getDefault().get(song).endCEdit(undoText);
        String s = (rhythm == null) ? "All channels reset" : rhythm.getName() + " channels reset";
        StatusDisplayer.getDefault().setStatusText(s);
    }

    public static void resetSettings(InstrumentSettings insSet, RhythmVoice rv)
    {
        insSet.set(rv.getPreferredInstrumentSettings());
    }

    /**
     * Reset the instrument to default value
     *
     * @param insMix The InstrumentMix containing the instrument.
     * @param rv     The key associated to the InstrumentMix
     */
    static public void resetInstrument(InstrumentMix insMix, RhythmVoice rv)
    {
        Instrument ins;
//        if (!(rv instanceof UserChannelRhythmVoiceKey))
//        {
//            ins = rv.getPreferredInstrument();
//            if (ins == null)
//            {
//                ins = JJazzSynth.getDelegate2DefaultInstrument(rv.getType());
//            }
//        } else
//        {
//            ins = JJazzSynth.getDelegate2DefaultInstrumentUser();
//        }
//        insMix.setInstrument(ins);
//        insMix.getSettings().setTransposition(0);
    }

}

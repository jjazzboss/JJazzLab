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
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.InstrumentSettings;
import org.jjazz.midimix.UserChannelRvKey;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.OutputSynthManager;
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
import org.openide.util.Exceptions;
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
        Rhythm visibleRhythm = mixConsole.getVisibleRhythm();
        Song song = mixConsole.getSong();
        assert song != null;
        JJazzUndoManagerFinder.getDefault().get(song).startCEdit(undoText);
        for (Integer channel : songMidiMix.getUsedChannels())
        {
            RhythmVoice rv = songMidiMix.getRhythmVoice(channel);
            if (visibleRhythm == null || rv instanceof UserChannelRvKey || visibleRhythm == rv.getContainer())
            {
                InstrumentMix insMix = new InstrumentMix(songMidiMix.getInstrumentMixFromChannel(channel));
                resetInstrument(insMix, rv);
                resetSettings(insMix.getSettings(), rv);
                if (rv instanceof UserChannelRvKey)
                {
                    songMidiMix.removeUserChannel();
                    try
                    {
                        songMidiMix.addUserChannel(insMix);
                    } catch (MidiUnavailableException ex)
                    {
                        // Should never happen since we removed it just before
                        Exceptions.printStackTrace(ex);
                    }
                } else
                {
                    songMidiMix.setInstrumentMix(channel, rv, insMix);
                }
                songMidiMix.setDrumsReroutedChannel(false, channel);
            }
        }
        JJazzUndoManagerFinder.getDefault().get(song).endCEdit(undoText);
        String s = (visibleRhythm == null) ? "All channels reset" : visibleRhythm.getName() + " channels reset";
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
        OutputSynth outSynth = OutputSynthManager.getInstance().getOutputSynth();
        if (!(rv instanceof UserChannelRvKey))
        {
            ins = outSynth.findInstrument(rv);

        } else
        {
            ins = outSynth.getUserInstrument();
        }
        insMix.setInstrument(ins);
        insMix.getSettings().setTransposition(0);
    }

}

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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.mixconsole.api.MixConsole;
import org.jjazz.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.jjazz.outputsynth.spi.OutputSynthManager;

@ActionID(category = "MixConsole", id = "org.jjazz.mixconsole.actions.resetchannels")
@ActionRegistration(displayName = "#CTL_ResetChannels", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/Edit", position = 350)
        })
public class ResetChannels extends AbstractAction
{

    private MidiMix songMidiMix;
    private final String undoText = ResUtil.getString(getClass(), "CTL_ResetChannels");
    private static final Logger LOGGER = Logger.getLogger(ResetChannels.class.getSimpleName());

    public ResetChannels(MidiMix context)
    {
        songMidiMix = context;
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_ResetChannelsDescription"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LOGGER.log(Level.FINE, "actionPerformed() songMidiMix={0}", songMidiMix);   
        MixConsole mixConsole = MixConsoleTopComponent.getInstance().getEditor();
        Rhythm visibleRhythm = mixConsole.getVisibleRhythm();
        Song song = mixConsole.getSong();
        assert song != null;   
        
        
        JJazzUndoManagerFinder.getDefault().get(song).startCEdit(undoText);
        
        
        for (Integer channel : songMidiMix.getUsedChannels())
        {
            RhythmVoice rv = songMidiMix.getRhythmVoice(channel);
            if (visibleRhythm == null || rv instanceof UserRhythmVoice || visibleRhythm == rv.getContainer())
            {
                InstrumentMix insMix = new InstrumentMix(songMidiMix.getInstrumentMix(channel));
                resetInstrument(insMix, rv);
                resetSettings(insMix.getSettings(), rv);
                songMidiMix.setInstrumentMix(channel, rv, insMix);
                songMidiMix.setDrumsReroutedChannel(false, channel);
            }
        }
        
        
        JJazzUndoManagerFinder.getDefault().get(song).endCEdit(undoText);
        String s = (visibleRhythm == null) ? ResUtil.getString(getClass(), "CTL_ALL_CHANNELS_RESET")
                : ResUtil.getString(getClass(), "CTL_RHYTHM_CHANNELS_RESET", visibleRhythm.getName());
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
     * @param rv The key associated to the InstrumentMix
     */
    static public void resetInstrument(InstrumentMix insMix, RhythmVoice rv)
    {
        Instrument ins;
        OutputSynth outSynth = OutputSynthManager.getDefault().getDefaultOutputSynth();
        if (!(rv instanceof UserRhythmVoice))
        {
            ins = outSynth.findInstrument(rv);

        } else
        {
            ins = outSynth.getUserSettings().getUserMelodicInstrument();
        }
        insMix.setInstrument(ins);
        insMix.setInstrumentEnabled(true);
        insMix.getSettings().setTransposition(0);
    }

}

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
import javax.swing.JMenuItem;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midimix.UserChannelRvKey;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.ui.mixconsole.api.MixConsole;
import static org.jjazz.ui.mixconsole.actions.Bundle.*;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.undomanager.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

//@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.usedefaultinstruments")
//@ActionRegistration(displayName = "#CTL_UseDefaultInstruments", lazy = false) // lazy=false needed for Presenter.Menu implementation 
//@ActionReferences(
//        {
//            @ActionReference(path = "Actions/MixConsole/MenuBar/Edit", position = 300)
//        })
@NbBundle.Messages(
        {
            "CTL_UseDefaultInstruments=Use default instruments",
            "CTL_TooltipUseDefaultInstruments=Change all channels to use the default instrument"
        })
public class UseDefaultInstruments extends AbstractAction implements Presenter.Menu // Presenter.Menu just to get the tooltip...
{

    private final String undoText = ResUtil.getString(getClass(), CTL_UseDefaultInstruments);

    private static final Logger LOGGER = Logger.getLogger(UseDefaultInstruments.class.getSimpleName());

    public UseDefaultInstruments()
    {
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, Bundle.CTL_TooltipUseDefaultInstruments());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MixConsole mixConsole = MixConsoleTopComponent.getInstance().getEditor();
        MidiMix songMidiMix = mixConsole.getMidiMix();
        if (songMidiMix == null)
        {
            return;
        }
        Song song = mixConsole.getSong();
        assert song != null;
        JJazzUndoManagerFinder.getDefault().get(song).startCEdit(undoText);
        Rhythm rhythm = mixConsole.getVisibleRhythm();
        for (Integer channel : songMidiMix.getUsedChannels())
        {
            RhythmVoice rv = songMidiMix.getRhythmVoice(channel);
            if (rhythm == null || rv instanceof UserChannelRvKey || rhythm == rv.getContainer())
            {
                InstrumentMix insMix = new InstrumentMix(songMidiMix.getInstrumentMixFromChannel(channel));
                setDefaultInstrument(insMix, rv);
                songMidiMix.setInstrumentMix(channel, rv, insMix);
            }
        }
        JJazzUndoManagerFinder.getDefault().get(song).endCEdit(undoText);        
        String s = (rhythm == null) ? "Set default instrument for all channels" : "Set default instruments for " + rhythm.getName() + " channels";
        StatusDisplayer.getDefault().setStatusText(s);
    }

    /**
     * Set the instrument of insMix to the delegate2default instrument.
     * <p>
     * Set also the transposition.
     *
     * @param insMix The InstrumentMix containing the instrument to be reset to the default instrument.
     */
    static public void setDefaultInstrument(InstrumentMix insMix, RhythmVoice rv)
    {
        Instrument ins;
        int transpose;
//        DefaultInstruments di = DefaultInstruments.getInstance();
//        if (rv instanceof UserChannelRhythmVoiceKey)
//        {
//            ins = JJazzSynth.getDelegate2DefaultInstrumentUser();
//            transpose = di.getUserInstrumentTranspose();
//        } else
//        {
//            ins = JJazzSynth.getDelegate2DefaultInstrument(rv.getType());
//            transpose = di.getTranspose(rv.getType());
//        }
//        if (ins != null)
//        {
//            insMix.setInstrument(ins);
//            insMix.getSettings().setTransposition(transpose);
//        }
    }

    @Override
    public JMenuItem getMenuPresenter()
    {
        return new JMenuItem(this);
    }
}

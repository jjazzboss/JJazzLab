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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.mixconsole.api.MixConsole;
import org.jjazz.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;

//@ActionID(category = "MixConsole", id = "org.jjazz.mixconsole.actions.reloadsongmix")
//// Need lazy=false for the tooltip to work!
//@ActionRegistration(displayName = "#CTL_ReloadSongMix", lazy = true)
//@ActionReferences(
//        {
//            @ActionReference(path = "Actions/MixConsole/MenuBar/File", position = 50)
//        })
public class ReloadSongMix extends AbstractAction
{

    private final String undoText = ResUtil.getString(getClass(), "CTL_ReloadSongMix");
    private static final Logger LOGGER = Logger.getLogger(ReloadSongMix.class.getSimpleName());

    public ReloadSongMix()
    {
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_ReloadSongMixTooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MixConsole mixConsole = MixConsoleTopComponent.getInstance().getEditor();
        MidiMix songMidiMix = mixConsole.getMidiMix();
        if (songMidiMix == null || songMidiMix.getSong() == null)
        {
            return;
        }
        Song song = songMidiMix.getSong();
        File songMixFile = MidiMix.getSongMixFile(song.getFile());
        if (songMixFile == null)
        {
            String msg = "Song mix file is not created yet.";
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        } else
        {
            MidiMix mm = ImportMix.loadMixFile(songMixFile);
            if (mm != null)
            {
                songMidiMix.importInstrumentMixes(mm);
                StatusDisplayer.getDefault().setStatusText("Reloaded song mix: " + songMixFile.getAbsolutePath());
            }
        }
    }
}

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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midimix.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.ui.mixconsole.MixConsole;
import static org.jjazz.ui.mixconsole.actions.Bundle.*;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;

//@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.reloadsongmix")
//// Need lazy=false for the tooltip to work!
//@ActionRegistration(displayName = "#CTL_ReloadSongMix", lazy = true)
//@ActionReferences(
//        {
//            @ActionReference(path = "Actions/MixConsole/MenuBar/File", position = 50)
//        })
@NbBundle.Messages(
        {
            "CTL_ReloadSongMix=Reload song mix",
            "CTL_ReloadSongMixTooltip=Reload the saved song mix"
        })
public class ReloadSongMix extends AbstractAction
{

    private String undoText = CTL_ReloadSongMix();
    private static final Logger LOGGER = Logger.getLogger(ReloadSongMix.class.getSimpleName());

    public ReloadSongMix()
    {
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, Bundle.CTL_ReloadSongMixTooltip());
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
        File songMixFile = FileDirectoryManager.getInstance().getSongMixFile(song.getFile());
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

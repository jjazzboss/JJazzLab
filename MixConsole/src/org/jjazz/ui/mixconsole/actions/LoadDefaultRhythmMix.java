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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midimix.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongStructure;
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

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.loadrhythmdefaultmix")
// Need lazy=false for the tooltip to work!
@ActionRegistration(displayName = "#CTL_LoadDefaultRhythmMix", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/File", position = 150)
        })
@NbBundle.Messages(
        {
            "CTL_LoadDefaultRhythmMix=Load default rhythm mix",
            "CTL_LoadDefaultRhythmMixTooltip=Load the default mix for the current rhythm"
        })
public class LoadDefaultRhythmMix extends AbstractAction
{

    private String undoText = CTL_LoadDefaultRhythmMix();
    private static final Logger LOGGER = Logger.getLogger(LoadDefaultRhythmMix.class.getSimpleName());

    public LoadDefaultRhythmMix()
    {
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, Bundle.CTL_LoadDefaultRhythmMixTooltip());
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
        Rhythm rhythm = mixConsole.getVisibleRhythm();
        List<Rhythm> rhythms = new ArrayList<>();
        if (rhythm == null)
        {
            rhythms.addAll(SongStructure.getUniqueRhythms(song.getSongStructure()));
        } else
        {
            rhythms.add(rhythm);
        }
        if (rhythms.isEmpty())
        {
            // Can happen if song structure empty
            return;
        }
        assert !rhythms.isEmpty() : "song.getSongStructure()=" + song.getSongStructure() + " rhythm=" + rhythm;
        String loadedFiles = "";
        for (Rhythm r : rhythms)
        {
            File f = FileDirectoryManager.getInstance().getRhythmMixFile(r);
            if (!f.exists())
            {
                String msg = "No default mix file found for rhythm " + r.getName() + ".\n\nUse '" + CTL_SaveRhythmDefaultMix() + "' to create one.";
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                continue;
            }
            MidiMix rMix = ImportMix.loadMixFile(f);
            if (rMix != null)
            {
                songMidiMix.importInstrumentMixes(rMix);
                loadedFiles += f.getAbsolutePath() + " ";
            }
        }
        if (!loadedFiles.isEmpty())
        {
            StatusDisplayer.getDefault().setStatusText("Restored rhythm default mix: " + loadedFiles);
        }
    }

}

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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midimix.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.jjazz.ui.mixconsole.api.MixConsole;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.util.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.saverhythmdefaultmix")
// Need lazy=false for the tooltip to work!
@ActionRegistration(displayName = "#CTL_SaveRhythmDefaultMix", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/File", position = 100)
        })
public class SaveRhythmDefaultMix extends AbstractAction
{

    private final String undoText = ResUtil.getString(getClass(), "CTL_SaveRhythmDefaultMix");
    private static final Logger LOGGER = Logger.getLogger(SaveRhythmDefaultMix.class.getSimpleName());

    public SaveRhythmDefaultMix()
    {
        putValue(NAME, undoText);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_SaveRhythmDefaultMixTooltip"));
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

        // Save only the visible rhythms
        Song song = songMidiMix.getSong();
        List<Rhythm> songRhythms = song.getSongStructure().getUniqueRhythms(true);
        if (songRhythms.isEmpty())
        {
            // Can happen is songstructure is empty
            return;
        }
        Rhythm rhythm = mixConsole.getVisibleRhythm();
        List<Rhythm> savedRhythms = new ArrayList<>();
        if (rhythm == null)
        {
            savedRhythms.addAll(songRhythms);
        } else
        {
            savedRhythms.add(rhythm);
        }

        assert !savedRhythms.isEmpty() : "song.getSongStructure()=" + song.getSongStructure() + " rhythm=" + rhythm;   //NOI18N

        String savedFiles = "";         // Used to show a message

        // Save each rhythm
        for (Rhythm r : savedRhythms)
        {
            File f = FileDirectoryManager.getInstance().getRhythmMixFile(r);
            MidiMix rhythmMix = new MidiMix();
            try
            {
                rhythmMix.addInstrumentMixes(songMidiMix, r);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.log(Level.SEVERE, "MidiMix(MidiMix, Rhythm unexpected exception!", ex);   //NOI18N
            }
            if (rhythmMix.saveToFileNotify(f, true))
            {
                savedFiles += f.getAbsolutePath() + " ";
            }
        }
        if (!savedFiles.isEmpty())
        {
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "SAVED RHYTHM DEFAULT MIX", savedFiles));
        }
    }

    // =================================================================
    // Private methods
    // =================================================================
}

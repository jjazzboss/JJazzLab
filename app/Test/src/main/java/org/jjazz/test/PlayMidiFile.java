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
package org.jjazz.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequencer;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * Play Midi file using default JJazzLab Midi configuration.
 */
@ActionID(category = "JJazz", id = "org.jjazz.test.playmidifile")
@ActionRegistration(displayName = "Play Midi File", lazy = true)
@ActionReferences(
        {
            // @ActionReference(path = "Menu/Edit", position = 37893),
            // @ActionReference(path = "Shortcuts", name = "SD-P")    // ctrl-shift-P
        })
public final class PlayMidiFile implements ActionListener, MetaEventListener
{

    private static final Logger LOGGER = Logger.getLogger(PlayMidiFile.class.getSimpleName());
    private static File lastFile = null;
    private Sequencer sequencer;

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.log(Level.INFO, "actionPerformed()");   
        JFileChooser chooser = UIUtilities.getFileChooserInstance();
        chooser.setDialogTitle("Play midi file");                
        chooser.resetChoosableFileFilters();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (lastFile != null)
        {
            chooser.setSelectedFile(lastFile);
        }
        int res = chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
        if (res != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        lastFile = chooser.getSelectedFile();

        try
        {
            var mc = MusicController.getInstance();
            sequencer = mc.acquireSequencer(this);
            if (sequencer == null)
            {
                LOGGER.severe("actionPerformed() can't acquire sequencer");   
                return;
            }
            sequencer.addMetaEventListener(this);
            InputStream is = new BufferedInputStream(new FileInputStream(lastFile));
            sequencer.setSequence(is);
            sequencer.start();
        } catch (IOException | InvalidMidiDataException ex)
        {
            Exceptions.printStackTrace(ex);
        }

    }

    @Override
    public void meta(MetaMessage meta)
    {
        if (meta.getType() == 47) // Meta Event for end of sequence
        {
            // This method  is called from the Sequencer thread, NOT from the EDT !
            // So if this method impacts the UI, it must use SwingUtilities.InvokeLater() (or InvokeAndWait())
            LOGGER.info("Play Midi file : END OF PLAYBACK");   
            Runnable doRun = new Runnable()
            {
                @Override
                public void run()
                {
                    sequencer.stop();
                    sequencer.removeMetaEventListener(PlayMidiFile.this);
                    var mc = MusicController.getInstance();
                    mc.releaseSequencer(PlayMidiFile.this);
                }
            };
            SwingUtilities.invokeLater(doRun);
        }
    }
}

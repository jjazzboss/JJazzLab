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
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import static org.jjazz.ui.mixconsole.actions.Bundle.*;
import org.jjazz.ui.utilities.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.importmix")
@ActionRegistration(displayName = "#CTL_ImportMix", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/File", position = 200)
        })
@NbBundle.Messages(
        {
            qsdqsd
            "CTL_ImportMix=Import Mix...",
            "ERR_CouldNotImportFile=Could not import file : ",
            "ERR_FileDoesNotExist=File does not exist : ",
            "ERR_CantReadFile=Can not read file : ",
            "CTL_FileImported=Mix imported from : "
        })
public class ImportMix extends AbstractAction
{

    private MidiMix songMidiMix;
    private final String undoText = ResUtil.getString(getClass(), CTL_ImportMix);
    private static final Logger LOGGER = Logger.getLogger(ImportMix.class.getSimpleName());

    public ImportMix(MidiMix context)
    {
        songMidiMix = context;
        putValue(NAME, undoText);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (songMidiMix.getInstrumentMixes().isEmpty())
        {
            // Can happen if song structure is empty
            return;
        }
        File mixFile = showLoadDialog();
        if (mixFile == null)
        {
            return;
        }
        MidiMix mm = loadMixFile(mixFile);
        if (mm != null)
        {
            songMidiMix.importInstrumentMixes(mm);
            StatusDisplayer.getDefault().setStatusText(CTL_FileImported() + mixFile.getAbsolutePath());
        }
    }

    /**
     * Load a mix file from specified file.
     *
     * @param file
     * @return Null if problem loading file.
     */
    static protected MidiMix loadMixFile(File file)
    {
        if (!file.exists())
        {
            String msg = ERR_FileDoesNotExist() + file.getAbsolutePath();
            LOGGER.log(Level.WARNING, msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return null;
        }
        if (!file.canRead())
        {
            String msg = ERR_CantReadFile() + file.getAbsolutePath();
            LOGGER.log(Level.WARNING, msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return null;
        }
        MidiMix mm = null;
        try
        {
            mm = MidiMix.loadFromFile(file);
        } catch (IOException ex)
        {
            String msg = "Problem loading mix file " + file.getAbsolutePath() + " : " + ex.getLocalizedMessage();
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return null;
        }
        return mm;
    }

    static protected File showLoadDialog()
    {
        JFileChooser chooser = Utilities.getFileChooserInstance();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(CTL_MixFiles() + " (" + "." + FileDirectoryManager.MIX_FILE_EXTENSION + ")", FileDirectoryManager.MIX_FILE_EXTENSION);
        chooser.resetChoosableFileFilters();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Load mix from file");        
        chooser.setSelectedFile(new File(""));

        int returnCode = chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());

        File mixFile = null;
        if (returnCode == JFileChooser.APPROVE_OPTION)
        {
            mixFile = chooser.getSelectedFile();
        }
        return mixFile;
    }

}

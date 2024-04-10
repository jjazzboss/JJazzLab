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
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.analytics.api.Analytics;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.windows.WindowManager;

@ActionID(category = "MixConsole", id = "org.jjazz.mixconsole.actions.importmix")
@ActionRegistration(displayName = "#CTL_ImportMix", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/File", position = 200)
        })
public class ImportMix extends AbstractAction
{

    private MidiMix songMidiMix;
    private final String undoText = ResUtil.getString(getClass(), "CTL_ImportMix");
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
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_FileImported", mixFile.getAbsolutePath()));
        }
        
        Analytics.logEvent("Import Mix");
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
            String msg = ResUtil.getString(ImportMix.class, "ERR_FileDoesNotExist", file.getAbsolutePath());
            LOGGER.log(Level.WARNING, msg);   
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return null;
        }
        if (!file.canRead())
        {
            String msg = ResUtil.getString(ImportMix.class, "ERR_CantReadFile", file.getAbsolutePath());
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
            String msg = ResUtil.getString(ImportMix.class, "ERR_InvalidMixFile", file.getAbsolutePath()) + " - " + ex.getLocalizedMessage();
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return null;
        }
        return mm;
    }

    static protected File showLoadDialog()
    {
        JFileChooser chooser = UIUtilities.getFileChooserInstance();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(ResUtil.getString(ImportMix.class, "CTL_MixFiles") + " (" + "." + MidiMix.MIX_FILE_EXTENSION + ")", MidiMix.MIX_FILE_EXTENSION);
        chooser.resetChoosableFileFilters();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle(ResUtil.getString(ImportMix.class, "CTL_LoadMixDialogTitle"));
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

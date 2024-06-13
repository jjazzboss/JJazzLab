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
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

//@ActionID(category = "MixConsole", id = "org.jjazz.mixconsole.actions.exportmix")
//@ActionRegistration(displayName = "#CTL_ExportMix", lazy = true)
//@ActionReferences(
//        {
//            // @ActionReference(path = "Actions/MixConsole/File", position = 300, separatorBefore = 290)
//        })

public class ExportMix extends AbstractAction
{

    private MidiMix songMidiMix;
    private final String undoText = ResUtil.getString(getClass(), "CTL_ExportMix");
    private static final Logger LOGGER = Logger.getLogger(ExportMix.class.getSimpleName());

    public ExportMix(MidiMix context)
    {
        songMidiMix = context;
        putValue(NAME, undoText);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        File copyFile = showSaveMixFileChooser(null);
        if (copyFile == null)
        {
            return;
        }
        songMidiMix.saveToFileNotify(copyFile, true);

    }

    /**
     * Prepare the JFileChooser to save specified file.
     * <p>
     * Ask for confirmation if file overwrite. Add extension to selected file if required.
     *
     * @param presetFile If null JFileChooser is not preset.
     * @return The actual save file selected by user. Null if cancelled.
     */
    static protected File showSaveMixFileChooser(File presetFile)
    {
        JFileChooser chooser = UIUtilities.getFileChooserInstance();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(ResUtil.getString(ExportMix.class, "CTL_MixFiles") + " (" + "." + MidiMix.MIX_FILE_EXTENSION + ")", MidiMix.MIX_FILE_EXTENSION);
        chooser.resetChoosableFileFilters();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle(ResUtil.getString(ExportMix.class, "CTL_SaveMixFile"));
        chooser.setFileFilter(filter);

        if (presetFile != null)
        {
            chooser.setCurrentDirectory(presetFile.getParentFile()); // required because if defaultSongFile does not yet exist, setSelectedFile does not set the current directory
        }
        chooser.setSelectedFile(presetFile);

        // Show the dialog !
        int returnCode = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        File mixFile = null;
        if (returnCode == JFileChooser.APPROVE_OPTION)
        {
            mixFile = chooser.getSelectedFile();
            String mixFileName = mixFile.getName();

            // Add extension if required
            if (!org.jjazz.utilities.api.Utilities.endsWithIgnoreCase(mixFileName, "." + MidiMix.MIX_FILE_EXTENSION))
            {
                mixFile = new File(mixFile.getParent(), mixFileName + "." + MidiMix.MIX_FILE_EXTENSION);
            }

            if (mixFile.exists())
            {
                // Confirm overwrite
                NotifyDescriptor nd = new NotifyDescriptor.Confirmation(mixFile.getName() + " - " + ResUtil.getString(ExportMix.class, "CTL_ConfirmFileReplace"), NotifyDescriptor.OK_CANCEL_OPTION);
                Object result = DialogDisplayer.getDefault().notify(nd);
                if (result != NotifyDescriptor.OK_OPTION)
                {
                    // Cancel
                    mixFile = null;
                }
            }
        }
        return mixFile;
    }

}

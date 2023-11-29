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
package org.jjazz.rhythmdatabase.api;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase.RpRhythmPair;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.jjazz.utilities.api.MultipleErrorsReportDialog;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

/**
 * Ac action to let users add rhythms, permanently of for the current session.
 */
public class AddRhythmsAction extends AbstractAction
{

    private static JFileChooser FILE_CHOOSER;
    private static AccessoryComponent accessoryComponent;
    private RpRhythmPair lastRpRhythmPair;


    private static final Logger LOGGER = Logger.getLogger(AddRhythmsAction.class.getSimpleName());


    public AddRhythmsAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_AddRhythms"));
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "HINT_AddRhythms"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();


        // Show filechooser
        JFileChooser chooser = getFileChooser();
        if (chooser == null || chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        var rhythmFiles = chooser.getSelectedFiles();


        // Log
        List<String> rhythmFileNames = Arrays.asList(rhythmFiles).stream().limit(10).map(f -> f.getName()).collect(Collectors.toList());
        Analytics.logEvent("Add Rhythms", Analytics.buildMap("Files", rhythmFileNames));


        // Process files
        MultipleErrorsReport errRpt = new MultipleErrorsReport();
        final List<RhythmDatabase.RpRhythmPair> pairs = new ArrayList<>();
        HashSet<TimeSignature> timeSigs = new HashSet<>();
        for (File f : rhythmFiles)
        {
            String ext = org.jjazz.utilities.api.Utilities.getExtension(f.getName()).toLowerCase();
            Rhythm r;
            for (RhythmProvider rp : rdb.getRhythmProviders())
            {
                if (Arrays.asList(rp.getSupportedFileExtensions()).contains(ext))
                {
                    try
                    {
                        r = rp.readFast(f);
                    } catch (IOException ex)
                    {
                        LOGGER.log(Level.WARNING, "btn_addRhythmsActionPerformed() ex={0}", ex);
                        errRpt.individualErrorMessages.add(ex.getLocalizedMessage());
                        continue;
                    }
                    pairs.add(new RhythmDatabase.RpRhythmPair(rp, r));
                    timeSigs.add(r.getTimeSignature());
                }
            }
        }


        // Notify possible errors to end-user
        if (!errRpt.individualErrorMessages.isEmpty())
        {
            errRpt.primaryErrorMessage = ResUtil.getString(getClass(), "ERR_RhythmFilesCouldNotBeRead",
                    errRpt.individualErrorMessages.size());
            errRpt.secondaryErrorMessage = "";
            MultipleErrorsReportDialog dlg = new MultipleErrorsReportDialog(WindowManager.getDefault().getMainWindow(), ResUtil.getString(
                    getClass(), "CTL_RhythmCreationErrors"), errRpt);
            dlg.setVisible(true);
        }


        lastRpRhythmPair = null;
        if (!pairs.isEmpty())
        {
            // Add to the rhythmdatabase
            int nbActuallyAdded = rdb.addExtraRhythms(pairs);   // This will update the rhythmTable on a task put on the EDT
            int nbAlreadyAdded = pairs.size() - nbActuallyAdded;


            // Notify user 
            String msg = ResUtil.getString(getClass(), "ProcessedFiles", pairs.size(), timeSigs);
            msg += ResUtil.getString(getClass(), "NewRhythmsAdded", nbActuallyAdded);
            msg += ResUtil.getString(getClass(), "PreExistingRhythmsSkipped", nbAlreadyAdded);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(d);

            lastRpRhythmPair = pairs.get(0);


            // Do we have to add rhythm files permanently ?
            if (!accessoryComponent.cb_SessionOnly.isSelected())
            {
                Path userRhythmDir;
                try
                {
                    userRhythmDir = FileDirectoryManager.getInstance().getUserRhythmDirectory().getCanonicalFile().toPath();        // throws IOException

                    for (var rprp : pairs)
                    {
                        Path src = rprp.r.getFile().getCanonicalFile().toPath();           // throws IOException
                        if (!src.startsWith(userRhythmDir))
                        {
                            // Copy the file into the user rhythm dir.
                            Path dest = userRhythmDir.resolve(src.getFileName());
                            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);             // throws various exceptions
                            LOGGER.info("actionPerformed() copied " + src.toString() + " to " + dest.toString());
                        }
                    }
                } catch (IOException | SecurityException ex)
                {
                    LOGGER.warning("actionPerformed() " + ex.getMessage());
                    NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                }

                rdb.forceRescan(false);
            }
        }
    }

    /**
     * Get the last RpRhythmPair.
     *
     * @return Can be null
     */
    public RpRhythmPair getLastRpRhythmPair()
    {
        return lastRpRhythmPair;
    }


    //==========================================================================================
    // Private methods
    //==========================================================================================    
    private JFileChooser getFileChooser()
    {
        if (FILE_CHOOSER != null)
        {
            return FILE_CHOOSER;
        }

        // Prepare FileChooser
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        FILE_CHOOSER = new JFileChooser();
        FILE_CHOOSER.resetChoosableFileFilters();
        FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FILE_CHOOSER.setAcceptAllFileFilterUsed(false);
        FILE_CHOOSER.setMultiSelectionEnabled(true);
        FILE_CHOOSER.setCurrentDirectory(null);
        FILE_CHOOSER.setDialogTitle(ResUtil.getString(getClass(), "CTL_AddRhythms"));


        // Add the accessory component
        accessoryComponent = new AccessoryComponent();
        FILE_CHOOSER.setAccessory(accessoryComponent);


        // Prepare the FileNameFilter
        StringBuilder sb = new StringBuilder();
        List<String> allExts = new ArrayList<>();
        List<RhythmProvider> rps = rdb.getRhythmProviders();
        for (RhythmProvider rp : rps)
        {
            for (String ext : rp.getSupportedFileExtensions())
            {
                allExts.add(ext);
                if (sb.length() != 0)
                {
                    sb.append(",");
                }
                sb.append(".").append(ext);
            }
        }
        if (allExts.isEmpty())
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ResUtil.getString(getClass(), "ERR_NoRhythmProviderFound."),
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            FILE_CHOOSER = null;
            return null;
        }
        FILE_CHOOSER.addChoosableFileFilter(
                new FileNameExtensionFilter(
                        ResUtil.getString(getClass(), "RhythmFiles", sb.toString()),
                        allExts.toArray(String[]::new)));


        return FILE_CHOOSER;
    }

    /**
     * An accessory component to the FileChooser to indicate that rhythms should be added for this session only.
     */
    private static class AccessoryComponent extends JPanel
    {

        private JCheckBox cb_SessionOnly = new JCheckBox(ResUtil.getString(getClass(), "CurrentSessionOnly"));

        public AccessoryComponent()
        {
            add(cb_SessionOnly);
        }
    }

}

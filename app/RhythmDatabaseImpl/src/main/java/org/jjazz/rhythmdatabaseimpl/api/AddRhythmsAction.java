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
package org.jjazz.rhythmdatabaseimpl.api;

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
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.jjazz.coreuicomponents.api.MultipleErrorsReportDialog;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.windows.WindowManager;

/**
 * An action to let users add rhythms, permanently of for the current session.
 */
public class AddRhythmsAction extends AbstractAction
{

    private record RhythmEntry(RhythmProvider rp, Rhythm r)
            {

    }
    private static JFileChooser FILE_CHOOSER;
    private static AccessoryComponent accessoryComponent;
    private RhythmInfo lastRhythmInfoAdded;


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
        final List<RhythmEntry> rhythmEntries = new ArrayList<>();
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
                    rhythmEntries.add(new RhythmEntry(rp, r));
                    timeSigs.add(r.getTimeSignature());
                }
            }
        }


        // Notify possible errors to end-user
        if (!errRpt.individualErrorMessages.isEmpty())
        {
            errRpt.primaryErrorMessage = ResUtil.getString(getClass(), "ERR_RhythmFilesCouldNotBeRead", errRpt.individualErrorMessages.size());
            errRpt.secondaryErrorMessage = "";
            MultipleErrorsReportDialog dlg = new MultipleErrorsReportDialog(ResUtil.getString(getClass(), "CTL_RhythmCreationErrors"), errRpt);
            dlg.setVisible(true);
        }


        lastRhythmInfoAdded = null;

        if (!rhythmEntries.isEmpty())
        {
            int nbActuallyAdded = 0;
            // Add to the rhythmdatabase
            Rhythm rLast = null;
            for (var entry : rhythmEntries)
            {
                rLast = entry.r();
                if (rdb.addRhythmInstance(entry.rp(), rLast))               // This will update the rhythmTable on a task put on the EDT
                {
                    nbActuallyAdded++;
                }
            }
            assert rLast != null;
            lastRhythmInfoAdded = rdb.getRhythm(rLast.getUniqueId());
            int nbAlreadyAdded = rhythmEntries.size() - nbActuallyAdded;


            // Notify user 
            String msg = ResUtil.getString(getClass(), "ProcessedFiles", rhythmEntries.size(), timeSigs);
            msg += ResUtil.getString(getClass(), "NewRhythmsAdded", nbActuallyAdded);
            msg += ResUtil.getString(getClass(), "PreExistingRhythmsSkipped", nbAlreadyAdded);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(d);


            // Do we have to add rhythm files permanently ?
            if (!accessoryComponent.cb_SessionOnly.isSelected())
            {
                Path userRhythmDir;
                try
                {
                    userRhythmDir = RhythmDirsLocator.getDefault().getUserRhythmsDirectory().getCanonicalFile().toPath();        // throws IOException

                    for (var rprp : rhythmEntries)
                    {
                        Path src = rprp.r().getFile().getCanonicalFile().toPath();           // throws IOException
                        if (!src.startsWith(userRhythmDir))
                        {
                            // Copy the file into the user rhythm dir.
                            Path dest = userRhythmDir.resolve(src.getFileName());
                            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);             // throws various exceptions
                            LOGGER.log(Level.INFO, "actionPerformed() copied {0} to {1}", new Object[]
                            {
                                src.toString(), dest.toString()
                            });
                        }
                    }
                } catch (IOException | SecurityException ex)
                {
                    LOGGER.log(Level.WARNING, "actionPerformed() {0}", ex.getMessage());
                    NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                }

                RhythmDatabaseFactoryImpl.getInstance().markForStartupRescan(true);
            }
        }
    }

    /**
     * Get the last added Rhythm.
     *
     * @return Can be null
     */
    public RhythmInfo getLastRhythmAdded()
    {
        return lastRhythmInfoAdded;
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
            NotifyDescriptor d = new NotifyDescriptor.Message(ResUtil.getString(getClass(), "ERR_NoRhythmProviderFound"),
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
            cb_SessionOnly.setToolTipText(ResUtil.getString(getClass(), "CurrentSessionOnlyTooltip"));
            add(cb_SessionOnly);
        }
    }

}

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
package org.jjazz.rhythm.database.api;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.AddRhythmInfoDialog;
import org.jjazz.rhythm.database.api.RhythmDatabase.RpRhythmPair;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.util.api.MultipleErrorsReport;
import org.jjazz.util.api.MultipleErrorsReportDialog;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * An action to add rhythms for the current session only.
 */
public class AddSessionOnlyRhythms extends AbstractAction
{

    private static final String PREF_HIDE_ADD_RHYTHM_INFO_DIALOG = "HideAddRhythmInfoDialog";
    private static File lastRhythmDir;
    private RpRhythmPair lastRpRhythmPair;


    private static final Preferences prefs = NbPreferences.forModule(AddSessionOnlyRhythms.class);
    private static final Logger LOGGER = Logger.getLogger(AddSessionOnlyRhythms.class.getSimpleName());


    public AddSessionOnlyRhythms()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_AddRhythms"));
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "HINT_AddRhythms"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();


        // Show notification first time
        if (!prefs.getBoolean(PREF_HIDE_ADD_RHYTHM_INFO_DIALOG, false))
        {
            AddRhythmInfoDialog dlg = new AddRhythmInfoDialog(WindowManager.getDefault().getMainWindow(), true);
            dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
            dlg.setVisible(true);
            prefs.putBoolean(PREF_HIDE_ADD_RHYTHM_INFO_DIALOG, dlg.isDoNotShowAnymmore());
        }


        // Prepare FileChooser
        JFileChooser chooser = Utilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(true);
        chooser.setCurrentDirectory(lastRhythmDir);
        chooser.setDialogTitle(ResUtil.getString(getClass(), "CTL_AddRhythms"));
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
            return;
        }
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter(
                        ResUtil.getString(getClass(), "RhythmFiles", sb.toString()),
                        allExts.toArray(String[]::new)));


        // Show filechooser
        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) != JFileChooser.APPROVE_OPTION)
        {
            // User cancelled
            return;
        }


        List<String> rhythmFiles = Arrays.asList(chooser.getSelectedFiles()).stream().limit(10).map(f -> f.getName()).collect(
                Collectors.toList());
        Analytics.logEvent("Add Rhythms", Analytics.buildMap("Files", rhythmFiles));


        // Process files
        MultipleErrorsReport errRpt = new MultipleErrorsReport();
        final List<RhythmDatabase.RpRhythmPair> pairs = new ArrayList<>();
        HashSet<TimeSignature> timeSigs = new HashSet<>();
        for (File f : chooser.getSelectedFiles())
        {
            lastRhythmDir = f.getParentFile();
            String ext = org.jjazz.util.api.Utilities.getExtension(f.getName()).toLowerCase();
            Rhythm r;
            for (RhythmProvider rp : rps)
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


        // Notify end-user of errors
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
            int nbActuallyAdded = rdb.addExtraRhythms(pairs);  // This will update the rhythmTable on a task put on the EDT
            int nbAlreadyAdded = pairs.size() - nbActuallyAdded;


            // Notify user 
            String msg = ResUtil.getString(getClass(), "ProcessedFiles", pairs.size(), timeSigs);
            msg += ResUtil.getString(getClass(), "NewRhythmsAdded", nbActuallyAdded);
            msg += ResUtil.getString(getClass(), "PreExistingRhythmsSkipped", nbAlreadyAdded);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(d);

            lastRpRhythmPair = pairs.get(0);
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

}

/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.base;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.startup.spi.OnShowingTask;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;

/**
 * Copy the default Midi files in the appropriate application config directory upon startup.
 * <p>
 * Could be an UpgradeTask since it should be executed only upon fresh start. But we use an OnShowingTask because a user dialog might be used.
 */
@ServiceProvider(service = OnShowingTask.class)
public class CopyMidiSynthsOnShowingTask implements OnShowingTask
{

    /**
     * Must be before RhythmDatase initialization.
     */
    public static final int ON_SHOWING_TASK_PRIORITY = 100;
    @StaticResource(relative = true)
    public static final String ZIP_RESOURCE_PATH = "resources/MidiSynthFiles.zip";
    private static final Logger LOGGER = Logger.getLogger(CopyMidiSynthsOnShowingTask.class.getSimpleName());

    @Override
    public void run()
    {
        if (UpgradeManager.getInstance().isFreshStart())
        {
            initializeDir();
        }
    }

    @Override
    public int getPriority()
    {
        return ON_SHOWING_TASK_PRIORITY;
    }

    @Override
    public String getName()
    {
        return "Copy default Midi synth definition files";
    }

    private void initializeDir()
    {
        File dir = FileDirectoryManager.getInstance().getAppConfigDirForSynths();
        if (dir == null || !dir.isDirectory())
        {
            LOGGER.log(Level.WARNING, "initializeDir() Could not access directory {0}.", dir);
        } else
        {
            // Copy files
            copyFilesOrNot(dir);
        }
    }

    /**
     * If dir is not empty ask user confirmation to replace files.
     * <p>
     * Normally dir will be empty for a real fresh start. But if user deleted its user settings and has changed some Midi synth definition file, better to ask
     * him if it's OK to copy the files over.
     *
     * @param dir Must exist.
     */
    private void copyFilesOrNot(File dir)
    {
        boolean isEmpty;
        try
        {
            isEmpty = Utilities.isEmpty(dir.toPath());
        } catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, "copyFilesOrNot() Can''t check if dir. is empty. ex={0}", ex.getMessage());
            return;
        }
        if (!isEmpty)
        {
            String msg = ResUtil.getString(getClass(), "MidiSynthFilesOverwriteConfirmation", dir.getAbsolutePath());
            String[] options = new String[]
            {
                "OK", ResUtil.getString(getClass(), "SKIP")
            };
            NotifyDescriptor d = new NotifyDescriptor(msg, ResUtil.getString(getClass(), "CTL_FirstTimeInit"), 0, NotifyDescriptor.QUESTION_MESSAGE,
                    options, "OK");
            Object result = DialogDisplayer.getDefault().notify(d);
            if (!result.equals("OK"))
            {
                return;
            }
        }
        // Copy the default rhythms
        List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
        LOGGER.log(Level.INFO, "copyFilesOrNot() Copied {0} Midi synth definition files to {1}",
                new Object[]
                {
                    res.size(), dir.getAbsolutePath()
                });
    }

}

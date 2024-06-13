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
package org.jjazz.base;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.startup.spi.OnShowingTask;

/**
 * Copy example songs in the JJazzLab user directory/ExampleSongs upon fresh start.
 * <p>
 * Can't use @OnStart or UpgradeTask because on Linux the NotifyDialogs are hidden behind the splash screen!
 */
@ServiceProvider(service = OnShowingTask.class)
public class CopyExampleSongsOnShowingTask implements OnShowingTask
{

    public static final int ON_SHOWING_TASK_PRIORITY = 1000;

    @StaticResource(relative = true)
    public static final String ZIP_RESOURCE_PATH = "resources/ExampleSongs.zip";
    public static final String DIR_NAME = "ExampleSongs";
    private static final Logger LOGGER = Logger.getLogger(CopyExampleSongsOnShowingTask.class.getSimpleName());

    @Override
    public void run()
    {
        // If not fresh startup do nothing
        if (!UpgradeManager.getInstance().isFreshStart())
        {
            return;
        }

        // Create the dir if it does not exists
        var fdm = FileDirectoryManager.getInstance();
        File dir = new File(fdm.getJJazzLabUserDirectory(), DIR_NAME);
        if (!dir.isDirectory() && !dir.mkdir())
        {
            LOGGER.log(Level.WARNING, "run() Could not create directory {0}.", dir.getAbsolutePath());   
        } else
        {
            // Copy files
            copyFilesOrNot(dir);
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
        return "Copy example song files";
    }

    /**
     * If dir is not empty ask user confirmation to replace files.
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
            String msg = ResUtil.getString(getClass(),"CTL_CopyExampleSongsConfirmOverwrite", dir.getAbsolutePath());
            //NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
            String[] options = new String[]
            {
                "OK", ResUtil.getString(getClass(),"SKIP")
            };
            NotifyDescriptor d = new NotifyDescriptor(msg, ResUtil.getString(getClass(),"CTL_FirstTimeInit"), 0, NotifyDescriptor.QUESTION_MESSAGE, options, "OK");
            //NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
            Object result = DialogDisplayer.getDefault().notify(d);

            if (!result.equals("OK"))
            {
                return;
            }
        }

        // Copy the default rhythms
        List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
        LOGGER.log(Level.INFO, "copyFilesOrNot() Copied {0} song files to {1}", new Object[]{res.size(), dir.getAbsolutePath()});   

    }

}

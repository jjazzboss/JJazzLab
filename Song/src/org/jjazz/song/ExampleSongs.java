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
package org.jjazz.song;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.startup.spi.StartupTask;
import org.jjazz.upgrade.UpgradeManager;
import org.jjazz.util.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;

/**
 * Copy example songs in the JJazzLab user directory/ExampleSongs upon fresh start.
 * <p>
 * Can't use @OnStart or UpgradeTask because on Linux the NotifyDialogs are hidden behind the splash screen!
 */
@ServiceProvider(service = StartupTask.class)
public class ExampleSongs implements StartupTask
{

    public static final int PRIORITY = 1000;

    @StaticResource(relative = true)
    public static final String ZIP_RESOURCE_PATH = "resources/ExampleSongs.zip";
    public static final String DIR_NAME = "ExampleSongs";
    private static final Logger LOGGER = Logger.getLogger(ExampleSongs.class.getSimpleName());

    @Override
    public boolean run()
    {
        // If not fresh startup do nothing
        if (!UpgradeManager.getInstance().isFreshStart())
        {
            return false;
        }

        // Create the dir if it does not exists
        var fdm = FileDirectoryManager.getInstance();
        File dir = new File(fdm.getJJazzLabUserDirectory(), DIR_NAME);
        if (!dir.isDirectory() && !dir.mkdir())
        {
            LOGGER.warning("run() Could not create directory " + dir.getAbsolutePath() + ".");
            return false;
        } else
        {
            // Copy files
            copyFilesOrNot(dir);
            return true;
        }
    }

    @Override
    public int getPriority()
    {
        return PRIORITY;
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
            LOGGER.warning("copyFilesOrNot() Can't check if dir. is empty. ex=" + ex.getLocalizedMessage());
            return;
        }
        if (!isEmpty)
        {
            String msg = "<html><b>EXAMPLE SONG FILES</b><br/><br/>JJazzLab will copy example song files to: <i>" + dir.getAbsolutePath() + "</i><br/><br/>"
                    + "Existing example song files will be overwritten. OK to proceed?";
            //NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
            String[] options = new String[]
            {
                "OK", "Skip"
            };
            NotifyDescriptor d = new NotifyDescriptor(msg, "JJazzLab first time initialization", 0, NotifyDescriptor.QUESTION_MESSAGE, options, "OK");
            //NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);
            Object result = DialogDisplayer.getDefault().notify(d);

            if (!result.equals("OK"))
            {
                return;
            }
        }

        // Copy the default rhythms
        List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
        LOGGER.info("copyFilesOrNot() Copied " + res.size() + " song files to " + dir.getAbsolutePath());

    }

}

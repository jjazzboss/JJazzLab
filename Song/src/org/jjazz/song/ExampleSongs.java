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
import org.jjazz.upgrade.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.jjazz.util.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;

/**
 * Copy example songs in the JJazzLab user directory/ExampleSongs upon fresh start.
 */
@ServiceProvider(service = UpgradeTask.class)
public class ExampleSongs implements UpgradeTask
{

    @StaticResource(relative = true)
    public static final String ZIP_RESOURCE_PATH = "resources/ExampleSongs.zip";
    public static final String DIR_NAME = "ExampleSongs";
    private static final Logger LOGGER = Logger.getLogger(UpgradeTask.class.getSimpleName());

    @Override
    public void upgrade(String oldVersion)
    {
        // Nothing
    }

    @Override
    public void initialize()
    {
        // Create the dir if it does not exists
        var fdm = FileDirectoryManager.getInstance();
        File dir = new File(fdm.getJJazzLabUserDirectory(), DIR_NAME);
        if (!dir.isDirectory() && !dir.mkdir())
        {
            LOGGER.warning("upgrade() Could not create directory " + dir.getAbsolutePath() + ".");
        } else
        {
            // Copy files
            copyFilesOrNot(dir);
        }

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
            String msg = "Fresh start: copying example song files to " + dir.getAbsolutePath() + ".\n\n"
                    + "OK to proceed?";
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(d);
            if (NotifyDescriptor.YES_OPTION != result)
            {
                return;
            }
        }

        // Copy the default rhythms
        List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
        LOGGER.info("copyFilesOrNot() Copied " + res.size() + " song files to " + dir.getAbsolutePath());

    }

}

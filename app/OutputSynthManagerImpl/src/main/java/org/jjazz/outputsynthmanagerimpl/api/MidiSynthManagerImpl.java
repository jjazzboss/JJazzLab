/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynthmanagerimpl.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midi.api.synths.DefaultMidiSynthManager;
import static org.jjazz.midi.spi.MidiSynthManager.loadFromResource;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.midi.spi.MidiSynthManager;
import org.jjazz.startup.spi.OnShowingTask;


@ServiceProvider(service = MidiSynthManager.class)
public class MidiSynthManagerImpl extends DefaultMidiSynthManager
{

    // Some builtin MidiSynth names retrieved from a .ins file
    public static String JJAZZLAB_SOUNDFONT_GM2_SYNTH_NAME = "JJazzLab SoundFont (GM2)";
    public static String JJAZZLAB_SOUNDFONT_GS_SYNTH_NAME = "JJazzLab SoundFont (GS)";
    public static String JJAZZLAB_SOUNDFONT_XG_SYNTH_NAME = "JJazzLab SoundFont (XG)";
    public static String YAMAHA_REF_SYNTH_NAME = "Tyros5 Synth";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_GM2_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_GM2.ins";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_GS_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_GS.ins";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_XG_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_XG.ins";
    @StaticResource(relative = true)
    private final static String YAMAHA_REF_SYNTH_PATH = "resources/YamahaRefSynth.ins";
    private static final String MIDISYNTH_FILES_DEST_DIRNAME = "MidiSynthFiles";
    private static final Logger LOGGER = Logger.getLogger(MidiSynthManagerImpl.class.getSimpleName());


    public MidiSynthManagerImpl()
    {
        // Add JJazzLab specific synths
        midiSynths.add(loadFromResource(getClass(), YAMAHA_REF_SYNTH_PATH));
        midiSynths.add(loadFromResource(getClass(), JJAZZLAB_SOUNDFONT_GS_SYNTH_PATH));
        midiSynths.add(loadFromResource(getClass(), JJAZZLAB_SOUNDFONT_GM2_SYNTH_PATH));
        midiSynths.add(loadFromResource(getClass(), JJAZZLAB_SOUNDFONT_XG_SYNTH_PATH));
        LOGGER.info("MidiSynthManagerImpl() Started");

    }

    /**
     * The directory where the default MidiSynth files bundled with JJazzLab are stored.
     *
     * @return Can't be null
     */
    static public File getAppConfigDirForSynths()
    {
        File dir = FileDirectoryManager.getInstance().getAppConfigDirectory(MIDISYNTH_FILES_DEST_DIRNAME);
        if (dir == null)
        {
            throw new IllegalStateException("dir is null");
        }
        return dir;
    }


    // ===============================================================================
    // Private methods
    // ===============================================================================
    // =====================================================================================
    // Startup Task
    // =====================================================================================
    /**
     * Copy the default Midi files in the app config directory.
     * <p>
     * Could be an UpgradeTask since it should be executed only upon fresh start. But we use a StartupTask because a user dialog might be
     * used.
     */
    @ServiceProvider(service = OnShowingTask.class)
    public static class CopyMidiSynthsTask implements OnShowingTask
    {

        public static final int PRIORITY = 100;
        @StaticResource(relative = true)
        public static final String ZIP_RESOURCE_PATH = "resources/MidiSynthFiles.zip";

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
            return PRIORITY;
        }

        @Override
        public String getName()
        {
            return "Copy default Midi synth definition files";
        }

        private void initializeDir()
        {
            File dir = getAppConfigDirForSynths();
            if (!dir.isDirectory())
            {
                LOGGER.log(Level.WARNING, "CopyMidiSynthsTask.initializeDir() Could not access directory {0}.", dir.getAbsolutePath());
            }
            else
            {
                // Copy files 
                copyFilesOrNot(dir);
            }
        }

        /**
         * If dir is not empty ask user confirmation to replace files.
         * <p>
         * Normally dir will be empty for a real fresh start. But if user deleted its user settings and has changed some Midi synth
         * definition file, better to ask him if it's OK to copy the files over.
         *
         * @param dir Must exist.
         */
        private void copyFilesOrNot(File dir)
        {
            boolean isEmpty;
            try
            {
                isEmpty = Utilities.isEmpty(dir.toPath());
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "CopyMidiSynthsTask.copyFilesOrNot() Can''t check if dir. is empty. ex={0}", ex.getMessage());
                return;
            }

            if (!isEmpty)
            {
                String msg = ResUtil.getString(getClass(), "MidiSynthManager.MidiSynthFilesOverwriteConfirmation", dir.getAbsolutePath());
                String[] options = new String[]
                {
                    "OK", ResUtil.getString(getClass(), "MidiSynthManager.Skip")
                };
                NotifyDescriptor d = new NotifyDescriptor(msg, ResUtil.getString(getClass(), "MidiSynthManager.FirstTimeInit"), 0,
                    NotifyDescriptor.QUESTION_MESSAGE, options, "OK");
                Object result = DialogDisplayer.getDefault().notify(d);

                if (!result.equals("OK"))
                {
                    return;
                }
            }

            // Copy the default rhythms
            List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
            LOGGER.log(Level.INFO, "CopyMidiSynthsTask.copyFilesOrNot() Copied {0} Midi synth definition files to {1}", new Object[]
            {
                res.size(),
                dir.getAbsolutePath()
            });

        }

    }
}

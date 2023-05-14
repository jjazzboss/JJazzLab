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
package org.jjazz.yamjjazz.rhythm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.jjazz.utilities.api.ExtensionFileFilter;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.yamjjazz.FormatNotSupportedException;
import org.openide.modules.InstalledFileLocator;


/**
 * A provider of YamJJazz rhythms.
 */
@ServiceProvider(service = RhythmProvider.class)
public class YamJJazzRhythmProvider implements RhythmProvider
{

    public static final String RP_ID = "YamJJazzRhythmProviderID";
    public static final String DEFAULT_FILES_DEST_DIRNAME = "modules/YamJJazzDefaultFiles";
    public static final String FILE_EXTENSION = "yjz";
    private List<Rhythm> fileRhythms;
    private Info info;
    private ExtensionFileFilter fileFilter;
    private static final Logger LOGGER = Logger.getLogger(YamJJazzRhythmProvider.class.getSimpleName());

    public YamJJazzRhythmProvider()
    {
        info = new Info(RP_ID, "YamJJazz extended styles", "YamJJazz rhythm provider (." + FILE_EXTENSION + ")", "JL", "1");
        fileFilter = new ExtensionFileFilter(getSupportedFileExtensions());
    }

    @Override
    public final String[] getSupportedFileExtensions()
    {
        return new String[]
        {
            FILE_EXTENSION
        };
    }

    @Override
    public Info getInfo()
    {
        return info;
    }

    @Override
    public void showUserSettingsDialog()
    {
        // Nothing
    }

    @Override
    public boolean hasUserSettings()
    {
        return false;
    }

    @Override
    public List<Rhythm> getBuiltinRhythms(MultipleErrorsReport errRpt)
    {
        return Collections.emptyList();

    }

    @Override
    public List<Rhythm> getFileRhythms(boolean forceRescan, MultipleErrorsReport errRpt)
    {
        // Use cached data if possible
        if (!forceRescan && fileRhythms != null)
        {
            return new ArrayList<>(fileRhythms);
        }


        // Reset data
        fileRhythms = new ArrayList<>();


        // Get the default rhythms
        for (File f : getDefaultRhythmFiles())
        {
            Rhythm r;
            try
            {
                r = readFast(f);
            } catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "getFileRhythms() ex={0}", ex.getLocalizedMessage());
                errRpt.individualErrorMessages.add(ex.getLocalizedMessage());
                continue;
            }
            fileRhythms.add(r);
        }


        // Get the list of user rhythm files
        File rDir = FileDirectoryManager.getInstance().getUserRhythmDirectory();
        if (!rDir.isDirectory())
        {
            LOGGER.log(Level.WARNING, "getFileRhythms() RhythmProvider={0} - Rhythm file directory does not exist : {1}", new Object[]{info.getName(),
                rDir.getAbsolutePath()});
            return fileRhythms;
        }
        HashSet<Path> userRhythmPaths = Utilities.listFiles(rDir, fileFilter, PREFIX_IGNORED_SUBDIR, SUBDIR_MAX_DEPTH);
        LOGGER.log(Level.FINE, "getFileRhythms()   userRhythmPaths={0}", userRhythmPaths);


        // Read the user rhythm files
        for (Path path : userRhythmPaths)
        {
            Rhythm r;
            try
            {
                r = readFast(path.toFile());
            } catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "getFileRhythms() ex={0}", ex.getLocalizedMessage());
                errRpt.individualErrorMessages.add(ex.getLocalizedMessage());
                continue;
            }
            fileRhythms.add(r);
        }


        if (!errRpt.individualErrorMessages.isEmpty())
        {
            errRpt.primaryErrorMessage = errRpt.individualErrorMessages.size() + " rhythm files could not be read.";
            errRpt.secondaryErrorMessage = "Rhythm Provider: YamJJazz Extended";
        }


        return new ArrayList<>(fileRhythms);
    }

    /**
     * Read extFile plus the content of the associated Yamaha standard file .sty or .prs.
     *
     * @param extFile The extension file (.yjz)
     * @return Can't be null
     * @throws IOException In case of file reading problem
     */
    @Override
    public Rhythm readFast(File extFile) throws IOException
    {
        if (!extFile.exists())
        {
            throw new IOException("File " + extFile.getAbsolutePath() + " not found.");
        } else if (!extFile.getName().toLowerCase().endsWith(FILE_EXTENSION))
        {
            throw new IOException("Invalid file extension for file: " + extFile);
        }


        // Search the associated standard style file
        File baseFile = findBaseStyleFile(extFile);     // throws IOException if not found


        LOGGER.log(Level.INFO, "readFast() Reading {0} and {1}", new Object[]
        {
            extFile.getAbsolutePath(), baseFile.getName()
        });

        // Create an empty style and fill it with non music data (create StyleParts)      
        Rhythm r = null;
        try
        {
            r = new YamJJazzRhythmImpl(baseFile, extFile);           // don't call loadResources() to save time & memory
        } catch (IOException | InvalidMidiDataException | FormatNotSupportedException ex)
        {
            throw new IOException("Problem reading files baseFile=" + baseFile.getAbsolutePath() + ", extFile=" + extFile.getAbsolutePath() + ". Ex=" + ex.getLocalizedMessage());
        }
        return r;
    }

    @Override
    public AdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts)
    {
        if (r == null || ts == null || r.getTimeSignature().equals(ts))
        {
            throw new IllegalArgumentException("r=" + r + " ts=" + ts);   //NOI18N
        }
        if (RhythmDatabase.getDefault().getRhythmProvider(r) == this)
        {
            return new YamJJazzAdaptedRhythmImpl(getInfo().getUniqueId(), (YamJJazzRhythm) r, ts);
        }
        return null;
    }

    // -------------------------------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------------------------------

   
        /**
         *
         * @return The list of files present in the directory for default rhythm files.
         */
    private File[] getDefaultRhythmFiles()
    {
        File[] res;
        File dir = InstalledFileLocator.getDefault().locate(DEFAULT_FILES_DEST_DIRNAME, "org.jjazz.yamjjazz", false);

        if (dir == null || !dir.isDirectory())
        {
            LOGGER.severe("getDefaultRhythmFiles() Can't find " + DEFAULT_FILES_DEST_DIRNAME);
            res = new File[0];
        } else
        {
            res = dir.listFiles(fileFilter);
        }

        return res;
    }

   
  
    private File findBaseStyleFile(File extFile) throws IOException
    {
        for (String ext : YamahaRhythmProvider.FILE_EXTENSIONS)
        {
            String stdFilename = Utilities.replaceExtension(extFile.getAbsolutePath(), ext);
            File stdFile = new File(stdFilename);
            if (stdFile.exists())
            {
                return stdFile;
            }
        }

        // No base style found
        throw new IOException("Base style file (.sty/.prs/.sst/.bcs) not found for extension style file: " + extFile.getAbsolutePath());
    }

}

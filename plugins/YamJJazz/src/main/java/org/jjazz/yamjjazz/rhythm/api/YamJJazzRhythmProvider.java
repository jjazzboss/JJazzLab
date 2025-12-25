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
package org.jjazz.yamjjazz.rhythm.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
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
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.yamjjazz.rhythm.YamJJazzAdaptedRhythmImpl;
import org.jjazz.yamjjazz.rhythm.YamJJazzRhythmImpl;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.Lookup;

/**
 * A provider of YamJJazz rhythms.
 */
@ServiceProvider(service = RhythmProvider.class)
public class YamJJazzRhythmProvider implements RhythmProvider
{

    public static final String RP_ID = "YamJJazzRhythmProviderID";
    public static final String FILE_EXTENSION = "yjz";
    private static final String DEFAULT_FILES_SUBDIR = "YamJJazz";
    @StaticResource(relative = true)
    private static final String DEFAULT_FILES_RESOURCE_ZIP = "resources/YamJJazzDefaultFiles.zip";
    private List<Rhythm> fileRhythms;
    private final Info info;
    private final ExtensionFileFilter fileFilter;
    private static final Logger LOGGER = Logger.getLogger(YamJJazzRhythmProvider.class.getSimpleName());

    public YamJJazzRhythmProvider()
    {
        info = new Info(RP_ID, "YamJJazz extended styles", "YamJJazz rhythm provider (." + FILE_EXTENSION + ")", "JL", "1");
        fileFilter = new ExtensionFileFilter(getSupportedFileExtensions());
    }

    static public YamJJazzRhythmProvider getInstance()
    {
        return Lookup.getDefault().lookup(YamJJazzRhythmProvider.class);
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
        var defaultRhythmFiles = getDefaultRhythmFiles(forceRescan);
        for (File f : defaultRhythmFiles)
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
        File rDir = RhythmDirsLocator.getDefault().getUserRhythmsDirectory();
        if (!rDir.isDirectory())
        {
            LOGGER.log(Level.WARNING, "getFileRhythms() RhythmProvider={0} - Rhythm file directory does not exist : {1}", new Object[]
            {
                info.getName(),
                rDir.getAbsolutePath()
            });
            return fileRhythms;
        }
        Set<Path> userRhythmPaths = Utilities.listFiles(rDir, fileFilter, PREFIX_IGNORED_SUBDIR, SUBDIR_MAX_DEPTH);
        LOGGER.log(Level.FINE, "getFileRhythms()   userRhythmPaths={0}", userRhythmPaths);


        // Read the user rhythm files
        for (Path path : userRhythmPaths)
        {
            if (defaultRhythmFiles.contains(path.toFile()))
            {
                // It might happen that the user rhythm directory is the same than the default rhythm directory (e.g. JJazzLabToolkit) 
                continue;
            }
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
            throw new IOException(
                "Problem reading files baseFile=" + baseFile.getAbsolutePath() + ", extFile=" + extFile.getAbsolutePath() + ". Ex=" + ex.getLocalizedMessage());
        }
        return r;
    }

    @Override
    public YamJJazzAdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts)
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

    static public boolean isMine(RhythmInfo ri)
    {
        return ri.rhythmProviderId().equals(RP_ID);
    }

    // -------------------------------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------------------------------
    /**
     * Get the list of rhythm files (matching getFilenameFile()) present in the directory for default rhythm files.
     * <p>
     * If files are not yet present, extract them.
     *
     * @param forceCopy If true always extract the files from zip resource file
     * @return
     */
    private List<File> getDefaultRhythmFiles(boolean forceCopy)
    {
        List<File> res = new ArrayList<>();
        var ddir = RhythmDirsLocator.getDefault().getDefaultRhythmsDirectory();
        File rDir = new File(ddir, DEFAULT_FILES_SUBDIR);
        if (!rDir.isDirectory() && !rDir.mkdir())
        {
            LOGGER.log(Level.SEVERE, "getDefaultRhythmFiles() Can''t create directory {0}", rDir.getAbsolutePath());
            return res;
        }

        File[] files = rDir.listFiles(fileFilter);
        if (forceCopy || files.length == 0)
        {
            for (File f : copyDefaultResourceFiles(rDir))
            {
                if (fileFilter.accept(rDir, f.getName()))
                {
                    res.add(f);
                }
            }
        } else
        {
            res.addAll(Arrays.asList(files));
        }
        return res;
    }

    /**
     * Copy the default rhythm files within the JAR to destPath.
     * <p>
     *
     * @param destDir
     * @return The list of copied files in destDir
     */
    private List<File> copyDefaultResourceFiles(File destDir)
    {
        List<File> res = Utilities.extractZipResource(getClass(), DEFAULT_FILES_RESOURCE_ZIP, destDir.toPath(), true);
        LOGGER.log(Level.INFO, "copyDefaultResourceFiles() Copied {0} default rhythm files to {1}", new Object[]
        {
            res.size(), destDir.getAbsolutePath()
        });
        return res;
    }

    private File findBaseStyleFile(File extFile) throws IOException
    {
        for (String ext : YamahaRhythmProvider.FILE_EXTENSIONS)
        {
            File stdFile = Utilities.replaceExtension(extFile, ext);
            if (stdFile.exists())
            {
                return stdFile;
            }
        }

        // No base style found
        throw new IOException("Base style file (.sty/.prs/.sst/.bcs) not found for extension style file: " + extFile.getAbsolutePath());
    }

}
